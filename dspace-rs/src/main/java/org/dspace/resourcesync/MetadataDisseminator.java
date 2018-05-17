/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree
 */
package org.dspace.resourcesync;

import java.io.IOException;
import java.io.OutputStream;
import java.sql.SQLException;

import org.dspace.authorize.AuthorizeException;
import org.dspace.content.Item;
import org.dspace.content.crosswalk.CrosswalkException;
import org.dspace.content.crosswalk.DisseminationCrosswalk;
import org.dspace.core.Context;
import org.dspace.core.factory.CoreServiceFactory;
import org.jdom.Document;
import org.jdom.Element;
import org.jdom.output.Format;
import org.jdom.output.XMLOutputter;

/**
 * @author Richard Jones
 *
 */
public class MetadataDisseminator
{
    public static void disseminate(Context context, Item item, String formatPrefix, OutputStream os)
            throws IOException, CrosswalkException, AuthorizeException, SQLException
    {
        DisseminationCrosswalk dc = (DisseminationCrosswalk) CoreServiceFactory.getInstance().getPluginService().getNamedPlugin(DisseminationCrosswalk.class, formatPrefix);
        Element element = dc.disseminateElement(context, item);

        // serialise the element out to the zip output stream
        element.detach();
        Document doc = new Document(element);
        XMLOutputter out = new XMLOutputter(Format.getPrettyFormat());
        out.output(doc, os);
    }
}
