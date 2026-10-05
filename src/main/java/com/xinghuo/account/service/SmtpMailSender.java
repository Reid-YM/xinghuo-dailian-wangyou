package com.xinghuo.account.service;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import javax.net.ssl.SSLSocket;
import javax.net.ssl.SSLSocketFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

/**
 * 极简 SMTP 发信客户端。
 *
 * <p>为什么手写而不是用 JavaMail：基线 jar 的依赖是固定的，构建脚本
 * （build_patch.sh）只编译覆盖源码、不向 BOOT-INF/lib 添加 jar，
 * 因此无法引入 jakarta.mail / spring-boot-starter-mail。
 *
 * <p>支持两种连接方式：
 * <ul>
 *   <li>465 隐式 TLS（腾讯云等云厂商的常用端口，25 端口默认被封）</li>
 *   <li>587 STARTTLS（明文连接后升级）</li>
 * </ul>
 *
 * <p>正文一律用 base64 编码 + UTF-8，避免中文正文的行长限制与点号转义问题。
 * 主题用 RFC 2047 的 =?UTF-8?B?...?= 编码。
 */
@Service
public class SmtpMailSender {

    private static final Logger log = LoggerFactory.getLogger(SmtpMailSender.class);

    private static final int CONNECT_TIMEOUT_MS = 10_000;
    private static final int READ_TIMEOUT_MS = 15_000;
    private static final int BODY_LINE_WIDTH = 76;

    private final String host;
    private final int port;
    private final String username;
    private final String password;
    private final String from;
    private final boolean implicitTls;
    private final boolean startTls;

    public SmtpMailSender(
            @Value("${mail.host:}") String host,
            @Value("${mail.port:465}") int port,
            @Value("${mail.username:}") String username,
            @Value("${mail.password:}") String password,
            @Value("${mail.from:}") String from,
            @Value("${mail.ssl:true}") boolean implicitTls,
            @Value("${mail.starttls:false}") boolean startTls) {
        this.host = host;
        this.port = port;
        this.username = username;
        this.password = password;
        this.from = from == null || from.isBlank() ? username : from;
        this.implicitTls = implicitTls;
        this.startTls = startTls;
    }

    /** 邮件功能是否已配置。未配置时上层应给出明确提示，而不是静默失败。 */
    public boolean isConfigured() {
        return host != null && !host.isBlank()
                && username != null && !username.isBlank()
                && password != null && !password.isBlank();
    }

    /**
     * 发送纯文本邮件。
     *
     * @throws AccountException 未配置或发送失败（502，由上层决定是否对用户暴露）
     */
    public void send(String to, String subject, String textBody) {
        if (!isConfigured()) {
            throw AccountException.serverError("MAIL_NOT_CONFIGURED",
                    "邮箱服务尚未配置，请联系客服");
        }
        if (to == null || to.isBlank()) {
            throw AccountException.badRequest("INVALID_EMAIL", "收件人为空");
        }
        Socket socket = null;
        try {
            socket = connect();
            BufferedReader in = new BufferedReader(
                    new InputStreamReader(socket.getInputStream(), StandardCharsets.US_ASCII));
            BufferedWriter out = new BufferedWriter(
                    new OutputStreamWriter(socket.getOutputStream(), StandardCharsets.US_ASCII));

            expect(in, 220, "服务端问候");
            ehlo(in, out, socket);
            if (startTls) {
                send(out, "STARTTLS");
                expect(in, 220, "STARTTLS");
                socket = upgradeToTls(socket);
                in = new BufferedReader(
                        new InputStreamReader(socket.getInputStream(), StandardCharsets.US_ASCII));
                out = new BufferedWriter(
                        new OutputStreamWriter(socket.getOutputStream(), StandardCharsets.US_ASCII));
                ehlo(in, out, socket);
            }
            authenticate(in, out);

            send(out, "MAIL FROM:<" + from + ">");
            expect(in, 250, "MAIL FROM");

            send(out, "RCPT TO:<" + to + ">");
            expect(in, 250, "RCPT TO");

            send(out, "DATA");
            expect(in, 354, "DATA");

            writeMessage(out, to, subject, textBody);
            expect(in, 250, "邮件正文");

            send(out, "QUIT");
        } catch (AccountException e) {
            throw e;
        } catch (IOException e) {
            log.error("SMTP 发送失败 host={} port={}", host, port, e);
            throw AccountException.serverError("MAIL_SEND_FAILED", "邮件发送失败，请稍后再试");
        } finally {
            closeQuietly(socket);
        }
    }

