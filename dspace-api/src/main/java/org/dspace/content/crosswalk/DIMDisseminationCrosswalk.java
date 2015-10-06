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
import org.dspace.content.*;
import org.dspace.content.factory.ContentServiceFactory;
import org.dspace.content.service.ItemService;
import org.dspace.core.Constants;
import org.dspace.core.Context;
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

    protected final ItemService itemService = ContentServiceFactory.getInstance().getItemService();

    private static final Namespace namespaces[] = { DIM_NS };

    @Override
    public Namespace[] getNamespaces()
    {
        return (Namespace[]) ArrayUtils.clone(namespaces);
    }

    /* No schema for DIM */ 
    @Override
    public String getSchemaLocation()
    {
        return DIM_NS.getURI() + " " + DIM_XSD;
    }

    
    @Override
    public Element disseminateElement(Context context, DSpaceObject dso)	throws CrosswalkException, IOException, SQLException, AuthorizeException
	{
    	if (dso.getType() != Constants.ITEM)
        {
            throw new CrosswalkObjectNotSupported("DIMDisseminationCrosswalk can only crosswalk an Item.");
        }
        Item item = (Item)dso;
        
    	List<MetadataValue> dc = itemService.getMetadata(item, Item.ANY, Item.ANY, Item.ANY, Item.ANY);
        Element dim = new Element("dim", DIM_NS);
        for (MetadataValue aDc : dc)
        {
            MetadataField metadataField = aDc.getMetadataField();
            MetadataSchema metadataSchema = metadataField.getMetadataSchema();
            Element field = new Element("field", DIM_NS);
            field.setAttribute("mdschema", metadataSchema.getName());
            field.setAttribute("element", metadataField.getElement());
            if (metadataField.getQualifier() != null) {
                field.setAttribute("qualifier", metadataField.getQualifier());
            }
            if (aDc.getLanguage() != null) {
                field.setAttribute("lang", aDc.getLanguage());
            }
            if (aDc.getValue() != null) {
                field.setText(aDc.getValue());
            }
            dim.addContent(field);
        }
        return dim;
	}
   
    @Override
    public List<Element> disseminateList(Context context, DSpaceObject dso) throws CrosswalkException, IOException, SQLException, AuthorizeException
	{
	    List<Element> result = new ArrayList<Element>(1);
	    result.add(disseminateElement(context, dso));
	    return result;
	}

    /* Only interested in disseminating items at this time */
    @Override
    public boolean canDisseminate(DSpaceObject dso)
    {
    	return (dso.getType() == Constants.ITEM);
    }

    @Override
    public boolean preferList()
    {
        return false;
    }
	
}
