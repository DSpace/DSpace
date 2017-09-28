/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.util;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.sql.SQLException;
import java.util.Date;
import java.util.List;

import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;

import org.dspace.content.authority.Choices;
import org.dspace.content.Bitstream;
import org.dspace.content.Bundle;
import org.dspace.content.factory.ContentServiceFactory;
import org.dspace.content.Item;
import org.dspace.content.MetadataValue;
import org.dspace.content.service.ItemService;
import org.dspace.core.Constants;
import org.dspace.core.Context;
import org.dspace.core.Utils;
import org.dspace.services.ConfigurationService;
import org.dspace.services.factory.DSpaceServicesFactory;

import com.lyncode.xoai.dataprovider.util.Base64Utils;
import com.lyncode.xoai.dataprovider.xml.xoai.Element;
import com.lyncode.xoai.dataprovider.xml.xoai.Metadata;
import com.lyncode.xoai.dataprovider.xml.xoai.ObjectFactory;
import org.dspace.authorize.AuthorizeException;
import org.dspace.content.service.BitstreamService;

/**
 * This class provides util methods to export item's citations in
 * common formats like RIS and BibTeX.
 * 
 * @author João Melo <jmelo@lyncode.com>
 * 
 */
public class ExportItemUtils 
{
    
    private static String prefix = null;
    
    private static final ConfigurationService configurationService = 
                            DSpaceServicesFactory.getInstance().getConfigurationService();
    
    private static BitstreamService bitstreamService = ContentServiceFactory.getInstance().getBitstreamService();
    
    private static Logger log = LogManager.getLogger(ExportItemUtils.class);
    
