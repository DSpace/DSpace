/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.sword;

import org.dspace.content.factory.ContentServiceFactory;
import org.dspace.content.service.BitstreamFormatService;
import org.dspace.core.Context;
import org.dspace.content.Collection;
import org.dspace.content.DSpaceObject;
import org.dspace.content.Item;
import org.dspace.content.BitstreamFormat;
import org.dspace.services.ConfigurationService;
import org.dspace.services.factory.DSpaceServicesFactory;
import org.purl.sword.base.SWORDErrorException;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.sql.SQLException;

import org.apache.log4j.Logger;

/**
 * @author Richard Jones
 *
 * Class to represent the principal configurations of the sword
 * service being offered.  Not all configuration is available through
 * this class, but the most useful common options, and those with
 * default values are available
 *
 * Note that changes to values via the api will not be persisted
 * between sword requests.
 *
 * For detailed descriptions of configuration values, see the sword
 * configuration documentation
 *
 */
public class SWORDConfiguration
{

    /** logger */
    public static final Logger log = Logger.getLogger(SWORDConfiguration.class);

    protected BitstreamFormatService bitstreamFormatService = ContentServiceFactory
            .getInstance().getBitstreamFormatService();
    
    protected ConfigurationService configurationService = DSpaceServicesFactory
            .getInstance().getConfigurationService();

    /** whether we can support noOp */
    private boolean noOp = true;

    /** whether we can be verbose */
    private boolean verbose = true;

    /** what our default max upload size is */
    private int maxUploadSize = -1;

    /** do we support mediation */
    private boolean mediated = false;

    /** should we keep the original package as bitstream */
    private boolean keepOriginal = false;

    /** item bundle in which sword deposits are stored */
    private String swordBundle = "SWORD";

    /** should we keep the original package as a file on ingest error */
    private boolean keepPackageOnFailedIngest = false;

    /** location of directory to store packages on ingest error */
    private String failedPackageDir = null;

    /** Accepted formats */
    private List<String> swordaccepts;

    /**
     * Initialise the sword configuration.  It is at this stage that the
     * object will interrogate the DSpace Configuration for details
     */
    public SWORDConfiguration()
    {
        // set the max upload size
        int mus = configurationService
                .getIntProperty("sword-server.max-upload-size");
        if (mus > 0)
        {
            this.maxUploadSize = mus;
        }

        // set the mediation value
        this.mediated = configurationService
                .getBooleanProperty("sword-server.on-behalf-of.enable");

        // find out if we keep the original as bitstream
        this.keepOriginal = configurationService
                .getBooleanProperty("sword-server.keep-original-package");

        // get the sword bundle
        String bundle = configurationService
                .getProperty("sword-server.bundle.name");
        if (bundle != null && "".equals(bundle))
        {
            this.swordBundle = bundle;
        }

        // find out if we keep the package as a file in specified directory
        this.keepPackageOnFailedIngest = configurationService
                .getBooleanProperty("sword-server.keep-package-on-fail",
                        false);

        // get directory path and name
        this.failedPackageDir = configurationService
                .getProperty("sword-server.failed-package.dir");

        // Get the accepted formats
        String[] acceptsFormats = configurationService
                .getArrayProperty("sword-server.accepts");
        swordaccepts = new ArrayList<String>();
        if (acceptsFormats == null)
        {
            acceptsFormats = new String[]{"application/zip"};
        }
        for (String element : acceptsFormats)
        {
            swordaccepts.add(element.trim());
        }
    }

    /**
     * Get the bundle name that SWORD will store its original deposit
     * packages in, when storing them inside an item.
     */
    public String getSwordBundle()
    {
        return swordBundle;
    }

    /**
     * Set the bundle name that sword will store its original deposit
     * packages in, when storing them inside an item.
     * @param swordBundle
     */
    public void setSwordBundle(String swordBundle)
    {
        this.swordBundle = swordBundle;
    }

    /**
     * Is this a no-op deposit?
     */
    public boolean isNoOp()
    {
        return noOp;
    }

    /**
     * Set whether this is a no-op deposit.
     *
     * @param noOp
     */
    public void setNoOp(boolean noOp)
    {
        this.noOp = noOp;
    }

    /**
     * Is this a verbose deposit?
     */
    public boolean isVerbose()
    {
        return verbose;
    }

    /**
     * Set whether this is a verbose deposit.
     * @param verbose
     */
    public void setVerbose(boolean verbose)
    {
        this.verbose = verbose;
    }

    /**
     * What is the max upload size (in bytes) for the sword interface?
     */
    public int getMaxUploadSize()
    {
        return maxUploadSize;
    }

    /**
     * set the max uplaod size (in bytes) for the sword interface
     * @param maxUploadSize
     */
    public void setMaxUploadSize(int maxUploadSize)
    {
        this.maxUploadSize = maxUploadSize;
    }

    /**
     * Does the server support mediated deposit (aka on-behalf-of)?
     */
    public boolean isMediated()
    {
        return mediated;
    }

    /**
     * Set whether the server supports mediated deposit (aka on-behalf-of).
     * @param mediated
     */
    public void setMediated(boolean mediated)
    {
        this.mediated = mediated;
    }

    /**
     * Should the repository keep the original package?
     */
    public boolean isKeepOriginal()
    {
        return keepOriginal;
    }

    /**
     * Set whether the repository should keep copies of the original package.
     * @param keepOriginal
     */
    public void setKeepOriginal(boolean keepOriginal)
    {
        this.keepOriginal = keepOriginal;
    }

