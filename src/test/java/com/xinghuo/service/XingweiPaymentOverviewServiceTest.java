package com.xinghuo.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;

class XingweiPaymentOverviewServiceTest {
    @Test
    void summarizesOnlyWechatPaymentsWithoutReturningProviderSecrets() {
        JdbcTemplate jdbc = new JdbcTemplate(new DriverManagerDataSource(
                "jdbc:h2:mem:xw-payment-overview;MODE=MySQL;DB_CLOSE_DELAY=-1", "sa", ""));
        jdbc.execute("""
                CREATE TABLE payment_order (
                  id BIGINT AUTO_INCREMENT PRIMARY KEY, payment_no VARCHAR(32),
                  business_no VARCHAR(40), subject VARCHAR(200), amount DECIMAL(12,2),
                  currency VARCHAR(8), provider VARCHAR(16), status VARCHAR(20),
                  created_at TIMESTAMP, paid_at TIMESTAMP, native_code_url VARCHAR(500),
                  provider_order_id VARCHAR(64))
                """);
        jdbc.update("""
                INSERT INTO payment_order(payment_no,business_no,subject,amount,currency,provider,status,created_at,native_code_url)
                VALUES ('XHP1','XH1','服务一',12.50,'CNY','WECHAT','CREATED',CURRENT_TIMESTAMP,'weixin://secret')
                """);
        jdbc.update("""
                INSERT INTO payment_order(payment_no,business_no,subject,amount,currency,provider,status,created_at,paid_at,provider_order_id)
                VALUES ('XHP2','XH2','服务二',20.00,'CNY','WECHAT','CAPTURED',CURRENT_TIMESTAMP,CURRENT_TIMESTAMP,'private-provider-id')
                """);
        jdbc.update("""
                INSERT INTO payment_order(payment_no,business_no,subject,amount,currency,provider,status,created_at)
                VALUES ('OTHER','XH3','其他渠道',30.00,'CNY','PAYPAL','CAPTURED',CURRENT_TIMESTAMP)
                """);

        XingweiPaymentOverviewService.Overview result = new XingweiPaymentOverviewService(jdbc).overview();

        assertEquals(2, result.counts().total());
        assertEquals(1, result.counts().pending());
        assertEquals(1, result.counts().captured());
        assertEquals(0, result.counts().cancelled());
        assertEquals(2, result.recent().size());
        assertEquals("XHP2", result.recent().getFirst().paymentNo());
        assertFalse(result.toString().contains("weixin://secret"));
        assertFalse(result.toString().contains("private-provider-id"));
    }
}
