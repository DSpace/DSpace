/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.signposting.service;

import java.util.List;
import javax.servlet.http.HttpServletRequest;

import org.dspace.app.rest.signposting.model.LinksetNode;
import org.dspace.content.DSpaceObject;
import org.dspace.content.Item;
import org.dspace.core.Context;

/**
 * Service for work with linksets.
 */
public interface LinksetService {

    /**
     * Returns list of linkset nodes for multiple linksets.
     *
     * @param request request
     * @param context context
     * @param item    item
     * @return two-dimensional list representing a list of lists where each list represents the linkset nodes.
     */
    List<List<LinksetNode>> createLinksetNodesForMultipleLinksets(
            HttpServletRequest request,
            Context context,
            Item item
    );

    /**
     * Returns list of linkset nodes for single linkset.
     *
     * @param request request
     * @param context context
     * @param object  dspace object
     * @return two-dimensional list representing a list of lists where each list represents the linkset nodes.
     */
    List<LinksetNode> createLinksetNodesForSingleLinkset(
            HttpServletRequest request,
            Context context,
            DSpaceObject object
    );
}
