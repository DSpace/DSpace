/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */

/* Created for LINDAT/CLARIAH-CZ (UFAL) */
package org.dspace.utils;

import java.io.InputStreamReader;
import java.io.Reader;
import java.util.List;
import java.util.Objects;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.dspace.app.util.DCInput;
import org.dspace.content.Bitstream;
import org.dspace.content.DSpaceObject;
import org.dspace.content.Item;
import org.dspace.content.MetadataValue;
import org.dspace.content.factory.ContentServiceFactory;
import org.dspace.content.service.BitstreamService;
import org.dspace.content.service.ItemService;
import org.dspace.core.Constants;
import org.dspace.core.Context;
import org.dspace.handle.factory.HandleServiceFactory;
import org.dspace.handle.service.HandleService;
import org.springframework.stereotype.Component;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.xml.sax.InputSource;

@Component

/**
 * Provides various information based on
 * provided metadata or strings.
 *
 * Class is copied from the LINDAT/CLARIAH-CZ (https://github.com/ufal/clarin-dspace/blob
 * /si-master-origin/dspace-oai/src/main/java/cz/cuni/mff/ufal/utils/ItemUtil.java) and modified by
 * @author Marian Berger (marian.berger at dataquest.sk)
 */
public class SpecialItemService {
    private SpecialItemService() {}
    /** log4j logger */
    private static final org.apache.log4j.Logger log = org.apache.log4j.Logger
            .getLogger(SpecialItemService.class);

    /**
     * Returns cmdi metadata of item, if uploaded and marked as local.hasCMDI = true.
     * @param handle handle of object for which we need metadata.
     * @return Document repserenting cmdi metadata uploaded to METADATA bundle of item.
     */
    public static Node getUploadedMetadata(String handle) {
        Node ret = null;
        Context context = null;
        try {
            context = new Context();
            ContentServiceFactory csf = ContentServiceFactory.getInstance();
            ItemService itemService = csf.getItemService();
            BitstreamService bitstreamService = csf.getBitstreamService();
            HandleService hs = HandleServiceFactory.getInstance().getHandleService();
            DSpaceObject dSpaceObject = hs.resolveToObject(context, handle);
            List<MetadataValue> metadataValues = itemService.getMetadataByMetadataString(((Item) dSpaceObject),
                    "local.hasCMDI");
            if (Objects.nonNull(dSpaceObject) && dSpaceObject.getType() == Constants.ITEM
                    && hasOwnMetadata(metadataValues)) {

                Bitstream bitstream = itemService.getBundles(((Item) dSpaceObject), "METADATA").get(0)
                        .getBitstreams().get(0);
                if (Objects.isNull(bitstream)) {
                    return ret;
                }
                context.turnOffAuthorisationSystem();
                Reader reader = new InputStreamReader(bitstreamService.retrieve(context, bitstream));
                context.restoreAuthSystemState();
                try {
                    DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
                    factory.setNamespaceAware(true);
                    DocumentBuilder builder = factory.newDocumentBuilder();
                    Document doc = builder.parse(new InputSource(reader));
                    ret = doc;
                } finally {
                    reader.close();
                }

            }
        } catch (Exception e) {
            log.error(e);
            try {
                ret = DocumentBuilderFactory.newInstance().newDocumentBuilder().newDocument();
            } catch (ParserConfigurationException ex) {
                log.error(ex);
            }
        } finally {
            closeContext(context);
        }
        return ret;
    }

    /**
     * Splits funding into separate values and creates document with those values.
     * @param mdValue String of funding, expected to have 4 fields separated by ;
     * @return document representing separated values from param
     */
    public static Node getFunding(String mdValue) {
        String ns = "http://www.clarin.eu/cmd/";
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        factory.setNamespaceAware(true);
        DocumentBuilder builder;
        try {
            builder = factory.newDocumentBuilder();
            Document doc = builder.newDocument();
            Element el = doc.createElementNS(ns, "funding");
            doc.appendChild(el);
            Element organization = doc.createElementNS(ns, "organization");
            Element projName = doc.createElementNS(ns, "projectName");
            Element code = doc.createElementNS(ns, "code");
            Element fundsType = doc.createElementNS(ns, "fundsType");

            if (Objects.isNull(mdValue)) {
                log.warn("Trying to extract funding from null value!");
                return null;
            }
            String[] values = mdValue
                    .split(DCInput.ComplexDefinitions.getSeparator(), -1);
            // mind the order in input forms, org;code;projname;type
            Element[] elements = {organization, code, projName, fundsType};
            for (int i = 0; i < elements.length; i++) {
                if (values.length <= i) {
                    elements[i].appendChild(doc.createTextNode(""));
                } else {
                    elements[i].appendChild(doc.createTextNode(values[i]));
                }
                el.appendChild(elements[i]);
            }
            return doc;
        } catch (ParserConfigurationException e) {
            return null;
        }
    }

