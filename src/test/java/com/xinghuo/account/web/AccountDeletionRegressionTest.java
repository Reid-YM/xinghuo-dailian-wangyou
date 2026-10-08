package com.xinghuo.account.web;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import com.xinghuo.account.repo.*;
import com.xinghuo.account.service.*;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.*;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.annotation.EnableTransactionManagement;

class AccountDeletionRegressionTest {
 @Configuration @EnableTransactionManagement static class Config {
  @Bean DriverManagerDataSource ds(){return new DriverManagerDataSource("jdbc:h2:mem:deletion;MODE=MySQL;DB_CLOSE_DELAY=-1","sa","");}
  @Bean JdbcTemplate jdbc(DriverManagerDataSource ds){return new JdbcTemplate(ds);}
  @Bean DataSourceTransactionManager transactionManager(DriverManagerDataSource ds){return new DataSourceTransactionManager(ds);}
  @Bean AccountRepository accounts(){
   var repo=mock(AccountRepository.class);when(repo.existsById(1L)).thenReturn(true);
   doThrow(new IllegalStateException("injected delete failure")).when(repo).deleteById(1L);return repo;
  }
  @Bean AccountAdminApiController controller(AccountRepository accounts,JdbcTemplate jdbc){
   return new AccountAdminApiController(mock(CommentService.class),mock(SubmissionService.class),accounts,mock(AccountAuditRepository.class),mock(PasswordEncoder.class),jdbc);
  }
 }
 @Test void deleteFailureRollsBackBothOwnershipUpdates() {
  try(var context=new AnnotationConfigApplicationContext(Config.class)) {
   var jdbc=context.getBean(JdbcTemplate.class);
   jdbc.execute("CREATE TABLE quotes(customer_account_id BIGINT)");
   jdbc.execute("CREATE TABLE customer_orders(customer_account_id BIGINT,version BIGINT)");
   jdbc.update("INSERT INTO quotes VALUES(1)");jdbc.update("INSERT INTO customer_orders VALUES(1,3)");
   assertThrows(IllegalStateException.class,()->context.getBean(AccountAdminApiController.class).deleteUser(1L));
   assertEquals(1L,jdbc.queryForObject("SELECT customer_account_id FROM quotes",Long.class));
   assertEquals(1L,jdbc.queryForObject("SELECT customer_account_id FROM customer_orders",Long.class));
   assertEquals(3L,jdbc.queryForObject("SELECT version FROM customer_orders",Long.class));
  }
 }
}
