/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.content.packager;

import java.io.IOException;
import java.sql.SQLException;
import java.util.Date;
import java.util.List;
import java.util.ArrayList;

import org.dspace.authorize.AuthorizeException;
import org.dspace.content.Bundle;
import org.dspace.content.DSpaceObject;
import org.dspace.content.Item;
import org.dspace.core.Constants;
import org.dspace.core.ConfigurationManager;
import org.dspace.core.Context;

import edu.harvard.hul.ois.mets.Agent;
import edu.harvard.hul.ois.mets.Mets;
import edu.harvard.hul.ois.mets.MetsHdr;
import edu.harvard.hul.ois.mets.Role;
import edu.harvard.hul.ois.mets.helper.MetsException;
import edu.harvard.hul.ois.mets.Type;
import edu.harvard.hul.ois.mets.Name;
import edu.harvard.hul.ois.mets.helper.PCData;

/**
 * Packager plugin to produce a
 * METS (Metadata Encoding and Transmission Standard) package
 * that is accepted as a DSpace METS SIP (Submission Information Package).
 * See <a href="http://www.loc.gov/standards/mets/">http://www.loc.gov/standards/mets/</a>
 * for more information on METS.
 * <p>
 * This class does not produce a true DSpace DIP, because there was no
 * DIP standard when it was implemented.  It does contain some features
 * beyond the requirements of a SIP (e.g. deposit licenses), anticipating
 * the DIP specification.
 * <p>
 * DSpaceMETSDisseminator was intended to be an useful example of a packager
 * plugin, and a way to create packages acceptable to the METS SIP importer.
 *
 * @author Larry Stone
 * @version $Revision$
 */
