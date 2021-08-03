/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.content.template;

import java.util.Arrays;
import java.util.List;

import org.apache.commons.collections.CollectionUtils;
import org.dspace.content.Item;
import org.dspace.content.MetadataValue;
import org.dspace.content.vo.MetadataValueVO;
import org.dspace.core.Context;

/**
 * Service that finds the right implementation of {@link TemplateItemValue} and
 * and supplies correct actual value for a given template item metadata value.
 *
 * @author Corrado Lombardi (corrado.lombardi at 4science.it)
 */
public class TemplateItemValueService {


    private final List<TemplateItemValue> templateItemValues;

    public TemplateItemValueService(
        final List<TemplateItemValue> templateItemValues) {
        this.templateItemValues = templateItemValues;
    }

    public List<MetadataValueVO> value(final Context context, final Item targetItem,
                               final Item templateItem,
                               final MetadataValue metadataValue) {
        if (CollectionUtils.isEmpty(templateItemValues)) {
            throw new UnsupportedOperationException(
                "No TemplateItemValue list defined to lookup value for " + metadataValue);
        }
        return templateItemValues.stream().filter(tiv -> tiv.appliesTo(metadataValue.getValue()))
                                 .findFirst()
                                 .map(tiv -> tiv.values(context, targetItem, templateItem, metadataValue))
                                 .orElseGet(() -> Arrays.asList(new MetadataValueVO(metadataValue)));
    }


}
