/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.xoai.util;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.dspace.app.util.MetadataExposure;
import org.dspace.authorize.AuthorizeException;
import org.dspace.authorize.AuthorizeManager;
import org.dspace.authorize.ResourcePolicy;
import org.dspace.content.Bitstream;
import org.dspace.content.Bundle;
import org.dspace.content.Item;
import org.dspace.content.Metadatum;
import org.dspace.content.authority.Choices;
import org.dspace.core.ConfigurationManager;
import org.dspace.core.Constants;
import org.dspace.core.Context;
import org.dspace.core.Utils;
import org.dspace.xoai.app.PermissionElementAdditional;
import org.dspace.xoai.data.DSpaceItem;

import com.lyncode.xoai.dataprovider.xml.xoai.Element;
import com.lyncode.xoai.dataprovider.xml.xoai.Metadata;
import com.lyncode.xoai.util.Base64Utils;

/**
 * 
 * @author Lyncode Development Team <dspace@lyncode.com>
 */
@SuppressWarnings("deprecation")
public class ItemUtils
{
    public static final String RESTRICTED_ACCESS = "restricted access";

    public static final String EMBARGOED_ACCESS = "embargoed access";

    public static final String OPEN_ACCESS = "open access";

    public static final String METADATA_ONLY_ACCESS = "metadata only access";

    private static Logger log = LogManager
            .getLogger(ItemUtils.class);

    private static SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
    
    public static Element getElement(List<Element> list, String name)
    {
        for (Element e : list)
            if (name.equals(e.getName()))
                return e;

        return null;
    }
    public static Element create(String name)
    {
        Element e = new Element();
        e.setName(name);
        return e;
    }

