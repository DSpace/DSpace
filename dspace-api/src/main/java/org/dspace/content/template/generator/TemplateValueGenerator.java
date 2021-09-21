/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.content.template.generator;

import java.util.List;

import org.dspace.content.Item;
import org.dspace.content.vo.MetadataValueVO;
import org.dspace.core.Context;

/**
 * Defines the contract to generate custom metadata values in a dynamic fashion.
 *
 * @author Corrado Lombardi (corrado.lombardi at 4science.it)
 */
public interface TemplateValueGenerator {

    /**
     * Generate a metadata value and authority starting from the given items and
     * params.
     *
     * @param  context      the DSpace context
     * @param  targetItem   the target item
     * @param  templateItem the template item
     * @param  extraParams  other params that could be usefull to the generator
     * @return              a list of {@link MetadataValueVO} with the
     *                      generated metadata value and authority
     */
    List<MetadataValueVO> generator(Context context, Item targetItem, Item templateItem, String extraParams);
}
