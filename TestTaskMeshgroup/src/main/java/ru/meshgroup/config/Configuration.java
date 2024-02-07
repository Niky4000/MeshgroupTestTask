package ru.meshgroup.config;

import javax.sql.DataSource;

//import io.swagger.v3.oas.models.OpenAPI;
//import io.swagger.v3.oas.models.info.Info;
import java.util.List;
import liquibase.integration.spring.SpringLiquibase;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.liquibase.LiquibaseProperties;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.jdbc.DataSourceBuilder;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.core.task.TaskExecutor;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.validation.DataBinder;
import org.springframework.validation.Validator;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;
import org.springframework.web.client.RestTemplate;


@org.springframework.context.annotation.Configuration
public class Configuration {

    @Bean
    public Validator getValidator() {
        return new LocalValidatorFactoryBean();
    }

    @Bean
    @ConfigurationProperties("spring.mesh-datasource")
    public DataSource meshDataSource() {
        return DataSourceBuilder.create().build();
    }

    @Bean
    public NamedParameterJdbcTemplate meshJdbcTemplate(@Qualifier("meshDataSource") DataSource dataSource) {
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
    public SpringLiquibase liquibase() {
        return springLiquibase(meshDataSource(), liquibaseProperties());
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
