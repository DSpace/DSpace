/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.harvest.cristin;

import org.dspace.authorize.AuthorizeException;
import org.jdom.Element;

import java.io.IOException;
import java.sql.SQLException;
import java.util.List;

public class DefaultIngestFilter implements IngestFilter
{
    @Override
    public boolean acceptIngest(List<Element> descMD, Element oreREM)
            throws SQLException, IOException, AuthorizeException
    {
        return true;
    }
}
