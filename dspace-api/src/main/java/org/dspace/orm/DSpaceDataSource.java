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

public class DSpaceDataSource extends DriverManagerDataSource {
	@Autowired ConfigurationService config;
	
    public DSpaceDataSource() {
        super.setDriverClassName(config.getProperty("db.driver"));
    }

    @Override
    public String getPassword() {
        return config.getProperty("db.password");
    }

    @Override
    public String getUrl() {
        return config.getProperty("db.url");
    }

    @Override
    public String getUsername() {
        return config.getProperty("db.username");
    }

}
