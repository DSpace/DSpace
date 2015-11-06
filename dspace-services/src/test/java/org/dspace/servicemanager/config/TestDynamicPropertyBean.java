/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
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
