/*
 * METSManifest.java
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

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.List;

import org.apache.commons.codec.binary.Base64;
import org.apache.log4j.Logger;
import org.dspace.authorize.AuthorizeException;
import org.dspace.content.Bitstream;
import org.dspace.content.DSpaceObject;
import org.dspace.content.Item;
import org.dspace.content.crosswalk.CrosswalkException;
import org.dspace.content.crosswalk.CrosswalkObjectNotSupported;
import org.dspace.content.crosswalk.IngestionCrosswalk;
import org.dspace.content.crosswalk.MetadataValidationException;
import org.dspace.core.ConfigurationManager;
import org.dspace.core.Constants;
import org.dspace.core.Context;
import org.dspace.core.PluginManager;
import org.dspace.uri.ExternalIdentifier;
import org.dspace.uri.ExternalIdentifierType;
import org.jdom.Document;
import org.jdom.Element;
import org.jdom.JDOMException;
import org.jdom.Namespace;
import org.jdom.input.SAXBuilder;
import org.jdom.output.Format;
import org.jdom.output.XMLOutputter;
import org.jdom.xpath.XPath;

/**
 * <P>
 * Manage the METS manifest document for METS importer classes,
 * such as the package importer <code>org.dspace.content.packager.MetsSubmission</code>
 * and the federated importer <code>org.dspace.app.mets.FederatedMETSImport</code>
 * </P>
 * <P>
 * It can parse the METS document, build an internal model, and give the importers
 * access to that model.  It also crosswalks
 * all of the descriptive and administrative metadata in the METS
 * manifest into the target DSpace Item, under control of the importer.
 * </P>
 *
 * <P>
 * It reads the following DSpace Configuration entries:
 * </P>
 * <UL>
 * <LI>Local XML schema (XSD) declarations, in the general format:
 * <br><code>mets.xsd.<em>identifier</em> = <em>namespace</em> <em>xsd-URL</em></code>
 * <br> eg. <code>mets.xsd.dc =  http://purl.org/dc/elements/1.1/ dc.xsd</code>
 * <br>Add a separate config entry for each schema.
 * </LI>
 * <p><LI>Crosswalk plugin mappings:
 * These tell it the name of the crosswalk plugin to invoke for metadata sections
 * with a particular value of <code>MDTYPE</code> (or <code>OTHERMDTYPE</code>)
 * By default, the crosswalk mechanism will look for a plugin with the
 * same name as the metadata type (e.g.  <code>"MODS"</code>,
 * <code>"DC"</code>).  This example line invokes the <code>QDC</code>
 * plugin when <code>MDTYPE="DC"</code>
 * <br><code>mets.submission.crosswalk.DC = QDC </code>
 * <br> general format is:
 * <br><code>mets.submission.crosswalk.<em>mdType</em> = <em>pluginName</em> </code>
 * </LI>
 * </UL>
 *
 *
 * @author Robert Tansley
 * @author WeiHua Huang
 * @author Rita Lee
 * @author Larry Stone
 * @see org.dspace.content.packager.MetsSubmission
 * @see org.dspace.app.mets.FederatedMETSImport
 */
public class METSManifest
{
    /**
     * Callback interface to retrieve data streams in mdRef elements.
     * "Package" or file reader returns an input stream for the
     * given relative path, e.g. to dereference <code>mdRef</code> elements.
     */
    public interface Mdref
    {
        /**
         * Make the contents of an external resource mentioned in
         * an <code>mdRef</code> element available as an <code>InputStream</code>.
         * The implementation must use the information in the
         * <code>mdRef</code> element, and the state in the object that
         * implements this interface, to find the actual metadata content.
         * <p>
         * For example, an implementation that ingests a directory of
         * files on the local filesystem would get a relative pathname
         * out of the <code>mdRef</code> and open that file.
         *
         * @param mdRef JDOM element of mdRef in the METS manifest.
         * @return stream containing the metadata mentioned in mdRef.
         * @throw MetadataValidationException if the mdRef is unacceptable or missing required information.
         * @throw IOException if it is returned by services called by this method.
         * @throw SQLException if it is returned by services called by this method.
         * @throw AuthorizeException if it is returned by services called by this method.
         */
        public InputStream getInputStream(Element mdRef)
            throws MetadataValidationException, IOException, SQLException, AuthorizeException;
    }

