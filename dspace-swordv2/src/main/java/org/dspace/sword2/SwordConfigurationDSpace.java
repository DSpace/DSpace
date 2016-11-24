/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.sword2;

import org.apache.log4j.Logger;
import org.dspace.content.BitstreamFormat;
import org.dspace.content.Collection;
import org.dspace.content.DSpaceObject;
import org.dspace.content.Item;
import org.dspace.content.factory.ContentServiceFactory;
import org.dspace.content.service.BitstreamFormatService;
import org.dspace.core.Context;
import org.swordapp.server.SwordConfiguration;
import org.swordapp.server.SwordError;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.ArrayUtils;
import org.dspace.services.ConfigurationService;
import org.dspace.services.factory.DSpaceServicesFactory;

public class SwordConfigurationDSpace implements SwordConfiguration
{
    /** logger */
    public static final Logger log = Logger
        .getLogger(SwordConfigurationDSpace.class);

    protected BitstreamFormatService bitstreamFormatService = ContentServiceFactory
        .getInstance().getBitstreamFormatService();
    
    protected ConfigurationService configurationService = DSpaceServicesFactory
        .getInstance().getConfigurationService();

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

    private boolean allowCommunityDeposit = false;

    private boolean entryFirst = false;

    /** Accepted formats */
    private List<String> swordaccepts;

    /**
     * Initialise the sword configuration.  It is at this stage that the
     * object will interrogate the DSpace Configuration for details
     */
    public SwordConfigurationDSpace()
    {
        // set the max upload size
        int mus = configurationService
            .getIntProperty("swordv2-server.max-upload-size");
        if (mus > 0)
        {
            this.maxUploadSize = mus;
        }

        // set the mediation value
        this.mediated = configurationService
            .getBooleanProperty("swordv2-server.on-behalf-of.enable", false);

        // find out if we keep the original as bitstream
        this.keepOriginal = configurationService
            .getBooleanProperty("swordv2-server.keep-original-package");

        // get the sword bundle
        String bundle = configurationService
            .getProperty("swordv2-server.bundle.name");
        if (StringUtils.isBlank(bundle))
        {
            this.swordBundle = bundle;
        }

        // find out if we keep the package as a file in specified directory
        this.keepPackageOnFailedIngest = configurationService
            .getBooleanProperty("swordv2-server.keep-package-on-fail", false);

        // get directory path and name
        this.failedPackageDir = configurationService
            .getProperty("swordv2-server.failed-package.dir");

        // Get the accepted formats
        String[] acceptsFormats = configurationService
            .getArrayProperty("swordv2-server.accepts");
        swordaccepts = new ArrayList<String>();
        if (ArrayUtils.isEmpty(acceptsFormats))
        {
            acceptsFormats = new String[]{"application/zip"};
        }
        for (String element : acceptsFormats)
        {
            swordaccepts.add(element.trim());
        }

        // find out if community deposit is allowed
        this.allowCommunityDeposit = configurationService
            .getBooleanProperty("swordv2-server.allow-community-deposit");

        // find out if we keep the package as a file in specified directory
        this.entryFirst = configurationService
            .getBooleanProperty("swordv2-server.multipart.entry-first", false);

    }

    ////////////////////////////////////////////////////////////////////////////////////
    // Utilities
    ///////////////////////////////////////////////////////////////////////////////////

    public String getStringProperty(String propName,
            String defaultValue, String[] allowedValues)
    {
        String cfg = configurationService.getProperty(propName);   
        if (StringUtils.isBlank(cfg))
        {
            return defaultValue;
        }
        boolean allowed = false;
        if (allowedValues != null)
        {
            for (String value : allowedValues)
            {
                if (cfg.equals(value))
                {
                    allowed = true;
                }
            }
        }
        else
        {
            allowed = true;
        }
        if (allowed)
        {
            return cfg;
        }
        return defaultValue;
    }

    public String getStringProperty(String propName,
            String defaultValue)
    {
        return this.getStringProperty(propName, defaultValue, null);
    }