    /**
     * Creates document representing separated/parsed contact info from param
     * @param mdValue Contact field with several values delimited by ;
     * @return document representing separated values
     */
    public static Node getContact(String mdValue) {
        String ns = "http://www.clarin.eu/cmd/";
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        factory.setNamespaceAware(true);
        DocumentBuilder builder;
        try {
            builder = factory.newDocumentBuilder();
            Document doc = builder.newDocument();
            Element el = doc.createElementNS(ns, "contactPerson");
            doc.appendChild(el);
            Element first = doc.createElementNS(ns, "firstName");
            Element last = doc.createElementNS(ns, "lastName");
            Element email = doc.createElementNS(ns, "email");
            Element affil = doc.createElementNS(ns, "affiliation");

            String[] values = mdValue
                    .split(DCInput.ComplexDefinitions.getSeparator(), -1);

            Element[] elements = {first, last, email, affil};
            for (int i = 0; i < elements.length; i++) {
                if (values.length <= i) {
                    elements[i].appendChild(doc.createTextNode(""));
                } else {
                    elements[i].appendChild(doc.createTextNode(values[i]));
                }
                el.appendChild(elements[i]);
            }

            return doc;
        } catch (ParserConfigurationException e) {
            return null;
        }
    }

    public static Node getSize(String mdValue) {
        String ns = "http://www.clarin.eu/cmd/";
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        factory.setNamespaceAware(true);
        DocumentBuilder builder;
        try {
            builder = factory.newDocumentBuilder();
            Document doc = builder.newDocument();
            Element el = doc.createElementNS(ns, "size");
            doc.appendChild(el);
            Element size = doc.createElementNS(ns, "size");
            Element unit = doc.createElementNS(ns, "unit");

            String[] values = mdValue
                    .split(DCInput.ComplexDefinitions.getSeparator(), -1);

            Element[] elements = {size, unit};
            for (int i = 0; i < elements.length; i++) {
                if (values.length <= i) {
                    elements[i].appendChild(doc.createTextNode(""));
                } else {
                    elements[i].appendChild(doc.createTextNode(values[i]));
                }
                el.appendChild(elements[i]);
            }
            return doc;
        } catch (ParserConfigurationException e) {
            return null;
        }
    }

    /**
     * Generates author document from provided string.
     * @param mdValue String containing author, possibly with separated Firstname by ;
     * @return document representing possibly separated values from param.
     */
    public static Node getAuthor(String mdValue) {
        String ns = "http://www.clarin.eu/cmd/";
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        factory.setNamespaceAware(true);
        DocumentBuilder builder;
        try {
            builder = factory.newDocumentBuilder();
            Document doc = builder.newDocument();
            Element el = doc.createElementNS(ns, "author");
            doc.appendChild(el);
            Element last = doc.createElementNS(ns, "lastName");

            if (Objects.isNull(mdValue) || mdValue.isEmpty()) {
                log.warn("Trying to extract author from empty string!");
                return null;
            }
            String[] values = mdValue
                    .split(",", 2);

            last.appendChild(doc.createTextNode(values[0]));
            el.appendChild(last);
            if (values.length > 1) {
                // this probably means that if there are multiple fields, first is surname, second
                // is first name. Taken from here:
                // https://github.com/ufal/clarin-dspace/blob/8780782ce2977d304f2390b745a98eaea00b8255/
                // dspace-oai/src/main/java/cz/cuni/mff/ufal/utils/ItemUtil.java#L168
                Element first = doc.createElementNS(ns, "firstName");
                first.appendChild(doc.createTextNode(values[1]));
                el.appendChild(first);
            }
            return doc;
        } catch (ParserConfigurationException e) {
            return null;
        }
    }

    public static boolean hasOwnMetadata(List<MetadataValue> metadataValues) {
        if (metadataValues.size() == 1 && metadataValues.get(0).getValue().equalsIgnoreCase("true")) {
            return true;
        }
        return false;
    }

    private static void closeContext(Context c) {
        if (Objects.nonNull(c)) {
            c.abort();
        }
    }
}