    /** log4j category */
    private static Logger log = Logger.getLogger(METSManifest.class);

    /** Canonical filename of METS manifest within a package or as a bitstream. */
    public final static String MANIFEST_FILE = "mets.xml";

    /** Prefix of DSpace configuration lines that map METS metadata type to
     * crosswalk plugin names.
     */
    private final static String CONFIG_METADATA_PREFIX = "mets.submission.crosswalk.";

    /** prefix of config lines identifying local XML Schema (XSD) files */
    private final static String CONFIG_XSD_PREFIX = "mets.xsd.";

    /** Dublin core element namespace */
    private static Namespace dcNS = Namespace
            .getNamespace("http://purl.org/dc/elements/1.1/");

    /** Dublin core term namespace (for qualified DC) */
    private static Namespace dcTermNS = Namespace
            .getNamespace("http://purl.org/dc/terms/");

    /** METS namespace -- includes "mets" prefix for use in XPaths */
    public static Namespace metsNS = Namespace
            .getNamespace("mets", "http://www.loc.gov/METS/");

    /** XLink namespace -- includes "xlink" prefix prefix for use in XPaths */
    private static Namespace xlinkNS = Namespace
            .getNamespace("xlink", "http://www.w3.org/1999/xlink");

    /** root element of the current METS manifest. */
    private Element mets = null;

    /** all mdRef elements in the manifest */
    private List mdFiles = null;

    /** <file> elements in "original" filegroup (bundle) */
    private List contentFiles = null;

    /** builder to use for mdRef streams, inherited from create() */
    private SAXBuilder parser = null;

    // Create list of local schemas at load time, since it depends only
    // on the DSpace configuration.
    private static String localSchemas;
    static
    {
        String dspace_dir = ConfigurationManager.getProperty("dspace.dir");
        File xsdPath1 = new File(dspace_dir+"/config/schemas/");
        File xsdPath2 = new File(dspace_dir+"/config/");

        Enumeration pe = ConfigurationManager.propertyNames();
        StringBuffer result = new StringBuffer();
        while (pe.hasMoreElements())
        {
            // config lines have the format:
            //  mets.xsd.{identifier} = {namespace} {xsd-URL}
            // e.g.
            //  mets.xsd.dc =  http://purl.org/dc/elements/1.1/ dc.xsd
            // (filename is relative to {dspace_dir}/config/schemas/)
            String key = (String)pe.nextElement();
            if (key.startsWith(CONFIG_XSD_PREFIX))
            {
                String spec  = ConfigurationManager.getProperty(key);
                String val[] = spec.trim().split("\\s+");
                if (val.length == 2)
                {
                    File xsd = new File(xsdPath1, val[1]);
                    if (!xsd.exists())
                         xsd = new File(xsdPath2, val[1]);
                    if (!xsd.exists())
                        log.warn("Schema file not found for config entry=\""+spec+"\"");
                    else
                    {
                        try
                        {
                            String u = xsd.toURL().toString();
                            if (result.length() > 0)
                                result.append(" ");
                            result.append(val[0]).append(" ").append(u);
                        }
                        catch (java.net.MalformedURLException e)
                        {
                            log.warn("Skipping badly formed XSD URL: "+e.toString());
                        }
                    }
                }
                else
                    log.warn("Schema config entry has wrong format, entry=\""+spec+"\"");
            }
        }
        localSchemas = result.toString();
        log.debug("Got local schemas = \""+localSchemas+"\"");
    }

    /**
     * Default constructor, only called internally.
     * @param builder XML parser (for parsing mdRef'd files and binData)
     * @param mets parsed METS document
     */
    private METSManifest(SAXBuilder builder, Element mets)
    {
        super();
        this.mets = mets;
        parser = builder;
    }

