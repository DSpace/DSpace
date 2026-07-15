/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.layout.service.impl;

import java.sql.SQLException;

import org.dspace.content.Item;
import org.dspace.core.Context;
import org.dspace.eperson.EPerson;
import org.dspace.layout.DynamicLayoutTab;
import org.dspace.layout.LayoutSecurity;
import org.dspace.layout.service.DynamicLayoutTabAccessService;
import org.dspace.layout.service.LayoutSecurityService;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * @author Corrado Lombardi (corrado.lombardi at 4science.it)
 */
public class DynamicLayoutTabAccessServiceImpl implements DynamicLayoutTabAccessService {

    private final LayoutSecurityService layoutSecurityService;

    /**
     * Creates the service with the given layout security service.
     *
     * @param layoutSecurityService the layout security service
     */
    @Autowired
    public DynamicLayoutTabAccessServiceImpl(LayoutSecurityService layoutSecurityService) {
        this.layoutSecurityService = layoutSecurityService;
    }

    @Override
    public boolean hasAccess(Context context, EPerson user, DynamicLayoutTab tab, Item item) {
        try {
            return layoutSecurityService.hasAccess(
                LayoutSecurity.valueOf(tab.getSecurity()), context, user, tab.getMetadataSecurityFields(),
                tab.getGroupSecurityFields(), item
            );
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }
}
