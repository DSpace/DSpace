package org.dspace.harvest.cristin;

import org.dspace.authorize.AuthorizeException;
import org.jdom.Element;

import java.io.IOException;
import java.sql.SQLException;
import java.util.List;

public interface IngestFilter
{
    public boolean acceptIngest(List<Element> descMD, Element oreREM)
            throws SQLException, IOException, AuthorizeException;;
}