    /**
     * Create a new manifest object from a serialized METS XML document.
     * Parse document read from the input stream, optionally validating.
     * @param is input stream containing serialized XML
     * @param validate if true, enable XML validation using schemas
     *   in document.  Also validates any sub-documents.
     * @throws MetadataValidationException if there is any error parsing
     *          or validating the METS.
     * @return new METSManifest object.
     */
    public static METSManifest create(InputStream is, boolean validate)
            throws IOException,
            MetadataValidationException
    {
        SAXBuilder builder = new SAXBuilder(validate);

        // Set validation feature
        if (validate)
            builder.setFeature("http://apache.org/xml/features/validation/schema",
                    true);

        // Tell the parser where local copies of schemas are, to speed up
        // validation.  Local XSDs are identified in the configuration file.
        if (localSchemas.length() > 0)
            builder.setProperty(
                    "http://apache.org/xml/properties/schema/external-schemaLocation",
                    localSchemas);

        // Parse the METS file
        Document metsDocument;

        try
        {
            metsDocument = builder.build(is);

            // XXX for temporary debugging
            /*
            XMLOutputter outputPretty = new XMLOutputter(Format.getPrettyFormat());
            log.debug("Got METS DOCUMENT:");
            log.debug(outputPretty.outputString(metsDocument));
              */
        }
        catch (JDOMException je)
        {
            throw new MetadataValidationException("Error validating METS in "
                    + is.toString(),  je);
        }

        return new METSManifest(builder, metsDocument.getRootElement());
    }

    /**
     * Gets name of the profile to which this METS document conforms.
     * @return value the PROFILE attribute of mets element, or null if none.
     */
    public String getProfile()
    {
        return mets.getAttributeValue("PROFILE");
    }

    /**
     * Gets all <code>file</code> elements which make up
     *   the item's content.
     * @return a List of <code>Element</code>s.
     */
    public List getContentFiles()
        throws MetadataValidationException
    {
        if (contentFiles != null)
            return contentFiles;

        Element fileSec = mets.getChild("fileSec", metsNS);
        if (fileSec == null)
            throw new MetadataValidationException("Invalid METS Manifest: DSpace requires a fileSec element, but it is missing.");

        contentFiles = new ArrayList();
        Iterator fgi = fileSec.getChildren("fileGrp", metsNS).iterator();
        while (fgi.hasNext())
        {
            Element fg = (Element)fgi.next();
            Iterator fi = fg.getChildren("file", metsNS).iterator();
            while (fi.hasNext())
            {
                Element f = (Element)fi.next();
                contentFiles.add(f);
            }
        }
        return contentFiles;
    }

    /**
     * Gets list of all <code>mdRef</code> elements in the METS
     *   document.  Used by ingester to e.g. check that all
     *   required files are present.
     * @return a List of <code>Element</code>s.
     */
    public List getMdFiles()
        throws MetadataValidationException
    {
        if (mdFiles == null)
        {
            try
            {
                // Use a special namespace with known prefix
                // so we get the right prefix.
                XPath xpath = XPath.newInstance("descendant::mets:mdRef");
                xpath.addNamespace(metsNS);
                mdFiles = xpath.selectNodes(mets);
            }
            catch (JDOMException je)
            {
                throw new MetadataValidationException("Failed while searching for mdRef elements in manifest: ", je);
            }
        }
        return mdFiles;
    }

    /**
     * Get the "original" file element for a derived file.
     * Finds the original from which this was derived by matching the GROUPID
     * attribute that binds it to its original.  For instance, the file for
     * a thumbnail image would have the same GROUPID as its full-size version.
     * <p>
     * NOTE: This pattern of relating derived files through the GROUPID
     * attribute is peculiar to the DSpace METS SIP profile, and may not be
     * generally useful with other sorts of METS documents.
     * @param file METS file element of derived file
     * @return file Element of original or null if none found.
     */
    public Element getOriginalFile(Element file)
    {
        String groupID = file.getAttributeValue("GROUPID");
        if (groupID == null || groupID.equals(""))
            return null;

        try
        {
            XPath xpath = XPath.newInstance(
"mets:fileSec/mets:fileGrp[@USE=\"CONTENT\"]/mets:file[@GROUPID=\""+groupID+"\"]");
            xpath.addNamespace(metsNS);
            List oFiles = xpath.selectNodes(mets);
            if (oFiles.size() > 0)
            {
                log.debug("Got ORIGINAL file for derived="+file.toString());
                return (Element)oFiles.get(0);
            }
            else
                return null;
        }
        catch (JDOMException je)
        {
            log.warn("Got exception on XPATH looking for Original file, "+je.toString());
            return null;
        }
    }

    // translate bundle name from METS to DSpace; METS may be "CONTENT"
    // or "ORIGINAL" for the DSPace "ORIGINAL", rest are left alone.
    private static String normalizeBundleName(String in)
    {
        if (in.equals("CONTENT"))
            return Constants.CONTENT_BUNDLE_NAME;
        else if (in.equals("MANIFESTMD"))
            return Constants.METADATA_BUNDLE_NAME;
        return in;
    }

