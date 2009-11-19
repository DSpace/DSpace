/**
 * $Id$
 * $URL$
 * *************************************************************************
 * Copyright (c) 2002-2009, DuraSpace.  All rights reserved
 * Licensed under the DuraSpace License.
 *
 * A copy of the DuraSpace License has been included in this
 * distribution and is available at: http://scm.dspace.org/svn/repo/licenses/LICENSE.txt
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