    /**
     * Retries all metadata of an item an build a metadata representation of it
     * for the xslt transformation. Item's citations can be easily exported in 
     * common formats like RIS and BibTeX by using the Metadata Object with xslt.
     * Additional information about bitstream, Repository, License are added and
     * can be read from the xslt as well.
     * 
     * @param item
     *            The item to retrieve the metadata of.
     * @return 
     *        Metadata object with all usefull iformation about the given item.
     */
    public static Metadata retrieveMetadata(Item item) 
    {
        Metadata metadata;

        // read all metadata into Metadata Object
        ObjectFactory factory = new ObjectFactory();
        metadata = factory.createMetadata();

        ItemService itemService = ContentServiceFactory.getInstance().getItemService();
        Context context = new Context();
        List<MetadataValue> dcValues = itemService.getMetadata(item, Item.ANY, Item.ANY, Item.ANY, Item.ANY);

        for (MetadataValue metadataValue : dcValues) 
        {
            Element valueElem = null;
            Element schema = getElement(metadata.getElement(), metadataValue.getMetadataField()
                    .getMetadataSchema().getName());

            if (schema == null) 
            {
                schema = create(factory, metadataValue.getMetadataField()
                        .getMetadataSchema().getName());
                metadata.getElement().add(schema);
            }
            valueElem = schema;

            // Has element.. with XOAI one could have only schema and value
            if (metadataValue.getMetadataField().getElement() != null
                    && !metadataValue.getMetadataField().getElement().equals("")) 
            {
                Element element = getElement(schema.getElement(),
                        metadataValue.getMetadataField().getElement());
                if (element == null) 
                {
                    element = create(factory, metadataValue.getMetadataField().getElement());
                    schema.getElement().add(element);
                }
                valueElem = element;

                // Qualified element
                if (metadataValue.getMetadataField().getQualifier() != null
                        && !metadataValue.getMetadataField().getQualifier().equals("")) 
                {
                    Element qualifier = getElement(element.getElement(),
                            metadataValue.getMetadataField().getQualifier());

                    if (qualifier == null) 
                    {
                        qualifier = create(factory, metadataValue.getMetadataField().getQualifier());
                        element.getElement().add(qualifier);
                    }
                    valueElem = qualifier;
                }
            }

            // Language
            if (metadataValue.getLanguage() != null
                    && !metadataValue.getLanguage().equals("")) 
            {
                Element language = getElement(valueElem.getElement(),
                        metadataValue.getLanguage());

                if (language == null) 
                {
                    language = create(factory, metadataValue.getLanguage());
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
                    language = create(factory, "none");
                    valueElem.getElement().add(language);
                }
                valueElem = language;
            }

            valueElem.getField().add(createValue(factory, "value", metadataValue.getValue()));

            if (metadataValue.getAuthority() != null) 
            {
                valueElem.getField().add(createValue(factory, "authority", metadataValue.getAuthority()));

                if (metadataValue.getConfidence() != Choices.CF_NOVALUE) 
                {
                    valueElem.getField().add(createValue(factory, "confidence",
                            metadataValue.getConfidence() + ""));
                }
            }
        }
        
        // Done! Metadata has been read!
        // Now adding bitstream info
        Element bundles = create(factory, "bundles");
        metadata.getElement().add(bundles);
        
        try 
        {
            List<Bundle> itemBundles = item.getBundles();
            
            for (Bundle b : itemBundles) 
            {
                Element bundle = create(factory, "bundle");
                bundles.getElement().add(bundle);
                bundle.getField()
                        .add(createValue(factory, "name", b.getName()));

                Element bitstreams = create(factory, "bitstreams");
                bundle.getElement().add(bitstreams);
                
                List<Bitstream> bits = b.getBitstreams();
                
                for (Bitstream bit : bits) 
                {
                    Element bitstream = create(factory, "bitstream");
                    bitstreams.getElement().add(bitstream);
                    String url = "";
                    String bsName = bitstream.getName();
                    String sid = String.valueOf(bit.getSequenceID());
                    String baseUrl = configurationService.getProperty("oai",
                            "bitstream.baseUrl");
                    String handle = null;
                    // get handle of parent Item of this bitstream, if there
                    // is one:
                    List<Bundle> bn = bit.getBundles();
                    
                    if (!bn.isEmpty()) 
                    {
                        List<Item> bi = bn.get(0).getItems();
                        
                        if (!bi.isEmpty()) 
                        {
                            handle = bi.get(0).getHandle();
                        }
                    }
                    
                    if (bsName == null) 
                    {
                        List<String> extensions = bit.getFormat(context).getExtensions();
                        bsName = "bitstream_" + sid
                                + (extensions.size() > 0 ? extensions.get(0) : "");
                    }
                    
                    if (handle != null && baseUrl != null) 
                    {
                        url = baseUrl + "/bitstream/"
                                + urlEncoding(handle) + "/"
                                + sid + "/"
                                + urlEncoding(bsName);
                    } 
                    else 
                    {
                        url = urlEncoding(bsName);
                    }

                    String cks = bit.getChecksum();
                    String cka = bit.getChecksumAlgorithm();
                    String oname = bit.getSource();
                    String name = bit.getName();

                    if (name != null) 
                    {
                        bitstream.getField().add(
                                createValue(factory, "name", name));
                    }
                    
                    if (oname != null) 
                    {
                        bitstream.getField().add(
                                createValue(factory, "originalName", name));
                    }
                    bitstream.getField().add(
                            createValue(factory, "format", bit.getFormat(context)
                                    .getMIMEType()));
                    bitstream.getField().add(
                            createValue(factory, "size", "" + bit.getSize()));
                    bitstream.getField().add(createValue(factory, "url", url));
                    bitstream.getField().add(
                            createValue(factory, "checksum", cks));
                    bitstream.getField().add(
                            createValue(factory, "checksumAlgorithm", cka));
                    bitstream.getField().add(
                            createValue(factory, "sid", bit.getSequenceID()
                                    + ""));
                }
            }
        } 
        catch (SQLException e1) 
        {
            e1.printStackTrace();
        }
        
        // Other info
        Element other = create(factory, "others");

        other.getField().add(
                createValue(factory, "handle", item.getHandle()));
        other.getField().add(
                createValue(factory, "identifier", buildIdentifier(item.getHandle())));
        other.getField().add(
                createValue(factory, "lastModifyDate", item
                        .getLastModified().toString()));
        other.getField().add(
                createValue(factory, "generatedDate", new Date().toString()));
        metadata.getElement().add(other);

        // Repository Info
        Element repository = create(factory, "repository");
        repository.getField().add(
                createValue(factory, "name",
                        configurationService.getProperty("dspace.name")));
        repository.getField().add(
                createValue(factory, "mail",
                        configurationService.getProperty("mail.admin")));
        metadata.getElement().add(repository);

        // Licensing info
        Element license = create(factory, "license");
        List<Bundle> licenseBundles;
        
        try 
        {
            licenseBundles = itemService.getBundles(item, Constants.LICENSE_BUNDLE_NAME);
            
            if (!licenseBundles.isEmpty()) 
            {
                Bundle licenseBundle = licenseBundles.get(0);
                List<Bitstream> licenseBits = licenseBundle.getBitstreams();
                
                if (!licenseBits.isEmpty()) 
                {
                    Bitstream licenseBit = licenseBits.get(0);
                    InputStream in;
                    
                    try 
                    {
                        in = bitstreamService.retrieve(context, licenseBit);
                        
                        ByteArrayOutputStream out = new ByteArrayOutputStream();
                        Utils.bufferedCopy(in, out);
                        license.getField().add(createValue(factory, "bin",
                                        Base64Utils.encode(out.toString())));
                        metadata.getElement().add(license);
                    } 
                    catch (IOException | AuthorizeException e) 
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
     * Builds an OAI persistent identifier of the given handle
     * 
     * @param handle 
     *              The handle identifier
     * @return 
     *        A string as an OAI persistent identifier of the Handle like: 
     *         oai:localhost:123456789/41
     */
    public static String buildIdentifier(String handle) 
    {
        if (prefix == null) 
        {
            prefix = configurationService.getProperty("oai",
                    "identifier.prefix");
        }
        return "oai:" + prefix + ":" + handle;
    }
    private static Element getElement(List<Element> list, String name) 
    {
        for (Element e : list) 
        {
            if (name.equals(e.getName())) 
            {
                return e;
            }
        }
        return null;
    }

    private static String urlEncoding(String s) 
    {
        try 
        {
            return URLEncoder.encode(s, "UTF-8");
        } 
        catch (UnsupportedEncodingException e) 
        {
            return s;
        }
    }

    private static Element create(ObjectFactory factory, String name) 
    {
        Element e = factory.createElement();
        e.setName(name);
        return e;
    }

    private static Element.Field createValue(ObjectFactory factory,
            String name, String value)
    {
        Element.Field e = factory.createElementField();
        e.setValue(value);
        e.setName(name);
        return e;
    }
}