    /**
     * set whether the repository should write file of the original package if ingest fails
     * @param keepOriginalOnFail
     */
    public void setKeepPackageOnFailedIngest(boolean keepOriginalOnFail)
    {
        keepPackageOnFailedIngest = keepOriginalOnFail;
    }

    /**
     * should the repository write file of the original package if ingest fails
     * @return keepPackageOnFailedIngest
     */
    public boolean isKeepPackageOnFailedIngest()
    {
        return keepPackageOnFailedIngest;
    }

    /**
     * set the directory to write file of the original package
     * @param dir
     */
    public void setFailedPackageDir(String dir)
    {
        failedPackageDir = dir;
    }

    /**
     * directory location of the files with original packages
     * for failed ingests
     * @return failedPackageDir
     */
    public String getFailedPackageDir()
    {
        return failedPackageDir;
    }

    /**
     * Get the list of mime types that the given dspace object will
     * accept as packages.
     *
     * @param context
     * @param dso
     * @throws DSpaceSWORDException
     */
    public List<String> getAccepts(Context context, DSpaceObject dso)
            throws DSpaceSWORDException
    {
        try
        {
            List<String> accepts = new ArrayList<String>();
            if (dso instanceof Collection)
            {
                for (String format : swordaccepts)
                {
                    accepts.add(format);
                }
            }
            else if (dso instanceof Item)
            {
                List<BitstreamFormat> bfs = bitstreamFormatService
                        .findNonInternal(context);
                for (BitstreamFormat bf : bfs)
                {
                    accepts.add(bf.getMIMEType());
                }
            }

            return accepts;
        }
        catch (SQLException e)
        {
            throw new DSpaceSWORDException(e);
        }
    }

    /**
     * Get the list of mime types that a Collection will accept as packages
     *
     * @return the list of mime types
     * @throws DSpaceSWORDException
     */
    public List<String> getCollectionAccepts() throws DSpaceSWORDException
    {
        List<String> accepts = new ArrayList<String>();
        for (String format : swordaccepts)
        {
            accepts.add(format);
        }
        return accepts;
    }

    /**
     * Get a map of packaging URIs to Q values for the packaging types which
     * the given collection will accept.
     *
     * The URI should be a unique identifier for the packaging type,
     * such as:
     *
     * http://purl.org/net/sword-types/METSDSpaceSIP
     *
     * and the Q value is a floating point between 0 and 1 which defines
     * how much  the server "likes" this packaging type
     *
     * @param col
     */
    public Map<String, Float> getAcceptPackaging(Collection col)
    {
        Map<String, String> identifiers = new HashMap<String, String>();
        Map<String, String> qs = new HashMap<String, String>();
        String handle = col.getHandle();

        // build the holding maps of identifiers and q values
        String acceptPackagingPrefix = "sword-server.accept-packaging";
        List<String> keys = configurationService.getPropertyKeys(acceptPackagingPrefix);
        for (String key : keys)
        {
            // extract the configuration into the holding Maps
            String suffix = key.substring(acceptPackagingPrefix.length()+1);

            String[] bits = suffix.split("\\.");
            if (bits.length == 2)
            {
                // global settings
                String value = configurationService.getProperty(key);
                if (bits[1].equals("identifier"))
                {
                    identifiers.put(bits[0], value);
                }
                else if (bits[1].equals("q"))
                {
                    qs.put(bits[0], value);
                }
            }

            // collection settings
            if (bits.length == 3 && bits[0].equals(handle))
            {
                // this is configuration for our collection
                String value = configurationService.getProperty(key);
                if (bits[2].equals("identifier"))
                {
                    identifiers.put(bits[1], value);
                }
                else if (bits[2].equals("q"))
                {
                    qs.put(bits[1], value);
                }
            }
        }

        // merge the holding maps into the Accept Packaging settings
        Map<String, Float> ap = new HashMap<String, Float>();
        for (String ik : identifiers.keySet())
        {
            String id = identifiers.get(ik);
            String qv = qs.get(ik);
            Float qf = Float.parseFloat(qv);
            ap.put(id, qf);
        }

        return ap;
    }

    /**
     * Is the given packaging/media type supported by the given DSpace
     * object?
     *
     * @param mediaType
     * @param dso
     * @throws DSpaceSWORDException
     * @throws SWORDErrorException
     */
    public boolean isSupportedMediaType(String mediaType, DSpaceObject dso)
            throws DSpaceSWORDException, SWORDErrorException
    {
        if (mediaType == null || "".equals(mediaType))
        {
            return true;
        }

        if (dso instanceof Collection)
        {
            Map<String, Float> accepts = this
                    .getAcceptPackaging((Collection) dso);
            for (String accept : accepts.keySet())
            {
                if (accept.equals(mediaType))
                {
                    return true;
                }
            }
        }
        else if (dso instanceof Item)
        {
            // items don't unpackage, so they don't care what the media type is
            return true;
        }
        return false;
    }

    /**
     * Is the given content MIME type acceptable to the given DSpace object?
     * @param context
     * @param type
     * @param dso
     * @throws DSpaceSWORDException
     */
    public boolean isAcceptableContentType(Context context, String type,
            DSpaceObject dso)
            throws DSpaceSWORDException
    {
        List<String> accepts = this.getAccepts(context, dso);
        return accepts.contains(type);
    }

    /**
     * Get the temp directory for storing files during deposit.
     *
     * @throws DSpaceSWORDException
     */
    public String getTempDir()
            throws DSpaceSWORDException
    {
        return (configurationService.getProperty("upload.temp.dir") != null)
                ?
                configurationService.getProperty("upload.temp.dir") :
                System.getProperty("java.io.tmpdir");
    }
}
