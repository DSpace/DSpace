package org.dspace.services.model;

/**
 * Created by IntelliJ IDEA.
 * User: kevinvandevelde
 * Date: 24-dec-2008
 * Time: 10:04:08
 * To change this template use File | Settings | File Templates.
 */
public class ObjectCount {
    private long count;
    private String value;

    public ObjectCount(){
    }

    public long getCount() {
        return count;
    }

    public void setCount(long count) {
        this.count = count;
    }

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }
}
