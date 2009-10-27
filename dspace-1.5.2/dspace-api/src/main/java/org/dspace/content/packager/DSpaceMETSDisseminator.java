/*
 * DSpaceMETSDisseminator.java
 *
 * Version: $Revision$
 *
 * Date: $Date$
 *
 * Copyright (c) 2002-2005, Hewlett-Packard Company and Massachusetts
 * Institute of Technology.  All rights reserved.
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
 * - Neither the name of the Hewlett-Packard Company nor the name of the
 * Massachusetts Institute of Technology nor the names of their
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
import java.io.InputStream;
import java.sql.SQLException;

import org.apache.log4j.Logger;
import org.dspace.authorize.AuthorizeException;
import org.dspace.content.Bitstream;
import org.dspace.content.BitstreamFormat;
import org.dspace.content.Bundle;
import org.dspace.content.Item;
import org.dspace.core.Constants;
import org.dspace.core.Context;
import org.dspace.license.CreativeCommons;

import edu.harvard.hul.ois.mets.AmdSec;
import edu.harvard.hul.ois.mets.BinData;
import edu.harvard.hul.ois.mets.Loctype;
import edu.harvard.hul.ois.mets.MdRef;
import edu.harvard.hul.ois.mets.MdWrap;
import edu.harvard.hul.ois.mets.Mdtype;
import edu.harvard.hul.ois.mets.Mets;
import edu.harvard.hul.ois.mets.RightsMD;
import edu.harvard.hul.ois.mets.helper.Base64;
import edu.harvard.hul.ois.mets.helper.MetsException;

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
    private final static String DSPACE_DEPOSIT_LICENSE_MDTYPE =
                                "DSpace Deposit License";

    // MDTYPE value for CC license -- "magic string"
    private final static String CREATIVE_COMMONS_LICENSE_MDTYPE =
                                "Creative Commons";

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
     * Get DMD choice for Item.  It defaults to MODS, but is overridden
     * by the package parameters if they contain any "dmd" keys.  The
     * params may contain one or more values for "dmd"; each of those is
     * the name of a crosswalk plugin, optionally followed by colon and
     * its METS MDTYPE name.
     */
    public String [] getDmdTypes(PackageParameters params)
        throws SQLException, IOException, AuthorizeException
    {

    // XXX maybe let dmd choices be configured in DSpace config too?

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
    public String getTechMdType(PackageParameters params)
        throws SQLException, IOException, AuthorizeException
    {
        return "PREMIS";
    }

    /**
     * Add rights MD (licenses) for DSpace item.  These
     * may include a deposit license, and Creative Commons.
     */
    public void addRightsMd(Context context, Item item, AmdSec amdSec)
        throws SQLException, IOException, AuthorizeException, MetsException
    {
        addDepositLicense(context, item, amdSec);
        addCreativeCommons(context, item, amdSec);
    }

    // Add deposit license, if any, as external file.
    // Give it a unique name including the SID in case there are other
    // deposit license artifacts in the Item.
    private boolean addDepositLicense(Context context, Item item, AmdSec amdSec)
        throws SQLException, IOException, AuthorizeException, MetsException
    {
        Bitstream licenseBs = findDepositLicense(context, item);

        if (licenseBs == null)
            return false;
        else
        {
            String resource = "depositlicense_"+
                              String.valueOf(licenseBs.getSequenceID())+".txt";
            addRightsStream(licenseBs.retrieve(), resource, "text/plain",
                           DSPACE_DEPOSIT_LICENSE_MDTYPE, amdSec);
            return true;
        }
    }

    // if there's a CC RDF license, chuck it in external file.
    private boolean addCreativeCommons(Context context, Item item, AmdSec amdSec)
        throws SQLException, IOException, AuthorizeException, MetsException
    {
        // License as <rightsMD><mdWrap><binData>base64encoded</binData>...
        Bitstream cc;
        
        if ((cc = CreativeCommons.getLicenseRdfBitstream(item)) != null)
        {
            addRightsStream(cc.retrieve(),
                            (gensym("creativecommons") + ".rdf"),
                            "text/rdf",
                            CREATIVE_COMMONS_LICENSE_MDTYPE, amdSec);
        }
        else if ((cc = CreativeCommons.getLicenseTextBitstream(item)) != null)
        {
            addRightsStream(cc.retrieve(),
                            (gensym("creativecommons") + ".txt"),
                            "text/plain",
                            CREATIVE_COMMONS_LICENSE_MDTYPE, amdSec);
        }
        else
            return false;
        return true;
    }

    // utility to add a stream to the METS manifest.
    // use external file and mdRef if possible, wrap and binData if not.
    private void addRightsStream(InputStream is , String resourceName,
                                 String mimeType, String mdType, AmdSec amdSec)
        throws IOException, MetsException
    {
        RightsMD rightsMD = new RightsMD();
        rightsMD.setID(gensym("rights"));
        if (extraFiles == null)
        {
            MdWrap rightsMDWrap = new MdWrap();
            rightsMDWrap.setMIMETYPE(mimeType);
            rightsMDWrap.setMDTYPE(Mdtype.OTHER);
            rightsMDWrap.setOTHERMDTYPE(mdType);
            BinData bin = new BinData();
            bin.getContent().add(new Base64(is));
            rightsMDWrap.getContent().add(bin);
            rightsMD.getContent().add(rightsMDWrap);
        }
        else
        {
            extraFiles.put(resourceName, is);
            MdRef rightsMDRef = new MdRef();
            rightsMDRef.setMIMETYPE(mimeType);
            rightsMDRef.setMDTYPE(Mdtype.OTHER);
            rightsMDRef.setOTHERMDTYPE(mdType);
            rightsMDRef.setLOCTYPE(Loctype.URL);
            rightsMDRef.setXlinkHref(resourceName);
            rightsMD.getContent().add(rightsMDRef);
        }
        amdSec.getContent().add(rightsMD);
    }

    /**
     * Utility to find the license bitstream from an item
     *
     * @param context
     *            DSpace context
     * @param item
     *            the item
     * @return the license bitstream or null
     *
     * @throws IOException
     *             if the license bitstream can't be read
     */
    private static Bitstream findDepositLicense(Context context, Item item)
            throws SQLException, IOException, AuthorizeException
    {
        // get license format ID
        int licenseFormatId = -1;
        BitstreamFormat bf = BitstreamFormat.findByShortDescription(context,
                "License");
        if (bf != null)
            licenseFormatId = bf.getID();

        Bundle[] bundles = item.getBundles(Constants.LICENSE_BUNDLE_NAME);
        for (int i = 0; i < bundles.length; i++)
        {
            // Assume license will be in its own bundle
            Bitstream[] bitstreams = bundles[i].getBitstreams();

            if (bitstreams[0].getFormat().getID() == licenseFormatId)
            {
                return bitstreams[0];
            }
        }

        // Oops! No license!
        return null;
    }

    // This is where we'd elaborate on the default structMap; nothing to add, yet.
    public void addStructMap(Context context, Item item,
                               PackageParameters params, Mets mets)
        throws SQLException, IOException, AuthorizeException, MetsException
    {
    }
}
