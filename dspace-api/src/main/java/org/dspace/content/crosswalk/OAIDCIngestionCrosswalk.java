/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.content.crosswalk;

import java.io.IOException;
import java.sql.SQLException;
import java.util.List;

import org.dspace.authorize.AuthorizeException;
import org.dspace.content.DSpaceObject;
import org.dspace.content.Item;
import org.dspace.core.Constants;
import org.dspace.core.Context;
import org.jdom.Element;
import org.jdom.Namespace;

/**
 * DIM ingestion crosswalk
 * <p>
 * Processes Dublic Core metadata encased in an oai_dc:dc wrapper
 *
 * @author Alexey Maslov
 * @version $Revision: 1 $
 */
public class OAIDCIngestionCrosswalk
    implements IngestionCrosswalk
{
    private static final Namespace DC_NS = Namespace.getNamespace("http://www.dspace.org/xmlns/dspace/dim");
    private static final Namespace OAI_DC_NS = Namespace.getNamespace("http://www.openarchives.org/OAI/2.0/oai_dc/");
    
	public void ingest(Context context, DSpaceObject dso, List<Element> metadata) throws CrosswalkException, IOException, SQLException, AuthorizeException {
        Element wrapper = new Element("wrap", metadata.get(0).getNamespace());
		wrapper.addContent(metadata);
		
		ingest(context,dso,wrapper);
	}

	public void ingest(Context context, DSpaceObject dso, Element root) throws CrosswalkException, IOException, SQLException, AuthorizeException {
		
		if (dso.getType() != Constants.ITEM)
        {
            throw new CrosswalkObjectNotSupported("DIMIngestionCrosswalk can only crosswalk an Item.");
        }
        Item item = (Item)dso;
        
        if (root == null) {
        	System.err.println("The element received by ingest was null");
        	return;
        }
        
        List<Element> metadata = root.getChildren();
        for (Element element : metadata) {
		// get language - prefer xml:lang, accept lang.
		String lang = element.getAttributeValue("lang", Namespace.XML_NAMESPACE);
		if (lang == null) {
			lang = element.getAttributeValue("lang");
		}
		item.addMetadata("dc", element.getName(), null, lang, element.getText());
        }
        
	}
	
}
