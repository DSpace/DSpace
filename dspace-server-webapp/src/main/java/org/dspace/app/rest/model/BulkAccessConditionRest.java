/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.model;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import com.fasterxml.jackson.annotation.JsonIgnore;
import org.dspace.app.rest.RestResourceController;

/**
 * The Bulk Access Condition Configuration REST Resource
 *
 * @author Mohamed Eskander (mohamed.eskander at 4science.it)
 */
public class BulkAccessConditionRest extends BaseObjectRest<String> {

    private static final long serialVersionUID = -7708437586052984082L;

    public static final String NAME = "bulkaccessconditionoption";
    public static final String PLURAL = "bulkaccessconditionoptions";
    public static final String CATEGORY = RestAddressableModel.CONFIGURATION;

    private String id;

    private List<AccessConditionOptionRest> itemAccessConditionOptions;

    private List<AccessConditionOptionRest> bitstreamAccessConditionOptions;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public List<AccessConditionOptionRest> getItemAccessConditionOptions() {
        if (Objects.isNull(itemAccessConditionOptions)) {
            itemAccessConditionOptions = new ArrayList<>();
        }
        return itemAccessConditionOptions;
    }

    public void setItemAccessConditionOptions(
        List<AccessConditionOptionRest> itemAccessConditionOptions) {
        this.itemAccessConditionOptions = itemAccessConditionOptions;
    }

    public List<AccessConditionOptionRest> getBitstreamAccessConditionOptions() {
        if (Objects.isNull(bitstreamAccessConditionOptions)) {
            bitstreamAccessConditionOptions = new ArrayList<>();
        }
        return bitstreamAccessConditionOptions;
    }

    public void setBitstreamAccessConditionOptions(
        List<AccessConditionOptionRest> bitstreamAccessConditionOptions) {
        this.bitstreamAccessConditionOptions = bitstreamAccessConditionOptions;
    }

    @Override
    public String getType() {
        return NAME;
    }

    @Override
    public String getCategory() {
        return CATEGORY;
    }

    @Override
    @JsonIgnore
    @SuppressWarnings("rawtypes")
    public Class getController() {
        return RestResourceController.class;
    }

}