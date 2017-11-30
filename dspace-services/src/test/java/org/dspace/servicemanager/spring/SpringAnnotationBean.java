/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.servicemanager.spring;

import org.dspace.servicemanager.example.ConcreteExample;
import org.dspace.servicemanager.example.ServiceExample;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Required;
import org.springframework.stereotype.Service;

/**
 * This bean is a bean which is annotated as a spring bean and should be found when the AC starts up
 * 
 * @author Aaron Zeckoski (azeckoski @ gmail.com)
 */
@Service
public class SpringAnnotationBean {

    private ServiceExample serviceExample;
    @Autowired
    @Required
    public void setServiceExample(ServiceExample serviceExample) {
        this.serviceExample = serviceExample;
    }

    private ConcreteExample concreteExample;
    @Autowired
    @Required
    public void setConcreteExample(ConcreteExample concreteExample) {
        this.concreteExample = concreteExample;
    }

    public String getExampleName() {
        return serviceExample.getName();
    }

    public String getOtherName() {
        return serviceExample.getOtherName();
    }

    public String getConcreteName() {
        return concreteExample.getName();
    }

    private String value = null;
    public void setTestName(String testName) {
        value = testName;
    }

    public String getSampleValue() {
        return value;
    }

}
