/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.content.template;

import java.util.List;

import org.dspace.content.Item;
import org.dspace.content.MetadataValue;
import org.dspace.content.vo.MetadataValueVO;
import org.dspace.core.Context;

/**
 * Defines the contract for the mapping between a template item metadata value and the actual value that has to be
 * set in the item created starting from the template.
 *
 * A template item metadata value can be:
 *
 * <li>a simple value</li>
 * <li>a special value, in this case a particular mapping is applied</li>
 *
 * @author Corrado Lombardi (corrado.lombardi at 4science.it)
 */
public interface TemplateItemValue {
    /**
     * Given a template item metadata value, returns the actual value to be set in target metadata.
     *
     *
     * @param context
     * @param targetItem
     * @param templateItem
     * @param metadataValue
     * @return
     */
    List<MetadataValueVO> values(final Context context, final Item targetItem,
                        final Item templateItem, final MetadataValue metadataValue);

    /**
     * Returns {@code true} if for input metadataValue this implementation of {@code TemplateItemValue} is to be
     * applied,
     * false otherwise.
     *
     * @param metadataValue
     * @return
     */
    boolean appliesTo(final String metadataValue);
}