    ///////////////////////////////////////////////////////////////////////////////////
    // Required by the SwordConfiguration interface
    //////////////////////////////////////////////////////////////////////////////////

    public boolean returnDepositReceipt()
    {
        return true;
    }

    public boolean returnStackTraceInError()
    {
        return configurationService.getBooleanProperty("swordv2-server.verbose-description.error.enable");
    }

    public boolean returnErrorBody()
    {
        return true;
    }

    public String generator()
    {
        return this.getStringProperty("swordv2-server.generator.url",
                DSpaceUriRegistry.DSPACE_SWORD_NS);
    }

    public String generatorVersion()
    {
        return this.getStringProperty("swordv2-server.generator.version",
                "2.0");
    }

    public String administratorEmail()
    {
        return this.getStringProperty("mail.admin", null);
    }

    public String getAuthType()
    {
        return this.getStringProperty("swordv2-server.auth-type", "Basic",
            new String[] { "Basic", "None" });
    }

    public boolean storeAndCheckBinary()
    {
        return true;
    }

    public String getTempDirectory()
    {
        return this.getStringProperty("swordv2-server.upload.tempdir", null);
    }

    public String getAlternateUrl()
    {
        return configurationService
            .getProperty("swordv2-server.error.alternate.url");
    }

    public String getAlternateUrlContentType()
    {
        return configurationService
            .getProperty("swordv2-server.error.alternate.content-type");
    }

    ///////////////////////////////////////////////////////////////////////////////////
    // Required by DSpace-side implementation
    ///////////////////////////////////////////////////////////////////////////////////

    public SwordUrlManager getUrlManager(Context context,
            SwordConfigurationDSpace config)
    {
        return new SwordUrlManager(config, context);
    }

    public List<String> getDisseminatePackaging()
            throws DSpaceSwordException, SwordError
    {
        List<String> dps = new ArrayList<String>();
        List<String> packagingFormats = configurationService.getPropertyKeys("swordv2-server.disseminate-packaging");
        for (String key : packagingFormats)
        {
            String value = configurationService.getProperty(key);

            // now we want to ensure that the packaging format we offer has a disseminator
            // associated with it
            boolean disseminable = true;
            try
            {
                SwordContentDisseminator disseminator = SwordDisseminatorFactory
                        .getContentInstance(null, value);
            }
            catch (SwordError e)
            {
                disseminable = false;
            }

            if (disseminable)
            {
                dps.add(value);
            }
        }
        return dps;
    }

    public boolean isEntryFirst()
    {
        return this.entryFirst;
    }

    public boolean allowCommunityDeposit()
    {
        return this.allowCommunityDeposit;
    }

    /**
     * Get the bundle name that SWORD will store its original deposit packages in, when
     * storing them inside an item
     *
     * @return SWORD bundle name
     */
    public String getSwordBundle()
    {
        return swordBundle;
    }

    /**
     * Set the bundle name that SWORD will store its original deposit packages in, when
     * storing them inside an item
     *
     * @param swordBundle SWORD bundle name
     */
    public void setSwordBundle(String swordBundle)
    {
        this.swordBundle = swordBundle;
    }

    /**
     * Is this a verbose deposit?
     *
     * @return true if this is verbose deposit
     */
    public boolean isVerbose()
    {
        return verbose;
    }

    /**
     * Set whether this is a verbose deposit.
     *
     * @param verbose
     *    verbose deposit
     */
    public void setVerbose(boolean verbose)
    {
        this.verbose = verbose;
    }

    /**
     * What is the max upload size (in bytes) for the SWORD interface?
     *
     * @return max upload size
     */
    public int getMaxUploadSize()
    {
        return maxUploadSize;
    }

    /**
     * Set the max upload size (in bytes) for the SWORD interface.
     *
     * @param maxUploadSize
     *     max upload size to set
     */
    public void setMaxUploadSize(int maxUploadSize)
    {
        this.maxUploadSize = maxUploadSize;
    }

    /**
     * Does the server support mediated deposit (aka on-behalf-of)?
     *
     * @return true if server supports mediated deposit
     */
    public boolean isMediated()
    {
        return mediated;
    }

