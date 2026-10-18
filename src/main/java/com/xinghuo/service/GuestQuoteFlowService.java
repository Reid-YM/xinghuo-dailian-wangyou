package com.xinghuo.service;

import com.xinghuo.dto.ApiDtos;
import com.xinghuo.exception.BadRequestException;
import com.xinghuo.exception.ConflictException;
import com.xinghuo.exception.NotFoundException;
import com.xinghuo.model.Quote;
import com.xinghuo.payment.GuestWeChatPaymentService;
import com.xinghuo.repository.QuoteRepository;
import com.xinghuo.util.HashUtil;
import jakarta.servlet.http.HttpServletRequest;
import java.math.BigDecimal;
import java.security.SecureRandom;
import java.sql.Timestamp;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Guest quote lifecycle: browser capability -> quote -> staff offer -> accept -> order -> payment.
 *
 * <p>Guest identity is deliberately independent from customer_accounts. Browser sessions and the
 * stable business customer are also separate: an original browser and a later browser opened from
 * a one-time staff link may both access the same guest_business_access id, while order/payment
 * ownership remains stable.
 */
@Service
public class GuestQuoteFlowService {
    public static final String COOKIE_NAME = "XH_GUEST";
    private static final SecureRandom RANDOM = new SecureRandom();
    private static final Duration SESSION_TTL = Duration.ofDays(90);
    private static final Duration DEFAULT_OFFER_TTL = Duration.ofDays(3);
    private static final Duration ACCESS_LINK_TTL = Duration.ofHours(24);

    private final JdbcTemplate jdbc;
    private final QuoteRepository quotes;
    private final OrderService orders;
    private final AuditService audit;
    private final ObjectProvider<GuestWeChatPaymentService> guestPayments;

    public GuestQuoteFlowService(
            JdbcTemplate jdbc,
            QuoteRepository quotes,
            OrderService orders,
            AuditService audit,
            ObjectProvider<GuestWeChatPaymentService> guestPayments) {
        this.jdbc = jdbc;
        this.quotes = quotes;
        this.orders = orders;
        this.audit = audit;
        this.guestPayments = guestPayments;
    }

    /** Resolve an existing browser capability or create a new one. The raw token is never stored. */
    @Transactional
    public SessionHandle ensureSession(String rawToken) {
        if (usableToken(rawToken)) {
            String hash = HashUtil.sha256(rawToken);
            List<Long> ids = jdbc.query("""
                    SELECT id FROM guest_session
                     WHERE token_hash=? AND status='ACTIVE' AND expires_at>CURRENT_TIMESTAMP
                     LIMIT 1
                    """, (rs, i) -> rs.getLong(1), hash);
            if (!ids.isEmpty()) {
                Instant expiry = Instant.now().plus(SESSION_TTL);
                jdbc.update("""
                        UPDATE guest_session
                           SET last_seen_at=CURRENT_TIMESTAMP,expires_at=?,updated_at=CURRENT_TIMESTAMP
                         WHERE id=? AND status='ACTIVE'
                        """, Timestamp.from(expiry), ids.getFirst());
                return new SessionHandle(ids.getFirst(), rawToken, false, expiry);
            }
        }

        for (int attempt = 0; attempt < 8; attempt++) {
            String token = randomToken();
            Instant expiry = Instant.now().plus(SESSION_TTL);
            try {
                jdbc.update("""
                        INSERT INTO guest_session(token_hash,status,expires_at,last_seen_at,created_at,updated_at)
                        VALUES (?,'ACTIVE',?,CURRENT_TIMESTAMP,CURRENT_TIMESTAMP,CURRENT_TIMESTAMP)
                        """, HashUtil.sha256(token), Timestamp.from(expiry));
                Long id = jdbc.queryForObject(
                        "SELECT id FROM guest_session WHERE token_hash=?",
                        Long.class,
                        HashUtil.sha256(token));
                if (id == null) {
                    throw new IllegalStateException("游客会话创建失败");
                }
                return new SessionHandle(id, token, true, expiry);
            } catch (DuplicateKeyException collision) {
                // A 256-bit collision is practically impossible; retry keeps the contract total.
            }
        }
        throw new IllegalStateException("暂时无法创建游客会话，请稍后重试");
    }

