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

    public static DynamicLayoutServiceFactory getInstance() {
        return DSpaceServicesFactory.getInstance().getServiceManager()
                .getServiceByName("dynamicLayoutServiceFactory", DynamicLayoutServiceFactory.class);
    }

    public abstract DynamicLayoutTabService getTabService();

    public abstract DynamicLayoutBoxService getBoxService();

    public abstract DynamicLayoutFieldService getFieldService();

    public abstract DynamicLayoutSectionService getSectionService();

    public abstract DynamicLayoutMetadataGroupService getMetadataGroupService();

    public abstract DynamicLayoutToolValidator getDynamicLayoutToolValidator();

    public abstract DynamicLayoutToolParser getDynamicLayoutToolParser();

    public abstract DynamicLayoutToolConverter getDynamicLayoutToolConverter();

}
