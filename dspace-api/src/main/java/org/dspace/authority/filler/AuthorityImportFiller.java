/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.authority.filler;

import java.sql.SQLException;
import java.util.List;

import org.dspace.content.Item;
import org.dspace.content.MetadataValue;
import org.dspace.content.dto.MetadataValueDTO;
import org.dspace.core.Context;

/**
 * Interface for classes that enrich the item related entities created during an
 * item submission using additional logic.
 *
 * @author Luca Giamminonni (luca.giamminonni at 4science.it)
 * @author Giuseppe Digilio (giuseppe.digilio at 4science.it)
 *
 */
public interface AuthorityImportFiller {

    /**
     * Returns true if the given item could be updated using the source metadata.
     *
     * @param context        the context
     * @param sourceMetadata the metadata related to the item to fill
     * @param itemToFill     the item to fill
     * @return true if the given item could be filled, false otherwise
     */
    boolean allowsUpdate(Context context, MetadataValue sourceMetadata, Item itemToFill);

    /**
     * Fill the given item using the source metadata.
     *
     * @param context        the context
     * @param sourceMetadata the metadata related to the item to fill
     * @param itemToFill     the item to fill
     * @throws SQLException if an error occurs during the metadata addition
     */
    void fillItem(Context context, MetadataValue sourceMetadata, Item itemToFill) throws SQLException;

    /**
     * Retrieves a list of metadata values derived from a related item and its
     * corresponding reference metadata.
     * <p>
     * This method is used to "flatten" or "extract" information from a relationship.
     * It uses the {@code metadata} parameter to identify the specific link and the
     * {@code relatedItem} as the source of the data to be returned.
     * </p>
     *
     * @param context     The DSpace context.
     * @param relatedItem The item that acts as the source of the information.
     * @param metadata    The metadata value that represents the relationship or
     * the pointer to the related item.
     * @return A list of {@link MetadataValueDTO} objects representing the extracted
     * data. May return an empty list if no metadata should be extracted.
     */
    List<MetadataValueDTO> getMetadataListByRelatedItemAndMetadata(Context context, Item relatedItem,
                                                                   MetadataValue metadata);
}
