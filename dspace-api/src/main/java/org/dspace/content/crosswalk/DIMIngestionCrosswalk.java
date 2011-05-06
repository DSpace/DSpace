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
 * Processes metadata encoded in DSpace Intermediate Format, without the overhead of XSLT processing.
 *
 * @author Alexey Maslov
 * @version $Revision: 1 $
 */
public class DIMIngestionCrosswalk
    implements IngestionCrosswalk
{
    private static final Namespace DIM_NS = Namespace.getNamespace("http://www.dspace.org/xmlns/dspace/dim");

	public void ingest(Context context, DSpaceObject dso, List<Element> metadata) throws CrosswalkException, IOException, SQLException, AuthorizeException {
        Element first = metadata.get(0);
	    if (first.getName().equals("dim") && metadata.size() == 1) {
            ingest(context,dso,first);
	    }
	    else if (first.getName().equals("field") && first.getParentElement() != null) {
            ingest(context,dso,first.getParentElement());
	    }
	    else {
            Element wrapper = new Element("wrap", metadata.get(0).getNamespace());
            wrapper.addContent(metadata);
            ingest(context,dso,wrapper);
	    }
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
        
        List<Element> metadata = root.getChildren("field",DIM_NS);
        for (Element field : metadata) {
        	item.addMetadata(field.getAttributeValue("mdschema"), field.getAttributeValue("element"), field.getAttributeValue("qualifier"), 
        			field.getAttributeValue("lang"), field.getText());
        }
        
	}
	
}
