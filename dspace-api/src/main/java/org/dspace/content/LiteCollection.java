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
