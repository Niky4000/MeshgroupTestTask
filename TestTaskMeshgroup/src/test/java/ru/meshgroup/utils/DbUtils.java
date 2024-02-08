package ru.meshgroup.utils;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import javax.sql.DataSource;
import liquibase.exception.LiquibaseException;
import liquibase.integration.spring.SpringLiquibase;
import org.apache.tomcat.jdbc.pool.PoolProperties;
import org.springframework.boot.autoconfigure.liquibase.LiquibaseProperties;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;

public class DbUtils {

    public static TransactionTemplate getJdbcTxTemplate(PlatformTransactionManager transactionManager) {
        TransactionTemplate tt = new TransactionTemplate(transactionManager);
        tt.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRED);
        return tt;
    }

    public static PlatformTransactionManager getTransactionManager(DataSource ds) {
        DataSourceTransactionManager mgr = new DataSourceTransactionManager();
        mgr.setDataSource(ds);
        return mgr;
    }

    public static NamedParameterJdbcTemplate getJdbcTemplate(DataSource dataSource) {
        return new NamedParameterJdbcTemplate(dataSource);
    }

    public static org.apache.tomcat.jdbc.pool.DataSource createInMemoryDataSource() {
        org.apache.tomcat.jdbc.pool.DataSource ds = new org.apache.tomcat.jdbc.pool.DataSource();
        PoolProperties props = setDataSourceProperties();
        ds.setPoolProperties(props);
        ds.setValidationQuery("select 1 from dual");
        ds.setTestOnBorrow(true);
        createH2Table(ds);
        return ds;
    }

    private static void createH2Table(org.apache.tomcat.jdbc.pool.DataSource ds) {
        try (Connection connection = ds.getConnection()) {
            connection.prepareStatement("create table h2table(id number)").execute();
        } catch (SQLException sqle) {
            throw new RuntimeException(sqle);
        }
    }

    private static PoolProperties setDataSourceProperties() {
        PoolProperties p = new PoolProperties();
        p.setName("InMemoryDataSource");
        p.setDriverClassName("org.h2.Driver");
        p.setUrl("jdbc:h2:mem:test;DB_CLOSE_DELAY=-1");
        p.setUsername("sa");
        p.setPassword("");
        p.setInitialSize(2);
        p.setMinIdle(2);
        return p;
    }

    public static SpringLiquibase liquibase(DataSource dataSource, String changeLog) throws LiquibaseException {
        return springLiquibase(dataSource, liquibaseProperties(changeLog));
    }

    private static LiquibaseProperties liquibaseProperties(String changeLog) {
        LiquibaseProperties liquibaseProperties = new LiquibaseProperties();
        liquibaseProperties.setEnabled(true);
        liquibaseProperties.setChangeLog(changeLog);
        return liquibaseProperties;
    }

    private static SpringLiquibase springLiquibase(DataSource dataSource, LiquibaseProperties properties) throws LiquibaseException {
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
        liquibase.afterPropertiesSet();
        return liquibase;
    }

    public static void shutdown(DataSource dataSource) throws SQLException {
        try (Connection connection = dataSource.getConnection(); Statement statement = connection.createStatement()) {
            statement.execute("SHUTDOWN");
        }
    }
}