    /**
     * Attach a newly created anonymous quote to the current browser session.
     *
     * <p>An idempotent replay may reuse the same quote only from a browser that already owns its
     * guest access. A different browser cannot steal an existing quote merely by replaying an
     * Idempotency-Key; cross-browser access must use a one-time staff link.
     */
    @Transactional
    public void attachQuote(long sessionId, UUID quoteId, String businessNo) {
        if (quoteId == null || businessNo == null || businessNo.isBlank()) {
            throw new IllegalArgumentException("游客询价信息不完整");
        }
        List<Long> owners = jdbc.query(
                "SELECT customer_account_id FROM quotes WHERE id=?",
                (rs, i) -> (Long) rs.getObject(1),
                quoteId.toString());
        if (owners.isEmpty()) {
            throw new NotFoundException();
        }
        if (owners.getFirst() != null) {
            return;
        }

        int created = jdbc.update("""
                INSERT IGNORE INTO guest_business_access(
                  quote_id,business_no,status,created_at,updated_at)
                VALUES (?,?,'ACTIVE',CURRENT_TIMESTAMP,CURRENT_TIMESTAMP)
                """, quoteId.toString(), businessNo.trim());
        Long accessId = jdbc.queryForObject(
                "SELECT id FROM guest_business_access WHERE quote_id=? AND business_no=?",
                Long.class,
                quoteId.toString(),
                businessNo.trim());
        if (accessId == null) {
            throw new IllegalStateException("游客业务凭证创建失败");
        }

        if (created == 0) {
            Integer alreadyOwned = jdbc.queryForObject("""
                    SELECT COUNT(*) FROM guest_business_session_access
                     WHERE guest_session_id=? AND guest_access_id=?
                    """, Integer.class, sessionId, accessId);
            if (alreadyOwned == null || alreadyOwned == 0) {
                throw new ConflictException("该询价已建立游客访问凭证，请使用原浏览器或客服安全链接查看");
            }
            return;
        }

        jdbc.update("""
                INSERT IGNORE INTO guest_business_session_access(
                  guest_session_id,guest_access_id,created_at)
                VALUES (?,?,CURRENT_TIMESTAMP)
                """, sessionId, accessId);
    }

