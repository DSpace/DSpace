/**
 * $Id: SpringAnnotationBean.java 3211 2008-10-16 13:22:12Z azeckoski $
 * $URL: https://scm.dspace.org/svn/repo/dspace2/core/trunk/impl/src/test/java/org/dspace/servicemanager/spring/SpringAnnotationBean.java $
 * SampleAnnotationBean.java - DSpace2 - Oct 5, 2008 11:25:47 PM - azeckoski
 **************************************************************************
 * Copyright (c) 2002-2009, The Duraspace Foundation.  All rights reserved.
 * Licensed under the Duraspace Foundation License.
 * 
 * A copy of the Duraspace License has been included in this
 * distribution and is available at: http://scm.dspace.org/svn/repo/licenses/LICENSE.txt
 *
 * 
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
