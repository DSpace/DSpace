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
import org.dspace.core.exception.SQLRuntimeException;
import org.dspace.eperson.EPerson;
import org.dspace.layout.CrisLayoutBox;
import org.dspace.layout.LayoutSecurity;
import org.dspace.layout.service.CrisLayoutBoxAccessService;
import org.dspace.layout.service.LayoutSecurityService;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * @author Corrado Lombardi (corrado.lombardi at 4science.it)
 */
public class CrisLayoutBoxAccessServiceImpl implements CrisLayoutBoxAccessService {

    private final LayoutSecurityService layoutSecurityService;

    @Autowired
    public CrisLayoutBoxAccessServiceImpl(LayoutSecurityService layoutSecurityService) {
        this.layoutSecurityService = layoutSecurityService;
    }

    @Override
    public boolean hasAccess(Context context, EPerson user, CrisLayoutBox box, Item item) {
        try {
            return layoutSecurityService.hasAccess(LayoutSecurity.valueOf(box.getSecurity()), context, user,
                box.getMetadataSecurityFields(), box.getGroupSecurityFields(), item);
        } catch (SQLException e) {
            throw new SQLRuntimeException(e);
        }
    }
}
