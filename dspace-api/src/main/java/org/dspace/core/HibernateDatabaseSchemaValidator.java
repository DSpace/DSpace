/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.core;

import org.hibernate.HibernateException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.orm.hibernate4.LocalSessionFactoryBean;

/**
 * Database schema validation when using the Hibernate persistence layer
 */
public class HibernateDatabaseSchemaValidator implements DatabaseSchemaValidator {

    @Autowired
    private ApplicationContext applicationContext;

    public String getDatabaseSchemaValidationError() {
        String validationError = "";

        try {
            applicationContext.getBean(LocalSessionFactoryBean.class);
        } catch (org.springframework.beans.factory.BeanCreationException ex) {
            //The hibernate validation exception is the cause of this BeanCreationException
            validationError = ex.getCause() == null ? ex.getMessage() : ex.getCause().getMessage();
        } catch (HibernateException ex) {
            validationError = ex.getMessage();
        }

        return validationError;
    }

}
