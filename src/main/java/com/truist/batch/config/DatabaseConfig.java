package com.truist.batch.config;

import javax.sql.DataSource;

import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.repository.support.JobRepositoryFactoryBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.jdbc.DataSourceBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.core.env.Environment;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.transaction.PlatformTransactionManager;

import lombok.extern.slf4j.Slf4j;

@Configuration
@Slf4j
public class DatabaseConfig {

    @Value("${spring.datasource.username}")
    private String userName;
    
    @Value("${spring.datasource.password}")
    private String password;
    
    @Value("${spring.datasource.url}")
    private String url;
    
    @Value("${spring.datasource.driver-class-name}")
    private String driverClass;
    
    @Autowired
    private Environment environment;

    @Bean
    @Primary
    public DataSource dataSource() {
        return DataSourceBuilder.create()
               .url(url)
               .username(userName)
               .password(password)
               .driverClassName(driverClass)
               .build();
    }

    @Bean
    @Primary
    public PlatformTransactionManager transactionManager(DataSource dataSource) {
        DataSourceTransactionManager txManager = new DataSourceTransactionManager(dataSource);
        
        // ✅ DEBUG-FRIENDLY TRANSACTION TIMEOUT
        int timeoutSeconds = getDebugFriendlyTimeout();
        txManager.setDefaultTimeout(timeoutSeconds);
        
        log.info("🕒 Transaction timeout set to {} seconds {}", 
            timeoutSeconds, 
            isDebugMode() ? "(DEBUG MODE - Extended)" : "(NORMAL MODE)");
        
        return txManager;
    }

    @Bean
    public JobRepository jobRepository(DataSource dataSource, 
                                     PlatformTransactionManager transactionManager) throws Exception {
        JobRepositoryFactoryBean factory = new JobRepositoryFactoryBean();
        factory.setDataSource(dataSource);
        factory.setTransactionManager(transactionManager);
        factory.setTablePrefix("CM3INT.BATCH_");
        
        factory.afterPropertiesSet();
        return factory.getObject();
    }
    
    @Bean
    @Primary
    public JdbcTemplate jdbcTemplate(DataSource dataSource) {
        JdbcTemplate template = new JdbcTemplate(dataSource);
        
        // ✅ DEBUG-FRIENDLY QUERY TIMEOUT
        if (isDebugMode()) {
            template.setQueryTimeout(3600); // 1 hour for debugging
            log.info("🐛 DEBUG MODE: Query timeout extended to 1 hour");
        } else {
            template.setQueryTimeout(300);  // 5 minutes for production
        }
        
        return template;
    }
    
    /**
     * Detects debug mode and returns appropriate timeout
     */
    private int getDebugFriendlyTimeout() {
        if (isDebugMode()) {
            return 3600; // 1 hour for debugging sessions
        }
        return 300; // 5 minutes for normal execution
    }
    
    /**
     * Detects if we're running in debug mode
     */
    private boolean isDebugMode() {
        // Check for JVM debug agent
        boolean debugJVM = java.lang.management.ManagementFactory.getRuntimeMXBean()
            .getInputArguments().toString().contains("-agentlib:jdwp");
        
        // Check system properties
        boolean debugProp = "true".equals(System.getProperty("debug.batch"));
        boolean debugEnv = "true".equals(System.getenv("DEBUG_BATCH"));
        
        // Check Spring profile
        boolean debugProfile = java.util.Arrays.asList(environment.getActiveProfiles()).contains("debug");
        
        // Check application property
        boolean debugConfig = environment.getProperty("batch.debug.enabled", Boolean.class, false);
        
        // Check IDE environment (IntelliJ sets this)
        boolean ideDebug = System.getProperty("java.class.path").contains("idea_rt.jar");
        
        return debugJVM || debugProp || debugEnv || debugProfile || debugConfig || ideDebug;
    }
}