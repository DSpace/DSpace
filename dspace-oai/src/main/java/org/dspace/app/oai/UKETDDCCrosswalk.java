/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.oai;

import java.sql.SQLException;
import java.util.Properties;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.dspace.core.ConfigurationManager;
import org.dspace.core.Utils;
import org.dspace.search.HarvestedItemInfo;
import org.dspace.content.*;

import ORG.oclc.oai.server.crosswalk.Crosswalk;
import ORG.oclc.oai.server.verb.CannotDisseminateFormatException;

/**
 * A Crosswalk implementation that extracts qualified Dublin Core from
 * DSpace items into the uketd_dc format.
 *
 * It supports the writing of UKETD_DC metadata
 * in a METS document and to make the schema URIs available for
 * inclusion in such a METS document. For this reason, the writing
 * of the metadata itself has been separated from the writing
 * of the schemas.
 * This version places the writing of the header and metadata
 * in its own method called by createMetadata so the headers are
 * included in the UKETD_METS that also uses those methods.
 * This allows the writeMetadata method to remain unchanged,
 * with no header information included. It is therefore consistent with
 * other DSpace crosswalks.
 *
 * @author Paul Needham (Cranfield University)
 * @author Jon Bell & Stuart Lewis (Aberystwyth University)
 */
public class UKETDDCCrosswalk extends Crosswalk
{
	// Pattern containing all the characters we want to filter out / replace
	// converting a String to xml
	private static final Pattern invalidXmlPattern =
		Pattern.compile("([^\\t\\n\\r\\u0020-\\ud7ff\\ue000-\\ufffd\\u10000-\\u10ffff]+|[&<>])");

    // String constants for metadata schemas...

    /** Used to open the metadata in a OAI-PMH record. */
    private String uketdIn = "<uketd_dc:uketddc";

    /** The identifier for the uketd namespace. */
    private String uketdNs = "uketd_dc";

    /** The URI of the uketd namespace. */
    private String uketdUri = "http://naca.central.cranfield.ac.uk/ethos-oai/2.0/";

    /** The identifier for the namespace of the DC used in the UKETD_DC metadata set. */
    private String dcNs = "dc";

    /** The URI of the DC namespace. */
    private String dcUri = "http://purl.org/dc/elements/1.1/";

    /** The identifier for the namespace of the qualified DC terms used in UKETD_DC. */
    private String dcTermsNs = "dcterms";

    /** The URI of the DC terms namespace. */
    private String dcTermsUri = "http://purl.org/dc/terms/";

    /** Identifier of the UKETD terms namespace.*/
    private String uketdTermsNs = "uketdterms";

    /** The URI of the uketd terms namespace. */
    private String uketdTermsUri = "http://naca.central.cranfield.ac.uk/ethos-oai/terms/";

    /** The xsi string (identifier and URI) used for UKETD records.*/
    private String xsi = "xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"";

    /** The xsi schema location tag, used in UKETD records. */
    private String schemaLoc = "xsi:schemaLocation";

    /** The URI of the uketd location namespace. */
    private static String uketdSchemaLocNs = "http://naca.central.cranfield.ac.uk/ethos-oai/2.0/";

    /** The URI of the uketd location. */
    private static String uketdSchemaLocUri = "http://naca.central.cranfield.ac.uk/ethos-oai/2.0/uketd_dc.xsd";


    /**
     * UKETDDCCrosswalk contructor.
     *
     * @param properties Not used
     * */
    public UKETDDCCrosswalk(Properties properties)
    {
        super(uketdSchemaLocNs + " " + uketdSchemaLocUri);
    }

    /**
     * Returns the identifier for the UKETD namespace.
     *
     * @return uketdNs
     */
    public String getUketdNs ()
    {
        return uketdNs;
    }

    /**
     * Returns the URI of the UKETD namespace.
     *
     * @return uketdUri
     */
    public String getUketdUri ()
    {
        return uketdUri;
    }

    /**
     * Returns the identifier for the Dublin Core namespace.
     *
     * @return dcNs
     */
    public String getDcNs ()
    {
        return dcNs;
    }

    /**
     * Returns the URI of the Dublin Core namespace.
     *
     * @return dcUri
     */
    public String getDcUri ()
    {
        return dcUri;
    }

    /**
     * Returns the identifier for the DC terms (qualifiers) namespace.
     *
     * @return cdTermsNs
     */
    public String getDcTermsNs ()
    {
        return dcTermsNs;
    }

