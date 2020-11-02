/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.content.template;

import org.apache.commons.lang.StringUtils;
import org.dspace.content.Item;
import org.dspace.content.MetadataValue;
import org.dspace.core.Context;

/**
 * Simple Template item value mapper, applied when value to be looked up does not contain any placeholder value.
 * It returns the same value supplied as an input
 *
 * @author Corrado Lombardi (corrado.lombardi at 4science.it)
 */
public class SimpleTemplateItemValue implements TemplateItemValue {
    @Override
    public MetadataValue value(final Context context, final Item targetItem,
                               final Item templateItem, final MetadataValue metadataValue) {
        if (!appliesTo(metadataValue.getValue())) {
            throw new IllegalArgumentException(
                "SimpleTemplateItemValue cannot find a value for " + metadataValue.getValue());
        }
        return metadataValue;
    }

    @Override
    public boolean appliesTo(final String metadataValue) {
        final boolean placeHolder =
            StringUtils.startsWith(metadataValue, "###") && StringUtils.endsWith(metadataValue, "###");
        return !placeHolder;
    }
}
