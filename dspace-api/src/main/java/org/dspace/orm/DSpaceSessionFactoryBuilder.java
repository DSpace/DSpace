/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.orm;

import java.io.IOException;
import java.util.Properties;

import javax.sql.DataSource;

import org.dspace.orm.entity.IDSpaceObject;
import org.dspace.services.ConfigurationService;
import org.hibernate.SessionFactory;
import org.hibernate.dialect.OracleDialect;
import org.hibernate.dialect.PostgreSQLDialect;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.orm.hibernate4.LocalSessionFactoryBean;

/**
 * 
 * @author João Melo <jmelo@lyncode.com>
 */
public class DSpaceSessionFactoryBuilder  {
	@Autowired ConfigurationService config;
	private DataSource datasource;
	
	private static LocalSessionFactoryBean local;
	private static SessionFactory sessionFac = null;
	
	public DataSource getDataSource() {
		return datasource;
	}

	public void setDataSource(DataSource datasource) {
		this.datasource = datasource;
	}

	public SessionFactory create () throws IOException {
		if (sessionFac == null) {
			local = new LocalSessionFactoryBean();
			local.setDataSource(datasource);
			local.setPackagesToScan(IDSpaceObject.class.getPackage().getName());
			Properties prop = new Properties();
			
			String dbdialect = config.getProperty("db.dialect");
			if (dbdialect == null) {
				// TODO: db.dialect must be defined in dspace.cfg
				String dbname = config.getProperty("db.name");
				if (dbname == null || !dbname.equals("oracle")) // Postgres is the default
					prop.put("hibernate.dialect", PostgreSQLDialect.class.getName());
				else
					prop.put("hibernate.dialect", OracleDialect.class.getName());
			} else {
				prop.put("hibernate.dialect", dbdialect);
			}
	
			prop.put("hibernate.connection.autocommit", false);
			prop.put("hibernate.current_session_context_class", "thread");
			
			local.setHibernateProperties(prop);
			local.afterPropertiesSet();
			sessionFac = local.getObject();
		}
		return sessionFac;
	}
}