    /** Publish a formal price to an anonymous quote. Previous active offers remain as history. */
    @Transactional
    public Map<String, Object> publishOffer(
            String businessNo,
            OfferRequest body,
            String operator,
            HttpServletRequest request) {
        String no = normalizeBusinessNo(businessNo);
        validateOffer(body);

        List<QuoteState> rows = jdbc.query("""
                SELECT q.id,q.status,q.customer_account_id,a.id AS guest_access_id
                  FROM quotes q
                  LEFT JOIN guest_business_access a
                    ON a.quote_id=q.id AND a.status='ACTIVE'
                 WHERE q.business_no=? FOR UPDATE
                """, (rs, i) -> new QuoteState(
                        rs.getString("id"),
                        rs.getString("status"),
                        (Long) rs.getObject("customer_account_id"),
                        (Long) rs.getObject("guest_access_id")), no);
        if (rows.isEmpty()) {
            throw new NotFoundException();
        }
        QuoteState quote = rows.getFirst();
        if (quote.accountId() != null) {
            throw new BadRequestException("该询价已经归属注册账号，请使用账号报价流程");
        }
        if (quote.guestAccessId() == null) {
            throw new BadRequestException("该历史询价没有游客访问凭证，不能在线发布游客报价");
        }
        if (List.of("CLOSED", "CANCELLED").contains(quote.status())) {
            throw new ConflictException("当前询价已关闭或取消，不能重新报价");
        }
        if ("CONVERTED".equals(quote.status())) {
            Integer validOrders = jdbc.queryForObject("""
                    SELECT COUNT(*) FROM customer_orders
                     WHERE source_quote_id=? AND business_no=? AND customer_account_id IS NULL
                       AND (guest_access_id IS NULL OR guest_access_id=?)
                    """, Integer.class, quote.quoteId(), no, quote.guestAccessId());
            if (validOrders == null || validOrders != 1) {
                throw new ConflictException("已转单询价缺少同主体订单，请联系客服核对订单归属");
            }
        }
        // Never reprice while a payable WeChat intent exists: its amount might already be
        // presented to the customer. Staff must close it through the payment workspace first.
        Integer activePayments = jdbc.queryForObject("""
                SELECT COUNT(*) FROM payment_order
                 WHERE business_no=? AND provider='WECHAT'
                   AND status IN ('CREATED','APPROVED','CAPTURED')
                """, Integer.class, no);
        if (activePayments != null && activePayments > 0) {
            throw new ConflictException("订单已有待支付或已支付的收款单，不能直接改价；请先在支付工作台核对处理");
        }

        jdbc.update("""
                UPDATE quote_offer
                   SET status='CANCELLED',cancelled_at=CURRENT_TIMESTAMP,updated_at=CURRENT_TIMESTAMP
                 WHERE quote_id=? AND status='QUOTED'
                """, quote.quoteId());
        Integer previous = jdbc.queryForObject(
                "SELECT COALESCE(MAX(version),0) FROM quote_offer WHERE quote_id=?",
                Integer.class,
                quote.quoteId());
        int version = (previous == null ? 0 : previous) + 1;
        Instant expiresAt = body.expiresAt() == null
                ? Instant.now().plus(DEFAULT_OFFER_TTL)
                : body.expiresAt();
        jdbc.update("""
                INSERT INTO quote_offer(
                  quote_id,business_no,version,subject,amount,currency,estimated_duration,
                  customer_message,status,quoted_by,quoted_at,expires_at,created_at,updated_at)
                VALUES (?,?,?,?,?,'CNY',?,?,'QUOTED',?,CURRENT_TIMESTAMP,?,CURRENT_TIMESTAMP,CURRENT_TIMESTAMP)
                """, quote.quoteId(), no, version, body.subject().trim(), body.amount(),
                trim(body.estimatedDuration()), trim(body.customerMessage()),
                operator == null || operator.isBlank() ? "system" : operator,
                Timestamp.from(expiresAt));
        // A manually converted quote remains CONVERTED: the existing order must not disappear
        // from the business lifecycle merely because staff publishes a price afterwards.
        if (!"CONVERTED".equals(quote.status())) {
            jdbc.update("UPDATE quotes SET status='QUOTED',updated_at=CURRENT_TIMESTAMP WHERE id=?",
                    quote.quoteId());
        }

        Map<String, Object> after = offerMap(latestOffer(no));
        audit.log(
                "GUEST_QUOTE_OFFER_PUBLISH",
                "QUOTE",
                no,
                operator == null ? "system" : operator,
                null,
                after,
                body.reason() == null || body.reason().isBlank()
                        ? "发布游客正式报价"
                        : body.reason().trim(),
                request);
        return after;
    }

    /** Create a single-use 24-hour link that can authorize another browser for this guest business. */
    @Transactional
    public AccessLink issueAccessLink(String businessNo, String operator) {
        String no = normalizeBusinessNo(businessNo);
        List<Long> accessIds = jdbc.query("""
                SELECT id FROM guest_business_access
                 WHERE business_no=? AND status='ACTIVE'
                 LIMIT 1 FOR UPDATE
                """, (rs, i) -> rs.getLong(1), no);
        if (accessIds.isEmpty()) {
            throw new BadRequestException("该询价不是可分享的游客业务");
        }
        long accessId = accessIds.getFirst();
        jdbc.update("""
                UPDATE guest_access_link
                   SET used_at=CURRENT_TIMESTAMP
                 WHERE guest_access_id=? AND used_at IS NULL
                """, accessId);

        for (int attempt = 0; attempt < 8; attempt++) {
            String token = randomToken();
            Instant expiresAt = Instant.now().plus(ACCESS_LINK_TTL);
            try {
                jdbc.update("""
                        INSERT INTO guest_access_link(
                          guest_access_id,token_hash,expires_at,used_at,created_by,created_at)
                        VALUES (?,?,?,NULL,?,CURRENT_TIMESTAMP)
                        """, accessId, HashUtil.sha256(token), Timestamp.from(expiresAt),
                        operator == null || operator.isBlank() ? "system" : operator);
                return new AccessLink("/guest/access/" + token, expiresAt);
            } catch (DuplicateKeyException collision) {
                // retry an astronomically unlikely hash collision
            }
        }
        throw new IllegalStateException("暂时无法生成游客安全链接，请稍后重试");
    }

