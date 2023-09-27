/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.bulkaccesscontrol.model;

import java.util.List;

import org.dspace.submit.model.AccessConditionOption;

/**
 * A collection of conditions to be met when bulk access condition.
 *
 * @author Mohamed Eskander (mohamed.eskander at 4science.it)
 */
public class BulkAccessConditionConfiguration {

    private String name;
    private List<AccessConditionOption> itemAccessConditionOptions;
    private List<AccessConditionOption> bitstreamAccessConditionOptions;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public List<AccessConditionOption> getItemAccessConditionOptions() {
        return itemAccessConditionOptions;
    }

    public void setItemAccessConditionOptions(
        List<AccessConditionOption> itemAccessConditionOptions) {
        this.itemAccessConditionOptions = itemAccessConditionOptions;
    }

    public List<AccessConditionOption> getBitstreamAccessConditionOptions() {
        return bitstreamAccessConditionOptions;
    }

    public void setBitstreamAccessConditionOptions(
        List<AccessConditionOption> bitstreamAccessConditionOptions) {
        this.bitstreamAccessConditionOptions = bitstreamAccessConditionOptions;
    }
}
