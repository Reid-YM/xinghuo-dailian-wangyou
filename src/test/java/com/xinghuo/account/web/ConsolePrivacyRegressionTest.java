package com.xinghuo.account.web;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.xinghuo.account.model.Account;
import com.xinghuo.account.service.AccountAuditService;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.test.util.ReflectionTestUtils;

class ConsolePrivacyRegressionTest {
 @Test void customerQueriesOnlyExposeCustomerSupplement() throws Exception {
  var jdbc=new JdbcTemplate(new DriverManagerDataSource("jdbc:h2:mem:privacy;MODE=MySQL;DB_CLOSE_DELAY=-1","sa",""));
  jdbc.execute("CREATE TABLE quotes(business_no VARCHAR(30),game VARCHAR(30),demand VARCHAR(30),current_progress VARCHAR(30),platform VARCHAR(30),expected_time VARCHAR(30),status VARCHAR(30),next_action VARCHAR(100),follow_up_note VARCHAR(100),customer_supplement VARCHAR(100),created_at TIMESTAMP,updated_at TIMESTAMP,customer_account_id BIGINT)");
  jdbc.update("INSERT INTO quotes(business_no,status,next_action,follow_up_note,customer_supplement,customer_account_id) VALUES('XH1234567890','NEW','PRIVATE_ACTION','PRIVATE_NOTE','CUSTOMER_TEXT',1)");
  var controller=new ConsoleApiController(jdbc,mock(AccountAuditService.class));
  var account=Account.builder().id(1L).build();
  Object detail=ReflectionTestUtils.invokeMethod(controller,"findQuote",account,"XH1234567890");
  Object list=ReflectionTestUtils.invokeMethod(controller,"quotes",account,0,10);
  var mapper=new ObjectMapper().findAndRegisterModules();
  for(Object result:new Object[]{detail,list}) {
   String json=mapper.writeValueAsString(result);
   assertTrue(json.contains("CUSTOMER_TEXT"));
   assertFalse(json.contains("PRIVATE_"));assertFalse(json.contains("followUpNote"));assertFalse(json.contains("nextAction"));
  }
 }
}
