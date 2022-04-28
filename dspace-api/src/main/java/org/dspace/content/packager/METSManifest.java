/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.content.packager;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.apache.commons.codec.binary.Base64;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.dspace.authorize.AuthorizeException;
import org.dspace.content.Bitstream;
import org.dspace.content.Bundle;
import org.dspace.content.DSpaceObject;
import org.dspace.content.crosswalk.AbstractPackagerWrappingCrosswalk;
import org.dspace.content.crosswalk.CrosswalkException;
import org.dspace.content.crosswalk.CrosswalkObjectNotSupported;
import org.dspace.content.crosswalk.IngestionCrosswalk;
import org.dspace.content.crosswalk.MetadataValidationException;
import org.dspace.content.crosswalk.StreamIngestionCrosswalk;
import org.dspace.core.Constants;
import org.dspace.core.Context;
import org.dspace.core.factory.CoreServiceFactory;
import org.dspace.services.ConfigurationService;
import org.dspace.services.factory.DSpaceServicesFactory;
import org.jdom2.Content;
import org.jdom2.Document;
import org.jdom2.Element;
import org.jdom2.JDOMException;
import org.jdom2.Namespace;
import org.jdom2.filter.Filters;
import org.jdom2.input.SAXBuilder;
import org.jdom2.output.Format;
import org.jdom2.output.XMLOutputter;
import org.jdom2.xpath.XPathExpression;
import org.jdom2.xpath.XPathFactory;

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
 * <br> e.g. <code>mets.xsd.dc =  http://purl.org/dc/elements/1.1/ dc.xsd</code>
 * <br>Add a separate configuration entry for each schema.
 * </LI>
 * <LI>Crosswalk plugin mappings:
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
 * @author Robert Tansley
 * @author WeiHua Huang
 * @author Rita Lee
 * @author Larry Stone
 */
public class METSManifest {
    /**
     * Callback interface to retrieve data streams in mdRef elements.
     * "Package" or file reader returns an input stream for the
     * given relative path, e.g. to dereference <code>mdRef</code> elements.
     */
    public interface Mdref {
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
         * @throws MetadataValidationException if the mdRef is unacceptable or missing required information.
         * @throws PackageValidationException  if package validation error
         * @throws IOException                 if IO error
         * @throws SQLException                if database error
         * @throws AuthorizeException          if authorization error
         */
        public InputStream getInputStream(Element mdRef)
            throws MetadataValidationException, PackageValidationException,
            IOException, SQLException, AuthorizeException;
    }

    /**
     * log4j category
     */
    private static final Logger log = LogManager.getLogger(METSManifest.class);

    private static final ConfigurationService configurationService
            = DSpaceServicesFactory.getInstance().getConfigurationService();
    /**
     * Canonical filename of METS manifest within a package or as a bitstream.
     */
    public static final String MANIFEST_FILE = "mets.xml";

    /**
     * Prefix of DSpace configuration lines that map METS metadata type to
     * crosswalk plugin names.
     */
    public static final String CONFIG_METS_PREFIX = "mets.";

    /**
     * prefix of configuration lines identifying local XML Schema (XSD) files
     */
    protected static final String CONFIG_XSD_PREFIX = CONFIG_METS_PREFIX + "xsd.";

    /**
     * Dublin core element namespace
     */
    protected static final Namespace dcNS = Namespace
        .getNamespace("http://purl.org/dc/elements/1.1/");

    /**
     * Dublin core term namespace (for qualified DC)
     */
    protected static final Namespace dcTermNS = Namespace
        .getNamespace("http://purl.org/dc/terms/");

    /**
     * METS namespace -- includes "mets" prefix for use in XPaths
     */
    public static final Namespace metsNS = Namespace
        .getNamespace("mets", "http://www.loc.gov/METS/");

    /**
     * XLink namespace -- includes "xlink" prefix prefix for use in XPaths
     */
    public static final Namespace xlinkNS = Namespace
        .getNamespace("xlink", "http://www.w3.org/1999/xlink");

    /**
     * root element of the current METS manifest.
     */
    protected Element mets = null;

    /**
     * all mdRef elements in the manifest
     */
    protected List mdFiles = null;

    /**
     * {@code <file>} elements in "original" file group (bundle)
     */
    protected List<Element> contentFiles = null;
    protected List<Element> bundleFiles = null;

    /**
     * builder to use for mdRef streams, inherited from create()
     */
    protected SAXBuilder parser = null;

    /**
     * name of packager who created this manifest object, for looking up configuration entries.
     */
    protected String configName;

    // Create list of local schemas at load time, since it depends only
    // on the DSpace configuration.
    protected static String localSchemas;

    static {
        String dspace_dir = configurationService.getProperty("dspace.dir");
        File xsdPath1 = new File(dspace_dir + "/config/schemas/");
        File xsdPath2 = new File(dspace_dir + "/config/");

        List<String> configKeys = configurationService.getPropertyKeys(CONFIG_XSD_PREFIX);
        StringBuilder result = new StringBuilder();
        for (String key : configKeys) {
            // config lines have the format:
            //  mets.xsd.{identifier} = {namespace} {xsd-URL}
            // e.g.
            //  mets.xsd.dc =  http://purl.org/dc/elements/1.1/ dc.xsd
            // (filename is relative to {dspace_dir}/config/schemas/)
            String spec = configurationService.getProperty(key);
            String val[] = spec.trim().split("\\s+");
            if (val.length == 2) {
                File xsd = new File(xsdPath1, val[1]);
                if (!xsd.exists()) {
                    xsd = new File(xsdPath2, val[1]);
                }
                if (!xsd.exists()) {
                    log.warn("Schema file not found for config entry=\"{}\"", spec);
                } else {
                    try {
                        String u = xsd.toURI().toURL().toString();
                        if (result.length() > 0) {
                            result.append(" ");
                        }
                        result.append(val[0]).append(" ").append(u);
                    } catch (java.net.MalformedURLException e) {
                        log.warn("Skipping badly formed XSD URL: {}", () -> e.toString());
                    }
                }
            } else {
                log.warn("Schema config entry has wrong format, entry=\"{}\"", spec);
            }
        }
        log.debug("Got local schemas = \"{}\"", () -> result.toString());
    }

