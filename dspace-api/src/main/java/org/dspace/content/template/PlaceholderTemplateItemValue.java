/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.content.template;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.apache.commons.lang3.StringUtils;
import org.dspace.content.Item;
import org.dspace.content.MetadataValue;
import org.dspace.content.template.generator.TemplateValueGenerator;
import org.dspace.content.vo.MetadataValueVO;
import org.dspace.core.Context;

/**
 * Implementation of {@link TemplateItemValue} that handles a special value, indicated as placeholder with syntax
 * '###generator.custom_values###'
 * where 'generator' can be:
 * <li>DATE</li>
 * <li>SUBMITTER</li>
 * <li>IDENTIFIER</li>
 * <li>GROUP</li>
 * <li>EPERSON</li>
 *
 * Actual value is returned by proper {@link TemplateValueGenerator} implementation provided for types in above list.
 *
 * @author Corrado Lombardi (corrado.lombardi at 4science.it)
 */
public class PlaceholderTemplateItemValue implements TemplateItemValue {
    private final Map<String, TemplateValueGenerator> generators;

    public PlaceholderTemplateItemValue(
        final Map<String, TemplateValueGenerator> generators) {
        this.generators = generators;
    }

    @Override
    public List<MetadataValueVO> values(final Context context, final Item targetItem,
                               final Item templateItem, final MetadataValue metadataValue) {
        if (!appliesTo(metadataValue.getValue())) {
            throw new IllegalArgumentException("Metadata value " + metadataValue.getValue() +
                                                   " does not contain any placeholder");
        }
        String[] splitted =
            metadataValue.getValue().substring(3, metadataValue.getValue().length() - 3).split("\\.", 2);

        return Optional.ofNullable(generators.get(splitted[0].toLowerCase()))
                       .map(g -> generateValue(context, targetItem, templateItem, metadataValue, splitted, g))
                       .orElseThrow(() -> new RuntimeException("Unable to find a generator for " + splitted[0]));

    }

    private List<MetadataValueVO> generateValue(final Context context, final Item targetItem, final Item templateItem,
                                        final MetadataValue metadataValue, final String[] splitted,
                                        final TemplateValueGenerator gen) {
        String extraParams = null;
        if (splitted.length == 2) {
            extraParams = splitted[1];
        }
        return gen.generator(context, targetItem, templateItem, extraParams);
    }

    @Override
    public boolean appliesTo(final String metadataValue) {
        return StringUtils.startsWith(metadataValue, "###") && StringUtils.endsWith(metadataValue, "###");
    }
}