    /**
     * Get the DSpace bundle name corresponding to the <code>USE</code> attribute of the file group enclosing this <code>file</code> element.
     * @return DSpace bundle name
     * @throws MetadataValidationException when there is no USE attribute on the enclosing fileGrp.
     */
    public static String getBundleName(Element file)
        throws MetadataValidationException
    {
        Element fg = file.getParentElement();
        String fgUse = fg.getAttributeValue("USE");
        if (fgUse == null)
            throw new MetadataValidationException("Invalid METS Manifest: every fileGrp element must have a USE attribute.");
        return normalizeBundleName(fgUse);
    }

    /**
     * Get the "local" file name of this <code>file</code> or <code>mdRef</code> element.
     * By "local" we mean the reference to the actual resource containing
     * the data for this file, e.g. a relative path within a Zip or tar archive
     * if the METS is serving as a manifest for that sort of package.
     * @return "local" file name (i.e.  relative to package or content
     *  directory) corresponding to this <code>file</code> or <code>mdRef</code> element.
     * @throws MetadataValidationException when there is not enough information to find a resource identifier.
     */
    public static String getFileName(Element file)
        throws MetadataValidationException
    {
        Element ref;
        if (file.getName().equals("file"))
        {
            ref = file.getChild("FLocat", metsNS);
            if (ref == null)
            {
                // check for forbidden FContent child first:
                if (file.getChild("FContent", metsNS) == null)
                    throw new MetadataValidationException("Invalid METS Manifest: Every file element must have FLocat child.");
                else
                    throw new MetadataValidationException("Invalid METS Manifest: file element has forbidden FContent child, only FLocat is allowed.");
            }
        }
        else if (file.getName().equals("mdRef"))
            ref = file;
        else
            throw new MetadataValidationException("getFileName() called with recognized element type: "+file.toString());
        String loctype = ref.getAttributeValue("LOCTYPE");
        if (loctype != null && loctype.equals("URL"))
        {
            String result = ref.getAttributeValue("href", xlinkNS);
            if (result == null)
                throw new MetadataValidationException("Invalid METS Manifest: FLocat/mdRef is missing the required xlink:href attribute.");
            return result;
        }
        throw new MetadataValidationException("Invalid METS Manifest: FLocat/mdRef does not have LOCTYPE=\"URL\" attribute.");
    }

    /**
     * Returns file element corresponding to primary bitstream.
     * There is <i>ONLY</i> a primary bitstream if the first <code>div</code> under
     * first </code>structMap</code> has an </code>fptr</code>.
     *
     * @return file element of Item's primary bitstream, or null if there is none.
     */
    public Element getPrimaryBitstream()
        throws MetadataValidationException
    {
        Element firstDiv = getFirstDiv();
        Element fptr = firstDiv.getChild("fptr", metsNS);
        if (fptr == null)
            return null;
        String id = fptr.getAttributeValue("FILEID");
        if (id == null)
            throw new MetadataValidationException("fptr for Primary Bitstream is missing the required FILEID attribute.");
        Element result = getElementByXPath("descendant::mets:file[@ID=\""+id+"\"]", false);
        if (result == null)
            throw new MetadataValidationException("Cannot find file element for Primary Bitstream: looking for ID="+id);
        return result;
    }

    /** Get the metadata type from within a *mdSec element.
     * @return metadata type name.
     */
    public String getMdType(Element mdSec)
        throws MetadataValidationException
    {
        Element md = mdSec.getChild("mdRef", metsNS);
        if (md == null)
            md = mdSec.getChild("mdWrap", metsNS);
        if (md == null)
            throw new MetadataValidationException("Invalid METS Manifest: ?mdSec element has neither mdRef nor mdWrap child.");
        String result = md.getAttributeValue("MDTYPE");
        if (result != null && result.equals("OTHER"))
            result = md.getAttributeValue("OTHERMDTYPE");
        if (result == null)
            throw new MetadataValidationException("Invalid METS Manifest: "+md.getName()+" has no MDTYPE or OTHERMDTYPE attribute.");
        return result;
    }