    public static Element.Field createValue(
            String name, String value)
    {
        Element.Field e = new Element.Field();
        e.setValue(value);
        e.setName(name);
        return e;
    }
    public static Metadata retrieveMetadata (Context context, Item item) {
        Metadata metadata;
        
        //DSpaceDatabaseItem dspaceItem = new DSpaceDatabaseItem(item);
        
        // read all metadata into Metadata Object
        metadata = new Metadata();
        Metadatum[] vals = item.getMetadata(Item.ANY, Item.ANY, Item.ANY, Item.ANY);
        for (Metadatum val : vals)
        {
            // Don't expose fields that are hidden by configuration
            try {
                if (MetadataExposure.isHidden(context,
                        val.schema,
                        val.element,
                        val.qualifier))
                {
                    continue;
                }
            } catch(SQLException se) {
                throw new RuntimeException(se);
            }

            Element valueElem = null;
            Element schema = getElement(metadata.getElement(), val.schema);
            if (schema == null)
            {
                schema = create(val.schema);
                metadata.getElement().add(schema);
            }
            valueElem = schema;

            // Has element.. with XOAI one could have only schema and value
            if (val.element != null && !val.element.equals(""))
            {
                Element element = getElement(schema.getElement(),
                        val.element);
                if (element == null)
                {
                    element = create(val.element);
                    schema.getElement().add(element);
                }
                valueElem = element;

                // Qualified element?
                if (val.qualifier != null && !val.qualifier.equals(""))
                {
                    Element qualifier = getElement(element.getElement(),
                            val.qualifier);
                    if (qualifier == null)
                    {
                        qualifier = create(val.qualifier);
                        element.getElement().add(qualifier);
                    }
                    valueElem = qualifier;
                }
            }

            // Language?
            if (val.language != null && !val.language.equals(""))
            {
                Element language = getElement(valueElem.getElement(),
                        val.language);
                if (language == null)
                {
                    language = create(val.language);
                    valueElem.getElement().add(language);
                }
                valueElem = language;
            }
            else
            {
                Element language = getElement(valueElem.getElement(),
                        "none");
                if (language == null)
                {
                    language = create("none");
                    valueElem.getElement().add(language);
                }
                valueElem = language;
            }

            valueElem.getField().add(createValue("value", val.value));
            if (val.authority != null) {
                valueElem.getField().add(createValue("authority", val.authority));
                if (val.confidence != Choices.CF_NOVALUE)
                    valueElem.getField().add(createValue("confidence", val.confidence + ""));
            }
        }
        // Done! Metadata has been read!
        // Now adding bitstream info
        Element bundles = create("bundles");
        metadata.getElement().add(bundles);

        Bundle[] bs;
        try
        {
            bs = item.getBundles();
            for (Bundle b : bs)
            {
                Element bundle = create("bundle");
                bundles.getElement().add(bundle);
                bundle.getField()
                        .add(createValue("name", b.getName()));

                Element bitstreams = create("bitstreams");
                bundle.getElement().add(bitstreams);
                Bitstream[] bits = b.getBitstreams();
                for (Bitstream bit : bits)
                {
                    Element bitstream = create("bitstream");
                    bitstreams.getElement().add(bitstream);
                    String url = "";
                    String bsName = bit.getName();
                    String sid = String.valueOf(bit.getSequenceID());
                    String baseUrl = ConfigurationManager.getProperty("oai",
                            "bitstream.baseUrl");
                    String handle = null;
                    // get handle of parent Item of this bitstream, if there
                    // is one:
                    Bundle[] bn = bit.getBundles();
                    if (bn.length > 0)
                    {
                        Item bi[] = bn[0].getItems();
                        if (bi.length > 0)
                        {
                            handle = bi[0].getHandle();
                        }
                    }
                    if (bsName == null)
                    {
                        String ext[] = bit.getFormat().getExtensions();
                        bsName = "bitstream_" + sid
                                + (ext.length > 0 ? ext[0] : "");
                    }
                    if (handle != null && baseUrl != null)
                    {
                        url = baseUrl + "/bitstream/"
                                + handle + "/"
                                + sid + "/"
                                + URLUtils.encode(bsName);
                    }
                    else
                    {
                        url = URLUtils.encode(bsName);
                    }

                    String cks = bit.getChecksum();
                    String cka = bit.getChecksumAlgorithm();
                    String oname = bit.getSource();
                    String name = bit.getName();
                    String description = bit.getDescription();
                    String drm = ItemUtils.getAccessRightsValue(AuthorizeManager.getPoliciesActionFilter(context, bit,  Constants.READ));
                        
                    if (name != null)
                        bitstream.getField().add(
                                createValue("name", name));
                    if (oname != null)
                        bitstream.getField().add(
                                createValue("originalName", name));
                    if (description != null)
                        bitstream.getField().add(
                                createValue("description", description));
                    bitstream.getField().add(
                            createValue("format", bit.getFormat()
                                    .getMIMEType()));
                    bitstream.getField().add(
                            createValue("size", "" + bit.getSize()));
                    bitstream.getField().add(createValue("url", url));
                    bitstream.getField().add(
                            createValue("checksum", cks));
                    bitstream.getField().add(
                            createValue("checksumAlgorithm", cka));
                    bitstream.getField().add(
                            createValue("sid", bit.getSequenceID()
                                    + ""));
                    bitstream.getField()
                            .add(createValue("drm", drm));
                }
            }
        }
        catch (SQLException e1)
        {
            e1.printStackTrace();
        }
        

        // Other info
        Element other = create("others");

        other.getField().add(
                createValue("handle", item.getHandle()));
        other.getField().add(
                createValue("identifier", DSpaceItem.buildIdentifier(item.getHandle())));
        other.getField().add(
                createValue("lastModifyDate", item
                        .getLastModified().toString()));
        metadata.getElement().add(other);

        // Repository Info
        Element repository = create("repository");
        repository.getField().add(
                createValue("name",
                        ConfigurationManager.getProperty("dspace.name")));
        repository.getField().add(
                createValue("mail",
                        ConfigurationManager.getProperty("mail.admin")));
        metadata.getElement().add(repository);

        // Licensing info
        Element license = create("license");
        Bundle[] licBundles;
        try
        {
            licBundles = item.getBundles(Constants.LICENSE_BUNDLE_NAME);
            if (licBundles.length > 0)
            {
                Bundle licBundle = licBundles[0];
                Bitstream[] licBits = licBundle.getBitstreams();
                if (licBits.length > 0)
                {
                    Bitstream licBit = licBits[0];
                    InputStream in;
                    try
                    {
                        in = licBit.retrieve();
                        ByteArrayOutputStream out = new ByteArrayOutputStream();
                        Utils.bufferedCopy(in, out);
                        license.getField().add(
                                createValue("bin",
                                        Base64Utils.encode(out.toString())));
                        metadata.getElement().add(license);
                    }
                    catch (AuthorizeException e)
                    {
                        log.warn(e.getMessage(), e);
                    }
                    catch (IOException e)
                    {
                        log.warn(e.getMessage(), e);
                    }
                    catch (SQLException e)
                    {
                        log.warn(e.getMessage(), e);
                    }

                }
            }
        }
        catch (SQLException e1)
        {
            log.warn(e1.getMessage(), e1);
        }
        
        return metadata;
    }
    
	/**
	 * Method to return a default value text to identify access rights:
	 * 'open access','embargoed access','restricted access','metadata only access'
	 *
	 * NOTE: embargoed access contains also embargo end date in the form "embargoed access|||yyyy-MM-dd"
	 *
	 * @param rps
	 * @return
	 */
	public static String getAccessRightsValue(List<ResourcePolicy> rps) {
		Date now = new Date();
		Date embargoEndDate = null;
		boolean openAccess = false;
		boolean groupRestricted = false;
		boolean withEmbargo = false;

		if (rps != null) {
			for (ResourcePolicy rp : rps) {
				if (rp.getGroupID() == 0) {
					if (rp.isDateValid()) {
						openAccess = true;
					} else if (rp.getStartDate() != null && rp.getStartDate().after(now)) {
						withEmbargo = true;
						embargoEndDate = rp.getStartDate();
					}
				} else if (rp.getGroupID() != 1) {
					if (rp.isDateValid()) {
						groupRestricted = true;
					} else if (rp.getStartDate() == null || rp.getStartDate().after(now)) {
						withEmbargo = true;
						embargoEndDate = rp.getStartDate();
					}
				}
			}
		}
		String value = METADATA_ONLY_ACCESS;
		// if there are fulltext build the values
		if (openAccess) {
			// open access
			value = OPEN_ACCESS;
		} else if (withEmbargo) {
			// all embargoed
			value = EMBARGOED_ACCESS + "|||" + sdf.format(embargoEndDate);
		} else if (groupRestricted) {
			// all restricted
			value = RESTRICTED_ACCESS;
		}
		return value;
	}

 }
