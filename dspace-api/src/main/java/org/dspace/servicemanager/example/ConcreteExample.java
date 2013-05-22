/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.servicemanager.example;

/**
 * Concrete example to fire up as a service.
 * This is fired up using XML in the case of Spring (though it does not
 * have to be).
 * 
 * @author Aaron Zeckoski (azeckoski @ gmail.com)
 */
public final class ConcreteExample {

    private String name = "azeckoski";
    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

}
