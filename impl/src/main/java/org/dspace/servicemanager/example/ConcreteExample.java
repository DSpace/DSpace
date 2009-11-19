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

/**
 * Example of a concrete example to fire up as a service,
 * this is fired up using XML in the case of spring (though it does not have to be)
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