    /**
     *  Returns MIME type of metadata content, if available.
     *  @return MIMEtype word, or null if none is available.
     */
    public String getMdContentMimeType(Element mdSec)
        throws MetadataValidationException
    {
        Element mdWrap = mdSec.getChild("mdWrap", metsNS);
        if (mdWrap != null)
        {
            String mimeType = mdWrap.getAttributeValue("MIMETYPE");
            if (mimeType == null && mdWrap.getChild("xmlData", metsNS) != null)
            mimeType = "text/xml";
            return mimeType;
        }
        Element mdRef = mdSec.getChild("mdRef", metsNS);
        if (mdRef != null)
            return mdRef.getAttributeValue("MIMETYPE");
        return null;
    }

    /**
     * Return contents of *md element as List of XML Element objects.
     * Gets content, dereferecing mdRef if necessary, or decoding and parsing
     * a binData that contains XML.
     * @return contents of metadata section, or empty list if no XML content is available.
     * @throws MetadataValidationException if METS is invalid, or there is an error parsing the XML.
     */
    public List getMdContentAsXml(Element mdSec, Mdref callback)
        throws MetadataValidationException, IOException, SQLException, AuthorizeException
    {
        try
        {
            Element mdRef = null;
            Element mdWrap = mdSec.getChild("mdWrap", metsNS);
            if (mdWrap != null)
            {
                Element xmlData = mdWrap.getChild("xmlData", metsNS);
                if (xmlData == null)
                {
                    Element bin = mdWrap.getChild("binData", metsNS);
                    if (bin == null)
                        throw new MetadataValidationException("Invalid METS Manifest: mdWrap element with neither xmlData nor binData child.");

                    // if binData is actually XML, return it; otherwise ignore.
                    else
                    {
                        String mimeType = mdWrap.getAttributeValue("MIMETYPE");
                        if (mimeType != null && mimeType.equalsIgnoreCase("text/xml"))
                        {
                            byte value[] = Base64.decodeBase64(bin.getText().getBytes());
                            Document mdd = parser.build(new ByteArrayInputStream(value));
                            List result = new ArrayList(1);
                            result.add(mdd.getRootElement());
                            return result;
                        }
                        else
                        {
                            log.warn("Ignoring binData section because MIMETYPE is not XML, but: "+mimeType);
                            return new ArrayList(0);
                        }
                   }
                }
                else
                {
                    return xmlData.getChildren();
                }
            }
            else if ((mdRef = mdSec.getChild("mdRef", metsNS)) != null)
            {
                String mimeType = mdRef.getAttributeValue("MIMETYPE");
                if (mimeType != null && mimeType.equalsIgnoreCase("text/xml"))
                {
                    Document mdd = parser.build(callback.getInputStream(mdRef));
                    List result = new ArrayList(1);
                    result.add(mdd.getRootElement());
                    return result;
                }
                else
                {
                    log.warn("Ignoring mdRef section because MIMETYPE is not XML, but: "+mimeType);
                    return new ArrayList(0);
                }
            }
            else
                throw new MetadataValidationException("Invalid METS Manifest: ?mdSec element with neither mdRef nor mdWrap child.");
        }
        catch (JDOMException je)
        {
            throw new MetadataValidationException("Error parsing or validating metadata section in mdRef or binData within "+mdSec.toString(), je);
        }

    }

    /**
     * Return contents of *md element as stream.
     * Gets content, dereferecing mdRef if necessary, or decoding
     * a binData element if necessary.
     * @return Stream containing contents of metadata section.  Never returns null.
     * @throws MetadataValidationException if METS format does not contain any metadata.
     */
    public InputStream getMdContentAsStream(Element mdSec, Mdref callback)
        throws MetadataValidationException, IOException, SQLException, AuthorizeException
    {
        Element mdRef = null;
        Element mdWrap = mdSec.getChild("mdWrap", metsNS);
        if (mdWrap != null)
        {
            Element xmlData = mdWrap.getChild("xmlData", metsNS);
            if (xmlData == null)
            {
                Element bin = mdWrap.getChild("binData", metsNS);
                if (bin == null)
                    throw new MetadataValidationException("Invalid METS Manifest: mdWrap element with neither xmlData nor binData child.");

                else
                {
                    byte value[] = Base64.decodeBase64(bin.getText().getBytes());
                    return new ByteArrayInputStream(value);
                }
            }
            else
            {
                XMLOutputter outputPretty = new XMLOutputter(Format.getPrettyFormat());
                return new ByteArrayInputStream(
                        outputPretty.outputString(xmlData.getChildren()).getBytes());
            }
        }
        else if ((mdRef = mdSec.getChild("mdRef", metsNS)) != null)
        {
            return callback.getInputStream(mdRef);
        }
        else
            throw new MetadataValidationException("Invalid METS Manifest: ?mdSec element with neither mdRef nor mdWrap child.");
    }