    /** Consume a one-time link and grant the current/new browser access to the same guest business. */
    @Transactional
    public ExchangeResult exchangeAccessLink(String rawLinkToken, String currentSessionToken) {
        if (!usableToken(rawLinkToken)) {
            throw new NotFoundException();
        }
        List<LinkRow> rows = jdbc.query("""
                SELECT l.id,l.guest_access_id,a.business_no
                  FROM guest_access_link l
                  JOIN guest_business_access a ON a.id=l.guest_access_id
                 WHERE l.token_hash=?
                   AND l.used_at IS NULL
                   AND l.expires_at>CURRENT_TIMESTAMP
                   AND a.status='ACTIVE'
                 LIMIT 1 FOR UPDATE
                """, (rs, i) -> new LinkRow(
                        rs.getLong("id"),
                        rs.getLong("guest_access_id"),
                        rs.getString("business_no")),
                HashUtil.sha256(rawLinkToken));
        if (rows.isEmpty()) {
            throw new NotFoundException();
        }
        LinkRow link = rows.getFirst();
        SessionHandle session = ensureSession(currentSessionToken);
        jdbc.update("""
                INSERT IGNORE INTO guest_business_session_access(
                  guest_session_id,guest_access_id,created_at)
                VALUES (?,?,CURRENT_TIMESTAMP)
                """, session.id(), link.accessId());
        int used = jdbc.update("""
                UPDATE guest_access_link SET used_at=CURRENT_TIMESTAMP
                 WHERE id=? AND used_at IS NULL
                """, link.linkId());
        if (used != 1) {
            throw new ConflictException("游客安全链接已被使用，请让客服重新生成");
        }
        return new ExchangeResult(session, link.businessNo());
    }

    @Transactional
    public Map<String, Object> snapshot(String rawToken, String businessNo) {
        Access access = requireAccess(rawToken, normalizeBusinessNo(businessNo), false);
        return snapshot(access);
    }

    /**
     * Accept the latest quote and atomically create the order plus a guest WeChat payment row.
     * Repeating an already completed accept is idempotent and simply returns the current state.
     */
    @Transactional
    public Map<String, Object> accept(
            String rawToken,
            String businessNo,
            HttpServletRequest request) {
        String no = normalizeBusinessNo(businessNo);
        Access access = requireAccess(rawToken, no, true);

        List<Offer> offers = jdbc.query("""
                SELECT id,version,subject,amount,currency,estimated_duration,customer_message,status,
                       quoted_at,expires_at,accepted_at
                  FROM quote_offer
                 WHERE business_no=? ORDER BY id DESC LIMIT 1 FOR UPDATE
                """, this::offer, no);
        if (offers.isEmpty()) {
            throw new ConflictException("客服尚未发布正式报价");
        }
        Offer offer = offers.getFirst();
        if ("ACCEPTED".equals(offer.status())) {
            return snapshot(access);
        }
        if (!"QUOTED".equals(offer.status())) {
            throw new ConflictException("当前报价已失效，请等待客服更新报价");
        }
        if (offer.expiresAt() != null && !offer.expiresAt().isAfter(Instant.now())) {
            jdbc.update("""
                    UPDATE quote_offer SET status='EXPIRED',updated_at=CURRENT_TIMESTAMP
                     WHERE id=? AND status='QUOTED'
                    """, offer.id());
            throw new ConflictException("当前报价已过期，请联系客服重新报价");
        }

        Quote quote = quotes.findById(UUID.fromString(access.quoteId())).orElseThrow(NotFoundException::new);
        if (quote.getCustomerAccountId() != null) {
            throw new ConflictException("该询价已经绑定注册账号，请登录账号继续");
        }
        if (quote.getStatus() == Quote.Status.CLOSED || quote.getStatus() == Quote.Status.CANCELLED) {
            throw new ConflictException("该询价已经关闭");
        }

        ApiDtos.AdminOrderResponse order = orders.createFromQuote(
                quote,
                "guest:" + access.accessId(),
                request,
                "游客接受正式报价");
        int owned = jdbc.update("""
                UPDATE customer_orders
                   SET guest_access_id=?, progress_text='报价已确认，等待支付并安排服务',
                       updated_at=CURRENT_TIMESTAMP
                 WHERE business_no=? AND customer_account_id IS NULL
                   AND (guest_access_id IS NULL OR guest_access_id=?)
                """, access.accessId(), no, access.accessId());
        if (owned != 1) {
            throw new ConflictException("订单客户归属发生冲突，请联系客服核对");
        }

        jdbc.update("""
                UPDATE quote_offer
                   SET status='ACCEPTED',accepted_at=CURRENT_TIMESTAMP,updated_at=CURRENT_TIMESTAMP
                 WHERE id=? AND status='QUOTED'
                """, offer.id());
        quote.setStatus(Quote.Status.CONVERTED);
        quote.setUpdatedAt(Instant.now());
        quotes.save(quote);

        GuestWeChatPaymentService paymentService = guestPayments.getIfAvailable();
        if (paymentService != null) {
            paymentService.prepare(order.orderNo(), access.accessId(), offer.amount(), offer.subject());
        }

        audit.log(
                "GUEST_QUOTE_ACCEPT",
                "QUOTE",
                no,
                "guest:" + access.accessId(),
                offerMap(offer),
                Map.of("orderNo", order.orderNo()),
                "游客接受正式报价",
                request);
        return snapshot(access);
    }

