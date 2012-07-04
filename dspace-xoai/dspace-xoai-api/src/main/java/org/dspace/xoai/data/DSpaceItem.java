/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.xoai.data;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.regex.Pattern;

import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.dspace.authorize.AuthorizeException;
import org.dspace.content.Bitstream;
import org.dspace.content.Bundle;
import org.dspace.content.Collection;
import org.dspace.content.Community;
import org.dspace.content.DCValue;
import org.dspace.content.Item;
import org.dspace.core.ConfigurationManager;
import org.dspace.core.Constants;
import org.dspace.core.Utils;
import org.dspace.xoai.util.XOAICacheManager;
import org.dspace.xoai.util.XOAIDatabaseManager;

import com.lyncode.xoai.common.dataprovider.core.ReferenceSet;
import com.lyncode.xoai.common.dataprovider.data.AbstractAbout;
import com.lyncode.xoai.common.dataprovider.data.AbstractItem;
import com.lyncode.xoai.common.dataprovider.util.Base64Utils;
import com.lyncode.xoai.common.dataprovider.xml.xoai.Element;
import com.lyncode.xoai.common.dataprovider.xml.xoai.Metadata;
import com.lyncode.xoai.common.dataprovider.xml.xoai.ObjectFactory;

/**
 * 
 * @author Lyncode Development Team <dspace@lyncode.com>
 */
@SuppressWarnings("deprecation")
public class DSpaceItem extends AbstractItem
{
    private static Logger log = LogManager.getLogger(DSpaceItem.class);

    private static List<ReferenceSet> getSets(Item item)
    {
        List<ReferenceSet> sets = new ArrayList<ReferenceSet>();
        List<Community> coms = new ArrayList<Community>();
        try
        {
            Collection[] itemCollections = item.getCollections();
            for (Collection col : itemCollections)
            {
                ReferenceSet s = new DSpaceSet(col);
                sets.add(s);
                for (Community com : XOAIDatabaseManager
                        .flatParentCommunities(col))
                    if (!coms.contains(com))
                        coms.add(com);
            }
            for (Community com : coms)
            {
                ReferenceSet s = new DSpaceSet(com);
                sets.add(s);
            }
        }
        catch (SQLException e)
        {
            log.error(e.getMessage(), e);
        }
        return sets;
    }

    private static String _prefix = null;

    private static String getIdentifier(Item item)
    {
        if (_prefix == null)
        {
            _prefix = ConfigurationManager.getProperty("xoai",
                    "identifier.prefix");
        }
        return "xoai:" + _prefix + ":" + item.getHandle();
    }

    private Item item;

    private List<ReferenceSet> sets;

    public DSpaceItem(Item item)
    {
        this.item = item;
        this.sets = getSets(item);
    }

    @Override
    public List<AbstractAbout> getAbout()
    {
        return new ArrayList<AbstractAbout>();
    }

    @Override
    public String getIdentifier()
    {
        return getIdentifier(item);
    }

    @Override
    public Date getDatestamp()
    {
        return item.getLastModified();
    }

    @Override
    public List<ReferenceSet> getSets()
    {
        return sets;
    }

    @Override
    public boolean isDeleted()
    {
        return item.isWithdrawn();
    }

    private static Element getElement(List<Element> list, String name)
    {
        for (Element e : list)
            if (name.equals(e.getName()))
                return e;

        return null;
    }

    private static Element create(ObjectFactory factory, String name)
    {
        Element e = factory.createElement();
        e.setName(name);
        return e;
    }