    // special call to crosswalk the guts of a metadata *Sec (dmdSec, amdSec)
    // because mdRef and mdWrap have to be handled differently.
    // It's a lot like getMdContentAsXml but cannot use that because xwalk
    // should be called with root element OR list depending on what was given.
    private void crosswalkMdContent(Element mdSec, Mdref callback,
                IngestionCrosswalk xwalk, Context context, DSpaceObject dso)
        throws CrosswalkException, IOException, SQLException, AuthorizeException
    {
        List xml = getMdContentAsXml(mdSec,callback);

        // if we get inappropriate metadata, e.g. PREMIS for Item, let it go.
        try
        {
            xwalk.ingest(context, dso, xml);
        }
        catch (CrosswalkObjectNotSupported e)
        {
            log.warn("Skipping metadata for inappropriate type of object: Object="+dso.toString()+", error="+e.toString());
        }
    }

    // return first <div> of first <structMap>;
    // in DSpace profile, this is where item-wide dmd and other metadata
    // lives as IDrefs.
    private Element getFirstDiv()
        throws MetadataValidationException
    {
        Element sm = mets.getChild("structMap", metsNS);
        if (sm == null)
            throw new MetadataValidationException("METS document is missing the required structMap element.");

        Element result = sm.getChild("div", metsNS);
        if (result == null)
            throw new MetadataValidationException("METS document is missing the required first div element in first structMap.");

        log.debug("Got firstDiv result="+result.toString());
        return (Element)result;
    }

    // return a single Element node found by one-off path.
    // use only when path varies each time you call it.
    private Element getElementByXPath(String path, boolean nullOk)
        throws MetadataValidationException
    {
        try
        {
            XPath xpath = XPath.newInstance(path);
            xpath.addNamespace(metsNS);
            xpath.addNamespace(xlinkNS);
            Object result = xpath.selectSingleNode(mets);
            if (result == null && nullOk)
                return null;
            else if (result instanceof Element)
                return (Element)result;
            else
                throw new MetadataValidationException("METSManifest: Failed to resolve XPath, path=\""+path+"\"");
        }
        catch (JDOMException je)
        {
            throw new MetadataValidationException("METSManifest: Failed to resolve XPath, path=\""+path+"\"", je);
        }
    }

    // Find crosswalk for the indicated metadata type (e.g. "DC", "MODS")
    // The crosswalk plugin name MAY be indirected in config file,
    // through an entry like
    //  mets.submission.crosswalk.{mdType} = {pluginName}
    //   e.g.
    //  mets.submission.crosswalk.DC = mysite-QDC
    private IngestionCrosswalk getCrosswalk(String type)
    {
        String xwalkName = ConfigurationManager.getProperty(CONFIG_METADATA_PREFIX + type);
        if (xwalkName == null)
            xwalkName = type;
        return (IngestionCrosswalk)
          PluginManager.getNamedPlugin(IngestionCrosswalk.class, xwalkName);
    }

    /**
     * Gets all dmdSec elements containing metadata for the DSpace Item.
     *
     * @return array of Elements, each a dmdSec.  May be empty but NOT null.
     * @throws MetadataValidationException if the METS is missing a reference to item-wide
     *          DMDs in the correct place.
     */
    public Element[] getItemDmds()
        throws MetadataValidationException
    {
        // div@DMDID is actually IDREFS, a space-separated list of IDs:
        Element firstDiv = getFirstDiv();
        String dmds = firstDiv.getAttributeValue("DMDID");
        if (dmds == null)
            throw new MetadataValidationException("Invalid METS: Missing reference to Item descriptive metadata, first div on first structmap must have a DMDID attribute.");
        String dmdID[] = dmds.split("\\s+");
        Element result[] = new Element[dmdID.length];

        for (int i = 0; i < dmdID.length; ++i)
            result[i] = getElementByXPath("mets:dmdSec[@ID=\""+dmdID[i]+"\"]", false);
        return result;
    }

