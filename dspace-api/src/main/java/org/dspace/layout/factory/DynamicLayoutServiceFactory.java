/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.layout.factory;

import org.dspace.layout.script.service.DynamicLayoutToolConverter;
import org.dspace.layout.script.service.DynamicLayoutToolParser;
import org.dspace.layout.script.service.DynamicLayoutToolValidator;
import org.dspace.layout.service.DynamicLayoutBoxService;
import org.dspace.layout.service.DynamicLayoutFieldService;
import org.dspace.layout.service.DynamicLayoutMetadataGroupService;
import org.dspace.layout.service.DynamicLayoutSectionService;
import org.dspace.layout.service.DynamicLayoutTabService;
import org.dspace.services.factory.DSpaceServicesFactory;

/**
 * Abstract factory for layout services.
 * 
 * @author Danilo Di Nuzzo (danilo.dinuzzo at 4science.it)
 *
 */
public abstract class DynamicLayoutServiceFactory {

    /**
     * Returns the instance.
     */
    public static DynamicLayoutServiceFactory getInstance() {
        return DSpaceServicesFactory.getInstance().getServiceManager()
                .getServiceByName("dynamicLayoutServiceFactory", DynamicLayoutServiceFactory.class);
    }

    /**
     * Returns the tab service.
     */
    public abstract DynamicLayoutTabService getTabService();

    /**
     * Returns the box service.
     */
    public abstract DynamicLayoutBoxService getBoxService();

    /**
     * Returns the field service.
     */
    public abstract DynamicLayoutFieldService getFieldService();

    /**
     * Returns the section service.
     */
    public abstract DynamicLayoutSectionService getSectionService();

    /**
     * Returns the metadata group service.
     */
    public abstract DynamicLayoutMetadataGroupService getMetadataGroupService();

    /**
     * Returns the dynamic layout tool validator.
     */
    public abstract DynamicLayoutToolValidator getDynamicLayoutToolValidator();

    /**
     * Returns the dynamic layout tool parser.
     */
    public abstract DynamicLayoutToolParser getDynamicLayoutToolParser();

    /**
     * Returns the dynamic layout tool converter.
     */
    public abstract DynamicLayoutToolConverter getDynamicLayoutToolConverter();

}
