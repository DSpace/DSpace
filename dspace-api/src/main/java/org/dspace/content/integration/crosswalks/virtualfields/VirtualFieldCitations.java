/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.content.integration.crosswalks.virtualfields;

import java.util.Iterator;
import java.util.List;
import java.util.Optional;

import org.apache.commons.collections4.IteratorUtils;
import org.apache.commons.collections4.ListUtils;
import org.apache.commons.lang3.StringUtils;
import org.dspace.content.Item;
import org.dspace.content.integration.crosswalks.csl.CSLGeneratorFactory;
import org.dspace.content.integration.crosswalks.csl.CSLResult;
import org.dspace.content.integration.crosswalks.csl.DSpaceListItemDataProvider;
import org.dspace.content.integration.crosswalks.virtualfields.postprocessors.VirtualFieldCitationsPostProcessor;
import org.dspace.core.Context;
import org.dspace.discovery.configuration.DiscoveryConfigurationUtilsService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectFactory;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Implementation of {@link VirtualField} that generates the citation for the
 * given item or, if a relation name is provided, for all the publications
 * related to the given item. The structure of the virtual field is
 * virtual.citations.{format}.{style}[.{relation-name}.{post-processor}] where:
 * <ul>
 * <li>format is the citation output format (text, fo, html etc..)</li>
 * <li>style is the citation style to be applied (apa, chicago etc...)</li>
 * <li>relation-name (optional) is name of the relation to be used to find the
 * item to be formatted</li>
 * <li>post-processor (optional) is the name of the post processor to be applied
 * to the generated citations</li>
 * </ul>
 * Example:
 * <ul>
 * <li><b>virtual.citations.text.apa.researchoutputs</b> generates the citations
 * for all the publications found with the researchoutputs relation in the apa
 * style</li>
 * <li><b>virtual.citations.fo.chicago</b> generates the citation for the given
 * item in the chicago style</li>
 * </ul>
 *
 * @author Luca Giamminonni (luca.giamminonni at 4science.it)
 *
 */
public class VirtualFieldCitations implements VirtualField {

    private final static Logger LOGGER = LoggerFactory.getLogger(VirtualFieldCitations.class);

    @Autowired
    private DiscoveryConfigurationUtilsService searchConfigurationUtilsService;

    @Autowired
    private ObjectFactory<DSpaceListItemDataProvider> dSpaceListItemDataProviderObjectFactory;

    @Autowired
    private CSLGeneratorFactory cslGeneratorFactory;

    @Autowired(required = false)
    private List<VirtualFieldCitationsPostProcessor> postProcessors;

    @Override
    public String[] getMetadata(Context context, Item item, String fieldName) {

        String[] virtualFieldName = fieldName.split("\\.");
        if (virtualFieldName.length < 4 || virtualFieldName.length > 6) {
            LOGGER.warn("Invalid citations virtual field: " + fieldName);
            return new String[] {};
        }

        Iterator<Item> itemIterator = getRelationName(virtualFieldName)
            .map(relationName -> findRelatedItems(context, item, relationName))
            .orElseGet(() -> IteratorUtils.singletonIterator(item));

        CSLResult cslResult = generateCitations(context, itemIterator, virtualFieldName);

        return getPostProcessor(virtualFieldName)
            .map(processor -> processor.process(context, item, cslResult))
            .map(CSLResult::getCitationEntries)
            .orElseGet(cslResult::getCitationEntries);

    }

    private CSLResult generateCitations(Context context, Iterator<Item> itemIterator, String[] virtualFieldName) {

        DSpaceListItemDataProvider itemDataProvider = getDSpaceListItemDataProviderInstance();

        itemDataProvider.processItems(itemIterator);

        String style = getStyle(virtualFieldName);
        String format = getFormat(virtualFieldName);

        return cslGeneratorFactory.getCSLGenerator().generate(itemDataProvider, style, format);

    }

    private DSpaceListItemDataProvider getDSpaceListItemDataProviderInstance() {
        return dSpaceListItemDataProviderObjectFactory.getObject();
    }

    private String getFormat(String[] virtualFieldName) {
        return virtualFieldName[2];
    }

    private String getStyle(String[] virtualFieldName) {
        String style = virtualFieldName[3];
        if (!StringUtils.endsWith(style, ".csl")) {
            style = style + ".csl";
        }
        return style;
    }

    private Optional<String> getRelationName(String[] virtualFieldName) {

        if (virtualFieldName.length == 4) {
            return Optional.empty();
        }

        if (virtualFieldName.length == 6) {
            return Optional.of(virtualFieldName[4]);
        }

        // if the virtual field has 5 sections than the last one can be the relation
        // name or the processor name
        String lastFieldSection = virtualFieldName[4];
        if (getPostProcessorByName(lastFieldSection).isPresent()) {
            return Optional.empty();
        } else {
            return Optional.of(lastFieldSection);
        }

    }

    private Optional<VirtualFieldCitationsPostProcessor> getPostProcessor(String[] virtualFieldName) {

        if (virtualFieldName.length == 4) {
            return Optional.empty();
        }

        return getPostProcessorByName(virtualFieldName[virtualFieldName.length - 1]);
    }

    private Optional<VirtualFieldCitationsPostProcessor> getPostProcessorByName(String name) {
        return ListUtils.emptyIfNull(postProcessors).stream()
            .filter(postProcessor -> postProcessor.getName().equals(name))
            .findFirst();
    }

    private Iterator<Item> findRelatedItems(Context context, Item item, String relationName) {
        return searchConfigurationUtilsService.findByRelation(context, item, relationName);
    }

}
