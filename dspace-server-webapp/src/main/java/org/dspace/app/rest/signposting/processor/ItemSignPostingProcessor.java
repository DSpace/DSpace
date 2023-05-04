/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.signposting.processor;

import java.sql.SQLException;

import org.dspace.content.Item;
import org.dspace.core.Context;
import org.dspace.handle.factory.HandleServiceFactory;
import org.dspace.handle.service.HandleService;

/**
 * ItemSignPostingProcessor interface represents SignPostingProcessor for an item.
 * 
 * @author Luigi Andrea Pascarelli (luigiandrea.pascarelli at 4science.com)
 * @author Francesco Pio Scognamiglio (francescopio.scognamiglio at 4science.com)
 */
public interface ItemSignPostingProcessor extends SignPostingProcessor<Item> {

    default String buildAnchor(Context context, Item item) throws SQLException {
        HandleService handleService =
                HandleServiceFactory.getInstance().getHandleService();
        return handleService.resolveToURL(context, item.getHandle());
    }
}
