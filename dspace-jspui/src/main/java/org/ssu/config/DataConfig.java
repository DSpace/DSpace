package org.ssu.config;

import liquibase.integration.spring.SpringLiquibase;
import org.apache.tomcat.jdbc.pool.DataSource;
import org.dspace.core.ConfigurationManager;
import org.hibernate.ejb.HibernatePersistence;
import org.jooq.SQLDialect;
import org.jooq.conf.Settings;
import org.jooq.impl.DataSourceConnectionProvider;
import org.jooq.impl.DefaultConfiguration;
import org.jooq.impl.DefaultDSLContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.DependsOn;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.jdbc.datasource.TransactionAwareDataSourceProxy;
import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean;
import org.springframework.transaction.annotation.EnableTransactionManagement;

import java.util.Properties;

@Configuration
@EnableTransactionManagement
@ComponentScan("org.ssu")
@EnableJpaRepositories("org.ssu.repository")
public class DataConfig {

    private static final String PROP_DATABASE_DRIVER = ConfigurationManager.getProperty("db.driver");
    private static final String PROP_DATABASE_PASSWORD = ConfigurationManager.getProperty("db.password");
    private static final String PROP_DATABASE_URL = ConfigurationManager.getProperty("db.url");
    private static final String PROP_DATABASE_USERNAME = ConfigurationManager.getProperty("db.username");
    private static final String PROP_HIBERNATE_DIALECT = ConfigurationManager.getProperty("db.hibernate.dialect");
    private static final String PROP_HIBERNATE_SHOW_SQL = ConfigurationManager.getProperty("db.hibernate.show_sql");
    private static final String PROP_ENTITYMANAGER_PACKAGES_TO_SCAN = ConfigurationManager.getProperty("db.entitymanager.packages.to.scan");
    private static final String PROP_HIBERNATE_HBM2DDL_AUTO = ConfigurationManager.getProperty("db.hibernate.hbm2ddl.auto");

    @Bean
    public TransactionAwareDataSourceProxy transactionAwareDataSource() {
        return new TransactionAwareDataSourceProxy(dataSource());
    }

    @Bean
    public DataSourceTransactionManager transactionManager() {
        return new DataSourceTransactionManager(dataSource());
    }

    @Bean
    public DataSourceConnectionProvider connectionProvider() {
        return new DataSourceConnectionProvider(transactionAwareDataSource());
    }

    @Bean
    @DependsOn("liquibase")
    public DefaultDSLContext dsl() {
        return new DefaultDSLContext(configuration());
    }

    @Bean
    public DefaultConfiguration configuration() {
        DefaultConfiguration jooqConfiguration = new DefaultConfiguration();
        jooqConfiguration.set(connectionProvider());

        Settings settings = new Settings().withUpdatablePrimaryKeys(true);
        SQLDialect dialect = SQLDialect.POSTGRES_9_3;
        jooqConfiguration.set(dialect);
        jooqConfiguration.set(settings);


        return jooqConfiguration;
    }

    @Bean
    public DataSource dataSource() {
        DataSource dataSource = new DataSource();

        dataSource.setDriverClassName(PROP_DATABASE_DRIVER);
        dataSource.setUrl(PROP_DATABASE_URL);
        dataSource.setUsername(PROP_DATABASE_USERNAME);
        dataSource.setPassword(PROP_DATABASE_PASSWORD);

        dataSource.setTestOnBorrow(true);
        dataSource.setTestWhileIdle(true);
        dataSource.setTestOnReturn(true);
        dataSource.setValidationQuery("SELECT 1");

        return dataSource;
    }

    @Bean
    @DependsOn("liquibase")
    public LocalContainerEntityManagerFactoryBean entityManagerFactory() {
        LocalContainerEntityManagerFactoryBean entityManagerFactoryBean = new LocalContainerEntityManagerFactoryBean();
        entityManagerFactoryBean.setDataSource(dataSource());
        entityManagerFactoryBean.setPersistenceProviderClass(HibernatePersistence.class);
        entityManagerFactoryBean.setPackagesToScan(PROP_ENTITYMANAGER_PACKAGES_TO_SCAN);
        entityManagerFactoryBean.setJpaProperties(getHibernateProperties());

        return entityManagerFactoryBean;
    }

    private Properties getHibernateProperties() {
        Properties properties = new Properties();
        properties.put("db.hibernate.dialect", PROP_HIBERNATE_DIALECT);
        properties.put("hibernate.show_sql", PROP_HIBERNATE_SHOW_SQL);
        properties.put("db.hibernate.hbm2ddl.auto", PROP_HIBERNATE_HBM2DDL_AUTO);

        return properties;
    }

    @Bean
    public SpringLiquibase liquibase() {
        SpringLiquibase liquibase = new SpringLiquibase();
        liquibase.setChangeLog("classpath:db/liquibase-changeLog.xml");
        liquibase.setDataSource(dataSource());
        return liquibase;
    }
}