    /**
     * Set whether the server supports mediated deposit (aka on-behalf-of).
     *
     * @param mediated
     *     mediated deposit state to set
     */
    public void setMediated(boolean mediated)
    {
        this.mediated = mediated;
    }

    /**
     * Should the repository keep the original package?
     *
     * @return true if should keep original package
     */
    public boolean isKeepOriginal()
    {
        return keepOriginal;
    }

    /**
     * set whether the repository should keep copies of the original package
     *
     * @param keepOriginal
     *     should keep original package?
     */
    public void setKeepOriginal(boolean keepOriginal)
    {
        this.keepOriginal = keepOriginal;
    }

    /**
     * set whether the repository should write file of the original package if ingest fails
     *
     * @param keepOriginalOnFail
     *     should keep original package if ingest fails?
     */
    public void setKeepPackageOnFailedIngest(boolean keepOriginalOnFail)
    {
        keepPackageOnFailedIngest = keepOriginalOnFail;
    }

    /**
     * should the repository write file of the original package if ingest fails
     *
     * @return whether the repository should keep original package if ingest fails
     */
    public boolean isKeepPackageOnFailedIngest()
    {
        return keepPackageOnFailedIngest;
    }

    /**
     * set the directory to write file of the original package
     *
     * @param dir
     *     directory where to store the original package
     */
    public void setFailedPackageDir(String dir)
    {
        failedPackageDir = dir;
    }

    /**
     * directory location of the files with original packages
     * for failed ingests
     *
     * @return directory where to store the original package
     */
    public String getFailedPackageDir()
    {
        return failedPackageDir;
    }

