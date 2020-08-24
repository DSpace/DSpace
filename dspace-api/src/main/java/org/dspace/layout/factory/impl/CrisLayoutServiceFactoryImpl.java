/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.layout.factory.impl;

import org.dspace.layout.factory.CrisLayoutServiceFactory;
import org.dspace.layout.service.CrisLayoutBoxService;
import org.dspace.layout.service.CrisLayoutFieldService;
import org.dspace.layout.service.CrisLayoutSectionService;
import org.dspace.layout.service.CrisLayoutTabService;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Implementation of factory for layout services.
 * 
 * @author Danilo Di Nuzzo (danilo.dinuzzo at 4science.it)
 *
 */
public class CrisLayoutServiceFactoryImpl extends CrisLayoutServiceFactory {

    @Autowired(required = true)
    private CrisLayoutTabService tabService;

    @Autowired(required = true)
    private CrisLayoutBoxService boxService;

    @Autowired(required = true)
    private CrisLayoutFieldService fieldService;

    @Autowired(required = true)
    private CrisLayoutSectionService sectionService;

    @Override
    public CrisLayoutTabService getTabService() {
        return this.tabService;
    }

    @Override
    public CrisLayoutBoxService getBoxService() {
        return this.boxService;
    }

    @Override
    public CrisLayoutFieldService getFieldService() {
        return this.fieldService;
    }

    @Override
    public CrisLayoutSectionService getSectionService() {
        return sectionService;
    }

}
