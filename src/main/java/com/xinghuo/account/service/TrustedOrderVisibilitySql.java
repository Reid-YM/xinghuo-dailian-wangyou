package com.xinghuo.account.service;

/**
 * Account-scoped read-only visibility for orders awaiting legacy ownership repair.
 *
 * <p>Only the order's authoritative source quote can prove account ownership.
 * Orders already owned by somebody else, guest-owned orders, anonymous contact
 * matches and broken source-quote references are never exposed as candidates.
 *
 * <p>Callers use alias "o" for customer_orders and bind the same account id twice.
 */
public final class TrustedOrderVisibilitySql {
    private TrustedOrderVisibilitySql() {
    }

    public static final String PREDICATE = """
            (o.customer_account_id = ?
              OR (o.customer_account_id IS NULL
                  AND o.guest_access_id IS NULL
                  AND EXISTS (
                      SELECT 1 FROM quotes proof
                       WHERE proof.customer_account_id = ?
                         AND ((o.source_quote_id IS NOT NULL AND proof.id = o.source_quote_id)
                              OR (o.source_quote_id IS NULL AND proof.business_no = o.business_no))
                  )))
            """;
}