    // ---------------------------------------------------------------- 协议

    private Socket connect() throws IOException {
        if (implicitTls) {
            SSLSocket ssl = (SSLSocket) SSLSocketFactory.getDefault()
                    .createSocket();
            ssl.connect(new InetSocketAddress(host, port), CONNECT_TIMEOUT_MS);
            ssl.setSoTimeout(READ_TIMEOUT_MS);
            ssl.startHandshake();
            return ssl;
        }
        Socket plain = new Socket();
        plain.connect(new InetSocketAddress(host, port), CONNECT_TIMEOUT_MS);
        plain.setSoTimeout(READ_TIMEOUT_MS);
        return plain;
    }

    private Socket upgradeToTls(Socket plain) throws IOException {
        SSLSocket ssl = (SSLSocket) ((SSLSocketFactory) SSLSocketFactory.getDefault())
                .createSocket(plain, host, port, true);
        ssl.setSoTimeout(READ_TIMEOUT_MS);
        ssl.startHandshake();
        return ssl;
    }

    private void ehlo(BufferedReader in, BufferedWriter out, Socket socket) throws IOException {
        // 服务端只要求一个合法的域名标识，用本机名即可。
        String name = socket.getInetAddress() == null ? "localhost" : "xinghuo.local";
        send(out, "EHLO " + name);
        expect(in, 250, "EHLO");
    }

    private void authenticate(BufferedReader in, BufferedWriter out) throws IOException {
        send(out, "AUTH LOGIN");
        expect(in, 334, "AUTH LOGIN");
        send(out, Base64.getEncoder().encodeToString(username.getBytes(StandardCharsets.UTF_8)));
        expect(in, 334, "AUTH 用户名");
        send(out, Base64.getEncoder().encodeToString(password.getBytes(StandardCharsets.UTF_8)));
        expect(in, 235, "AUTH 密码");
    }

    private void writeMessage(BufferedWriter out, String to, String subject, String body)
            throws IOException {
        String encodedSubject = "=?UTF-8?B?"
                + Base64.getEncoder().encodeToString(subject.getBytes(StandardCharsets.UTF_8))
                + "?=";
        out.write("From: " + from + "\r\n");
        out.write("To: " + to + "\r\n");
        out.write("Subject: " + encodedSubject + "\r\n");
        out.write("MIME-Version: 1.0\r\n");
        out.write("Content-Type: text/plain; charset=UTF-8\r\n");
        out.write("Content-Transfer-Encoding: base64\r\n");
        out.write("\r\n");

        String encoded = Base64.getEncoder().encodeToString(body.getBytes(StandardCharsets.UTF_8));
        for (int i = 0; i < encoded.length(); i += BODY_LINE_WIDTH) {
            out.write(encoded.substring(i, Math.min(i + BODY_LINE_WIDTH, encoded.length())));
            out.write("\r\n");
        }
        // base64 正文不会出现行首的点，因此无需 dot-stuffing。
        out.write("\r\n.\r\n");
        out.flush();
    }

    private void send(BufferedWriter out, String line) throws IOException {
        out.write(line + "\r\n");
        out.flush();
    }

    /**
     * 读取 SMTP 响应。
     *
     * <p>多行响应形如 {@code 250-XXX} 连续若干行，最后一行是 {@code 250 XXX}（空格分隔）。
     * 只看首行会误判，必须读到分隔符为空格的那一行。
     */
    private void expect(BufferedReader in, int expectedCode, String stage) throws IOException {
        List<String> lines = new ArrayList<>();
        String code = null;
        for (int i = 0; i < 100; i++) {
            String line = in.readLine();
            if (line == null) {
                throw new IOException("SMTP 连接在 " + stage + " 阶段提前关闭");
            }
            lines.add(line);
            if (line.length() >= 3) {
                code = line.substring(0, 3);
            }
            boolean last = line.length() < 4 || line.charAt(3) == ' ';
            if (last) {
                break;
            }
        }
        if (code == null || Integer.parseInt(code) != expectedCode) {
            throw new IOException("SMTP " + stage + " 期望 " + expectedCode + "，实际: " + lines);
        }
    }

    private void closeQuietly(Socket socket) {
        if (socket == null) {
            return;
        }
        try {
            socket.close();
        } catch (IOException ignored) {
            // 关闭失败无需处理
        }
    }
}
