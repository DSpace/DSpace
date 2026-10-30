/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.layout.factory.impl;

import org.dspace.layout.factory.DynamicLayoutServiceFactory;
import org.dspace.layout.script.service.DynamicLayoutToolConverter;
import org.dspace.layout.script.service.DynamicLayoutToolParser;
import org.dspace.layout.script.service.DynamicLayoutToolValidator;
import org.dspace.layout.service.DynamicLayoutBoxService;
import org.dspace.layout.service.DynamicLayoutFieldService;
import org.dspace.layout.service.DynamicLayoutMetadataGroupService;
import org.dspace.layout.service.DynamicLayoutSectionService;
import org.dspace.layout.service.DynamicLayoutTabService;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Implementation of factory for layout services.
 * 
 * @author Danilo Di Nuzzo (danilo.dinuzzo at 4science.it)
 *
 */
public class DynamicLayoutServiceFactoryImpl extends DynamicLayoutServiceFactory {

    @Autowired(required = true)
    private DynamicLayoutTabService tabService;

    @Autowired(required = true)
    private DynamicLayoutBoxService boxService;

    @Autowired(required = true)
    private DynamicLayoutFieldService fieldService;

    @Autowired(required = true)
    private DynamicLayoutMetadataGroupService metadataGroupService;

    @Autowired(required = true)
    private DynamicLayoutSectionService sectionService;

    @Autowired(required = true)
    private DynamicLayoutToolValidator validator;

    @Autowired(required = true)
    private DynamicLayoutToolParser parser;

    @Autowired(required = true)
    private DynamicLayoutToolConverter converter;

    @Override
    public DynamicLayoutTabService getTabService() {
        return this.tabService;
    }

    @Override
    public DynamicLayoutBoxService getBoxService() {
        return this.boxService;
    }

    @Override
    public DynamicLayoutFieldService getFieldService() {
        return this.fieldService;
    }

    @Override
    public DynamicLayoutMetadataGroupService getMetadataGroupService() {
        return this.metadataGroupService;
    }

    @Override
    public DynamicLayoutSectionService getSectionService() {
        return sectionService;
    }

    @Override
    public DynamicLayoutToolValidator getDynamicLayoutToolValidator() {
        return validator;
    }

    @Override
    public DynamicLayoutToolParser getDynamicLayoutToolParser() {
        return parser;
    }

    @Override
    public DynamicLayoutToolConverter getDynamicLayoutToolConverter() {
        return converter;
    }

}
