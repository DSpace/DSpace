/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.content;

/**
 * Created with IntelliJ IDEA.
 * User: peterdietz
 * Date: 9/6/13
 * Time: 1:29 PM
 * To change this template use File | Settings | File Templates.
 */
public class LiteCollection {
    private String handle;

    private String name;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getHandle() {
        return handle;
    }

    public void setHandle(String handle) {
        this.handle = handle;
    }

}
