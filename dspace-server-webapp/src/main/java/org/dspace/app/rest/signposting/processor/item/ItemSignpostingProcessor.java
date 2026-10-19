/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.signposting.processor.item;

import org.dspace.app.rest.signposting.processor.AbstractSignPostingProcessor;
import org.dspace.app.rest.signposting.processor.SignPostingProcessor;
import org.dspace.content.Item;
import org.dspace.core.Context;
import org.dspace.util.FrontendUrlService;

/**
 * An abstract class represents {@link SignPostingProcessor } for an item.
 *
 * @author Luigi Andrea Pascarelli (luigiandrea.pascarelli at 4science.com)
 * @author Francesco Pio Scognamiglio (francescopio.scognamiglio at 4science.com)
 */
public abstract class ItemSignpostingProcessor extends AbstractSignPostingProcessor
        implements SignPostingProcessor<Item> {

    protected final FrontendUrlService frontendUrlService;

    public ItemSignpostingProcessor(FrontendUrlService frontendUrlService) {
        this.frontendUrlService = frontendUrlService;
    }

    public String buildAnchor(Context context, Item item) {
        return frontendUrlService.generateUrl(context, item);
    }
}
