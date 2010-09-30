/**
 * Copyright (c) 2002-2010, DuraSpace.  All rights reserved
 * Licensed under the DuraSpace License.
 *
 * A copy of the DuraSpace License has been included in this
 * distribution and is available at: http://www.dspace.org/license
 */
package org.dspace.servicemanager.example;

/**
 * Example of a concrete example to fire up as a service.
 * This is fired up using XML in the case of Spring (though it does not
 * have to be).
 * 
 * @author Aaron Zeckoski (azeckoski @ gmail.com)
 */
public class ConcreteExample {

    private String name = "azeckoski";
    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

}
