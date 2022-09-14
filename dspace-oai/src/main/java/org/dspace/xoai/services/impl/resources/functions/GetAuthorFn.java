/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */

package org.dspace.xoai.services.impl.resources.functions;

import org.dspace.utils.SpecialItemService;
import org.w3c.dom.Node;

/**
 * Serves as proxy for call from XSL engine. Calls SpecialItemService.
 */
public class GetAuthorFn extends NodeXslFunction {
    @Override
    protected String getFnName() {
        return "getAuthor";
    }

    @Override
    protected Node getNode(String param) {
        return SpecialItemService.getAuthor(param);
    }
}
