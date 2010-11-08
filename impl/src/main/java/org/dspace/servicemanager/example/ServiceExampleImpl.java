/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.servicemanager.example;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Required;
import org.springframework.stereotype.Service;

/**
 * Example implementation of the example service.  Spring annotations 
 * will require the injection of a ConcreteExample instance, whose name 
 * will be returned by {@link #getOtherName}.
 * 
 * @author Aaron Zeckoski (azeckoski @ gmail.com)
 */
@Service // for Spring
public final class ServiceExampleImpl implements ServiceExample {

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
