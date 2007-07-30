/*
 * PREMISCrosswalk.java
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

package org.dspace.content.crosswalk;

import java.io.IOException;
import java.net.URLEncoder;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.apache.log4j.Logger;
import org.dspace.authorize.AuthorizeException;
import org.dspace.content.Bitstream;
import org.dspace.content.BitstreamFormat;
import org.dspace.content.Bundle;
import org.dspace.content.DSpaceObject;
import org.dspace.content.FormatIdentifier;
import org.dspace.content.Item;
import org.dspace.core.ConfigurationManager;
import org.dspace.core.Constants;
import org.dspace.core.Context;
import org.jdom.Element;
import org.jdom.Namespace;

/**
 * PREMIS Crosswalk
 * <p>
 * Translate between DSpace Bitstream properties and PREMIS metadata format
 * (see <a href="http://www.oclc.org/research/projects/pmwg/">
 * http://www.oclc.org/research/projects/pmwg/</a> for details).
 * This is intended to implement the requirements of the DSpace METS SIP
 * specification for both ingest and dissemination.
 *
 * @author Larry Stone
 * @version $Revision$
 */
public class PREMISCrosswalk
    implements IngestionCrosswalk, DisseminationCrosswalk
{
    /** log4j category */
    private static Logger log = Logger.getLogger(PREMISCrosswalk.class);

    private static final Namespace PREMIS_NS =
        Namespace.getNamespace("premis", "http://www.loc.gov/standards/premis");

    // XML schemaLocation fragment for this crosswalk, from config.
    private String schemaLocation =
        PREMIS_NS.getURI()+" http://www.loc.gov/standards/premis/PREMIS-v1-0.xsd";

    private static final Namespace XLINK_NS =
        Namespace.getNamespace("xlink", "http://www.w3.org/TR/xlink");

    private static final Namespace namespaces[] = { PREMIS_NS };

    /*----------- Submission functions -------------------*/

    public void ingest(Context context, DSpaceObject dso, Element root)
        throws CrosswalkException, IOException, SQLException, AuthorizeException
    {
        if (!(root.getName().equals("premis")))
            throw new MetadataValidationException("Wrong root element for PREMIS: "+root.toString());
        ingest(context, dso, root.getChildren());
    }

    public void ingest(Context context, DSpaceObject dso, List ml)
        throws CrosswalkException, IOException, SQLException, AuthorizeException
    {
        // we only understand how to crosswalk PREMIS to a Bitstream.
        if (dso.getType() != Constants.BITSTREAM)
            throw new CrosswalkObjectNotSupported("Wrong target object type, PREMISCrosswalk can only crosswalk to a Bitstream.");

        Bitstream bitstream = (Bitstream)dso;
        String MIMEType = null;
        String bsName = null;
        Iterator mi = ml.iterator();
        while (mi.hasNext())
        {
            Element me = (Element)mi.next();

            // if we're fed a <premis> wrapper object, recurse on its guts:
            if (me.getName().equals("premis"))
                ingest(context, dso, me.getChildren());

            // "object" section:
            else if (me.getName().equals("object"))
            {
                // originalName becomes new bitstream source and (default) name
                Element on = me.getChild("originalName", PREMIS_NS);
                if (on != null)
                    bsName = on.getTextTrim();

                // Reconcile technical metadata with bitstream content;
                // check that length and message digest (checksum) match.
                // XXX FIXME: wait for Checksum Checker code to add better test.
                Element oc = me.getChild("objectCharacteristics", PREMIS_NS);
                if (oc != null)
                {
                    String ssize = oc.getChildTextTrim("size", PREMIS_NS);
                    if (ssize != null)
                    {
                        try
                        {
                            int size = Integer.parseInt(ssize);
                            if (bitstream.getSize() != size)
                                throw new MetadataValidationException(
                                 "Bitstream size ("+String.valueOf(bitstream.getSize())+
                                 ") does not match size in PREMIS ("+ssize+"), rejecting it.");
                        }
                        catch (NumberFormatException ne)
                        {
                            throw new MetadataValidationException("Bad number value in PREMIS object/objectCharacteristics/size: "+ssize, ne);
                        }
                    }
                    Element fixity = oc.getChild("fixity", PREMIS_NS);
                    if (fixity != null)
                    {
                        String alg = fixity.getChildTextTrim("messageDigestAlgorithm", PREMIS_NS);
                        String md = fixity.getChildTextTrim("messageDigest", PREMIS_NS);
                        String b_alg = bitstream.getChecksumAlgorithm();
                        String b_md = bitstream.getChecksum();
                        if (alg != null && md != null &&
                            b_alg != null && b_md != null &&
                            alg.equals(b_alg))
                        {
                            if (md.equals(b_md))
                                log.debug("Bitstream checksum agrees with PREMIS: "+bitstream.getName());
                            else
                                throw new MetadataValidationException("Bitstream "+alg+" Checksum does not match value in PREMIS ("+b_md+" != "+md+"), for bitstream: "+bitstream.getName());
                        }
                        else
                            log.warn("Cannot test checksum on bitstream="+bitstream.getName()+
                                     ", algorithm in PREMIS is different: "+alg);
                    }

                    // Look for formatDesignation/formatName, which is
                    // MIME Type.  Match with DSpace bitstream format.
                    Element format = oc.getChild("format", PREMIS_NS);
                    if (format != null)
                    {
                        Element fd = format.getChild("formatDesignation", PREMIS_NS);
                        if (fd != null)
                            MIMEType = fd.getChildTextTrim("formatName", PREMIS_NS);
                    }
                }

                // Apply new bitstream name if we found it.
                if (bsName != null)
                {
                    bitstream.setName(bsName);
                    log.debug("Changing bitstream id="+String.valueOf(bitstream.getID())+"name and source to: "+bsName);
                }

                // reconcile bitstream format; if there's a MIMEtype,
                // get it from that, otherwise try to divine from file extension
                // (guessFormat() looks at bitstream Name, which we just set)
                BitstreamFormat bf = (MIMEType == null) ? null :
                        BitstreamFormat.findByMIMEType(context, MIMEType);
                if (bf == null)
                    bf = FormatIdentifier.guessFormat(context, bitstream);
                if (bf != null)
                    bitstream.setFormat(bf);
            }
            else
                log.debug("Skipping element: "+me.toString());
        }
        bitstream.update();
    }

    /*----------- Dissemination functions -------------------*/

    public Namespace[] getNamespaces()
    {
        return namespaces;
    }

    public String getSchemaLocation()
    {
        return schemaLocation;
    }

    public boolean canDisseminate(DSpaceObject dso)
    {
        return true;
    }

    public Element disseminateElement(DSpaceObject dso)
        throws CrosswalkException,
               IOException, SQLException, AuthorizeException
    {
        if (dso.getType() != Constants.BITSTREAM)
            throw new CrosswalkObjectNotSupported("PREMISCrosswalk can only crosswalk a Bitstream.");
        Bitstream bitstream = (Bitstream)dso;

        Element premis = new Element("premis", PREMIS_NS);
        Element object = new Element("object", PREMIS_NS);
        premis.addContent(object);

        // objectIdentifier is required
        Element oid = new Element("objectIdentifier", PREMIS_NS);
        Element oit = new Element("objectIdentifierType", PREMIS_NS);
        oit.setText("URL");
        oid.addContent(oit);
        Element oiv = new Element("objectIdentifierValue", PREMIS_NS);

        // objectIdentifier value: by preference, if available:
        //  a. DSpace "persistent" URL to bitstream, if components available.
        //  b. name of bitstream, if any
        //  c. made-up name based on sequence ID and extension.
        String sid = String.valueOf(bitstream.getSequenceID());
        String baseUrl = ConfigurationManager.getProperty("dspace.url");
        String handle = null;
        // get handle of parent Item of this bitstream, if there is one:
        Bundle[] bn = bitstream.getBundles();
        if (bn.length > 0)
        {
            Item bi[] = bn[0].getItems();
            if (bi.length > 0)
                handle = bi[0].getHandle();
        }
        // get or make up name for bitstream:
        String bsName = bitstream.getName();
        if (bsName == null)
        {
            String ext[] = bitstream.getFormat().getExtensions();
            bsName = "bitstream_"+sid+ (ext.length > 0 ? ext[0] : "");
        }
        if (handle != null && baseUrl != null)
            oiv.setText(baseUrl
                    + "/bitstream/"
                    + URLEncoder.encode(handle, "UTF-8")
                    + "/"
                    + sid
                    + "/"
                    + URLEncoder.encode(bsName, "UTF-8"));
        else
            oiv.setText(URLEncoder.encode(bsName, "UTF-8"));

        oid.addContent(oiv);
        object.addContent(oid);

        // objectCategory is fixed value, "File".
        Element oc = new Element("objectCategory", PREMIS_NS);
        oc.setText("File");
        object.addContent(oc);

        Element ochar = new Element("objectCharacteristics", PREMIS_NS);
        object.addContent(ochar);

        // checksum if available
        String cks = bitstream.getChecksum();
        String cka = bitstream.getChecksumAlgorithm();
        if (cks != null && cka != null)
        {
            Element fixity = new Element("fixity", PREMIS_NS);
            Element mda = new Element("messageDigestAlgorithm", PREMIS_NS);
            mda.setText(cka);
            fixity.addContent(mda);
            Element md = new Element("messageDigest", PREMIS_NS);
            md.setText(cks);
            fixity.addContent(md);
            ochar.addContent(fixity);
        }

        // size
        Element size = new Element("size", PREMIS_NS);
        size.setText(String.valueOf(bitstream.getSize()));
        ochar.addContent(size);

        //  Punt and set formatName to the MIME type; the best we can
        //  do for now in the absence of any usable global format registries.
        // objectCharacteristics/format/formatDesignation/
        //                                       formatName <- MIME Type
        //
        Element format = new Element("format", PREMIS_NS);
        Element formatDes = new Element("formatDesignation", PREMIS_NS);
        Element formatName = new Element("formatName", PREMIS_NS);
        formatName.setText(bitstream.getFormat().getMIMEType());
        formatDes.addContent(formatName);
        format.addContent(formatDes);
        ochar.addContent(format);

        // originalName <- name (or source if none)
        String oname = bitstream.getName();
        if (oname == null)
            oname = bitstream.getSource();
        if (oname != null)
        {
            Element on = new Element("originalName", PREMIS_NS);
            on.setText(oname);
            object.addContent(on);
        }

        return premis;
    }

    public List disseminateList(DSpaceObject dso)
        throws CrosswalkException,
               IOException, SQLException, AuthorizeException
    {
        List result = new ArrayList(1);
        result.add(disseminateElement(dso));
        return result;
    }

    public boolean preferList()
    {
        return false;
    }
}
