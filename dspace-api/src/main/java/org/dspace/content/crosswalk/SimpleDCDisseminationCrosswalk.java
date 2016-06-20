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
import org.dspace.core.SelfNamedPlugin;
import org.jdom.Element;
import org.jdom.Namespace;

/**
 * Disseminator for Simple Dublin Core metadata in XML format.
 * Logic stolen from OAIDCCrosswalk.  This is mainly intended
 * as a proof-of-concept, to use crosswalk plugins in the OAI-PMH
 * server.
 *
 * @author Larry Stone
 * @version $Revision$
 */
public class SimpleDCDisseminationCrosswalk extends SelfNamedPlugin
    implements DisseminationCrosswalk
{
    // namespaces of interest.

    // XXX FIXME: may also want http://www.openarchives.org/OAI/2.0/oai_dc/  for OAI

    private static final Namespace DC_NS =
        Namespace.getNamespace("dc", "http://purl.org/dc/elements/1.1/");

    // simple DC schema for OAI
    private static final String DC_XSD =
                "http://dublincore.org/schemas/xmls/simpledc20021212.xsd";
                //"http://www.openarchives.org/OAI/2.0/oai_dc.xsd";

    private static final String schemaLocation =
        DC_NS.getURI()+" "+DC_XSD;

    private static final Namespace namespaces[] =
        { DC_NS, XSI_NS };

    private static final String aliases[] = { "SimpleDC", "DC" };
    protected final ItemService itemService = ContentServiceFactory.getInstance().getItemService();
    

    public static String[] getPluginNames()
    {
        return (String[]) ArrayUtils.clone(aliases);
    }

    @Override
    public Element disseminateElement(Context context, DSpaceObject dso)
        throws CrosswalkException,
               IOException, SQLException, AuthorizeException
    {
        Element root = new Element("simpledc", DC_NS);
        root.setAttribute("schemaLocation", schemaLocation, XSI_NS);
        root.addContent(disseminateListInternal(dso, false));
        return root;
    }

    /**
     * Returns object's metadata as XML elements.
     * Simple-minded copying of elements: convert contributor.author to
     * "creator" but otherwise just grab element name without qualifier.
     * @param context context
     * @throws CrosswalkException if crosswalk error
     * @throws IOException if IO error
     * @throws SQLException if database error
     * @throws AuthorizeException if authorization error
     */
    @Override
    public List<Element> disseminateList(Context context, DSpaceObject dso)
        throws CrosswalkException,
               IOException, SQLException, AuthorizeException
    {
        return disseminateListInternal(dso, true);
    }

    public List<Element> disseminateListInternal(DSpaceObject dso, boolean addSchema)
        throws CrosswalkException,
               IOException, SQLException, AuthorizeException
    {
        if (dso.getType() != Constants.ITEM)
        {
            throw new CrosswalkObjectNotSupported("SimpleDCDisseminationCrosswalk can only crosswalk an Item.");
        }

        Item item = (Item)dso;
        List<MetadataValue> allDC = itemService.getMetadata(item, MetadataSchema.DC_SCHEMA, Item.ANY, Item.ANY, Item.ANY);

        List<Element> dcl = new ArrayList<Element>(allDC.size());

        for (MetadataValue metadataValue : allDC) {
            // Do not include description.provenance
            MetadataField metadataField = metadataValue.getMetadataField();
            if (!(metadataField.getElement().equals("description") &&
                    (metadataField.getQualifier() != null && metadataField.getQualifier().equals("provenance")))) {
                String element;

                // contributor.author exposed as 'creator'
                if (metadataField.getElement().equals("contributor")
                        && (metadataField.getQualifier() != null)
                        && metadataField.getQualifier().equals("author")) {
                    element = "creator";
                } else {
                    element = metadataField.getElement();
                }
                Element field = new Element(element, DC_NS);
                field.addContent(metadataValue.getValue());
                if (addSchema) {
                    field.setAttribute("schemaLocation", schemaLocation, XSI_NS);
                }
                dcl.add(field);
            }
        }
        return dcl;
    }

    @Override
    public Namespace[] getNamespaces()
    {
        return (Namespace[]) ArrayUtils.clone(namespaces);
    }

    @Override
    public String getSchemaLocation()
    {
        return schemaLocation;
    }

    @Override
    public boolean canDisseminate(DSpaceObject dso)
    {
        return dso.getType() == Constants.ITEM;
    }

    @Override
    public boolean preferList()
    {
        return true;
    }
}
