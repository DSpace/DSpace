/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.content.virtual;

import java.sql.SQLException;
import java.util.List;

import org.dspace.content.Item;
import org.dspace.core.Context;

/**
 * This interface describes beans to be used for the {@link VirtualMetadataPopulator} implementation.
 * The config is located in core-services.xml whilst the actual code implementation is located in
 * {@link org.dspace.content.ItemServiceImpl}
 */
public interface VirtualMetadataConfiguration {

    /**
     * This method will return a list filled with String values which will be determine by the bean that's responsible
     * of handling the metadata fields when fully traversed through all the {@link Related} beans
     * @param context   The relevant DSpace context
     * @param item      The item that will be used to either retrieve metadata values from or to find
     *                  the related item through its relationships
     * @return The list of String values of all the metadata values as constructed by the responsible bean
     * @throws SQLException If something goes wrong
     */
    List<String> getValues(Context context, Item item) throws SQLException;

    /**
     * Generic setter for the useForPlace property
     * @param useForPlace   The boolean value that the useForPlace property will be set to
     */
    void setUseForPlace(boolean useForPlace);

    /**
     * Generic getter for the useForPlace property
     * @return  The useForPlace to be used by this bean
     */
    boolean getUseForPlace();

    /**
     * Generic setter for the populateWithNameVariant
     * This property defines whether the value should be retrieved from the left/rightward on the Relationship (true)
     * or through the configuration and usual way (false)
     * @param populateWithNameVariant   The boolean value that the populateWithNameVariant property will be set to
     */
    void setPopulateWithNameVariant(boolean populateWithNameVariant);

    /**
     * Generic getter for the populateWithNameVariant property
     * @return  The populatewithNameVariant to be used by this bean
     */
    boolean getPopulateWithNameVariant();
}
