package org.dspace.content.virtual;

import java.sql.SQLException;

import org.dspace.content.Item;
import org.dspace.core.Context;

/**
 * This interface describes beans to be used for the {@link VirtualMetadataPopulator} implementation.
 * The config is located in core-services.xml whilst the actual code implementation is located in
 * {@link org.dspace.content.ItemServiceImpl}
 */
public interface VirtualBean {

    /**
     * This method will return the String value for the metadata field that is configured in the
     * {@link Concatenate} bean in the config and it will traverse all the {@link Related} beans
     * in the config that are chained together until it finds a {@link Concatenate} bean for which
     * the value can be retrieved.
     * @param context   The relevant DSpace context
     * @param item      The item that will be used to either retrieve metadata values from or to find
     *                  the related item through its relationships
     * @return The String value of all the metadata values of all fields defined in {@link Concatenate}
     *                  bean which will be concatenated with a seperator that's defined in the same bean
     * @throws SQLException If something goes wrong
     */
    String getValue(Context context, Item item) throws SQLException;
}
