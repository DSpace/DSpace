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
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang.ArrayUtils;
import org.dspace.authorize.AuthorizeException;
import org.dspace.content.Metadatum;
import org.dspace.content.DSpaceObject;
import org.dspace.content.Item;
import org.dspace.core.Constants;
import org.jdom.Element;
import org.jdom.Namespace;

/**
 * DIM dissemination crosswalk
 * <p>
 * Produces the metadata encoded in DSpace Intermediate Format, without the overhead of XSLT processing.
 *
 * @author Alexey Maslov
 * @version $Revision: 1 $
 */
public class DIMDisseminationCrosswalk
    implements DisseminationCrosswalk
{
    // Non-existant XSD schema
    public static final String DIM_XSD = "null";
    
    // Namespaces 
    public static final Namespace DIM_NS =
        Namespace.getNamespace("dim", "http://www.dspace.org/xmlns/dspace/dim");

    private static final Namespace namespaces[] = { DIM_NS };

    public Namespace[] getNamespaces()
    {
        return (Namespace[]) ArrayUtils.clone(namespaces);
    }

    /* No schema for DIM */ 
    public String getSchemaLocation()
    {
        return DIM_NS.getURI() + " " + DIM_XSD;
    }

    
    public Element disseminateElement(DSpaceObject dso)	throws CrosswalkException, IOException, SQLException, AuthorizeException 
	{
    	if (dso.getType() != Constants.ITEM)
        {
            throw new CrosswalkObjectNotSupported("DIMDisseminationCrosswalk can only crosswalk an Item.");
        }
        Item item = (Item)dso;
        
    	Metadatum[] dc = item.getMetadata(Item.ANY, Item.ANY, Item.ANY, Item.ANY);
        Element dim = new Element("dim", DIM_NS);
        for (int i = 0; i < dc.length; i++)
        {
            Element field = new Element("field", DIM_NS);
            field.setAttribute("mdschema", dc[i].schema);
            field.setAttribute("element", dc[i].element);
            if (dc[i].qualifier != null)
            {
                field.setAttribute("qualifier", dc[i].qualifier);
            }
            if (dc[i].language != null)
            {
                field.setAttribute("lang", dc[i].language);
            }
            if (dc[i].value != null)
            {
                field.setText(dc[i].value);
            }
            dim.addContent(field);
        }
        return dim;
	}
   
    public List<Element> disseminateList(DSpaceObject dso) throws CrosswalkException, IOException, SQLException, AuthorizeException
	{
	    List<Element> result = new ArrayList<Element>(1);
	    result.add(disseminateElement(dso));
	    return result;
	}

    /* Only interested in disseminating items at this time */
    public boolean canDisseminate(DSpaceObject dso)
    {
    	return (dso.getType() == Constants.ITEM);
    }

    public boolean preferList()
    {
        return false;
    }
	
}
