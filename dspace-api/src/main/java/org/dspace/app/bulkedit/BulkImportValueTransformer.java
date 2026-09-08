/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.bulkedit;
import org.dspace.content.vo.MetadataValueVO;
import org.dspace.core.Context;

/**
 * Strategy interface for transforming a single metadata value while it is being
 * read from a bulk import file, before it is applied to the target item.
 * <p>
 * Implementations are registered per metadata field in
 * {@link BulkImportTransformerService}: when a value is imported for a field that
 * has an associated transformer, the service delegates to
 * {@link #transform(Context, MetadataValueVO)} and uses the returned value in place
 * of the original one. This allows custom, field-specific manipulation of the
 * imported values (for example normalising a value or resolving an authority).
 * </p>
 *
 * @author Mykhaylo Boychuk (mykhaylo.boychuk at 4science.it)
 */
public interface BulkImportValueTransformer {

    /**
     * Transforms the given metadata value read from the bulk import file.
     * <p>
     * Since {@link MetadataValueVO} is immutable, implementations that need to change
     * the value (its text, authority, confidence or security level) must return a new
     * instance; they may also return the given value unchanged when no transformation
     * applies.
     * </p>
     *
     * @param context       the current DSpace context
     * @param metadataValue the metadata value parsed from the import file
     * @return the transformed metadata value to apply to the item
     */
    MetadataValueVO transform(Context context, MetadataValueVO metadataValue);

}