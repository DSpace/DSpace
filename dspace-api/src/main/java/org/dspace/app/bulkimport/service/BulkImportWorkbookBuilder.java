/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.bulkimport.service;

import java.util.Iterator;
import java.util.function.BiConsumer;
import java.util.logging.Level;

import org.apache.poi.ss.usermodel.Workbook;
import org.dspace.app.bulkimport.model.EntityRow;
import org.dspace.content.Collection;
import org.dspace.content.Item;
import org.dspace.content.converter.ItemDTOConverter;
import org.dspace.content.dto.ItemDTO;
import org.dspace.core.Context;

/**
 * Service that allow to build a workbook in bulk import format from the given
 * entities.
 *
 * @author Luca Giamminonni (luca.giamminonni at 4Science)
 *
 */
public interface BulkImportWorkbookBuilder {

    /**
     * Build a workbook containing the given sources, using the given converter to
     * convert the sources to the entity rows to be included in the workbook itself.
     *
     * @param  context    the DSpace context
     * @param  collection the collection related to the sources
     * @param  sources    the sources to be placed inside the generated workbook
     * @param  converter  the converter between the source and the {@link EntityRow}
     * @return            the workbook
     */
    <T> Workbook build(Context context, Collection collection, Iterator<T> sources, ItemDTOConverter<T> converter);

    /**
     * Build a workbook containing the given items.
     *
     * @param  context    the DSpace context
     * @param  collection the collection related to the given entities
     * @param  items      the items to be placed inside the generated workbook
     * @return            the workbook
     */
    Workbook build(Context context, Collection collection, Iterator<ItemDTO> items);

    /**
     * Build a workbook containing the given items.
     *
     * @param  context    the DSpace context
     * @param  collection the collection related to the given entities
     * @param  items      the items to be placed inside the generated workbook
     * @return            the workbook
     */
    Workbook buildForItems(Context context, Collection collection, Iterator<Item> items,
                           BiConsumer<Level, String> logHandler);
}
