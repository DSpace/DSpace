/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.content.virtual;

import java.util.LinkedList;
import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.dspace.content.Item;
import org.dspace.content.MetadataValue;
import org.dspace.content.service.ItemService;
import org.dspace.core.Context;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * A bean implementing the {@link VirtualMetadataConfiguration} interface to achieve the generation of Virtual
 * metadata
 * The Collected bean will take all the values of each metadata field defined in the list and it'll
 * create a list of virtual metadata fields defined by the map in which it's defined.
 * All values from the metadata fields will returned as separate elements
 */
public class Collected implements VirtualMetadataConfiguration {

    @Autowired
    private ItemService itemService;

    /**
     * This property determines whether this RelationshipMetadataValue should be used in place calculation or not.
     * This is retrieved from Spring configuration when constructing RelationshipMetadataValues. This Spring
     * configuration is located in the core-services.xml configuration file.
     * Putting this property on true will imply that we're now mixing plain-text metadatavalues with the
     * metadatavalues that are constructed through Relationships with regards to the place attribute.
     * For example, currently the RelationshipMetadataValue dc.contributor.author that is constructed through a
     * Relationship for a Publication will have its useForPlace set to true. This means that the place
     * calculation will take both these RelationshipMetadataValues into account together with the normal
     * plain text metadatavalues.
     */
    private boolean useForPlace;
    /**
     * The fields for which the metadata will be retrieved
     */
    private List<String> fields;

    /**
     * Generic getter for the fields property
     * @return The list of fields to be used in this bean
     */
    public List<String> getFields() {
        return fields;
    }

    /**
     * Generic setter for the fields property
     * @param fields    the list of fields to which the fields property will be set to
     */
    public void setFields(List<String> fields) {
        this.fields = fields;
    }
    /**
     * Generic setter for the useForPlace property
     * @param useForPlace   The boolean value that the useForPlace property will be set to
     */
    @Override
    public void setUseForPlace(boolean useForPlace) {
        this.useForPlace = useForPlace;
    }

    /**
     * Generic getter for the useForPlace property
     * @return  The useForPlace to be used by this bean
     */
    @Override
    public boolean getUseForPlace() {
        return useForPlace;
    }

    @Override
    public void setPopulateWithNameVariant(boolean populateWithNameVariant) { }

    @Override
    public boolean getPopulateWithNameVariant() {
        return false;
    }

    /**
     * this method will retrieve the metadata values from the given item for all the metadata fields listed
     * in the fields property and it'll return all those values as a list
     * @param context   The relevant DSpace context
     * @param item      The item that will be used to either retrieve metadata values from
     * @return The String values for all of the retrieved metadatavalues
     */
    public List<String> getValues(Context context, Item item) {
        List<String> resultValues = new LinkedList<>();
        List<String> value = this.getFields();
        for (String s : value) {
            String[] splittedString = s.split("\\.");

            List<MetadataValue> resultList = itemService.getMetadata(item,
                                                                     splittedString.length > 0 ? splittedString[0] :
                                                                         null,
                                                                     splittedString.length > 1 ? splittedString[1] :
                                                                         null,
                                                                     splittedString.length > 2 ? splittedString[2] :
                                                                         null,
                                                                     Item.ANY, false);

            for (MetadataValue metadataValue : resultList) {
                if (StringUtils.isNotBlank(metadataValue.getValue())) {
                    resultValues.add(metadataValue.getValue());
                }
            }
        }

        return resultValues;
    }
}