    /**
     * Returns the URI of the DC terms namespace.
     *
     * @return dcTermsUri
     */
    public String getDcTermsUri ()
    {
        return dcTermsUri;
    }

    /**
     * Returns the identifier for the UKETD terms namespace.
     *
     * @return uketdTermsNs
     */
    public String getUketdTermsNs ()
    {
        return uketdTermsNs;
    }

    /**
     * Returns the URI of the UKETD terms namespace.
     *
     * @return uketdTermsUri
     */
    public String getUketdTermsUri ()
    {
        return uketdTermsUri;
    }

    /**
     * Returns the identifier for the UKETD schema location.
     *
     * @return uketdSchemaLocNs
     */
    public String getUketdSchemaLocNs ()
    {
        return uketdSchemaLocNs;
    }

    /**
     * Returns the URI of the UKETD schema location.
     *
     * @return uketdSchemaLocUri
     */
    public String getUketdSchemaLocUri ()
    {
        return uketdSchemaLocUri;
    }

    /**
     * Shows what items UKETD_DC OAI-PMH is available for.
     * This is every item in the repository.
     *
     * @return a boolean (true)
     */
    public boolean isAvailableFor(Object nativeItem)
    {
        // We have DC for everything
        return true;
    }

    /**
     * Creates the metadata necessary for UKTEDDC crosswalk.
     * Adds the name space details and schemas to the metadata itself.
     * It therefore creates a complete OAI-PMH record that matches
     * the UKETD_DC metadata prefix.
     *
     * @return The OAI-PMH xml
     */
    public String createMetadata (Object nativeItem)
                                throws CannotDisseminateFormatException
    {
        // Get the Item
        Item item = ((HarvestedItemInfo) nativeItem).item;

        // Write the record out
        return writeMetadataWithSchema(item);
    }

   /**
     * Write the item's metadata, headed by the schema namespace
     * details. Separated from createMetadata, so UKETD_METS can
     * use the method with an Item , not an Object (nativeItem).
     *
     * @param item The org.dspace.content.Item
     * @return a String, the item's metadata in UKETD_DC format.
     * @throws SQLException
     */
    public String writeMetadataWithSchema (Item item)
    {
        StringBuffer metadata = new StringBuffer ();
        metadata.append(uketdIn).append(" ");
        metadata.append ("xmlns:" + uketdNs + "=\"" + uketdUri + "\" ");
        metadata.append ("xmlns:" + dcNs + "=\"" + dcUri + "\" ");
        metadata.append ("xmlns:" + dcTermsNs + "=\"" + dcTermsUri + "\" ");
        metadata.append ("xmlns:" + uketdTermsNs + "=\"" + uketdTermsUri + "\" ");
        metadata.append(xsi).append(" ");
        metadata.append (schemaLoc + "=\"" + uketdSchemaLocNs + " ");
        metadata.append(uketdSchemaLocUri).append("\">\n");
        metadata.append (writeMetadata (item));
        metadata.append ("</uketd_dc:uketddc>\n");
        return metadata.toString ( );
    }


