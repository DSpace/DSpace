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
import org.dspace.content.Item;
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
 * OAI server.
 *
 * @author Larry Stone
 * @version $Revision: 5844 $
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

    public Namespace[] getNamespaces()
    {
        return (Namespace[]) ArrayUtils.clone(namespaces);
    }

    public String getSchemaLocation()
    {
        return schemaLocation;
    }

    public List<Element> disseminateList(DSpaceObject dso)
        throws CrosswalkException,
               IOException, SQLException, AuthorizeException
    {
        List<Element> result = new ArrayList<Element>(1);
        result.add(disseminateElement(dso));
        return result;
    }

    public Element disseminateElement(DSpaceObject dso)
        throws CrosswalkException,
               IOException, SQLException, AuthorizeException
    {
        if (dso.getType() != Constants.ITEM)
        {
            throw new CrosswalkObjectNotSupported("METSDisseminationCrosswalk can only crosswalk an Item.");
        }
        Item item = (Item)dso;

        PackageDisseminator dip = (PackageDisseminator)
          PluginManager.getNamedPlugin(PackageDisseminator.class, METS_PACKAGER_PLUGIN);
        if (dip == null)
        {
            throw new CrosswalkInternalException("Cannot find a disseminate plugin for package=" + METS_PACKAGER_PLUGIN);
        }

        try
        {
            // Set the manifestOnly=true param so we just get METS document
            PackageParameters pparams = new PackageParameters();
            pparams.put("manifestOnly", "true");

            // Create a temporary file to disseminate into
            String tempDirectory = ConfigurationManager.getProperty("upload.temp.dir");
            File tempFile = File.createTempFile("METSDissemination" + item.hashCode(), null, new File(tempDirectory));
            tempFile.deleteOnExit();

            // Disseminate METS to temp file
            Context context = new Context();
            dip.disseminate(context, item, pparams, tempFile);

            try
            {
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

    public boolean canDisseminate(DSpaceObject dso)
    {
        return true;
    }

    public boolean preferList()
    {
        return false;
    }
}
