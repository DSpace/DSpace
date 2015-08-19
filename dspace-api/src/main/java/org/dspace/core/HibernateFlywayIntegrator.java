/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.core;

import org.flywaydb.core.Flyway;
import org.flywaydb.core.api.FlywayException;
import org.hibernate.cfg.Configuration;
import org.hibernate.dialect.*;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.integrator.spi.Integrator;
import org.hibernate.metamodel.source.MetadataImplementor;
import org.hibernate.service.spi.SessionFactoryServiceRegistry;

import java.util.ArrayList;

/**
 * Flyway integrator for hibernate.
 *
 * @author kevinvandevelde at atmire.com
 */
public class HibernateFlywayIntegrator implements Integrator {
    @Override
    public void integrate(Configuration configuration, SessionFactoryImplementor sessionFactory, SessionFactoryServiceRegistry serviceRegistry) {
        final Flyway flyway = new Flyway();
        String url = (String) sessionFactory.getProperties().get("hibernate.connection.url");
        String username = (String) sessionFactory.getProperties().get("hibernate.connection.username");
        String password = (String) sessionFactory.getProperties().get("hibernate.connection.password");

        flyway.setDataSource(url, username, password);
        flyway.setEncoding("UTF-8");

        ArrayList<String> scriptLocations = new ArrayList<>();

        String hibernateType = "";
        Dialect dialect = Dialect.getDialect(sessionFactory.getProperties());
        if(dialect instanceof Oracle8iDialect)
        {
            hibernateType = "oracle";
        }else if (dialect instanceof PostgreSQL81Dialect)
        {
            hibernateType = "postgres";
        }else{
            hibernateType = "h2";
        }


        // Also add the Java package where Flyway will load SQL migrations from (based on DB Type)
        scriptLocations.add("classpath:org.dspace.storage.rdbms.sqlmigration." + hibernateType);

        // Also add the Java package where Flyway will load Java migrations from
        // NOTE: this also loads migrations from any sub-package
        scriptLocations.add("classpath:org.dspace.storage.rdbms.migration");

        //We cannot request services at this point, so we will have to do it the old fashioned way
        if (ConfigurationManager.getProperty("workflow", "workflow.framework").equals("xmlworkflow"))
        {
            scriptLocations.add("classpath:org.dspace.storage.rdbms.sqlmigration.workflow." + hibernateType + ".xmlworkflow");
        }else{
            scriptLocations.add("classpath:org.dspace.storage.rdbms.sqlmigration.workflow." + hibernateType + ".basicWorkflow");
        }


        flyway.setLocations(scriptLocations.toArray(new String[scriptLocations.size()]));
        try {
            flyway.migrate();
        } catch (FlywayException e) {
            throw new RuntimeException("Error while performing flyway migration", e);
        }
    }

    @Override
    public void integrate(MetadataImplementor metadata, SessionFactoryImplementor sessionFactory, SessionFactoryServiceRegistry serviceRegistry) {

    }

    @Override
    public void disintegrate(SessionFactoryImplementor sessionFactory, SessionFactoryServiceRegistry serviceRegistry) {

    }
}
