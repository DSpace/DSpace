/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * https://github.com/CILEA/dspace-cris/wiki/License
 */
package org.dspace.app.cris.ws;

import java.io.IOException;

import org.dspace.app.cris.model.ws.User;
import org.dspace.discovery.SearchServiceException;
import org.jdom.Element;

public interface IWSService
{
    public Element marshall(String query, String paginationStart,
            String paginationLimit, String[] splitProjection, String type,
            Element root, User userWS, String nameRoot, String sort, String sortOrder, String parent)
            throws SearchServiceException, IOException;
}
