/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.content.packager;

import java.io.IOException;
import java.io.InputStream;
import java.sql.SQLException;

import org.dspace.app.mediafilter.MediaFilter;
import org.dspace.authorize.AuthorizeException;
import org.dspace.content.Bitstream;
import org.dspace.content.Collection;
import org.dspace.content.DSpaceObject;
import org.dspace.content.Item;
import org.dspace.content.crosswalk.CrosswalkException;
import org.dspace.content.crosswalk.MetadataValidationException;
import org.dspace.core.Constants;
import org.dspace.core.Context;
import org.dspace.core.factory.CoreServiceFactory;
import org.dspace.core.service.PluginService;
import org.jdom2.Element;

/**
 * Packager plugin to ingest a
 * METS (Metadata Encoding and Transmission Standard) package
 * that conforms to the DSpace METS SIP (Submission Information Package) Profile.
 * See <a href="http://www.loc.gov/standards/mets/">http://www.loc.gov/standards/mets/</a>
 * for more information on METS, and
 * <a href="http://www.dspace.org/standards/METS/SIP/profilev0p9p1/metssipv0p9p1.pdf">
 * http://www.dspace.org/standards/METS/SIP/profilev0p9p1/metssipv0p9p1.pdf</a>
 * (or a similar file in the /standards/METS/SIP resource hierarchy)
 * for more information about the DSpace METS SIP profile.
 *
 * @author Larry Stone
 * @author Tim Donohue
 * @version $Revision$
 * @see org.dspace.content.packager.METSManifest
 * @see AbstractMETSIngester
 * @see AbstractPackageIngester
 * @see PackageIngester
 */