    /**
     * Writes the UKETD_DC metadata for the specified item.
     * It simply gets hold of the Dublin Core for an Item
     * and converts it to UKEDT_DC, including the splitting
     * of the Dublin Core publisher and type fields.
     * The metadata is identical to that returned by
     * the original version's create metadata method,
     * without the schema information.
     * This method does no checking of the correctness of the
     * metadata format, nor does it throw any exception.
     *
     * @param item a org.dspace.content.Item
     * @return a String, the item's metadata in UKETD_DC xml.
     */
    public String writeMetadata(Item item)
    {
        // The string we are constructing
        StringBuffer metadata = new StringBuffer();

        // Get all the DC
        DCValue[] allDC = item.getMetadata(MetadataSchema.DC_SCHEMA, Item.ANY, Item.ANY, Item.ANY);

        // Get the handle of the item
        String itemhandle = item.getHandle();

        for (int i = 0; i < allDC.length; i++)
        {
            // Get the element, qualifier and value
            String element = allDC[i].element;
            String qualifier = allDC[i].qualifier;
            String value = Utils.addEntities(allDC[i].value);

            // title
            if (allDC[i].element.equals("title"))
            {
                if (allDC[i].qualifier != null) {
                    if (allDC[i].qualifier.equals("alternative"))
                    {
                        // title.alternative exposed as 'dcterms:alternative'
                        this.makeDCTermsElement(qualifier, null, value, metadata);
                    }
                } else
                {
                    this.makeDCElement(element, null, value, metadata);
                }
            }

            // contributor
            if (allDC[i].element.equals("contributor"))
            {
                if (allDC[i].qualifier != null) {
                    if (allDC[i].qualifier.equals("author"))
                    {
                        this.makeDCElement("creator", null, value, metadata);
                    } else if ((allDC[i].qualifier.equals("advisor")) ||
                               (allDC[i].qualifier.equals("sponsor")))
                    {
                        // contributor.qualifier exposed as 'uketdterms:qualifier'
                        this.makeUKDCTermsElement(qualifier, null, value, metadata);
                    } else if (allDC[i].qualifier.equals("funder"))
                    {
										    // contributor.qualifier exposed as 'uketdterms:qualifier'
                        this.makeUKDCTermsElement("sponsor", null, value, metadata);
                    } else
                    {
                        // contributor.qualifier exposed as 'dcterms:qualifier'
                        this.makeDCTermsElement(qualifier, null, value, metadata);
                    }
                } else {
                    this.makeDCElement(element, null, value, metadata);
                }
            }

            // subject
            if (allDC[i].element.equals("subject"))
            {
                if (allDC[i].qualifier != null) {
                    boolean ddc = allDC[i].qualifier.equals("ddc");
                    boolean lcc = allDC[i].qualifier.equals("lcc");
                    boolean lcsh = allDC[i].qualifier.equals("lcsh");
                    boolean mesh = allDC[i].qualifier.equals("mesh");
                    boolean udc = allDC[i].qualifier.equals("udc");
                    if (ddc || lcc || lcsh || mesh || udc)
                    {
                        // subject.qualifier exposed as 'dc:element xsi:type="dcterms:qualifier"'
                        qualifier = qualifier.toUpperCase();
                        this.makeDCElement(element, qualifier, value, metadata);
                    } else
                    {
                        this.makeDCElement(element, null, value, metadata);
                    }
                } else
                {
                    this.makeDCElement(element, null, value, metadata);
                }
            }

            // description
            if (allDC[i].element.equals("description"))
            {
                if (allDC[i].qualifier != null)
                {
                    if (allDC[i].qualifier.equals("abstract"))
                    {
                        // e.g. description.abstract exposed as 'dcterms:abstract'
                        this.makeDCTermsElement(qualifier, null, value, metadata);
                    } else if  (allDC[i].qualifier.equals("sponsorship"))
                    {
                        // description.sponsorship exposed as 'uketdterms:sponsor"'
                        this.makeUKDCTermsElement("sponsor", null, value, metadata);
                    }
                } else {
                    this.makeDCElement(element, null, value, metadata);
                }
            }

            // publisher
            if (allDC[i].element.equals("publisher"))
            {
                if (allDC[i].qualifier != null) {
                    if ((allDC[i].qualifier.equals("department")) ||
                    (allDC[i].qualifier.equals("commercial")))
                    {
                        this.makeUKDCTermsElement(qualifier, null, value, metadata);
                    }
                } else {
                    String[] pubParts = value.split("(?<!(&[0-9a-zA-Z#]{2,4}));");
                    this.makeUKDCTermsElement("institution", null,
                                              pubParts[0], metadata);
                    StringBuffer dept = new StringBuffer();
                    if ((pubParts.length > 1) && (pubParts[1] != null)) {
                        dept.append(pubParts[1] + ";");
                    }
                    if ((pubParts.length > 2) && (pubParts[2] != null)) {
                        dept.append(" " + pubParts[2]);
                    }
                    if (dept.length() > 0) {
                            this.makeUKDCTermsElement("department", null,
                                                      dept.toString(), metadata);
                    }
                }

            }

            // date
            if (allDC[i].element.equals("date"))
            {
                if (allDC[i].qualifier != null)
                {
                    if (allDC[i].qualifier.equals("issued"))
                    {
                        this.makeDCTermsElement(qualifier, null, value, metadata);
                    } else
                    {
                        this.makeDCElement(element, null, value, metadata);
                    }
                } else
                {
                    this.makeDCElement(element, null, value, metadata);
                }
            }

            // type
            if (allDC[i].element.equals("type"))
            {
                if (allDC[i].qualifier != null)
                {
                    if ((allDC[i].qualifier.equals("qualificationlevel")) ||
                        (allDC[i].qualifier.equals("qualificationname")))
                    {
                        this.makeUKDCTermsElement(qualifier, null, value, metadata);
                    }
                } else {
                    String[] Typepart = value.split("[;]");
                    this.makeDCElement(element, null, Typepart[0], metadata);
                    if ((Typepart.length > 1) && (Typepart[1] != null))
                    {
                        this.makeUKDCTermsElement("qualificationlevel", null,
                                                  Typepart[1], metadata);
                    } if ((Typepart.length > 2) && (Typepart[2] != null))
                    {
                        this.makeUKDCTermsElement("qualificationname", null,
                                                  Typepart[2], metadata);
                    }
                }
            }

            // language
            if (allDC[i].element.equals("language"))
            {
                if (allDC[i].qualifier != null) {
                    if (allDC[i].qualifier.equals("iso"))
                    {
                        // language.iso exposed as 'dc:element xsi:type="dcterms:qualifier"'
                        this.makeDCElement(element, "ISO639-2", value, metadata);
                    } else
                    {
                        this.makeDCElement(element, null, value, metadata);
                    }
                } else
                {
                    this.makeDCElement(element, null, value, metadata);
                }
            }

            // relation
            if (allDC[i].element.equals("relation"))
            {
                if (allDC[i].qualifier != null) {
                    if (allDC[i].qualifier.equals("hasversion"))
                    {
                        // relation.hasversion exposed as 'dcterms:qualifier'
                        this.makeDCElement("hasVersion", null, value, metadata);
                    } else if ((allDC[i].qualifier.equals("references")) ||
                               (allDC[i].qualifier.equals("requires")))
                    {
                        // relation.references exposed as 'dcterms:qualifier'
                        this.makeDCTermsElement(qualifier, null, value, metadata);
                    } else
                    {
                        this.makeDCElement(element, null, value, metadata);
                    }
                } else
                {
                    this.makeDCElement(element, null, value, metadata);
                }
            }

            // format
            if (allDC[i].element.equals("format"))
            {
                if (allDC[i].qualifier != null)
                {
                    if (allDC[i].qualifier.equals("extent"))
                    {
                        // format exposed as 'dcterms:qualifier'
                        this.makeDCTermsElement(qualifier, null, value, metadata);
                    } else if (allDC[i].qualifier.equals("mimetype"))
                    {
                        this.makeDCElement(element, "IMT", value, metadata);
                    }
                } else
                {
                    // format exposed as 'dc:element'
                    this.makeDCElement(element, null, value, metadata);
                }
            }

            // identifier
            if (allDC[i].element.equals("identifier"))
            {
                if (allDC[i].qualifier != null)
                {
                    if (allDC[i].qualifier.equals("uri"))
                    {
                        this.makeDCTermsElement("isReferencedBy", "URI", value, metadata);
                    } else if (allDC[i].qualifier.equals("citation"))
                    {
                        this.makeDCTermsElement("hasVersion", null, value, metadata);
                    } else if (allDC[i].qualifier.equals("grantnumber"))
                    {
                        this.makeUKDCTermsElement(qualifier, null, value, metadata);
                    }
                } else
                {
                    // identifier exposed as 'dc:element'
                    this.makeDCElement(element, null, value, metadata);
                }
            }

            // rights
            if (allDC[i].element.equals("rights"))
            {
                if (allDC[i].qualifier != null)
                {
                    if ((allDC[i].qualifier.equals("embargodate")) ||
                        (allDC[i].qualifier.equals("embargoreason")))
                    {
                        this.makeUKDCTermsElement(qualifier, null, value, metadata);
                    } else
                    {
                        // rights exposed as 'dc:element'
                        this.makeDCElement(element, null, value, metadata);
                    }
                } else
                {
                    // rights exposed as 'dc:element'
                    this.makeDCElement(element, null, value, metadata);
                }
            }
        }

        // Generate bitstream URIs
        Bundle[] bundles = {};
        try
        {
            bundles = item.getBundles("ORIGINAL");
            String url;
            if (bundles.length > 0)
            {
                // Itterate through each bundle
                for (int i = 0; i < bundles.length; i++)
                {
                    // Itterate through each bitstream
                    Bitstream[] bitstreams = bundles[i].getBitstreams();
                    for (int k = 0; k < bitstreams.length ; k++)
                    {
                        // Skip internal types
                        if (!bitstreams[k].getFormat().isInternal())
                        {
                            url = ConfigurationManager.getProperty("dspace.url") +
                                  "/bitstream/" + itemhandle + "/" +
                                  bitstreams[k].getSequenceID() + "/" +
                                  bitstreams[k].getName();
                            this.makeDCElement("identifier", "URI", url, metadata);
                            this.makeUKDCTermsElement("checksum",
                                                      bitstreams[k].getChecksumAlgorithm(),
                                                      bitstreams[k].getChecksum(), metadata);
                        }
                    }
                }
            }
        } catch (SQLException sqle)
        {
            // Nothing we can do
        }

        // Return the metadata - all done!
        return metadata.toString();
    }