public class DSpaceMETSDisseminator
    extends AbstractMETSDisseminator
{
    /**
     * Identifier for the package we produce, i.e. DSpace METS SIP
     * Profile.  Though not strictly true, there is no DIP standard yet
     * so it's the most meaningful label we can apply.
     */
    protected static final String PROFILE_LABEL = "DSpace METS SIP Profile 1.0";

    // MDTYPE value for deposit license -- "magic string"
    // NOTE: format is  <label-for-METS>:<DSpace-crosswalk-name>
    protected static final String DSPACE_DEPOSIT_LICENSE_MDTYPE = "DSpaceDepositLicense:DSPACE_DEPLICENSE";

    // MDTYPE value for CC license in RDF -- "magic string"
    // NOTE: format is  <label-for-METS>:<DSpace-crosswalk-name>
    protected static final String CREATIVE_COMMONS_RDF_MDTYPE = "CreativeCommonsRDF:DSPACE_CCRDF";

    // MDTYPE value for CC license in Text -- "magic string"
    // NOTE: format is  <label-for-METS>:<DSpace-crosswalk-name>
    protected static final String CREATIVE_COMMONS_TEXT_MDTYPE = "CreativeCommonsText:DSPACE_CCTXT";

    /**
     * Return identifier string for the profile this produces.
     *
     * @return string name of profile.
     */
    @Override
    public String getProfile()
    {
        return PROFILE_LABEL;
    }

    /**
     * Returns name of METS fileGrp corresponding to a DSpace bundle name.
     * They are mostly the same except for bundle "ORIGINAL" maps to "CONTENT".
     * Don't worry about the metadata bundles since they are not
     * packaged as fileGrps, but in *mdSecs.
     * @param bname name of DSpace bundle.
     * @return string name of fileGrp
     */
    @Override
    public String bundleToFileGrp(String bname)
    {
        if (bname.equals("ORIGINAL"))
        {
            return "CONTENT";
        }
        else
        {
            return bname;
        }
    }

    /**
     * Create metsHdr element - separate so subclasses can override.
     * @return mets header
     */
    @Override
    public MetsHdr makeMetsHdr(Context context, DSpaceObject dso,
                               PackageParameters params)
    {
        MetsHdr metsHdr = new MetsHdr();
        
        // FIXME: CREATEDATE is now: maybe should be item create?
        metsHdr.setCREATEDATE(new Date());

        // Agent
        Agent agent = new Agent();
        agent.setROLE(Role.CUSTODIAN);
        agent.setTYPE(Type.ORGANIZATION);
        Name name = new Name();
        name.getContent()
                .add(new PCData(ConfigurationManager
                                .getProperty("dspace.name")));
        agent.getContent().add(name);
        metsHdr.getContent().add(agent);
        return metsHdr;
    }


    /**
     * Get DMD choice for Item.  It defaults to MODS, but is overridden
     * by the package parameters if they contain any "dmd" keys.  The
     * params may contain one or more values for "dmd"; each of those is
     * the name of a crosswalk plugin, optionally followed by colon and
     * its METS MDTYPE name.
     * @return array of DMD types
     * @throws IOException if IO error
     * @throws SQLException if database error
     * @throws AuthorizeException if authorization error
     */
    @Override
    public String [] getDmdTypes(Context context, DSpaceObject dso, PackageParameters params)
        throws SQLException, IOException, AuthorizeException
    {

    // XXX FIXME maybe let dmd choices be configured in DSpace config?

        String result[] = null;
        if (params != null)
        {
            result = params.getProperties("dmd");
        }
        if (result == null || result.length == 0)
        {
            result = new String[1];
            result[0] = "MODS";
        }
        return result;
    }

    /**
     * Get name of technical metadata crosswalk for Bitstreams.
     * Default is PREMIS.  This is both the name of the crosswalk plugin
     * and the METS MDTYPE.
     * @return array of TechMD types
     * @throws IOException if IO error
     * @throws SQLException if database error
     * @throws AuthorizeException if authorization error 
     */
    @Override
    public String[] getTechMdTypes(Context context, DSpaceObject dso, PackageParameters params)
        throws SQLException, IOException, AuthorizeException
    {
        if (dso.getType() == Constants.BITSTREAM)
    {
            String result[] = new String[1];
            result[0] = "PREMIS";
            return result;
    }
        else
        {
            return new String[0];
        }
    }

    @Override
    public String[] getSourceMdTypes(Context context, DSpaceObject dso, PackageParameters params)
        throws SQLException, IOException, AuthorizeException
    {
        return new String[0];
    }

    @Override
    public String[] getDigiprovMdTypes(Context context, DSpaceObject dso, PackageParameters params)
        throws SQLException, IOException, AuthorizeException
        {
        return new String[0];
    }

    /**
     * Add rights MD (licenses) for DSpace item.  These
     * may include a deposit license, and Creative Commons.
     * @return array of RightsMD types
     * @throws IOException if IO error
     * @throws SQLException if database error
     * @throws AuthorizeException if authorization error
     */
    @Override
    public String[] getRightsMdTypes(Context context, DSpaceObject dso, PackageParameters params)
            throws SQLException, IOException, AuthorizeException
    {
        List<String> result = new ArrayList<String>();

        if (dso.getType() == Constants.ITEM)
        {
            Item item = (Item)dso;
            if (PackageUtils.findDepositLicense(context, item) != null)
            {
                result.add(DSPACE_DEPOSIT_LICENSE_MDTYPE);
            }

            if (creativeCommonsService.getLicenseRdfBitstream(item) != null)
            {
                result.add(CREATIVE_COMMONS_RDF_MDTYPE);
            }
            else if (creativeCommonsService.getLicenseTextBitstream(item) != null)
            {
                result.add(CREATIVE_COMMONS_TEXT_MDTYPE);
            }
        }
        
        return result.toArray(new String[result.size()]);
    }

    // This is where we'd elaborate on the default structMap; nothing to add, yet.
    @Override
    public void addStructMap(Context context, DSpaceObject dso,
                               PackageParameters params, Mets mets)
        throws SQLException, IOException, AuthorizeException, MetsException
    {
    }

    // only exclude metadata bundles from package.
    @Override
    public boolean includeBundle(Bundle bundle)
    {
        return ! PackageUtils.isMetaInfoBundle(bundle);
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
    public String getParameterHelp()
    {
        String parentHelp = super.getParameterHelp();

        //Return superclass help info, plus the extra parameter/option that this class supports
        return parentHelp +
                "\n\n" +
                "* dmd=[dmdSecType]      " +
                   "(Repeatable) Type(s) of the METS <dmdSec> which should be created in the dissemination package (defaults to MODS)";
    }
}
