/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.content.template.generator;

import org.dspace.content.Item;
import org.dspace.content.vo.MetadataValueVO;
import org.dspace.core.Context;

/**
 * {@link TemplateValueGenerator} implementation that fills target metadata value with target item's id.
 *
 * @author Corrado Lombardi (corrado.lombardi at 4science.it)
 */
public class IdentifierValueGenerator implements TemplateValueGenerator {

    @Override
    public MetadataValueVO generator(Context context, Item targetItem, Item templateItem, String extraParams) {
        return new MetadataValueVO(targetItem.getID().toString());
    }
}