    /** Authorize and request a Native payment code for the current guest business. */
    @Transactional
    public com.xinghuo.payment.WeChatNativePaymentService.Payment nativePayment(
            String rawToken,
            String businessNo) {
        String no = normalizeBusinessNo(businessNo);
        Access access = requireAccess(rawToken, no, false);
        GuestWeChatPaymentService payments = guestPayments.getIfAvailable();
        if (payments == null) {
            throw new BadRequestException("当前暂未开放微信在线支付");
        }
        com.xinghuo.payment.WeChatNativePaymentService.Payment payment =
                payments.latestForGuest(no, access.accessId());
        if (payment == null) {
            // Recover a payment intent when the customer accepted while WeChat pay was disabled,
            // or when staff converted the order before publishing the accepted price.
            Offer offer = latestOffer(no);
            if (offer == null || !"ACCEPTED".equals(offer.status())) {
                throw new ConflictException("请先确认客服正式报价，再生成支付二维码");
            }
            List<String> orderNumbers = jdbc.query("""
                    SELECT order_no FROM customer_orders
                     WHERE business_no=? AND source_quote_id=? AND customer_account_id IS NULL
                       AND guest_access_id=?
                     LIMIT 1
                    """, (rs, rowNum) -> rs.getString(1),
                    no, access.quoteId(), access.accessId());
            if (orderNumbers.isEmpty()) {
                throw new ConflictException("订单未绑定该游客业务，暂时无法生成支付二维码");
            }
            payment = payments.prepare(orderNumbers.getFirst(), access.accessId(),
                    offer.amount(), offer.subject());
        }
        return payments.nativeQr(payment.paymentNo(), access.accessId());
    }

    @Transactional
    public com.xinghuo.payment.WeChatNativePaymentService.Payment payment(
            String rawToken,
            String businessNo) {
        String no = normalizeBusinessNo(businessNo);
        Access access = requireAccess(rawToken, no, false);
        GuestWeChatPaymentService payments = guestPayments.getIfAvailable();
        return payments == null ? null : payments.latestForGuest(no, access.accessId());
    }

