package com.xinghuo.account.service;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import com.xinghuo.account.model.AccountToken;
import com.xinghuo.account.repo.AccountTokenRepository;
import java.time.Instant;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.*;
import org.springframework.transaction.annotation.EnableTransactionManagement;
import org.springframework.transaction.support.TransactionTemplate;

class CodeAttemptRegressionTest {
 @Configuration @EnableTransactionManagement static class Config {
  @Bean DriverManagerDataSource ds(){return new DriverManagerDataSource("jdbc:h2:mem:attempts;MODE=MySQL;DB_CLOSE_DELAY=-1","sa","");}
  @Bean JdbcTemplate jdbc(DriverManagerDataSource ds){return new JdbcTemplate(ds);}
  @Bean DataSourceTransactionManager transactionManager(DriverManagerDataSource ds){return new DataSourceTransactionManager(ds);}
  @Bean AccountTokenRepository tokens(JdbcTemplate jdbc){
   var repo=mock(AccountTokenRepository.class);
   when(repo.consumeAtomically(anyString(),any(),any())).thenAnswer(i->jdbc.update("UPDATE user_token SET used_at=? WHERE token_hash=? AND used_at IS NULL AND expires_at>?",java.sql.Timestamp.from(i.getArgument(2,Instant.class)),i.getArgument(0),java.sql.Timestamp.from(i.getArgument(2,Instant.class))));
   when(repo.findByTokenHash(anyString())).thenReturn(Optional.of(AccountToken.builder().userId(1L).build()));
   return repo;
  }
  @Bean AccountTokenService service(AccountTokenRepository repo,JdbcTemplate jdbc){return new AccountTokenService(repo,jdbc);}
 }
 @Test void failedAttemptsSurviveOuterRollbackAndExhaustCorrectCode() throws Exception {
  try(var context=new AnnotationConfigApplicationContext(Config.class)) {
   var jdbc=context.getBean(JdbcTemplate.class);var service=context.getBean(AccountTokenService.class);
   jdbc.execute("DROP TABLE IF EXISTS user_token");
   jdbc.execute("CREATE TABLE user_token(id BIGINT PRIMARY KEY, token_hash VARCHAR(64), payload VARCHAR(200), purpose VARCHAR(24), expires_at TIMESTAMP, used_at TIMESTAMP, failed_attempts INT DEFAULT 0)");
   jdbc.update("INSERT INTO user_token(id,token_hash,payload,purpose,expires_at) VALUES(1,?,?,?,?)",AccountTokenService.sha256("scope:123456"),"scope","RESET_PASSWORD",java.sql.Timestamp.from(Instant.now().plusSeconds(600)));
   var outer=new TransactionTemplate(context.getBean(DataSourceTransactionManager.class));
   for(int i=0;i<5;i++) outer.executeWithoutResult(status->{assertTrue(service.consumeCode("scope","000000",AccountToken.Purpose.RESET_PASSWORD).isEmpty());status.setRollbackOnly();});
   assertEquals(5,jdbc.queryForObject("SELECT failed_attempts FROM user_token WHERE id=1",Integer.class));
   assertTrue(service.consumeCode("scope","123456",AccountToken.Purpose.RESET_PASSWORD).isEmpty());
   assertNotNull(jdbc.queryForObject("SELECT used_at FROM user_token WHERE id=1",java.sql.Timestamp.class));
   jdbc.update("INSERT INTO user_token(id,token_hash,payload,purpose,expires_at) VALUES(2,?,?,?,?)",AccountTokenService.sha256("parallel:123456"),"parallel","RESET_PASSWORD",java.sql.Timestamp.from(Instant.now().plusSeconds(600)));
   try(var pool=java.util.concurrent.Executors.newFixedThreadPool(6)) {
    var tasks=new java.util.ArrayList<java.util.concurrent.Callable<Boolean>>();
    for(int i=0;i<12;i++) tasks.add(()->service.consumeCode("parallel","000000",AccountToken.Purpose.RESET_PASSWORD).isEmpty());
    for(var result:pool.invokeAll(tasks)) assertTrue(result.get());
   }
   assertEquals(5,jdbc.queryForObject("SELECT failed_attempts FROM user_token WHERE id=2",Integer.class));
   assertTrue(service.consumeCode("parallel","123456",AccountToken.Purpose.RESET_PASSWORD).isEmpty());
  }
 }
}