    private static Element.Field createValue(ObjectFactory factory, String value)
    {
        Element.Field e = factory.createElementField();
        e.setValue(value);
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

    private Metadata metadata = null;

    private String URLEncode (String value) {
        try
        {
            return URLEncoder.encode(value, "UTF-8");
        }
        catch (UnsupportedEncodingException e)
        {
            log.error(e.getMessage(), e);
            return value;
        }
    }
    
    public Metadata retrieveMetadata () {
        Metadata metadata;
        // read all metadata into Metadata Object
        ObjectFactory factory = new ObjectFactory();
        metadata = factory.createMetadata();
        DCValue[] vals = this.item.getMetadata(Item.ANY, Item.ANY, Item.ANY, Item.ANY);
        for (DCValue val : vals)
        {
            Element valueElem = null;
            Element schema = getElement(metadata.getElement(), val.schema);
            if (schema == null)
            {
                schema = create(factory, val.schema);
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
                    element = create(factory, val.element);
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
                        qualifier = create(factory, val.qualifier);
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
                    language = create(factory, val.language);
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

            valueElem.getField().add(createValue(factory, val.value));
        }
        // Done! Metadata readed!
        // Now adding bitstream info
        Element bundles = create(factory, "bundles");
        metadata.getElement().add(bundles);

        Bundle[] bs;
        try
        {
            bs = item.getBundles();
            for (Bundle b : bs)
            {
                Element bundle = create(factory, "bundle");
                bundles.getElement().add(bundle);
                bundle.getField()
                        .add(createValue(factory, "name", b.getName()));

                Element bitstreams = create(factory, "bitstreams");
                bundle.getElement().add(bitstreams);
                Bitstream[] bits = b.getBitstreams();
                for (Bitstream bit : bits)
                {
                    Element bitstream = create(factory, "bitstream");
                    bitstreams.getElement().add(bitstream);
                    String url = "";
                    String bsName = bitstream.getName();
                    String sid = String.valueOf(bit.getSequenceID());
                    String baseUrl = ConfigurationManager.getProperty("xoai",
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
                                + this.URLEncode(handle) + "/"
                                + sid + "/"
                                + this.URLEncode(bsName);
                    }
                    else
                    {
                        url = URLEncode(bsName);
                    }

                    String cks = bit.getChecksum();
                    String cka = bit.getChecksumAlgorithm();
                    String oname = bit.getSource();
                    String name = bit.getName();

                    if (name != null)
                        bitstream.getField().add(
                                createValue(factory, "name", name));
                    if (oname != null)
                        bitstream.getField().add(
                                createValue(factory, "originalName", name));
                    bitstream.getField().add(
                            createValue(factory, "format", bit.getFormat()
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
                createValue(factory, "lastModifyDate", item
                        .getLastModified().toString()));
        metadata.getElement().add(other);

        // Repository Info
        Element repository = create(factory, "repository");
        repository.getField().add(
                createValue(factory, "name",
                        ConfigurationManager.getProperty("dspace.name")));
        repository.getField().add(
                createValue(factory, "mail",
                        ConfigurationManager.getProperty("mail.admin")));
        metadata.getElement().add(repository);

        // Licensing info
        Element license = create(factory, "license");
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
                                createValue(factory, "bin",
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
    
    @Override
    public Metadata getMetadata()
    {
        if (metadata == null)
        {
            metadata = XOAICacheManager.getMetadata(this);
        }
        return metadata;
    }

    private List<String> getMetadata(List<Element> elems, String[] parts)
    {
        List<String> list = new ArrayList<String>();
        if (parts.length > 1)
        {
            if (parts[0].equals("*"))
            {
                for (Element e : elems)
                {
                    if (e.getElement() != null)
                        list.addAll(this.getMetadata(e.getElement(),
                                Arrays.copyOfRange(parts, 1, parts.length)));
                }
            }
            else
            {
                Element e = getElement(elems, parts[0]);
                if (e != null)
                    list.addAll(this.getMetadata(e.getElement(),
                            Arrays.copyOfRange(parts, 1, parts.length)));
            }
        }
        else if (parts.length == 1)
        {
            // Here we could have reached our target (named fields)
            for (Element e : elems)
            {
                for (Element.Field f : e.getField())
                {
                    if (parts[0].equals("*"))
                        list.add(f.getValue());
                    else if (parts[0].equals(f.getName()))
                        list.add(f.getValue());
                }
            }

            if (parts[0].equals("*"))
            {
                for (Element e : elems)
                {
                    if (e.getElement() != null)
                        list.addAll(this.getMetadata(e.getElement(),
                                Arrays.copyOfRange(parts, 1, parts.length)));
                }
            }
            else
            {
                Element e = getElement(elems, parts[0]);
                if (e != null)
                    list.addAll(this.getMetadata(e.getElement(),
                            Arrays.copyOfRange(parts, 1, parts.length)));
            }
        }
        else
        {
            // Here we have reached our target (unnamed fields)
            for (Element e : elems)
            {
                for (Element.Field f : e.getField())
                {
                    if (f.getName() == null || f.getName().equals(""))
                        list.add(f.getValue());
                }
            }
        }
        return list;
    }

    public List<String> getMetadata(String field)
    {
        String[] parts = field.split(Pattern.quote("."));
        return getMetadata(this.getMetadata().getElement(), parts);
    }

    public Item getItem()
    {
        return item;
    }
}