    private Map<String, Object> snapshot(Access access) {
        List<Map<String, Object>> quoteRows = jdbc.query("""
                SELECT q.business_no,q.status,q.game,q.demand,q.current_progress,q.expected_time,
                       o.order_no,o.status AS order_status,o.progress_percent,o.progress_text,
                       o.updated_at AS order_updated_at,o.finished_at AS order_finished_at
                  FROM quotes q
                  LEFT JOIN customer_orders o ON o.source_quote_id=q.id
                 WHERE q.id=? AND q.business_no=?
                 LIMIT 1
                """, (rs, i) -> {
                    Map<String, Object> row = new LinkedHashMap<>();
                    row.put("businessNo", rs.getString("business_no"));
                    row.put("quoteStatus", rs.getString("status"));
                    row.put("game", rs.getString("game"));
                    row.put("demand", rs.getString("demand"));
                    row.put("currentProgress", rs.getString("current_progress"));
                    row.put("expectedTime", rs.getString("expected_time"));
                    String orderNo = rs.getString("order_no");
                    if (orderNo != null) {
                        Map<String, Object> order = new LinkedHashMap<>();
                        order.put("orderNo", orderNo);
                        order.put("status", rs.getString("order_status"));
                        order.put("progressPercent", rs.getObject("progress_percent"));
                        order.put("progressText", rs.getString("progress_text"));
                        Timestamp updated = rs.getTimestamp("order_updated_at");
                        order.put("updatedAt", updated == null ? null : updated.toInstant());
                        Timestamp finished = rs.getTimestamp("order_finished_at");
                        order.put("finishedAt", finished == null ? null : finished.toInstant());
                        row.put("order", order);
                    } else {
                        row.put("order", null);
                    }
                    return row;
                }, access.quoteId(), access.businessNo());
        if (quoteRows.isEmpty()) {
            throw new NotFoundException();
        }
        Map<String, Object> out = quoteRows.getFirst();
        Offer offer = latestOffer(access.businessNo());
        out.put("customerType", "GUEST");
        out.put("offer", offer == null ? null : offerMap(offer));
        boolean offerActive = offer != null
                && "QUOTED".equals(offer.status())
                && (offer.expiresAt() == null || offer.expiresAt().isAfter(Instant.now()));
        boolean alreadyPaid = false;
        // The order may predate the offer (staff manually converted the quote). That does not
        // remove the customer's right to accept an unpaid quote and bind the existing order.

        List<Map<String, Object>> payments = jdbc.query("""
                SELECT payment_no,subject,amount,currency,status,native_expires_at,paid_at,updated_at
                  FROM payment_order
                 WHERE business_no=? AND guest_access_id=? AND provider='WECHAT'
                 ORDER BY id DESC LIMIT 1
                """, (rs, i) -> {
                    Map<String, Object> payment = new LinkedHashMap<>();
                    payment.put("paymentNo", rs.getString("payment_no"));
                    payment.put("subject", rs.getString("subject"));
                    payment.put("amount", rs.getBigDecimal("amount"));
                    payment.put("currency", rs.getString("currency"));
                    payment.put("status", rs.getString("status"));
                    Timestamp expires = rs.getTimestamp("native_expires_at");
                    Timestamp paid = rs.getTimestamp("paid_at");
                    Timestamp updated = rs.getTimestamp("updated_at");
                    payment.put("expiresAt", expires == null ? null : expires.toInstant());
                    payment.put("paidAt", paid == null ? null : paid.toInstant());
                    payment.put("updatedAt", updated == null ? null : updated.toInstant());
                    return payment;
                }, access.businessNo(), access.accessId());
        Map<String, Object> payment = payments.isEmpty() ? null : payments.getFirst();
        out.put("payment", payment);
        alreadyPaid = payment != null && "CAPTURED".equals(payment.get("status"));
        out.put("canAccept", offerActive && !alreadyPaid
                && !"CLOSED".equals(out.get("quoteStatus"))
                && !"CANCELLED".equals(out.get("quoteStatus")));
        out.put("paymentEnabled", guestPayments.getIfAvailable() != null);
        return out;
    }

    private Access requireAccess(String rawToken, String businessNo, boolean lock) {
        if (!usableToken(rawToken)) {
            throw new NotFoundException();
        }
        String sql = """
                SELECT a.id,a.quote_id,a.business_no
                  FROM guest_business_access a
                  JOIN guest_business_session_access x ON x.guest_access_id=a.id
                  JOIN guest_session s ON s.id=x.guest_session_id
                 WHERE s.token_hash=?
                   AND s.status='ACTIVE'
                   AND s.expires_at>CURRENT_TIMESTAMP
                   AND a.business_no=?
                   AND a.status='ACTIVE'
                 LIMIT 1
                """ + (lock ? " FOR UPDATE" : "");
        String tokenHash = HashUtil.sha256(rawToken);
        List<Access> rows = jdbc.query(
                sql,
                (rs, i) -> new Access(
                        rs.getLong("id"),
                        rs.getString("quote_id"),
                        rs.getString("business_no")),
                tokenHash,
                businessNo);
        if (rows.isEmpty()) {
            throw new NotFoundException();
        }
        jdbc.update("""
                UPDATE guest_session
                   SET last_seen_at=CURRENT_TIMESTAMP,updated_at=CURRENT_TIMESTAMP
                 WHERE token_hash=? AND status='ACTIVE'
                """, tokenHash);
        return rows.getFirst();
    }