    /**
     * Private wrapper method to create a DC term element.
     *
     * @param element The element name
     * @param qualifier The qualifier name (or null)
     * @param value The value of the element
     * @param buffer The buffer to add the element to
     * @return The buffer with the new element appended to
     */
    private StringBuffer makeDCElement(String element, String qualifier,
                                            String value, StringBuffer buffer)
    {
        return this.makeTermsElement(element, qualifier, value,
                                     buffer, "dc", "dcterms");
    }

    /**
     * Private wrapper method to create a DCterms term element.
     *
     * @param element The element name
     * @param qualifier The qualifier name (or null)
     * @param value The value of the element
     * @param buffer The buffer to add the element to
     * @return The buffer with the new element appended to
     */
    private StringBuffer makeDCTermsElement(String element, String qualifier,
                                            String value, StringBuffer buffer)
    {
        return this.makeTermsElement(element, qualifier, value,
                                     buffer, "dcterms", "dcterms");
    }

    /**
     * Private wrapper method to create a UKETD DC term element.
     *
     * @param element The element name
     * @param qualifier The qualifier name (or null)
     * @param value The value of the element
     * @param buffer The buffer to add the element to
     * @return The buffer with the new element appended to
     */
    private StringBuffer makeUKDCTermsElement(String element, String qualifier,
                                              String value, StringBuffer buffer)
    {
        return this.makeTermsElement(element, qualifier, value,
                                     buffer, "uketdterms", "uketdterms");
    }

