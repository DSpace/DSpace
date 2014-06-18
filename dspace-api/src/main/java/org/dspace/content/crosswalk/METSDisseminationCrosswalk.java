/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.content.crosswalk;

import java.io.File;
import java.io.IOException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang.ArrayUtils;
import org.dspace.authorize.AuthorizeException;
import org.dspace.content.DSpaceObject;
import org.dspace.content.packager.PackageDisseminator;
import org.dspace.content.packager.PackageException;
import org.dspace.content.packager.PackageParameters;
import org.dspace.core.ConfigurationManager;
import org.dspace.core.Constants;
import org.dspace.core.Context;
import org.dspace.core.PluginManager;
import org.jdom.Document;
import org.jdom.Element;
import org.jdom.JDOMException;
import org.jdom.Namespace;
import org.jdom.input.SAXBuilder;

/**
 * METS dissemination crosswalk
 * <p>
 * Produces a METS manifest for the DSpace item as a metadata
 * description -- intended to work within an application like the
 * OAI-PMH server.
 *
 * @author Larry Stone
 * @version $Revision$
 */
public class METSDisseminationCrosswalk
    implements DisseminationCrosswalk
{
    // Plugin Name of METS packager to use for manifest;
    // maybe make  this configurable.
    private static final String METS_PACKAGER_PLUGIN = "METS";

    /**
     * MODS namespace.
     */
    public static final Namespace MODS_NS =
        Namespace.getNamespace("mods", "http://www.loc.gov/mods/v3");

    private static final Namespace XLINK_NS =
        Namespace.getNamespace("xlink", "http://www.w3.org/TR/xlink");


    /** METS namespace -- includes "mets" prefix for use in XPaths */
    private static final Namespace METS_NS = Namespace
            .getNamespace("mets", "http://www.loc.gov/METS/");

    private static final Namespace namespaces[] = { METS_NS, MODS_NS, XLINK_NS };

    /**  URL of METS XML Schema */
    private static final String METS_XSD = "http://www.loc.gov/standards/mets/mets.xsd";

    private static final String schemaLocation =
        METS_NS.getURI()+" "+METS_XSD;

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
    public List<Element> disseminateList(DSpaceObject dso)
        throws CrosswalkException,
               IOException, SQLException, AuthorizeException
    {
        List<Element> result = new ArrayList<Element>(1);
        result.add(disseminateElement(dso));
        return result;
    }

    @Override
    public Element disseminateElement(DSpaceObject dso)
        throws CrosswalkException,
               IOException, SQLException, AuthorizeException
    {
        if (!canDisseminate(dso))
        {
            throw new CrosswalkObjectNotSupported("METSDisseminationCrosswalk cannot disseminate a DSpaceObject of type: " + Constants.typeText[dso.getType()]);
        }
        
        PackageDisseminator dip = (PackageDisseminator)
          PluginManager.getNamedPlugin(PackageDisseminator.class, METS_PACKAGER_PLUGIN);
        if (dip == null)
        {
            throw new CrosswalkInternalException("Cannot find a disseminate plugin for package=" + METS_PACKAGER_PLUGIN);
        }

        try
        {
            // Set the manifestOnly=true param so we just get METS document (and not content files, etc)
            PackageParameters pparams = new PackageParameters();
            pparams.put("manifestOnly", "true");

            // Create a temporary file to disseminate into
            String tempDirectory = (ConfigurationManager.getProperty("upload.temp.dir") != null)
                ? ConfigurationManager.getProperty("upload.temp.dir") : System.getProperty("java.io.tmpdir"); 

            File tempFile = File.createTempFile("METSDissemination" + dso.hashCode(), null, new File(tempDirectory));
            tempFile.deleteOnExit();

            // Disseminate METS to temp file
            Context context = new Context();
            dip.disseminate(context, dso, pparams, tempFile);
            context.complete();

            try
            {
                //Return just the root Element of the METS file
                SAXBuilder builder = new SAXBuilder();
                Document metsDocument = builder.build(tempFile);
                return metsDocument.getRootElement();
            }
            catch (JDOMException je)
            {
                throw new MetadataValidationException("Error parsing METS (see wrapped error message for more details) ",je);
            }
        }
        catch (PackageException pe)
        {
            throw new CrosswalkInternalException("Failed making METS manifest in packager (see wrapped error message for more details) ",pe);
        }
    }

    @Override
    public boolean canDisseminate(DSpaceObject dso)
    {
        //can disseminate most types of DSpaceObjects (Site, Community, Collection, Item)
        if(dso.getType()==Constants.SITE || 
           dso.getType()==Constants.COMMUNITY ||
           dso.getType()==Constants.COLLECTION ||
           dso.getType()==Constants.ITEM)
        {    
            return true;
        }
        else
            return false;
    }

    @Override
    public boolean preferList()
    {
        return false;
    }
}