    private Offer latestOffer(String businessNo) {
        List<Offer> rows = jdbc.query("""
                SELECT id,version,subject,amount,currency,estimated_duration,customer_message,status,
                       quoted_at,expires_at,accepted_at
                  FROM quote_offer
                 WHERE business_no=? ORDER BY id DESC LIMIT 1
                """, this::offer, businessNo);
        return rows.isEmpty() ? null : rows.getFirst();
    }

    private Offer offer(java.sql.ResultSet rs, int rowNum) throws java.sql.SQLException {
        Timestamp quoted = rs.getTimestamp("quoted_at");
        Timestamp expires = rs.getTimestamp("expires_at");
        Timestamp accepted = rs.getTimestamp("accepted_at");
        return new Offer(
                rs.getLong("id"),
                rs.getInt("version"),
                rs.getString("subject"),
                rs.getBigDecimal("amount"),
                rs.getString("currency"),
                rs.getString("estimated_duration"),
                rs.getString("customer_message"),
                rs.getString("status"),
                quoted == null ? null : quoted.toInstant(),
                expires == null ? null : expires.toInstant(),
                accepted == null ? null : accepted.toInstant());
    }

    private Map<String, Object> offerMap(Offer offer) {
        if (offer == null) {
            return Map.of();
        }
        Map<String, Object> row = new LinkedHashMap<>();
        row.put("version", offer.version());
        row.put("subject", offer.subject());
        row.put("amount", offer.amount());
        row.put("currency", offer.currency());
        row.put("estimatedDuration", offer.estimatedDuration());
        row.put("customerMessage", offer.customerMessage());
        row.put("status", offer.status());
        row.put("quotedAt", offer.quotedAt());
        row.put("expiresAt", offer.expiresAt());
        row.put("acceptedAt", offer.acceptedAt());
        return row;
    }

    private void validateOffer(OfferRequest body) {
        if (body == null
                || body.amount() == null
                || body.amount().signum() <= 0
                || body.amount().scale() > 2
                || body.amount().compareTo(new BigDecimal("9999999.99")) > 0) {
            throw new BadRequestException("报价必须为大于零的人民币金额，最多两位小数");
        }
        if (body.subject() == null
                || body.subject().isBlank()
                || body.subject().trim().getBytes(java.nio.charset.StandardCharsets.UTF_8).length > 127) {
            throw new BadRequestException("请填写不超过 127 字节的服务名称");
        }
        if (trim(body.estimatedDuration()) != null
                && trim(body.estimatedDuration()).length() > 120) {
            throw new BadRequestException("预计时长不能超过 120 字");
        }
        if (trim(body.customerMessage()) != null
                && trim(body.customerMessage()).length() > 1000) {
            throw new BadRequestException("报价说明不能超过 1000 字");
        }
        if (body.expiresAt() != null && !body.expiresAt().isAfter(Instant.now())) {
            throw new BadRequestException("报价有效期必须晚于当前时间");
        }
    }

    private static String normalizeBusinessNo(String value) {
        String no = value == null ? "" : value.trim();
        if (no.isEmpty() || no.length() > 80) {
            throw new BadRequestException("业务编号无效");
        }
        return no;
    }

    private static String trim(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }

    private static boolean usableToken(String token) {
        return token != null && token.length() >= 32 && token.length() <= 128;
    }

    private static String randomToken() {
        byte[] value = new byte[32];
        RANDOM.nextBytes(value);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(value);
    }

    public record SessionHandle(long id, String token, boolean newlyIssued, Instant expiresAt) {}

    public record AccessLink(String path, Instant expiresAt) {}

    public record ExchangeResult(SessionHandle session, String businessNo) {}

    public record OfferRequest(
            BigDecimal amount,
            String subject,
            String estimatedDuration,
            String customerMessage,
            Instant expiresAt,
            String reason) {}

    private record QuoteState(String quoteId, String status, Long accountId, Long guestAccessId) {}
    private record Access(long accessId, String quoteId, String businessNo) {}
    private record LinkRow(long linkId, long accessId, String businessNo) {}
    private record Offer(
            long id,
            int version,
            String subject,
            BigDecimal amount,
            String currency,
            String estimatedDuration,
            String customerMessage,
            String status,
            Instant quotedAt,
            Instant expiresAt,
            Instant acceptedAt) {}
}