public class DSpaceMETSIngester
    extends AbstractMETSIngester {
    // first part of required mets@PROFILE value
    protected static final String PROFILE_START = "DSpace METS SIP Profile";

    // just check the profile name.
    @Override
    void checkManifest(METSManifest manifest)
        throws MetadataValidationException {
        String profile = manifest.getProfile();
        if (profile == null) {
            throw new MetadataValidationException("Cannot accept METS with no PROFILE attribute!");
        } else if (!profile.startsWith(PROFILE_START)) {
            throw new MetadataValidationException("METS has unacceptable PROFILE value, profile=" + profile);
        }
    }

    /**
     * Choose DMD section(s) to crosswalk.
     * <p>
     * The algorithm is:<br>
     * 1. Use whatever the <code>dmd</code> parameter specifies as the primary DMD.<br>
     * 2. If (1) is unspecified, find MODS (preferably) or DC as primary DMD.<br>
     * 3. If (1) or (2) succeeds, crosswalk it and ignore all other DMDs with
     * same GROUPID<br>
     * 4. Crosswalk remaining DMDs not eliminated already.
     *
     * @throws CrosswalkException         if crosswalk error
     * @throws PackageValidationException if validation error
     * @throws IOException                if IO error
     * @throws SQLException               if database error
     * @throws AuthorizeException         if authorization error
     */
    @Override
    public void crosswalkObjectDmd(Context context, DSpaceObject dso,
                                   METSManifest manifest,
                                   MdrefManager callback,
                                   Element dmds[], PackageParameters params)
        throws CrosswalkException, PackageValidationException,
        AuthorizeException, SQLException, IOException {
        int found = -1;

        // Check to see what dmdSec the user specified in the 'dmd' parameter
        String userDmd = null;
        if (params != null) {
            userDmd = params.getProperty("dmd");
        }
        if (userDmd != null && userDmd.length() > 0) {
            for (int i = 0; i < dmds.length; ++i) {
                if (userDmd.equalsIgnoreCase(manifest.getMdType(dmds[i]))) {
                    found = i;
                }
            }
        }

        // MODS is preferred, if nothing specified by user
        if (found == -1) {
            for (int i = 0; i < dmds.length; ++i) {
                //NOTE: METS standard actually says this should be MODS (all uppercase). But,
                // just in case, we're going to be a bit more forgiving.
                if ("MODS".equalsIgnoreCase(manifest.getMdType(dmds[i]))) {
                    found = i;
                }
            }
        }

        // DC acceptable if no MODS
        if (found == -1) {
            for (int i = 0; i < dmds.length; ++i) {
                //NOTE: METS standard actually says this should be DC (all uppercase). But,
                // just in case, we're going to be a bit more forgiving.
                if ("DC".equalsIgnoreCase(manifest.getMdType(dmds[i]))) {
                    found = i;
                }
            }
        }

        String groupID = null;
        if (found >= 0) {
            manifest.crosswalkItemDmd(context, params, dso, dmds[found], callback);
            groupID = dmds[found].getAttributeValue("GROUPID");

            if (groupID != null) {
                for (int i = 0; i < dmds.length; ++i) {
                    String g = dmds[i].getAttributeValue("GROUPID");
                    if (g != null && !g.equals(groupID)) {
                        manifest.crosswalkItemDmd(context, params, dso, dmds[i], callback);
                    }
                }
            }
        } else {
            // otherwise take the first.  Don't xwalk more than one because
            // each xwalk _adds_ metadata, and could add duplicate fields.
            if (dmds.length > 0) {
                manifest.crosswalkItemDmd(context, params, dso, dmds[0], callback);
            }
        }
    }


    /**
     * Policy:  For DSpace deposit license, take deposit license
     * supplied by explicit argument first, else use collection's
     * default deposit license.
     * For Creative Commons, look for a rightsMd containing a CC license.
     *
     * @throws PackageValidationException if validation error
     * @throws IOException                if IO error
     * @throws SQLException               if database error
     * @throws AuthorizeException         if authorization error
     */
    @Override
    public void addLicense(Context context, Item item, String license,
                           Collection collection, PackageParameters params)
        throws PackageValidationException,
        AuthorizeException, SQLException, IOException {
        if (PackageUtils.findDepositLicense(context, item) == null) {
            PackageUtils.addDepositLicense(context, license, item, collection);
        }
    }

    @Override
    public void finishObject(Context context, DSpaceObject dso,
                             PackageParameters params)
        throws PackageValidationException, CrosswalkException,
        AuthorizeException, SQLException, IOException {
        // nothing to do.
    }

    @Override
    public int getObjectType(METSManifest manifest)
        throws PackageValidationException {
        return Constants.ITEM;
    }

    // return name of derived file as if MediaFilter created it, or null
    // only needed when importing a SIP without canonical DSpace derived file naming.
    private String makeDerivedFilename(String bundleName, String origName) {
        PluginService pluginService = CoreServiceFactory.getInstance().getPluginService();

        // get the MediaFilter that would create this bundle:
        String mfNames[] = pluginService.getAllPluginNames(MediaFilter.class);

        for (int i = 0; i < mfNames.length; ++i) {
            MediaFilter mf = (MediaFilter) pluginService.getNamedPlugin(MediaFilter.class, mfNames[i]);
            if (bundleName.equals(mf.getBundleName())) {
                return mf.getFilteredName(origName);
            }
        }

        return null;
    }

    /**
     * Take a second pass over files to correct names of derived files
     * (e.g. thumbnails, extracted text) to what DSpace expects:
     *
     * @throws MetadataValidationException if validation error
     * @throws IOException                 if IO error
     * @throws SQLException                if database error
     * @throws AuthorizeException          if authorization error
     */
    @Override
    public void finishBitstream(Context context,
                                Bitstream bs,
                                Element mfile,
                                METSManifest manifest,
                                PackageParameters params)
        throws MetadataValidationException, SQLException, AuthorizeException, IOException {
        String bundleName = METSManifest.getBundleName(mfile);
        if (!bundleName.equals(Constants.CONTENT_BUNDLE_NAME)) {
            String opath = manifest.getOriginalFilePath(mfile);
            if (opath != null) {
                // String ofileId = origFile.getAttributeValue("ID");
                // Bitstream obs = (Bitstream)fileIdToBitstream.get(ofileId);

                String newName = makeDerivedFilename(bundleName, opath);

                if (newName != null) {
                    //String mfileId = mfile.getAttributeValue("ID");
                    //Bitstream bs = (Bitstream)fileIdToBitstream.get(mfileId);
                    bs.setName(context, newName);
                    bitstreamService.update(context, bs);
                }
            }
        }
    }

    @Override
    public String getConfigurationName() {
        return "dspaceSIP";
    }


    public boolean probe(Context context, InputStream in, PackageParameters params) {
        throw new UnsupportedOperationException("PDF package ingester does not implement probe()");
    }

    /**
     * Returns a user help string which should describe the
     * additional valid command-line options that this packager
     * implementation will accept when using the <code>-o</code> or
     * <code>--option</code> flags with the Packager script.
     *
     * @return a string describing additional command-line options available
     * with this packager
     */
    @Override
    public String getParameterHelp() {
        String parentHelp = super.getParameterHelp();

        //Return superclass help info, plus the extra parameter/option that this class supports
        return parentHelp +
            "\n\n" +
            "* dmd=[dmdSecType]      " +
            "Type of the METS <dmdSec> which should be used for primary item metadata (defaults to MODS, then DC)";
    }
}