    /**
     * Default constructor, only called internally.
     *
     * @param builder    XML parser (for parsing mdRef'd files and binData)
     * @param mets       parsed METS document
     * @param configName configuration name
     */
    protected METSManifest(SAXBuilder builder, Element mets, String configName) {
        super();
        this.mets = mets;
        this.parser = builder;
        this.configName = configName;
    }

    /**
     * Create a new manifest object from a serialized METS XML document.
     * Parse document read from the input stream, optionally validating.
     *
     * @param is         input stream containing serialized XML
     * @param validate   if true, enable XML validation using schemas
     *                   in document.  Also validates any sub-documents.
     * @param configName config name
     * @return new METSManifest object.
     * @throws IOException                 if IO error
     * @throws MetadataValidationException if there is any error parsing
     *                                     or validating the METS.
     */
    public static METSManifest create(InputStream is, boolean validate, String configName)
        throws IOException,
        MetadataValidationException {
        SAXBuilder builder = new SAXBuilder(validate);

        builder.setIgnoringElementContentWhitespace(true);

        // Set validation feature
        if (validate) {
            builder.setFeature("http://apache.org/xml/features/validation/schema", true);

            // Tell the parser where local copies of schemas are, to speed up
            // validation & avoid XXE attacks from remote schemas. Local XSDs are identified in the configuration file.
            if (localSchemas.length() > 0) {
                builder.setProperty("http://apache.org/xml/properties/schema/external-schemaLocation", localSchemas);
            }
        } else {
            // disallow DTD parsing to ensure no XXE attacks can occur.
            // See https://cheatsheetseries.owasp.org/cheatsheets/XML_External_Entity_Prevention_Cheat_Sheet.html
            builder.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
        }

        // Parse the METS file
        Document metsDocument;

        try {
            metsDocument = builder.build(is);

            /*** XXX leave commented out except if needed for
             *** viewing the METS document that actually gets read.
             *
             * XMLOutputter outputPretty = new XMLOutputter(Format.getPrettyFormat());
             * log.debug("Got METS DOCUMENT:");
             * log.debug(outputPretty.outputString(metsDocument));
             ****/
        } catch (JDOMException je) {
            throw new MetadataValidationException("Error validating METS in "
                                                      + is.toString(), je);
        }

        return new METSManifest(builder, metsDocument.getRootElement(), configName);
    }

    /**
     * Gets name of the profile to which this METS document conforms.
     *
     * @return value the PROFILE attribute of mets element, or null if none.
     */
    public String getProfile() {
        return mets.getAttributeValue("PROFILE");
    }

    /**
     * Return the OBJID attribute of the METS manifest.
     * This is where the Handle URI/URN of the object can be found.
     *
     * @return OBJID attribute of METS manifest
     */
    public String getObjID() {
        return mets.getAttributeValue("OBJID");
    }

    /**
     * Gets all <code>file</code> elements which make up
     * the item's content.
     *
     * @return a List of <code>Element</code>s.
     * @throws MetadataValidationException if validation error
     */
    public List<Element> getBundleFiles()
        throws MetadataValidationException {
        if (bundleFiles != null) {
            return bundleFiles;
        }

        bundleFiles = new ArrayList<>();
        Element fileSec = mets.getChild("fileSec", metsNS);

        if (fileSec != null) {
            Iterator fgi = fileSec.getChildren("fileGrp", metsNS).iterator();
            while (fgi.hasNext()) {
                Element fg = (Element) fgi.next();
                bundleFiles.add(fg);
            }
        }
        return bundleFiles;
    }

    public List<Element> getContentFiles()
        throws MetadataValidationException {
        if (contentFiles != null) {
            return contentFiles;
        }

        contentFiles = new ArrayList<>();
        Element fileSec = mets.getChild("fileSec", metsNS);

        if (fileSec != null) {
            Iterator fgi = fileSec.getChildren("fileGrp", metsNS).iterator();
            while (fgi.hasNext()) {
                Element fg = (Element) fgi.next();
                Iterator fi = fg.getChildren("file", metsNS).iterator();
                while (fi.hasNext()) {
                    Element f = (Element) fi.next();
                    contentFiles.add(f);
                }
            }
        }
        return contentFiles;
    }