    /**
     * Get the list of MIME types that the given DSpace object will
     * accept as packages.
     *
     * @param context
     *     The relevant DSpace Context.
     * @param dso
     *     DSpace object to check
     * @return list of MIME types that the given DSpace object will
     * accept as packages.
     * @throws DSpaceSwordException
     *     can be thrown by the internals of the DSpace SWORD implementation
     */
    public List<String> getAccepts(Context context, DSpaceObject dso)
            throws DSpaceSwordException
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
                // items will take any of the bitstream formats registered, plus
                // any swordaccepts mimetypes
                List<BitstreamFormat> bfs = bitstreamFormatService
                        .findNonInternal(context);
                for (BitstreamFormat bf : bfs)
                {
                    accepts.add(bf.getMIMEType());
                }
                for (String format : swordaccepts)
                {
                    if (!accepts.contains(format))
                    {
                        accepts.add(format);
                    }
                }
            }

            return accepts;
        }
        catch (SQLException e)
        {
            throw new DSpaceSwordException(e);
        }
    }

    /**
     * Get the list of mime types that a Collection will accept as packages
     *
     * @return the list of mime types
     * @throws DSpaceSwordException
     *     can be thrown by the internals of the DSpace SWORD implementation
     */
    public List<String> getCollectionAccepts() throws DSpaceSwordException
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
     * how much the server "likes" this packaging type.
     *
     * @param col
     *     target collection
     * @return map of packaging URIs to Q values for the packaging types which
     * the given collection will accept.
     */
    public List<String> getAcceptPackaging(Collection col)
    {
        // accept-packaging.METSDSpaceSIP = http://purl.org/net/sword-types/METSDSpaceSIP
        // accept-packaging.[handle].METSDSpaceSIP = http://purl.org/net/sword-types/METSDSpaceSIP
        String handle = col.getHandle();
        List<String> aps = new ArrayList<String>();

        // build the holding maps of identifiers
        String acceptPackagingPrefix = "swordv2-server.accept-packaging.collection";
        List<String> acceptFormats = configurationService.getPropertyKeys(acceptPackagingPrefix);
        for (String key : acceptFormats)
        {
            // extract the configuration into the holding Maps

            // the suffix will be [typeid] or [handle].[typeid]
            String suffix = key.substring(acceptPackagingPrefix.length()+1);

            // is there a handle which represents this collection?
            boolean withHandle = false;
            if (suffix.startsWith(handle))
            {
                withHandle = true;
            }

            // is there NO handle
            boolean general = false;
            if (suffix.indexOf(".") == -1)
            {
                // a handle would be separated from the identifier of the package type
                general = true;
            }

            if (withHandle || general)
            {
                String value = configurationService.getProperty(key);
                aps.add(value);
            }
        }

        return aps;
    }

    public List<String> getItemAcceptPackaging()
    {
        List<String> aps = new ArrayList<String>();

        // build the holding maps of identifiers
        String acceptPackagingPrefix = "swordv2-server.accept-packaging.item";
        List<String> acceptFormats = configurationService.getPropertyKeys(acceptPackagingPrefix);
        for (String key : acceptFormats)
        {
            // extract the configuration into the holding Maps
            String value = configurationService.getProperty(key);
            aps.add(value);
        }

        return aps;
    }

    /**
     * Is the given packaging/media type supported by the given DSpace
     * object?
     *
     * @param packageFormat
     *     packaging/media type to check
     * @param dso
     *     DSpace object to check
     * @return true if supported
     * @throws DSpaceSwordException
     *     can be thrown by the internals of the DSpace SWORD implementation
     * @throws SwordError
     *     SWORD error per SWORD spec
     */
    public boolean isAcceptedPackaging(String packageFormat, DSpaceObject dso)
            throws DSpaceSwordException, SwordError
    {
        if (packageFormat == null || "".equals(packageFormat))
        {
            return true;
        }

        if (dso instanceof Collection)
        {
            List<String> accepts = this.getAcceptPackaging((Collection) dso);
            for (String accept : accepts)
            {
                if (accept.equals(packageFormat))
                {
                    return true;
                }
            }
        }
        else if (dso instanceof Item)
        {
            List<String> accepts = this.getItemAcceptPackaging();
            for (String accept : accepts)
            {
                if (accept.equals(packageFormat))
                {
                    return true;
                }
            }
        }

        return false;
    }

    /**
     * Is the given content MIME type acceptable to the given DSpace object.
     *
     * @param context
     *     The relevant DSpace Context.
     * @param type
     *     MIME type to check
     * @param dso
     *     DSpace object to compare to
     * @return true if acceptable
     * @throws DSpaceSwordException
     *     can be thrown by the internals of the DSpace SWORD implementation
     *     can be thrown by the internals of the DSpace SWORD implementation
     */
    public boolean isAcceptableContentType(Context context, String type,
            DSpaceObject dso)
            throws DSpaceSwordException
    {
        List<String> accepts = this.getAccepts(context, dso);
        for (String acc : accepts)
        {
            if (this.contentTypeMatches(type, acc))
            {
                return true;
            }
        }
        return accepts.contains(type);
    }

    private boolean contentTypeMatches(String type, String pattern)
    {
        if ("*/*".equals(pattern.trim()))
        {
            return true;
        }

        // get the prefix and suffix match patterns
        String[] bits = pattern.trim().split("/");
        String prefixPattern = bits.length > 0 ? bits[0] : "*";
        String suffixPattern = bits.length > 1 ? bits[1] : "*";

        // get the incoming type prefix and suffix
        String[] tbits = type.trim().split("/");
        String typePrefix = tbits.length > 0 ? tbits[0] : "*";
        String typeSuffix = tbits.length > 1 ? tbits[1] : "*";

        boolean prefixMatch = false;
        boolean suffixMatch = false;

        if ("*".equals(prefixPattern) || prefixPattern.equals(typePrefix))
        {
            prefixMatch = true;
        }

        if ("*".equals(suffixPattern) || suffixPattern.equals(typeSuffix))
        {
            suffixMatch = true;
        }

        return prefixMatch && suffixMatch;
    }

    public String getStateUri(String state)
    {
        return configurationService
            .getProperty("swordv2-server.state." + state + ".uri");
    }

    public String getStateDescription(String state)
    {
        return configurationService
            .getProperty("swordv2-server.state." + state + ".description");
    }

    public boolean allowUnauthenticatedMediaAccess()
    {
        return false;
    }
}
