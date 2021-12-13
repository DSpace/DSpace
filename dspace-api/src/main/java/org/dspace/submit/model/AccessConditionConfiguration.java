/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.submit.model;
import java.util.List;

/**
* @author Mykhaylo Boychuk (mykhaylo.boychuk at 4science.com)
*/
public class AccessConditionConfiguration {

    private String name;
    private Boolean discoverable;
    private List<AccessConditionOption> options;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Boolean getDiscoverable() {
        return discoverable;
    }

    public void setDiscoverable(Boolean discoverable) {
        this.discoverable = discoverable;
    }

    public List<AccessConditionOption> getOptions() {
        return options;
    }

    public void setOptions(List<AccessConditionOption> options) {
        this.options = options;
    }

}