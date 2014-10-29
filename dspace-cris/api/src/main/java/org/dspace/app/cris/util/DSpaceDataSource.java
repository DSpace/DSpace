/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * https://github.com/CILEA/dspace-cris/wiki/License
 */
package org.dspace.app.cris.util;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.Properties;

import org.dspace.services.ConfigurationService;


public class DSpaceDataSource {
	private ConfigurationService configurationService;
	private Properties hibernateProperties;
	
	public Properties getHibernateProperties(){
	    if(hibernateProperties!=null) {
	        return hibernateProperties;
	    }
		String dspaceDir = configurationService.getProperty("dspace.dir");
		Properties prop = new Properties();
		FileReader reader = null;
		try
		{
			File file = new File(dspaceDir + File.separator + "config" + File.separator + "hibernate.properties");
			reader = new FileReader(file);
			prop.load(reader);
			reader.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
		finally
		{
			if (reader != null)
			{
				try {
					reader.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}
		this.hibernateProperties = prop;
		return this.hibernateProperties;
	}
	
	public String getUsername()
	{
		return configurationService.getProperty("db.username");
	}
	
	public String getPassword()
	{
		return configurationService.getProperty("db.password");
	}
	
	public String getUrl()
	{
		return configurationService.getProperty("db.url");
	}
	
	public String getDriver()
	{
		return configurationService.getProperty("db.driver");
	}

    public String getHibernateAcquireIncrement()
    {
        return getHibernateProperties().getProperty("hibernate.acquireIncrement");
    }

    public String getHibernateInitialPoolSize()
    {
        return getHibernateProperties().getProperty("hibernate.initialPoolSize");
    }

    public String getHibernateMinPoolSize()
    {
        return getHibernateProperties().getProperty("hibernate.minPoolSize");
    }
    
    public String getHibernateMaxPoolSize()
    {
        return getHibernateProperties().getProperty("hibernate.maxPoolSize");
    }

    public String getHibernateMaxIdleTime()
    {
        return getHibernateProperties().getProperty("hibernate.maxIdleTime");
    }
    
    public String getHibernateMaxStatements()
    {
        return getHibernateProperties().getProperty("hibernate.maxStatements");
    }
    
    public String getHibernateIdleConnectionTestPeriod()
    {
        return getHibernateProperties().getProperty("hibernate.idleConnectionTestPeriod");
    }

    public String getHibernateLoginTimeout()
    {
        return getHibernateProperties().getProperty("hibernate.loginTimeout");
    }
        	
	
	public void setConfigurationService(
			ConfigurationService configurationService) {
		this.configurationService = configurationService;
	}
	
	public ConfigurationService getConfigurationService() {
		return configurationService;
	}
}