    /**
     * Gets list of all <code>mdRef</code> elements in the METS
     * document.  Used by ingester to e.g. check that all
     * required files are present.
     *
     * @return a List of <code>Element</code>s.
     * @throws MetadataValidationException if validation error
     */
    public List getMdFiles()
        throws MetadataValidationException {
        if (mdFiles == null) {
            // Use a special namespace with known prefix
            // so we get the right prefix.
            XPathExpression<Element> xpath =
                XPathFactory.instance()
                            .compile("descendant::mets:mdRef", Filters.element(), null, metsNS);
            mdFiles = xpath.evaluate(mets);
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
     *
     * @param file METS file element of derived file
     * @return file path of original or null if none found.
     */
    public String getOriginalFilePath(Element file) {
        String groupID = file.getAttributeValue("GROUPID");
        if (groupID == null || groupID.equals("")) {
            return null;
        }

        XPathExpression<Element> xpath =
            XPathFactory.instance()
                        .compile(
                            "mets:fileSec/mets:fileGrp[@USE=\"CONTENT\"]/mets:file[@GROUPID=\"" + groupID + "\"]",
                            Filters.element(), null, metsNS);
        List<Element> oFiles = xpath.evaluate(mets);
        if (oFiles.size() > 0) {
            if (log.isDebugEnabled()) {
                log.debug("Got ORIGINAL file for derived=" + file.toString());
            }
            Element flocat = oFiles.get(0).getChild("FLocat", metsNS);
            if (flocat != null) {
                return flocat.getAttributeValue("href", xlinkNS);
            }
        }
        return null;
    }

    // translate bundle name from METS to DSpace; METS may be "CONTENT"
    // or "ORIGINAL" for the DSpace "ORIGINAL", rest are left alone.
    protected static String normalizeBundleName(String in) {
        if (in.equals("CONTENT")) {
            return Constants.CONTENT_BUNDLE_NAME;
        } else if (in.equals("MANIFESTMD")) {
            return Constants.METADATA_BUNDLE_NAME;
        }
        return in;
    }

    /**
     * Get the DSpace bundle name corresponding to the <code>USE</code>
     * attribute of the file group enclosing this <code>file</code> element.
     *
     * @param file file element
     * @return DSpace bundle name
     * @throws MetadataValidationException when there is no USE attribute on the enclosing fileGrp.
     */
    public static String getBundleName(Element file)
        throws MetadataValidationException {
        return getBundleName(file, true);
    }

    /**
     * Get the DSpace bundle name corresponding to the <code>USE</code>
     * attribute of the file group enclosing this <code>file</code> element.
     *
     * @param file      file element
     * @param getParent parent flag
     * @return DSpace bundle name
     * @throws MetadataValidationException when there is no USE attribute on the enclosing fileGrp.
     */
    public static String getBundleName(Element file, boolean getParent)
        throws MetadataValidationException {
        Element fg = file;
        if (getParent) {
            fg = file.getParentElement();
        }
        String fgUse = fg.getAttributeValue("USE");
        if (fgUse == null) {
            throw new MetadataValidationException(
                "Invalid METS Manifest: every fileGrp element must have a USE attribute.");
        }
        return normalizeBundleName(fgUse);
    }

    /**
     * Get the "local" file name of this <code>file</code> or <code>mdRef</code> element.
     * By "local" we mean the reference to the actual resource containing
     * the data for this file, e.g. a relative path within a Zip or tar archive
     * if the METS is serving as a manifest for that sort of package.
     *
     * @param file file element
     * @return "local" file name (i.e.  relative to package or content
     * directory) corresponding to this <code>file</code> or <code>mdRef</code> element.
     * @throws MetadataValidationException when there is not enough information to find a resource identifier.
     */
    public static String getFileName(Element file)
        throws MetadataValidationException {
        Element ref;
        if (file.getName().equals("file")) {
            ref = file.getChild("FLocat", metsNS);
            if (ref == null) {
                // check for forbidden FContent child first:
                if (file.getChild("FContent", metsNS) == null) {
                    throw new MetadataValidationException(
                        "Invalid METS Manifest: Every file element must have FLocat child.");
                } else {
                    throw new MetadataValidationException(
                        "Invalid METS Manifest: file element has forbidden FContent child, only FLocat is allowed.");
                }
            }
        } else if (file.getName().equals("mdRef")) {
            ref = file;
        } else {
            throw new MetadataValidationException(
                "getFileName() called with recognized element type: " + file.toString());
        }
        String loctype = ref.getAttributeValue("LOCTYPE");
        if (loctype != null && loctype.equals("URL")) {
            String result = ref.getAttributeValue("href", xlinkNS);
            if (result == null) {
                throw new MetadataValidationException(
                    "Invalid METS Manifest: FLocat/mdRef is missing the required xlink:href attribute.");
            }
            return result;
        }
        throw new MetadataValidationException(
            "Invalid METS Manifest: FLocat/mdRef does not have LOCTYPE=\"URL\" attribute.");
    }

    /**
     * Returns file element corresponding to primary bitstream.
     * There is <i>ONLY</i> a primary bitstream if the first {@code div} under
     * first {@code structMap} has an {@code fptr}.
     *
     * @return file element of Item's primary bitstream, or null if there is none.
     * @throws MetadataValidationException if validation error
     */
    public Element getPrimaryOrLogoBitstream()
        throws MetadataValidationException {
        Element objDiv = getObjStructDiv();
        Element fptr = objDiv.getChild("fptr", metsNS);
        if (fptr == null) {
            return null;
        }
        String id = fptr.getAttributeValue("FILEID");
        if (id == null) {
            throw new MetadataValidationException(
                "fptr for Primary Bitstream is missing the required FILEID attribute.");
        }
        Element result = getElementByXPath("descendant::mets:file[@ID=\"" + id + "\"]", false);
        if (result == null) {
            throw new MetadataValidationException(
                "Cannot find file element for Primary Bitstream: looking for ID=" + id);
        }
        return result;
    }

    /**
     * Get the metadata type from within a *mdSec element.
     *
     * @param mdSec mdSec element
     * @return metadata type name.
     * @throws MetadataValidationException if validation error
     */
    public String getMdType(Element mdSec)
        throws MetadataValidationException {
        Element md = mdSec.getChild("mdRef", metsNS);
        if (md == null) {
            md = mdSec.getChild("mdWrap", metsNS);
        }
        if (md == null) {
            throw new MetadataValidationException(
                "Invalid METS Manifest: ?mdSec element has neither mdRef nor mdWrap child.");
        }
        String result = md.getAttributeValue("MDTYPE");
        if (result != null && result.equals("OTHER")) {
            result = md.getAttributeValue("OTHERMDTYPE");
        }
        if (result == null) {
            throw new MetadataValidationException(
                "Invalid METS Manifest: " + md.getName() + " has no MDTYPE or OTHERMDTYPE attribute.");
        }
        return result;
    }

    /**
     * Returns MIME type of metadata content, if available.
     *
     * @param mdSec mdSec element
     * @return MIMEtype word, or null if none is available.
     * @throws MetadataValidationException if validation error
     */
    public String getMdContentMimeType(Element mdSec)
        throws MetadataValidationException {
        Element mdWrap = mdSec.getChild("mdWrap", metsNS);
        if (mdWrap != null) {
            String mimeType = mdWrap.getAttributeValue("MIMETYPE");
            if (mimeType == null && mdWrap.getChild("xmlData", metsNS) != null) {
                mimeType = "text/xml";
            }
            return mimeType;
        }
        Element mdRef = mdSec.getChild("mdRef", metsNS);
        if (mdRef != null) {
            return mdRef.getAttributeValue("MIMETYPE");
        }
        return null;
    }

    /**
     * Return contents of *md element as List of XML Element objects.
     * Gets content, dereferencing mdRef if necessary, or decoding and parsing
     * a binData that contains XML.
     *
     * @param mdSec    mdSec element
     * @param callback mdref callback
     * @return contents of metadata section, or empty list if no XML content is available.
     * @throws MetadataValidationException if METS is invalid, or there is an error parsing the XML.
     * @throws PackageValidationException  if invalid package
     * @throws IOException                 if IO error
     * @throws SQLException                if database error
     * @throws AuthorizeException          if authorization error
     */
    private List<Element> getMdContentAsXml(Element mdSec, Mdref callback)
        throws MetadataValidationException, PackageValidationException,
        IOException, SQLException, AuthorizeException {
        try {
            // XXX sanity check: if this has more than one child, consider it
            // an error since we cannot deal with more than one mdRef|mdWrap
            // child.  This may be considered a bug and need to be fixed,
            // so it's best to bring it to the attention of users.
            List mdc = mdSec.getChildren();
            if (mdc.size() > 1) {
                // XXX scaffolding for debugging diagnosis; at least one
                //  XML parser stupidly includes newlines in prettyprinting
                //  as text content objects..
                String id = mdSec.getAttributeValue("ID");
                StringBuilder sb = new StringBuilder();
                for (Iterator mi = mdc.iterator(); mi.hasNext(); ) {
                    sb.append(", ").append(((Content) mi.next()).toString());
                }
                throw new MetadataValidationException("Cannot parse METS with " + mdSec
                    .getQualifiedName() + " element that contains more than one child, size=" + String
                    .valueOf(mdc.size()) + ", ID=" + id + "Kids=" + sb.toString());
            }
            Element mdRef = null;
            Element mdWrap = mdSec.getChild("mdWrap", metsNS);
            if (mdWrap != null) {
                Element xmlData = mdWrap.getChild("xmlData", metsNS);
                if (xmlData == null) {
                    Element bin = mdWrap.getChild("binData", metsNS);
                    if (bin == null) {
                        throw new MetadataValidationException(
                            "Invalid METS Manifest: mdWrap element with neither xmlData nor binData child.");
                    } else {
                        // if binData is actually XML, return it; otherwise ignore.

                        String mimeType = mdWrap.getAttributeValue("MIMETYPE");
                        if (mimeType != null && mimeType.equalsIgnoreCase("text/xml")) {
                            byte value[] = Base64.decodeBase64(bin.getText().getBytes(StandardCharsets.UTF_8));
                            Document mdd = parser.build(new ByteArrayInputStream(value));
                            List<Element> result = new ArrayList<>(1);
                            result.add(mdd.getRootElement());
                            return result;
                        } else {
                            log.warn("Ignoring binData section because MIMETYPE is not XML, but: " + mimeType);
                            return new ArrayList<>(0);
                        }
                    }
                } else {
                    return xmlData.getChildren();
                }
            } else {
                mdRef = mdSec.getChild("mdRef", metsNS);
                if (mdRef != null) {
                    String mimeType = mdRef.getAttributeValue("MIMETYPE");
                    if (mimeType != null && mimeType.equalsIgnoreCase("text/xml")) {
                        // This next line triggers a false-positive XXE warning from LGTM, even though we disallow DTD
                        // parsing during initialization of parser in create()
                        Document mdd = parser.build(callback.getInputStream(mdRef)); // lgtm [java/xxe]
                        List<Element> result = new ArrayList<>(1);
                        result.add(mdd.getRootElement());
                        return result;
                    } else {
                        log.warn("Ignoring mdRef section because MIMETYPE is not XML, but: " + mimeType);
                        return new ArrayList<>(0);
                    }

                } else {
                    throw new MetadataValidationException(
                        "Invalid METS Manifest: ?mdSec element with neither mdRef nor mdWrap child.");
                }
            }
        } catch (JDOMException je) {
            throw new MetadataValidationException(
                "Error parsing or validating metadata section in mdRef or binData within " + mdSec.toString(), je);
        }

    }

    /**
     * Return contents of *md element as stream.
     * Gets content, dereferencing mdRef if necessary, or decoding
     * a binData element if necessary.
     *
     * @param mdSec    mdSec element
     * @param callback mdref callback
     * @return Stream containing contents of metadata section.  Never returns null.
     * @throws MetadataValidationException if METS format does not contain any metadata.
     * @throws PackageValidationException  if invalid package
     * @throws IOException                 if IO error
     * @throws SQLException                if database error
     * @throws AuthorizeException          if authorization error
     */
    public InputStream getMdContentAsStream(Element mdSec, Mdref callback)
        throws MetadataValidationException, PackageValidationException,
        IOException, SQLException, AuthorizeException {
        Element mdRef = null;
        Element mdWrap = mdSec.getChild("mdWrap", metsNS);
        if (mdWrap != null) {
            Element xmlData = mdWrap.getChild("xmlData", metsNS);
            if (xmlData == null) {
                Element bin = mdWrap.getChild("binData", metsNS);
                if (bin == null) {
                    throw new MetadataValidationException(
                        "Invalid METS Manifest: mdWrap element with neither xmlData nor binData child.");
                } else {
                    byte value[] = Base64.decodeBase64(bin.getText().getBytes(StandardCharsets.UTF_8));
                    return new ByteArrayInputStream(value);
                }
            } else {
                XMLOutputter outputPretty = new XMLOutputter(Format.getPrettyFormat());
                return new ByteArrayInputStream(
                    outputPretty.outputString(xmlData.getChildren()).getBytes(StandardCharsets.UTF_8));
            }
        } else {
            mdRef = mdSec.getChild("mdRef", metsNS);
            if (mdRef != null) {
                return callback.getInputStream(mdRef);
            } else {
                throw new MetadataValidationException(
                    "Invalid METS Manifest: ?mdSec element with neither mdRef nor mdWrap child.");
            }
        }
    }


    /**
     * Return the {@code <div>} which describes this DSpace Object (and its contents)
     * from the {@code <structMap>}.  In all cases, this is the first {@code <div>}
     * in the first {@code <structMap>}.
     *
     * @return Element which is the DSpace Object Contents {@code <div>}
     * @throws MetadataValidationException if metadata validation error
     */
    public Element getObjStructDiv()
        throws MetadataValidationException {
        //get first <structMap>
        Element sm = mets.getChild("structMap", metsNS);
        if (sm == null) {
            throw new MetadataValidationException("METS document is missing the required structMap element.");
        }

        //get first <div>
        Element result = sm.getChild("div", metsNS);
        if (result == null) {
            throw new MetadataValidationException(
                "METS document is missing the required first div element in first structMap.");
        }

        if (log.isDebugEnabled()) {
            log.debug("Got getObjStructDiv result=" + result.toString());
        }

        return (Element) result;
    }

    /**
     * Get an array of child object {@code <div>}s from the METS Manifest {@code <structMap>}.
     * These {@code <div>}s reference the location of any child objects METS manifests.
     *
     * @return a List of {@code Element}s, each a {@code <div>}.  May be empty but NOT null.
     * @throws MetadataValidationException if metadata validation error
     */
    public List getChildObjDivs()
        throws MetadataValidationException {
        //get the <div> in <structMap> which describes the current object's contents
        Element objDiv = getObjStructDiv();

        //get the child <div>s -- these should reference the child METS manifest
        return objDiv.getChildren("div", metsNS);
    }

    /**
     * Retrieve the file paths for the children objects' METS Manifest files.
     * These file paths are located in the {@code <mptr>} where @LOCTYPE=URL
     *
     * @return a list of Strings, corresponding to relative file paths of children METS manifests
     * @throws MetadataValidationException if metadata validation error
     */
    public String[] getChildMetsFilePaths()
        throws MetadataValidationException {
        //get our child object <div>s
        List childObjDivs = getChildObjDivs();

        List<String> childPathList = new ArrayList<>();

        if (childObjDivs != null && !childObjDivs.isEmpty()) {
            Iterator childIterator = childObjDivs.iterator();
            //For each Div, we want to find the underlying <mptr> with @LOCTYPE=URL
            while (childIterator.hasNext()) {
                Element childDiv = (Element) childIterator.next();
                //get all child <mptr>'s
                List childMptrs = childDiv.getChildren("mptr", metsNS);

                if (childMptrs != null && !childMptrs.isEmpty()) {
                    Iterator mptrIterator = childMptrs.iterator();
                    //For each mptr, we want to find the one with @LOCTYPE=URL
                    while (mptrIterator.hasNext()) {
                        Element mptr = (Element) mptrIterator.next();
                        String locType = mptr.getAttributeValue("LOCTYPE");
                        //if @LOCTYPE=URL, then capture @xlink:href as the METS Manifest file path
                        if (locType != null && locType.equals("URL")) {
                            String filePath = mptr.getAttributeValue("href", xlinkNS);
                            if (filePath != null && filePath.length() > 0) {
                                childPathList.add(filePath);
                            }
                        }
                    } //end <mptr> loop
                } //end if <mptr>'s exist
            } //end child <div> loop
        } //end if child <div>s exist

        String[] childPaths = new String[childPathList.size()];
        childPaths = (String[]) childPathList.toArray(childPaths);
        return childPaths;
    }

    /**
     * Return the reference to the Parent Object from the "Parent" {@code <structMap>}.
     * This parent object is the owner of current object.
     *
     * @return Link to the Parent Object (this is the Handle of that Parent)
     * @throws MetadataValidationException if metadata validation error
     */
    public String getParentOwnerLink()
        throws MetadataValidationException {

        //get a list of our structMaps
        List<Element> childStructMaps = mets.getChildren("structMap", metsNS);
        Element parentStructMap = null;

        // find the <structMap LABEL='Parent'>
        if (!childStructMaps.isEmpty()) {
            for (Element structMap : childStructMaps) {
                String label = structMap.getAttributeValue("LABEL");
                if (label != null && label.equalsIgnoreCase("Parent")) {
                    parentStructMap = structMap;
                    break;
                }
            }
        }

        if (parentStructMap == null) {
            throw new MetadataValidationException(
                "METS document is missing the required structMap[@LABEL='Parent'] element.");
        }

        //get first <div>
        Element linkDiv = parentStructMap.getChild("div", metsNS);
        if (linkDiv == null) {
            throw new MetadataValidationException(
                "METS document is missing the required first div element in structMap[@LABEL='Parent'].");
        }

        //the link is in the <mptr> in the @xlink:href attribute
        Element mptr = linkDiv.getChild("mptr", metsNS);
        if (mptr != null) {
            return mptr.getAttributeValue("href", xlinkNS);
        }

        //return null if we couldn't find the link
        return null;
    }


    // return a single Element node found by one-off path.
    // use only when path varies each time you call it.
    protected Element getElementByXPath(String path, boolean nullOk)
        throws MetadataValidationException {
        XPathExpression<Element> xpath =
            XPathFactory.instance()
                        .compile(path, Filters.element(), null, metsNS, xlinkNS);
        Element result = xpath.evaluateFirst(mets);
        if (result == null && nullOk) {
            return null;
        } else if (result == null && !nullOk) {
            throw new MetadataValidationException("METSManifest: Failed to resolve XPath, path=\"" + path + "\"");
        } else {
            return result;
        }
    }

    // Find crosswalk for the indicated metadata type (e.g. "DC", "MODS")
    protected Object getCrosswalk(String type, Class clazz) {
        /**
         * Allow DSpace Config to map the metadata type to a
         * different crosswalk name either per-packager or for METS
         * in general.  First, look for config key like:
         *   mets.<packagerName>.ingest.crosswalk.MDNAME = XWALKNAME
         * then try
         *   mets.default.ingest.crosswalk.MDNAME = XWALKNAME
         */
        String xwalkName = configurationService.getProperty(
            CONFIG_METS_PREFIX + configName + ".ingest.crosswalk." + type);
        if (xwalkName == null) {
            xwalkName = configurationService.getProperty(
                CONFIG_METS_PREFIX + "default.ingest.crosswalk." + type);
            if (xwalkName == null) {
                xwalkName = type;
            }
        }
        return CoreServiceFactory.getInstance().getPluginService().getNamedPlugin(clazz, xwalkName);
    }

    /**
     * Gets all dmdSec elements containing metadata for the DSpace Item.
     *
     * @return array of Elements, each a dmdSec.  May be empty but NOT null.
     * @throws MetadataValidationException if the METS is missing a reference to item-wide
     *                                     DMDs in the correct place.
     */
    public Element[] getItemDmds()
        throws MetadataValidationException {
        // div@DMDID is actually IDREFS, a space-separated list of IDs:
        Element objDiv = getObjStructDiv();
        String dmds = objDiv.getAttributeValue("DMDID");
        if (dmds == null) {
            throw new MetadataValidationException(
                "Invalid METS: Missing reference to Item descriptive metadata, first div on first structmap must have" +
                    " a DMDID attribute.");
        }

        return getDmdElements(dmds);
    }


    /**
     * Gets all dmdSec elements from a space separated list
     *
     * @param dmdList space-separated list of DMDIDs
     * @return array of Elements, each a dmdSec.  May be empty but NOT null.
     * @throws MetadataValidationException if the METS is missing a reference to item-wide
     *                                     DMDs in the correct place.
     */
    public Element[] getDmdElements(String dmdList)
        throws MetadataValidationException {
        if (dmdList != null && !dmdList.isEmpty()) {
            String dmdID[] = dmdList.split("\\s+");
            Element result[] = new Element[dmdID.length];

            for (int i = 0; i < dmdID.length; ++i) {
                result[i] = getElementByXPath("mets:dmdSec[@ID=\"" + dmdID[i] + "\"]", false);
            }
            return result;
        } else {
            return new Element[0];
        }
    }


    /**
     * Return rights metadata section(s) relevant to item as a whole.
     *
     * @return array of rightsMd elements, possibly empty but never null.
     * @throws MetadataValidationException if METS is invalid, e.g. referenced amdSec is missing.
     */
    public Element[] getItemRightsMD()
        throws MetadataValidationException {
        // div@ADMID is actually IDREFS, a space-separated list of IDs:
        Element objDiv = getObjStructDiv();
        String amds = objDiv.getAttributeValue("ADMID");
        if (amds == null) {
            if (log.isDebugEnabled()) {
                log.debug("getItemRightsMD: No ADMID references found.");
            }
            return new Element[0];
        }
        String amdID[] = amds.split("\\s+");
        List<Element> resultList = new ArrayList<>();
        for (int i = 0; i < amdID.length; ++i) {
            List rmds = getElementByXPath("mets:amdSec[@ID=\"" + amdID[i] + "\"]", false).
                                                                                             getChildren("rightsMD",
                                                                                                         metsNS);
            if (rmds.size() > 0) {
                resultList.addAll(rmds);
            }
        }
        return resultList.toArray(new Element[resultList.size()]);
    }

    /**
     * Invokes appropriate crosswalks on Item-wide descriptive metadata.
     *
     * @param context  context
     * @param callback mdref callback
     * @param dso      DSpaceObject
     * @param params   package params
     * @param dmdSec   dmdSec element
     * @throws MetadataValidationException if METS error
     * @throws CrosswalkException          if crosswalk error
     * @throws PackageValidationException  if invalid package
     * @throws IOException                 if IO error
     * @throws SQLException                if database error
     * @throws AuthorizeException          if authorization error
     */
    public void crosswalkItemDmd(Context context, PackageParameters params,
                                 DSpaceObject dso,
                                 Element dmdSec, Mdref callback)
        throws MetadataValidationException, PackageValidationException,
        CrosswalkException, IOException, SQLException, AuthorizeException {
        crosswalkXmd(context, params, dso, dmdSec, callback, false);
    }

    /**
     * Crosswalk all technical and source metadata sections that belong
     * to the whole object.
     *
     * @param context  context
     * @param callback mdref callback
     * @param params   package params
     * @param dso      DSpaceObject
     * @throws MetadataValidationException if METS is invalid, e.g. referenced amdSec is missing.
     * @throws PackageValidationException  if invalid package
     * @throws IOException                 if IO error
     * @throws SQLException                if database error
     * @throws AuthorizeException          if authorization error
     */
    public void crosswalkObjectOtherAdminMD(Context context, PackageParameters params,
                                            DSpaceObject dso, Mdref callback)
        throws MetadataValidationException, PackageValidationException,
        CrosswalkException, IOException, SQLException, AuthorizeException {
        for (String amdID : getAmdIDs()) {
            Element amdSec = getElementByXPath("mets:amdSec[@ID=\"" + amdID + "\"]", false);
            for (Iterator ti = amdSec.getChildren("techMD", metsNS).iterator(); ti.hasNext(); ) {
                crosswalkXmd(context, params, dso, (Element) ti.next(), callback, false);
            }
            for (Iterator ti = amdSec.getChildren("digiprovMD", metsNS).iterator(); ti.hasNext(); ) {
                crosswalkXmd(context, params, dso, (Element) ti.next(), callback, false);
            }
            for (Iterator ti = amdSec.getChildren("rightsMD", metsNS).iterator(); ti.hasNext(); ) {
                crosswalkXmd(context, params, dso, (Element) ti.next(), callback, false);
            }
        }
    }

    /**
     * Just crosswalk the sourceMD sections; used to set the handle and parent of AIP.
     *
     * @param context  context
     * @param callback mdref callback
     * @param params   package params
     * @param dso      DSpaceObject
     * @return true if any metadata section was actually crosswalked, false otherwise
     * @throws MetadataValidationException if METS is invalid, e.g. referenced amdSec is missing.
     * @throws PackageValidationException  if invalid package
     * @throws IOException                 if IO error
     * @throws SQLException                if database error
     * @throws AuthorizeException          if authorization error
     * @throws CrosswalkException          if crosswalk error
     */
    public boolean crosswalkObjectSourceMD(Context context, PackageParameters params,
                                           DSpaceObject dso, Mdref callback)
        throws MetadataValidationException, PackageValidationException,
        CrosswalkException, IOException, SQLException, AuthorizeException {
        boolean result = false;

        for (String amdID : getAmdIDs()) {
            Element amdSec = getElementByXPath("mets:amdSec[@ID=\"" + amdID + "\"]", false);
            for (Iterator ti = amdSec.getChildren("sourceMD", metsNS).iterator(); ti.hasNext(); ) {
                crosswalkXmd(context, params, dso, (Element) ti.next(), callback, false);
                result = true;
            }
        }
        return result;
    }

    /**
     * Get an array of all AMDID values for this object
     *
     * @return array of all AMDID values for this object
     * @throws MetadataValidationException if metadata validation error
     */
    protected String[] getAmdIDs()
        throws MetadataValidationException {
        // div@ADMID is actually IDREFS, a space-separated list of IDs:
        Element objDiv = getObjStructDiv();
        String amds = objDiv.getAttributeValue("ADMID");
        if (amds == null) {
            if (log.isDebugEnabled()) {
                log.debug("crosswalkObjectTechMD: No ADMID references found.");
            }
            return new String[0];
        }
        return amds.split("\\s+");
    }

    // Crosswalk *any* kind of metadata section - techMD, rightsMD, etc.
    protected void crosswalkXmd(Context context, PackageParameters params,
                                DSpaceObject dso,
                                Element xmd, Mdref callback, boolean createMissingMetadataFields)
        throws MetadataValidationException, PackageValidationException,
        CrosswalkException, IOException, SQLException, AuthorizeException {
        String type = getMdType(xmd);

        //First, try to find the IngestionCrosswalk to use
        IngestionCrosswalk xwalk = (IngestionCrosswalk) getCrosswalk(type, IngestionCrosswalk.class);

        // If metadata is not simply applicable to object,
        // let it go with a warning.
        try {
            // If we found the IngestionCrosswalk, crosswalk our XML-based content
            if (xwalk != null) {
                // Check if our Crosswalk actually wraps another Packager Plugin
                if (xwalk instanceof AbstractPackagerWrappingCrosswalk) {
                    // If this crosswalk wraps another Packager Plugin, we can pass it our Packaging Parameters
                    // (which essentially allow us to customize the ingest process of the crosswalk)
                    AbstractPackagerWrappingCrosswalk wrapper = (AbstractPackagerWrappingCrosswalk) xwalk;
                    wrapper.setPackagingParameters(params);
                }

                xwalk.ingest(context, dso, getMdContentAsXml(xmd, callback), false);
            } else {
                // Otherwise, try stream-based crosswalk
                StreamIngestionCrosswalk sxwalk =
                    (StreamIngestionCrosswalk) getCrosswalk(type, StreamIngestionCrosswalk.class);

                if (sxwalk != null) {
                    // Check if our Crosswalk actually wraps another Packager Plugin
                    if (sxwalk instanceof AbstractPackagerWrappingCrosswalk) {
                        // If this crosswalk wraps another Packager Plugin, we can pass it our Packaging Parameters
                        // (which essentially allow us to customize the ingest process of the crosswalk)
                        AbstractPackagerWrappingCrosswalk wrapper = (AbstractPackagerWrappingCrosswalk) sxwalk;
                        wrapper.setPackagingParameters(params);
                    }

                    // If we found a Stream-based crosswalk that matches, we now want to
                    // locate the stream we are crosswalking.  This stream should be
                    // references in METS via an <mdRef> element
                    // (which is how METS references external files)
                    Element mdRef = xmd.getChild("mdRef", metsNS);
                    if (mdRef != null) {
                        InputStream in = null;
                        try {
                            in = callback.getInputStream(mdRef);
                            sxwalk.ingest(context, dso, in,
                                          mdRef.getAttributeValue("MIMETYPE"));
                        } finally {
                            if (in != null) {
                                in.close();
                            }
                        }
                    } else {
                        // If we couldn't find an <mdRef>, then we'll try an <mdWrap>
                        // with a <binData> element instead.
                        // (this is how METS wraps embedded base64-encoded content streams)

                        Element mdWrap = xmd.getChild("mdWrap", metsNS);
                        if (mdWrap != null) {
                            Element bin = mdWrap.getChild("binData", metsNS);
                            if (bin == null) {
                                throw new MetadataValidationException(
                                    "Invalid METS Manifest: mdWrap element for streaming crosswalk without binData " +
                                        "child.");
                            } else {
                                byte value[] = Base64.decodeBase64(bin.getText().getBytes(StandardCharsets.UTF_8));
                                sxwalk.ingest(context, dso,
                                              new ByteArrayInputStream(value),
                                              mdWrap.getAttributeValue("MIMETYPE"));
                            }
                        } else {
                            throw new MetadataValidationException("Cannot process METS Manifest: " +
                                                                      "Metadata of type=" + type + " requires a " +
                                                                      "reference to a stream (mdRef), which was not " +
                                                                      "found in " + xmd
                                .getName());
                        }
                    }
                } else {
                    throw new MetadataValidationException("Cannot process METS Manifest: " +
                                                              "No crosswalk found for contents of " + xmd
                        .getName() + " element, MDTYPE=" + type);
                }
            }
        } catch (CrosswalkObjectNotSupported e) {
            log.warn("Skipping metadata section " + xmd
                .getName() + ", type=" + type + " inappropriate for this type of object: Object=" + dso
                .toString() + ", error=" + e.toString());
        }
    }

    /**
     * Crosswalk the metadata associated with a particular <code>file</code>
     * element into the bitstream it corresponds to.
     *
     * @param context   a dspace context.
     * @param params    any PackageParameters which may affect how bitstreams are crosswalked
     * @param bitstream bitstream target of the crosswalk
     * @param fileId    value of ID attribute in the file element responsible
     *                  for the contents of that bitstream.
     * @param callback  mdref callback
     * @throws MetadataValidationException if METS is invalid, e.g. referenced amdSec is missing.
     * @throws PackageValidationException  if invalid package
     * @throws IOException                 if IO error
     * @throws SQLException                if database error
     * @throws AuthorizeException          if authorization error
     * @throws CrosswalkException          if crosswalk error
     */
    public void crosswalkBitstream(Context context, PackageParameters params,
                                   Bitstream bitstream,
                                   String fileId, Mdref callback)
        throws MetadataValidationException, PackageValidationException,
        CrosswalkException, IOException, SQLException, AuthorizeException {
        Element file = getElementByXPath("descendant::mets:file[@ID=\"" + fileId + "\"]", false);
        if (file == null) {
            throw new MetadataValidationException(
                "Failed in Bitstream crosswalk, Could not find file element with ID=" + fileId);
        }

        // In DSpace METS SIP spec, admin metadata is only "highly
        // recommended", not "required", so it is OK if there is no ADMID.
        String amds = file.getAttributeValue("ADMID");
        if (amds == null) {
            log.warn("Got no bitstream ADMID, file@ID=" + fileId);
            return;
        }
        String amdID[] = amds.split("\\s+");
        for (int i = 0; i < amdID.length; ++i) {
            Element amdSec = getElementByXPath("mets:amdSec[@ID=\"" + amdID[i] + "\"]", false);
            for (Iterator ti = amdSec.getChildren("techMD", metsNS).iterator(); ti.hasNext(); ) {
                crosswalkXmd(context, params, bitstream, (Element) ti.next(), callback, false);
            }
            for (Iterator ti = amdSec.getChildren("sourceMD", metsNS).iterator(); ti.hasNext(); ) {
                crosswalkXmd(context, params, bitstream, (Element) ti.next(), callback, false);
            }
            for (Iterator ti = amdSec.getChildren("rightsMD", metsNS).iterator(); ti.hasNext(); ) {
                crosswalkXmd(context, params, bitstream, (Element) ti.next(), callback, false);
            }
        }
    }


    public void crosswalkBundle(Context context, PackageParameters params,
                                Bundle bundle,
                                String fileId, Mdref callback)
        throws MetadataValidationException, PackageValidationException,
        CrosswalkException, IOException, SQLException, AuthorizeException {
        Element file = getElementByXPath("descendant::mets:fileGrp[@ADMID=\"" + fileId + "\"]", false);
        if (file == null) {
            throw new MetadataValidationException(
                "Failed in Bitstream crosswalk, Could not find file element with ID=" + fileId);
        }

        // In DSpace METS SIP spec, admin metadata is only "highly
        // recommended", not "required", so it is OK if there is no ADMID.
        String amds = file.getAttributeValue("ADMID");
        if (amds == null) {
            log.warn("Got no bitstream ADMID, file@ID=" + fileId);
            return;
        }
        String amdID[] = amds.split("\\s+");
        for (int i = 0; i < amdID.length; ++i) {
            Element amdSec = getElementByXPath("mets:amdSec[@ID=\"" + amdID[i] + "\"]", false);
            for (Iterator ti = amdSec.getChildren("techMD", metsNS).iterator(); ti.hasNext(); ) {
                crosswalkXmd(context, params, bundle, (Element) ti.next(), callback, false);
            }
            for (Iterator ti = amdSec.getChildren("sourceMD", metsNS).iterator(); ti.hasNext(); ) {
                crosswalkXmd(context, params, bundle, (Element) ti.next(), callback, false);
            }
            for (Iterator ti = amdSec.getChildren("rightsMD", metsNS).iterator(); ti.hasNext(); ) {
                crosswalkXmd(context, params, bundle, (Element) ti.next(), callback, false);
            }
        }
    }

    /**
     * @return root element of METS document.
     */
    public Element getMets() {
        return mets;
    }

    /**
     * Return entire METS document as an inputStream
     *
     * @return entire METS document as a stream
     */
    public InputStream getMetsAsStream() {
        XMLOutputter outputPretty = new XMLOutputter(Format.getPrettyFormat());

        return new ByteArrayInputStream(
            outputPretty.outputString(mets).getBytes(StandardCharsets.UTF_8));
    }
}
