/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.orm;

import java.util.Properties;

import org.dspace.services.ConfigurationService;
import org.hibernate.dialect.OracleDialect;
import org.hibernate.dialect.PostgreSQLDialect;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.orm.hibernate4.LocalSessionFactoryBean;

/**
 * 
 * @author João Melo <jmelo@lyncode.com>
 */
public class DSpaceHibernateSessionFactory extends LocalSessionFactoryBean {
	@Autowired ConfigurationService config;
	
	@Override
	public Properties getHibernateProperties() {
		
		Properties prop = new Properties();
		
		String dbname = config.getProperty("db.name");
		String dialect = config.getProperty("db.dialect");
		if (dialect == null) {
			if (dbname == null || !dbname.equals("oracle")) // Postgres is the default
				prop.put("hibernate.dialect", PostgreSQLDialect.class.getName());
			else
				prop.put("hibernate.dialect", OracleDialect.class.getName());
		} else {
			prop.put("hibernate.dialect", dialect);
		}
		
		prop.put("hibernate.connection.autocommit", false);
		return prop;
	}
	
}
