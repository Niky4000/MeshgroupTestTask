package ru.meshgroup.config;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import javax.sql.DataSource;
//import io.swagger.v3.oas.models.OpenAPI;
//import io.swagger.v3.oas.models.info.Info;
import liquibase.integration.spring.SpringLiquibase;
import org.apache.ignite.Ignite;
import org.apache.ignite.IgniteAtomicLong;
import org.apache.ignite.IgniteLock;
import org.apache.ignite.IgniteQueue;
import org.apache.ignite.cache.CacheAtomicityMode;
import org.apache.ignite.cache.CacheMode;
import org.apache.ignite.configuration.CollectionConfiguration;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.liquibase.LiquibaseProperties;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.jdbc.DataSourceBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.annotation.EnableTransactionManagement;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.validation.Validator;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;
import ru.meshgroup.service.bean.OperationBean;
import ru.meshgroup.utils.IgniteUtils;

@EnableTransactionManagement
@org.springframework.context.annotation.Configuration
public class Configuration {

    @Bean
    public Validator getValidator() {
        return new LocalValidatorFactoryBean();
    }

    @Bean
    @Qualifier("meshDataSource")
    @ConfigurationProperties("spring.mesh-datasource")
    public DataSource getMeshDataSource() {
        return DataSourceBuilder.create().build();
    }

    @Bean
    @Qualifier("meshJdbc")
    public NamedParameterJdbcTemplate getMeshJdbcTemplate(@Qualifier("meshDataSource") DataSource dataSource) {
        return new NamedParameterJdbcTemplate(dataSource);
    }

    @Bean
    public PlatformTransactionManager meshTransactionManager(@Qualifier("meshDataSource") DataSource ds) {
        DataSourceTransactionManager mgr = new DataSourceTransactionManager();
        mgr.setDataSource(ds);
        return mgr;
    }

    @Bean
    @Qualifier("meshJdbcTxTemplate")
    public TransactionTemplate meshJdbcTxTemplate(@Qualifier("meshTransactionManager") PlatformTransactionManager transactionManager) {
        TransactionTemplate tt = new TransactionTemplate(transactionManager);
        tt.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRED);
        return tt;
    }

    @Bean
    public SpringLiquibase liquibase(@Qualifier("meshDataSource") DataSource ds) {
        return springLiquibase(ds, liquibaseProperties());
    }

    @Bean
    @ConfigurationProperties(prefix = "spring.mesh-datasource.liquibase")
    public LiquibaseProperties liquibaseProperties() {
        return new LiquibaseProperties();
    }

//    @Bean
//    public OpenAPI customOpenApi() {
//        return new OpenAPI().info(new Info().title("Mesh Service API"));
//    }
    private static SpringLiquibase springLiquibase(DataSource dataSource, LiquibaseProperties properties) {
        SpringLiquibase liquibase = new SpringLiquibase();
        liquibase.setDataSource(dataSource);
        liquibase.setChangeLog(properties.getChangeLog());
        liquibase.setContexts(properties.getContexts());
        liquibase.setDefaultSchema(properties.getDefaultSchema());
        liquibase.setDropFirst(properties.isDropFirst());
        liquibase.setShouldRun(properties.isEnabled());
        liquibase.setLabels(properties.getLabels());
        liquibase.setChangeLogParameters(properties.getParameters());
        liquibase.setRollbackFile(properties.getRollbackFile());
        return liquibase;
    }

    @Bean
    public Ignite getIgnite(@Value("${ignite.host-list}") String hostListStr,
            @Value("${ignite.instance-name}") String instanceName,
            @Value("${ignite.port}") Integer initialLocalPort,
            @Value("${ignite.end-port}") Integer endPort,
            @Value("${ignite.local-port}") Integer localPort,
            @Value("${ignite.client-port}") Integer clientPort,
            @Value("${ignite.client-port-range}") Integer clientPortRange) {
        Set<String> hostSet = new HashSet<>(Arrays.asList(hostListStr.split(",")));
        Ignite ignite = IgniteUtils.createServerInstance(new ArrayList<>(hostSet), instanceName, initialLocalPort, endPort, localPort, clientPort, clientPortRange);
        return ignite;
    }

    @Bean
    public IgniteAtomicLong getActiveOperationsCounter(Ignite ignite) {
        return ignite.atomicLong("activeOperationsCounter", 0, true);
    }

    @Bean
    @Qualifier("operationLock")
    public IgniteLock getLock(Ignite ignite) {
        return ignite.reentrantLock("operationLock", true, false, true);
    }

    @Bean
    @Qualifier("queuedOperationsLock")
    public IgniteLock getQueuedOperationsLock(Ignite ignite) {
        return ignite.reentrantLock("queuedOperationsLock", true, false, true);
    }

    @Bean
    public IgniteQueue<OperationBean> getOperationQueue(Ignite ignite) {
        return ignite.queue("operationQueue", 0, new CollectionConfiguration().setGroupName("queues")
                .setAtomicityMode(CacheAtomicityMode.ATOMIC).setBackups(1).setCacheMode(CacheMode.PARTITIONED)
                .setCollocated(true));
//        return ignite.queue("operationQueue", 0, null);
    }

    @Bean
    @Qualifier("meshExecutor")
    public Executor getExecutor() {
        return Executors.newCachedThreadPool();
    }
//    @Bean
//    @Qualifier("meshProviderExecutor")
//    @ConfigurationProperties("app.task.mesh-provider")
//    public TaskExecutor meshProviderExecutor() {
//        return new ThreadPoolTaskExecutor();
//    }
//	@Bean
//	public MeshBeanPostProcessor getMeshBeanPostProcessor() {
//		return new MeshBeanPostProcessor();
//	}
}