    /**
     * Private wrapper method to create an element.
     *
     * @param element The element name
     * @param qualifier The qualifier name (or null)
     * @param value The value of the element
     * @param buffer The buffer to add the element to
     * @param terms The namespace of the term
     * @return The buffer with the new element appended to
     */

    private StringBuffer makeTermsElement(String element, String qualifier,
                                          String value, StringBuffer buffer,
                                          String namespace, String terms)
    {
        // Escape XML chars <, > and &
        // Also replace all invalid characters with ' '
        if (value != null)
        {
          	StringBuffer valueBuf = new StringBuffer(value.length());
           	Matcher xmlMatcher = invalidXmlPattern.matcher(value.trim());
           	while (xmlMatcher.find())
           	{
           		String group = xmlMatcher.group();

           		// group will either contain a character that we need to encode for xml
           		// (ie. <, > or &), or it will be an invalid character
           		// test the contents and replace appropriately
           		if ("&".equals(group))
                   {
                       xmlMatcher.appendReplacement(valueBuf, "&amp;");
                   }
           		else if ("<".equals(group))
                   {
                       xmlMatcher.appendReplacement(valueBuf, "&lt;");
                   }
           		else if (">".equals(group))
                   {
                       xmlMatcher.appendReplacement(valueBuf, "&gt;");
                   }
           		else
                   {
                       xmlMatcher.appendReplacement(valueBuf, " ");
                   }
              }

           	  // add bit of the string after the final match
           	  xmlMatcher.appendTail(valueBuf);

            if (qualifier == null)
            {
                buffer.append("<").append(namespace).append(":").append(element).append(">").append(valueBuf.toString()).append("</").append(namespace).append(":").append(element).append(">\n");
            } else
            {
                buffer.append("<").append(namespace).append(":").append(element).append(" xsi:type=\"").append(terms).append(":").append(qualifier).append("\">").append(valueBuf.toString()).append("</").append(namespace).append(":").append(element).append(">\n");
            }
        }
        else
        {
            buffer.append("<").append(namespace).append(":").append(element).append(" />\n");
        }

        // Return the updated buffer
        return buffer;
    }
}