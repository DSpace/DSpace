/**
 * $Id: ConcreteExample.java 3887 2009-06-18 03:45:35Z mdiggory $
 * $URL: https://scm.dspace.org/svn/repo/dspace2/core/trunk/impl/src/main/java/org/dspace/servicemanager/example/ConcreteExample.java $
 * ConcreteExample.java - DSpace2 - Oct 16, 2008 12:40:47 PM - azeckoski
 **************************************************************************
 * Copyright (c) 2008 Aaron Zeckoski
 * Licensed under the Apache License, Version 2.0
 * 
 * A copy of the Apache License has been included in this 
 * distribution and is available at: http://www.apache.org/licenses/LICENSE-2.0.txt
 *
 * Aaron Zeckoski (azeckoski @ gmail.com) (aaronz @ vt.edu) (aaron @ caret.cam.ac.uk)
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
