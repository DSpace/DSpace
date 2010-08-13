/*
 * DSpaceMETSDisseminator.java
 *
 * Version: $Revision$
 *
 * Date: $Date$
 *
 * Copyright (c) 2002-2009, The DSpace Foundation.  All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are
 * met:
 *
 * - Redistributions of source code must retain the above copyright
 * notice, this list of conditions and the following disclaimer.
 *
 * - Redistributions in binary form must reproduce the above copyright
 * notice, this list of conditions and the following disclaimer in the
 * documentation and/or other materials provided with the distribution.
 *
 * - Neither the name of the DSpace Foundation nor the names of its
 * contributors may be used to endorse or promote products derived from
 * this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 * ``AS IS'' AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 * LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR
 * A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT
 * HOLDERS OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT,
 * INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING,
 * BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS
 * OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 * ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR
 * TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE
 * USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH
 * DAMAGE.
 */

package org.dspace.content.packager;

import java.io.IOException;
import java.sql.SQLException;
import java.util.Date;
import java.util.List;
import java.util.ArrayList;

import org.apache.log4j.Logger;
import org.dspace.authorize.AuthorizeException;
import org.dspace.content.Bitstream;
import org.dspace.content.Bundle;
import org.dspace.content.DSpaceObject;
import org.dspace.content.Item;
import org.dspace.core.Constants;
import org.dspace.core.ConfigurationManager;
import org.dspace.core.Context;
import org.dspace.license.CreativeCommons;

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
 * METS (Metadata Encoding & Transmission Standard) package
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
    /** log4j category */
    private static Logger log = Logger.getLogger(DSpaceMETSDisseminator.class);

    /**
     * Identifier for the package we produce, i.e. DSpace METS SIP
     * Profile.  Though not strictly true, there is no DIP standard yet
     * so it's the most meaningful label we can apply.
     */
    private final static String PROFILE_LABEL = "DSpace METS SIP Profile 1.0";

    // MDTYPE value for deposit license -- "magic string"
    // NOTE: format is  <label-for-METS>:<DSpace-crosswalk-name>
    private final static String DSPACE_DEPOSIT_LICENSE_MDTYPE =
        "DSpaceDepositLicense:DSPACE_DEPLICENSE";

    // MDTYPE value for CC license in RDF -- "magic string"
    // NOTE: format is  <label-for-METS>:<DSpace-crosswalk-name>
    private final static String CREATIVE_COMMONS_RDF_MDTYPE =
        "CreativeCommonsRDF:DSPACE_CCRDF";

    // MDTYPE value for CC license in Text -- "magic string"
    // NOTE: format is  <label-for-METS>:<DSpace-crosswalk-name>
    private final static String CREATIVE_COMMONS_TEXT_MDTYPE =
        "CreativeCommonsText:DSPACE_CCTXT";

    /**
     * Return identifier string for the profile this produces.
     *
     * @return string name of profile.
     */
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
    public String bundleToFileGrp(String bname)
    {
        if (bname.equals("ORIGINAL"))
            return "CONTENT";
        else
            return bname;
    }

    /**
     * Create metsHdr element - separate so subclasses can override.
     */
    public MetsHdr makeMetsHdr(Context context, DSpaceObject dso,
                               PackageParameters params)
    {
        MetsHdr metsHdr = new MetsHdr();
        metsHdr.setCREATEDATE(new Date()); // FIXME: CREATEDATE is now:
                                           // maybe should be item create
        // date?

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
     */
    public String [] getDmdTypes(Context context, DSpaceObject dso, PackageParameters params)
        throws SQLException, IOException, AuthorizeException
    {

    // XXX FIXME maybe let dmd choices be configured in DSpace config?

        String result[] = null;
        if (params != null)
            result = params.getProperties("dmd");
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
     */
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
            return new String[0];
    }

    public String[] getSourceMdTypes(Context context, DSpaceObject dso, PackageParameters params)
        throws SQLException, IOException, AuthorizeException
    {
        return new String[0];
    }
        
    public String[] getDigiprovMdTypes(Context context, DSpaceObject dso, PackageParameters params)
        throws SQLException, IOException, AuthorizeException
        {
        return new String[0];
    }

    public String makeBitstreamURL(Bitstream bitstream, PackageParameters params)
        {
        String base = "bitstream_"+String.valueOf(bitstream.getID());
        String ext[] = bitstream.getFormat().getExtensions();
        return (ext.length > 0) ? base+"."+ext[0] : base;
    }

    /**
     * Add rights MD (licenses) for DSpace item.  These
     * may include a deposit license, and Creative Commons.
     */
    public String[] getRightsMdTypes(Context context, DSpaceObject dso, PackageParameters params)
            throws SQLException, IOException, AuthorizeException
    {
        List<String> result = new ArrayList<String>();

        if (dso.getType() == Constants.ITEM)
        {
            Item item = (Item)dso;
            if (PackageUtils.findDepositLicense(context, item) != null)
                result.add(DSPACE_DEPOSIT_LICENSE_MDTYPE);

            if (CreativeCommons.getLicenseRdfBitstream(item) != null)
                result.add(CREATIVE_COMMONS_RDF_MDTYPE);
            else if (CreativeCommons.getLicenseTextBitstream(item) != null)
                result.add(CREATIVE_COMMONS_TEXT_MDTYPE);
            }
        return result.toArray(new String[result.size()]);
    }

    // This is where we'd elaborate on the default structMap; nothing to add, yet.
    public void addStructMap(Context context, DSpaceObject dso,
                               PackageParameters params, Mets mets)
        throws SQLException, IOException, AuthorizeException, MetsException
    {
    }

    // only exclude metadata bundles from package.
    public boolean includeBundle(Bundle bundle)
    {
        return ! PackageUtils.isMetaInfoBundle(bundle);
    }
}
