/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
/**
 * This based heavily on the CommonsConfigurationFactoryBean available in
 * 'spring-modules-jakarta-commons' version 0.8:
 * https://java.net/projects/springmodules/sources/svn/content/tags/release-0_8/projects/commons/src/java/org/springmodules/commons/configuration/CommonsConfigurationFactoryBean.java?rev=2110
 * <P>
 * As this module is no longer maintained by Spring, it is now recommended to
 * maintain it within your own codebase, so that minor updates can be made to
 * support new versions of Apache Commons Configuration (as needed). See this
 * Spring ticket: https://jira.spring.io/browse/SPR-10213
 * <P>
 * For DSpace, we've specifically updated this bean to automatically load all
 * configurations from the DSpaceConfigurationService (which uses Commons
 * Configuration internally). See constructor below.
 * <P>
 * This bean is loaded in 'spring-dspace-core-services.xml' where it is wired
 * up to PropertyPlaceholderConfigurer.
 */
package org.dspace.servicemanager.config;

import java.net.URL;

import org.apache.commons.configuration.CompositeConfiguration;
import org.apache.commons.configuration.Configuration;
import org.apache.commons.configuration.ConfigurationConverter;
import org.apache.commons.configuration.PropertiesConfiguration;
import org.dspace.services.ConfigurationService;
import org.springframework.beans.factory.FactoryBean;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.core.io.Resource;
import org.springframework.util.Assert;

/**
 * FactoryBean which wraps a Commons CompositeConfiguration object for usage
 * with PropertiesLoaderSupport. This allows the configuration object to behave
 * like a normal java.util.Properties object which can be passed on to
 * setProperties() method allowing PropertyOverrideConfigurer and
 * PropertyPlaceholderConfigurer to take advantage of Commons Configuration.
 * <p/> Internally a CompositeConfiguration object is used for merging multiple
 * Configuration objects.
 *
 * @see java.util.Properties
 * @see org.springframework.core.io.support.PropertiesLoaderSupport
 *
 * @author Costin Leau
 *
 */
public class DSpaceConfigurationFactoryBean implements InitializingBean, FactoryBean {

	private CompositeConfiguration configuration;

	private Configuration[] configurations;

	private Resource[] locations;

	private boolean throwExceptionOnMissing = true;

    /**
     * Initialize all properties via the passed in DSpace ConfigurationService
     * @param configurationService
     */
	public DSpaceConfigurationFactoryBean(ConfigurationService configurationService)
    {
        Assert.notNull(configurationService.getConfiguration());
        this.configuration = new CompositeConfiguration(configurationService.getConfiguration());
	}

    /**
     * Initialize all properties via the passed in Commons Configuration
     * @param configuration
     */
	public DSpaceConfigurationFactoryBean(Configuration configuration) {
		Assert.notNull(configuration);
		this.configuration = new CompositeConfiguration(configuration);
	}

	/**
	 * @see org.springframework.beans.factory.FactoryBean#getObject()
	 */
	public Object getObject() throws Exception {
		return (configuration != null) ? ConfigurationConverter.getProperties(configuration) : null;
	}

	/**
	 * @see org.springframework.beans.factory.FactoryBean#getObjectType()
	 */
	public Class getObjectType() {
		return java.util.Properties.class;
	}

	/**
	 * @see org.springframework.beans.factory.FactoryBean#isSingleton()
	 */
	public boolean isSingleton() {
		return true;
	}

	/**
	 * @see org.springframework.beans.factory.InitializingBean#afterPropertiesSet()
	 */
	public void afterPropertiesSet() throws Exception {
		if (configuration == null && (configurations == null || configurations.length == 0)
				&& (locations == null || locations.length == 0))
			throw new IllegalArgumentException("no configuration object or location specified");

		if (configuration == null)
			configuration = new CompositeConfiguration();

		configuration.setThrowExceptionOnMissing(throwExceptionOnMissing);

		if (configurations != null) {
			for (int i = 0; i < configurations.length; i++) {
				configuration.addConfiguration(configurations[i]);
			}
		}

		if (locations != null) {
			for (int i = 0; i < locations.length; i++) {
				URL url = locations[i].getURL();
				Configuration props = new PropertiesConfiguration(url);
				configuration.addConfiguration(props);
			}
		}
	}

	/**
	 * @return Returns the configurations.
	 */
	public Configuration[] getConfigurations() {
		return configurations;
	}

	/**
	 * Set the commons configurations objects which will be used as properties.
	 *
	 * @param configurations
	 */
	public void setConfigurations(Configuration[] configurations) {
		this.configurations = configurations;
	}

	public Resource[] getLocations() {
		return locations;
	}

	/**
	 * Shortcut for loading configuration from Spring resources. It will
	 * internally create a PropertiesConfiguration object based on the URL
	 * retrieved from the given Resources.
	 *
	 * @param locations
	 */
	public void setLocations(Resource[] locations) {
		this.locations = locations;
	}

	public boolean isThrowExceptionOnMissing() {
		return throwExceptionOnMissing;
	}

	/**
	 * Set the underlying Commons CompositeConfiguration throwExceptionOnMissing
	 * flag.
	 * @param throwExceptionOnMissing
	 */
	public void setThrowExceptionOnMissing(boolean throwExceptionOnMissing) {
		this.throwExceptionOnMissing = throwExceptionOnMissing;
	}

	/**
	 * Getter for the underlying CompositeConfiguration object.
	 *
	 * @return
	 */
	public CompositeConfiguration getConfiguration() {
		return configuration;
	}

}

