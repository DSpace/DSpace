package org.dspace.xmlworkflow.cristin;

import org.dspace.authorize.AuthorizeException;
import org.dspace.harvest.cristin.IngestFilter;
import org.jdom.Element;

import java.io.IOException;
import java.sql.SQLException;
import java.util.List;

/**
 * Implementation of the IngestFilter which determines whether the
 * incoming harvest record should be accepted or not
 */
public class CristinIngestFilter implements IngestFilter
{
    /**
     * Determine whether the incoming harvest should be accepted
     *
     * This always returns true
     *
     * @param elements
     * @param element
     * @return
     * @throws SQLException
     * @throws IOException
     * @throws AuthorizeException
     */
    public boolean acceptIngest(List<Element> elements, Element element)
            throws SQLException, IOException, AuthorizeException
    {
        // for the time being this lets everything through, but in the
        // future this is where we can plug in filtering features
        return true;
    }
}
