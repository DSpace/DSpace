/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.orm;

import org.dspace.services.ConfigurationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.datasource.DriverManagerDataSource;

/**
 * 
 * @author João Melo <jmelo@lyncode.com>
 *
 */
public class DSpaceDataSourceBuilder {
	@Autowired ConfigurationService config;
	private static DriverManagerDataSource driver;
	
	public DriverManagerDataSource create () {
		if (driver == null) {
			driver = new DriverManagerDataSource();
			driver.setDriverClassName(config.getProperty("db.driver"));
			
			driver.setUrl(config.getProperty("db.url"));
			String user = config.getProperty("db.username");
			if (user != null) {
				driver.setUsername(user);
				String pass = config.getProperty("db.password");
				if (pass != null) driver.setPassword(pass);
			}
		}
		return driver;
	}
}
