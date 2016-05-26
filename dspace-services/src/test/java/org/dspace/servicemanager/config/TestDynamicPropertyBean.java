/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.servicemanager.config;

/**
 * A test bean which we will configure to load its one property via ConfigurationService.
 * <P>
 * See 'spring-test-beans.xml' and DSpaceConfigurationFactoryBeanTest.
 * @author Tim Donohue
 */
public class TestDynamicPropertyBean
{
    private String value;

    public void setProperty(String value)
    {
        this.value = value;
    }

    public String getProperty()
    {
        return this.value;
    }
}