    /**
     * Return rights metadata section(s) relevant to item as a whole.
     * @return array of rightsMd elements, possibly empty but never null.
     * @throws MetadataValidationException if METS is invalid, e.g. referenced amdSec is missing.
     */
    public Element[] getItemRightsMD()
        throws MetadataValidationException
    {
        // div@ADMID is actually IDREFS, a space-separated list of IDs:
        Element firstDiv = getFirstDiv();
        String amds = firstDiv.getAttributeValue("ADMID");
        if (amds == null)
        {
            log.debug("getItemRightsMD: No ADMID references found.");
            return new Element[0];
        }
        String amdID[] = amds.split("\\s+");
        List resultList = new ArrayList();
        for (int i = 0; i < amdID.length; ++i)
        {
            List rmds = getElementByXPath("mets:amdSec[@ID=\""+amdID[i]+"\"]", false).
                            getChildren("rightsMD", metsNS);
            if (rmds.size() > 0)
                resultList.addAll(rmds);
        }
        return (Element[])resultList.toArray(new Element[resultList.size()]);
    }

    /**
     * Invokes appropriate crosswalks on Item-wide descriptive metadata.
     */
    public void crosswalkItem(Context context, Item item, Element dmd, Mdref callback)
        throws MetadataValidationException,
               CrosswalkException, IOException, SQLException, AuthorizeException
    {
        String type = getMdType(dmd);
        IngestionCrosswalk xwalk = getCrosswalk(type);

        if (xwalk == null)
            throw new MetadataValidationException("Cannot process METS Manifest: "+
                "No crosswalk found for MDTYPE="+type);
        crosswalkMdContent(dmd, callback, xwalk, context, item);
    }

    /**
     * Crosswalk the metadata associated with a particular <code>file</code>
     * element into the bitstream it corresponds to.
     * @param context a dspace context.
     * @param bs bitstream target of the crosswalk
     * @param fileId value of ID attribute in the file element responsible
     *  for the contents of that bitstream.
     */
    public void crosswalkBitstream(Context context, Bitstream bitstream,
                                   String fileId, Mdref callback)
        throws MetadataValidationException,
               CrosswalkException, IOException, SQLException, AuthorizeException
    {
        Element file = getElementByXPath("descendant::mets:file[@ID=\""+fileId+"\"]", false);
        if (file == null)
            throw new MetadataValidationException("Failed in Bitstream crosswalk, Could not find file element with ID="+fileId);

        // In DSpace METS SIP spec, admin metadata is only "highly
        // recommended", not "required", so it is OK if there is no ADMID.
        String amds = file.getAttributeValue("ADMID");
        if (amds == null)
        {
            log.warn("Got no bitstream ADMID, file@ID="+fileId);
            return;
        }
        String amdID[] = amds.split("\\s+");
        for (int i = 0; i < amdID.length; ++i)
        {
            List techMDs = getElementByXPath("mets:amdSec[@ID=\""+amdID[i]+"\"]", false).
                                 getChildren("techMD", metsNS);
            Iterator ti = techMDs.iterator();
            while (ti.hasNext())
            {
                Element techMD = (Element)ti.next();
                if (techMD != null)
                {
                    String type = getMdType(techMD);
                    IngestionCrosswalk xwalk = getCrosswalk(type);
                    log.debug("Got bitstream techMD of type="+type+", for file ID="+fileId);
                     
                    if (xwalk == null)
                        throw new MetadataValidationException("Cannot process METS Manifest: "+
                            "No crosswalk found for techMD MDTYPE="+type);
                    crosswalkMdContent(techMD, callback, xwalk, context, bitstream);
                }
            }
        }
    }

    /**
     * Find URI (if any) identifier labelling this manifest.
     *
     * @return uri (never null)
     * @throws MetadataValidationException if no uri available.
     */
    public String getURI() throws MetadataValidationException
    {
        // TODO: XXX Make configurable? URI optionally passed in?
        // FIXME: Not sure if OBJID is really the right place

        String uri = mets.getAttributeValue("OBJID");

        if (uri != null)
        {
            Object[] types =
                    PluginManager.getPluginSequence(ExternalIdentifierType.class);
            if (types != null)
            {
                for (ExternalIdentifierType t : (ExternalIdentifierType[]) types)
                {
                    if (uri.startsWith(t.getNamespace() + ":"))
                    {
                        // It's something we understand.
                        return uri;
                    }
                }
            }
        }

        throw new MetadataValidationException("Item has no valid URI (OBJID)");
    }
}
