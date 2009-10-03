/**
 * $Id: ServiceExampleImpl.java 3887 2009-06-18 03:45:35Z mdiggory $
 * $URL: https://scm.dspace.org/svn/repo/dspace2/core/trunk/impl/src/main/java/org/dspace/servicemanager/example/ServiceExampleImpl.java $
 * ServiceExampleImpl.java - DSpace2 - Oct 16, 2008 12:29:15 PM - azeckoski
 **************************************************************************
 * Copyright (c) 2002-2009, The Duraspace Foundation.  All rights reserved.
 * Licensed under the Duraspace Foundation License.
 * 
 * A copy of the Duraspace License has been included in this
 * distribution and is available at: http://scm.dspace.org/svn/repo/licenses/LICENSE.txt
 *
 * 
 */

package org.dspace.servicemanager.example;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Required;
import org.springframework.stereotype.Service;

/**
 * Example impl of the example service
 * 
 * @author Aaron Zeckoski (azeckoski @ gmail.com)
 */
@Service // for Spring
public class ServiceExampleImpl implements ServiceExample {

    private ConcreteExample concreteExample;
    @Autowired // Spring
    @Required // Spring
    public void setConcreteExample(ConcreteExample concreteExample) {
        this.concreteExample = concreteExample;
    }

    private String name = "aaronz";
    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getOtherName() {
        return concreteExample.getName();
    }

}
