/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.content.clarin;

/**
 * This is NOT a service.
 * This class is representing a featured service link in the ref box (item view). The featured services are defined
 * in the `clarin-dspace.cfg` file.
 * This class holds the link for redirecting to the Featured Service in the another language.
 *
 * @author Milan Majchrak (milan.majchrak at dataquest.sk)
 */
public class ClarinFeaturedServiceLink {
    private String key;
    private String value;

    public String getKey() {
        return key;
    }

    public void setKey(String key) {
        this.key = key;
    }

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }
}
