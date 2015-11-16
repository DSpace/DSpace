package ua.edu.sumdu.essuir.config;

import org.dspace.core.ConfigurationManager;
import org.hibernate.jpa.HibernatePersistenceProvider;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.orm.jpa.JpaTransactionManager;
import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean;
import org.springframework.transaction.annotation.EnableTransactionManagement;

import javax.sql.DataSource;
import java.util.Properties;

@Configuration
@EnableTransactionManagement
@ComponentScan("ua.edu.sumdu.essuir")
@EnableJpaRepositories("ua.edu.sumdu.essuir.repository")
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
    public DataSource dataSource() {
        DriverManagerDataSource dataSource = new DriverManagerDataSource();

        dataSource.setDriverClassName(PROP_DATABASE_DRIVER);
        dataSource.setUrl(PROP_DATABASE_URL);
        dataSource.setUsername(PROP_DATABASE_USERNAME);
        dataSource.setPassword(PROP_DATABASE_PASSWORD);

        return dataSource;
    }

    @Bean
    public LocalContainerEntityManagerFactoryBean entityManagerFactory() {
        LocalContainerEntityManagerFactoryBean entityManagerFactoryBean = new LocalContainerEntityManagerFactoryBean();
        entityManagerFactoryBean.setDataSource(dataSource());
        entityManagerFactoryBean.setPersistenceProviderClass(HibernatePersistenceProvider.class);
        entityManagerFactoryBean.setPackagesToScan(PROP_ENTITYMANAGER_PACKAGES_TO_SCAN);

        entityManagerFactoryBean.setJpaProperties(getHibernateProperties());

        return entityManagerFactoryBean;
    }

    @Bean
    public JpaTransactionManager transactionManager() {
        JpaTransactionManager transactionManager = new JpaTransactionManager();
        transactionManager.setEntityManagerFactory(entityManagerFactory().getObject());

        return transactionManager;
    }


    private Properties getHibernateProperties() {
        Properties properties = new Properties();
        properties.put("db.hibernate.dialect", PROP_HIBERNATE_DIALECT);
        properties.put("db.hibernate.show_sql", PROP_HIBERNATE_SHOW_SQL);
        properties.put("db.hibernate.hbm2ddl.auto", PROP_HIBERNATE_HBM2DDL_AUTO);

        return properties;
    }

}
