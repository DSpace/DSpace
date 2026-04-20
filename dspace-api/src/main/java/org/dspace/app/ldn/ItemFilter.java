/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.ldn;

/**
 * model class for the item filters configured into item-filters.xml
 *
 * @author Mohamed Eskander (mohamed.eskander at 4science.com)
 */
public class ItemFilter {

    private String id;

    public ItemFilter(String id) {
        this.id = id;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }
}
