/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.resourcesync;

import org.dspace.authorize.AuthorizeException;
import org.dspace.content.Item;
import org.dspace.content.crosswalk.CrosswalkException;
import org.dspace.content.crosswalk.DisseminationCrosswalk;
import org.dspace.core.PluginManager;
import org.jdom.Document;
import org.jdom.Element;
import org.jdom.output.Format;
import org.jdom.output.XMLOutputter;

import java.io.IOException;
import java.io.OutputStream;
import java.sql.SQLException;

/**
 * @author Richard Jones
 *
 */
public class MetadataDisseminator
{
    public static void disseminate(Item item, String formatPrefix, OutputStream os)
            throws IOException, CrosswalkException, AuthorizeException, SQLException
    {
        DisseminationCrosswalk dc = (DisseminationCrosswalk) PluginManager.getNamedPlugin(DisseminationCrosswalk.class, formatPrefix);
        Element element = dc.disseminateElement(item);

        // serialise the element out to the zip output stream
        element.detach();
        Document doc = new Document(element);
        XMLOutputter out = new XMLOutputter(Format.getPrettyFormat());
        out.output(doc, os);
    }
}
