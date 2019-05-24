package org.dspace.xmlworkflow.cristin;

import org.apache.log4j.Logger;
import org.dspace.authorize.AuthorizeException;
import org.dspace.content.Bitstream;
import org.dspace.content.BitstreamFormat;
import org.dspace.content.Bundle;
import org.dspace.content.Item;
import org.dspace.content.crosswalk.CrosswalkException;
import org.dspace.content.factory.ContentServiceFactory;
import org.dspace.content.service.BitstreamFormatService;
import org.dspace.content.service.BitstreamService;
import org.dspace.content.service.BundleService;
import org.dspace.content.service.ItemService;
import org.dspace.core.Context;
import org.jdom.*;
import org.jdom.input.SAXBuilder;
import org.jdom.xpath.XPath;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.net.ConnectException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLDecoder;
import java.sql.SQLException;
import java.util.*;

/**
 * Class to provide file and bundle management utilities for the Duo module
 */
public class CristinFileManager {
    /**
     * log4j category
     */
    private static Logger log = Logger.getLogger(CristinFileManager.class);

    /* Namespaces */
    public static final Namespace ATOM_NS =
            Namespace.getNamespace("atom", "http://www.w3.org/2005/Atom");
    private static final Namespace ORE_ATOM =
            Namespace.getNamespace("oreatom", "http://www.openarchives.org/ore/atom/");
    private static final Namespace ORE_NS =
            Namespace.getNamespace("ore", "http://www.openarchives.org/ore/terms/");
    private static final Namespace RDF_NS =
            Namespace.getNamespace("rdf", "http://www.w3.org/1999/02/22-rdf-syntax-ns#");
    private static final Namespace DCTERMS_NS =
            Namespace.getNamespace("dcterms", "http://purl.org/dc/terms/");
    private static final Namespace DS_NS =
            Namespace.getNamespace("ds", "http://www.dspace.org/objectModel/");
    private final BundleService bundleService;
    private final BitstreamService bitstreamService;
    private final BitstreamFormatService bitstreamFormatService;
    private final ItemService itemService;

    public CristinFileManager() {
        bundleService = ContentServiceFactory.getInstance().getBundleService();
        bitstreamService = ContentServiceFactory.getInstance().getBitstreamService();
        bitstreamFormatService = ContentServiceFactory.getInstance().getBitstreamFormatService();
        itemService = ContentServiceFactory.getInstance().getItemService();
    }

    /**
     * Get a list of all of the bitstreams in the named bundle in the provided item
     *
     * @param item
     * @param bundleName
     * @return
     * @throws SQLException
     */
    public List<Bitstream> getExistingBitstreams(Item item, String bundleName)
            throws SQLException {
        List<Bitstream> bss = new ArrayList<>();
        ItemService itemService = ContentServiceFactory.getInstance().getItemService();
        List<Bundle> bundles = itemService.getBundles(item, bundleName);
        for (Bundle bundle : bundles) {
            List<Bitstream> bitstreams = bundle.getBitstreams();
            for (Bitstream bitstream : bitstreams) {
                bss.add(bitstream);
            }
        }
        return bss;
    }

    /**
     * List all of the bitstreams which appear in the given bundle in the ORE description
     * of the item provided by Cristin.  This will include the metadata bitstream
     *
     * @param doc        ORE root xml element
     * @param bundleName
     * @return
     * @throws IOException
     */
    public List<CristinBitstream> listBitstreamsInBundle(Document doc, String bundleName)
            throws IOException {
        return this.listBitstreamsInBundle(doc, bundleName, true);
    }

    /**
     * List all of the bitstreams which appear in the given bundle in the ORE description
     * of the item provided by Cristin.  Pass in the omitMetadata flag to include or exclude
     * the metadata bitstream.
     *
     * @param doc          ORE root xml element
     * @param bundleName
     * @param omitMetadata
     * @return
     * @throws IOException
     */
    public List<CristinBitstream> listBitstreamsInBundle(Document doc, String bundleName, boolean omitMetadata)
            throws IOException {
        String mdUrl = null;
        List<Element> bitstreams = new ArrayList<Element>();

        // first action is to get all the information we can from the ORE doc.
        // This will hopefully include a link to the cristin.xml bitstream
        // which we will need in order to learn about md5 checksums and file sequences
        List<Element> links = this.listBitstreams(doc);
        for (Element link : links) {
            String incomingBundle = this.getIncomingBundleName(doc, link);

            if (bundleName.equals(incomingBundle)) {
                // this is a bitstream from the correct bundle
                // only register it if it is not a metadata bitstream
                boolean metadataBitstream = this.isMetadataBitstream(link.getAttributeValue("href"));
                if (metadataBitstream) {
                    mdUrl = link.getAttributeValue("href");
                }
                if (!metadataBitstream || (metadataBitstream && !omitMetadata)) {
                    bitstreams.add(link);
                }
            }
        }

        // if we found a metadata bitstream, then let's get the information out of it
        // this is where we will discover md5 checksums and file sequences
        List<Element> fulltexts = null;
        if (mdUrl != null) {
            try {
                InputStream in = this.getInputStream(mdUrl);
                Document cristin = (new SAXBuilder()).build(in);

                // get the fulltekst elements
                XPath ftXpath = XPath.newInstance("/frida/forskningsresultat/fellesdata/fulltekst");
                fulltexts = ftXpath.selectNodes(cristin);
            } catch (JDOMException e) {
                throw new IOException(e);
            }
        }

        List<CristinBitstream> ibs = this.makeCristinBitstreams(bitstreams, fulltexts);

        return ibs;
    }

    private List<CristinBitstream> makeCristinBitstreams(List<Element> links, List<Element> fulltexts) {
        List<CristinBitstream> ibs = new ArrayList<>();
        for (Element link : links) {
            CristinBitstream ib = new CristinBitstream();
            ib.setUrl(link.getAttributeValue("href"));
            ib.setName(link.getAttributeValue("title"));
            ib.setMimetype(link.getAttributeValue("type"));
            if (fulltexts != null) {
                /*
                <fulltekst>
                    <nr>1</nr>
                    <type>preprint</type>
                    <navn>1-introduction.doc</navn>
                    <antallBytes>93696</antallBytes>
                    <dato>2011-07-27</dato>
                    <personreferanse>Marianne Elisabeth Lien, UIO</personreferanse>
                    <md5>checksum</md5>
                </fulltekst>
                */
                for (Element ft : fulltexts) {
                    // we're going to match by name on the assumption that the filenames
                    // are unique within the context of the item
                    // FIXME: once we have md5 checksums in the ORE feed, we will be able
                    // to match on checksum, which will be a more formal solution
                    if (ft.getChildTextTrim("navn").equals(ib.getName())) {
                        String nr = ft.getChildTextTrim("nr");
                        String md5 = ft.getChildTextTrim("md5");

                        int seq = -1;
                        if (nr != null) {
                            seq = Integer.parseInt(nr);
                        }
                        ib.setOrder(seq);

                        if (md5 != null) {
                            ib.setMd5(md5);
                        }
                    }
                }
            }
            ibs.add(ib);
        }
        return ibs;
    }

    private String getFilename(String url) {
        // FIXME: yeah yeah, this would look better with a regex
        String[] bits = url.split("\\?");
        String[] urlParts = bits[0].split("/");
        String filename = urlParts[urlParts.length - 1];
        try {
            return URLDecoder.decode(filename, "UTF-8");
        } catch (UnsupportedEncodingException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Does the URL extracted from the Cristin ORE document refer to a metadata bitstream?
     *
     * @param url
     * @return
     */
    public boolean isMetadataBitstream(String url) {
        // https://w3utv-dspace01.uio.no/dspace/xmlui/bitstream/handle/123456789/982/cristin-12087.xml?sequence=2
        String filename = this.getFilename(url);
        if (filename.startsWith("cristin-") && filename.endsWith(".xml")) {
            return true;
        }
        return false;
    }

    /**
     * Get the name of the bundle for the given link in the given document
     *
     * @param doc
     * @param link
     * @return
     * @throws IOException
     */
    public String getIncomingBundleName(Document doc, Element link)
            throws IOException {
        try {
            String href = link.getAttributeValue("href");
            XPath xpathDesc = XPath.newInstance("/atom:entry/oreatom:triples/rdf:Description[@rdf:about=\"" + href + "\"]");
            xpathDesc.addNamespace(ATOM_NS);
            xpathDesc.addNamespace(ORE_ATOM);
            xpathDesc.addNamespace(RDF_NS);
            List<Element> descs = xpathDesc.selectNodes(doc);
            for (Element desc : descs) {
                Element dcdesc = desc.getChild("description", DCTERMS_NS);
                return dcdesc.getText();
            }
        } catch (JDOMException e) {
            throw new IOException("JDOM exception occured while ingesting the ORE", e);
        }

        return null;
    }

    /**
     * List all of the bitstream elements in the given ORE document
     *
     * @param doc
     * @return
     * @throws IOException
     */
    public List<Element> listBitstreams(Document doc)
            throws IOException {
        XPath xpathLinks;
        List<Element> aggregatedResources;
        // String entryId;
        try {
            xpathLinks = XPath.newInstance("/atom:entry/atom:link[@rel=\"" + ORE_NS.getURI() + "aggregates" + "\"]");
            xpathLinks.addNamespace(ATOM_NS);
            aggregatedResources = xpathLinks.selectNodes(doc);

            // xpathLinks = XPath.newInstance("/atom:entry/atom:link[@rel='alternate']/@href");
            // xpathLinks.addNamespace(ATOM_NS);
            // entryId = ((Attribute) xpathLinks.selectSingleNode(doc)).getValue();
        } catch (JDOMException e) {
            throw new IOException("JDOM exception occured while ingesting the ORE", e);
        }

        return aggregatedResources;
    }

    /**
     * is the given element from an ORE description of a DSpace items a metadata bitstream?
     *
     * @param desc
     * @return
     */
    public boolean isMetadataBitstream(Element desc) {
        // https://w3utv-dspace01.uio.no/dspace/xmlui/bitstream/handle/123456789/982/cristin-12087.xml?sequence=2

        Attribute about = desc.getAttribute("about", RDF_NS);
        String url = about.getValue();

        // FIXME: yeah yeah, this would look better with a regex
        String[] bits = url.split("\\?");
        String[] urlParts = bits[0].split("/");
        String filename = urlParts[urlParts.length - 1];

        if (filename.startsWith("cristin-") && filename.endsWith(".xml")) {
            return true;
        }
        return false;
    }

    /**
     * Get an input stream which will allow us to read the bits from this remote URL
     *
     * @param href
     * @return
     * @throws MalformedURLException
     * @throws IOException
     */
    public InputStream getInputStream(String href)
            throws MalformedURLException, IOException {
        // ingest the bitstream from the remote url
        URL ARurl = null;
        InputStream in = null;
        if (href != null) {
            try {
                // Make sure the url string escapes all the oddball characters
                String processedURL = encodeForURL(href);
                // Generate a requeset for the aggregated resource
                ARurl = new URL(processedURL);
                in = ARurl.openStream();
            } catch (FileNotFoundException fe) {
                log.error("The provided URI failed to return a resource: " + href);
            } catch (ConnectException fe) {
                log.error("The provided URI was invalid: " + href);
            }
        } else {
            throw new IOException("Could not obtain resource from " + href);
        }
        return in;
    }

    /**
     * Ingest the bitstream from the given remote url with the given properties
     *
     * @param context
     * @param href         Remote URL from which to retrieve the bitstream
     * @param bsName       Name to give the bitstream in DSpace
     * @param mimeString   Mimetype to give the bitstream in DSpace
     * @param targetBundle Target bundle to store the bitstream in
     * @return
     * @throws MalformedURLException
     * @throws IOException
     * @throws CrosswalkException
     * @throws AuthorizeException
     * @throws SQLException
     */
    public Bitstream ingestBitstream(Context context, String href, String bsName, String mimeString, Bundle targetBundle)
            throws MalformedURLException, IOException, CrosswalkException, AuthorizeException, SQLException {
        Bitstream bitstream;

        InputStream in = this.getInputStream(href);

        // ingest and update
        if (in != null) {
            bitstream = bitstreamService.create(context, targetBundle, in);
            bitstream.setName(context, bsName);

            // Identify the format
            BitstreamFormat bsFormat = bitstreamFormatService.findByMIMEType(context, mimeString);
            if (bsFormat == null) {
                bsFormat = bitstreamFormatService.guessFormat(context, bitstream);
            }
            bitstreamService.setFormat(context, bitstream, bsFormat);
            bitstreamService.update(context, bitstream);

            bundleService.addBitstream(context, targetBundle, bitstream);
            bundleService.update(context, targetBundle);
        } else {
            throw new CrosswalkException("Could not retrieve bitstream");
        }

        return bitstream;
    }

    /**
     * Get the ordering parameter of the given bitstream
     *
     * @param bitstream
     * @param item
     * @param bundleName
     * @return
     * @throws SQLException
     */
    public int getBitstreamOrder(Bitstream bitstream, Item item, String bundleName)
            throws SQLException {
        List<Bundle> original = ContentServiceFactory.getInstance().getItemService().getBundles(item, bundleName);
        List<Bitstream> bitstreams = original.get(0).getBitstreams();
        return bitstreams.indexOf(bitstream);
    }

    /**
     * Does the incoming bitstream exactly match one of the provided list of bitstreams.
     * <p>
     * Match is determined by equivalency of checksums
     *
     * @param ib
     * @param existingBitstreams
     * @return
     */
    public boolean bitstreamInstanceAlreadyExists(CristinBitstream ib, List<Bitstream> existingBitstreams) {
        for (Bitstream bs : existingBitstreams) {
            if (ib.getMd5() != null) {
                if (ib.getMd5().equals(bs.getChecksum())) {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * Is the name of the incoming bitstream the same as the name of any of the list of existing bitstreams
     * provided
     *
     * @param ib
     * @param existingBitstreams
     * @return
     */
    public boolean bitstreamNameAlreadyExists(CristinBitstream ib, List<Bitstream> existingBitstreams) {
        for (Bitstream bs : existingBitstreams) {
            if (ib.getName() != null) {
                if (ib.getName().equals(bs.getName())) {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * Find a matching bitstream in the array of bitstreams if one exists.
     * <p>
     * NOTE: in an ideal world this would use checksum mapping, but checksums are not
     * routinely available in the incoming bitstreams.  So matching is done with the
     * much weaker condition of matching filename.
     *
     * @param ib
     * @param bitstreams
     * @return the matching bitstream if one is found, or null if none is
     */
    public Bitstream findBitstream(CristinBitstream ib, Bitstream[] bitstreams) {
        for (Bitstream bs : bitstreams) {
            if (ib.getName() != null) {
                if (ib.getName().equals(bs.getName())) {
                    return bs;
                }
            }
        }
        return null;
    }

    /**
     * Is the incoming bitstream a new bitstream?
     *
     * @param ib
     * @param existingBitstreams
     * @return
     */
    public boolean isNewBitstream(CristinBitstream ib, List<Bitstream> existingBitstreams) {
        for (Bitstream bs : existingBitstreams) {
            if (ib.getMd5() != null) {
                if (ib.getMd5().equals(bs.getChecksum())) {
                    return false;
                }
            }
        }
        return true;
    }

    /**
     * Does the DSpace bitstream match one of the incoming bitstreams?
     *
     * @param bitstream
     * @param ibs
     * @return
     */
    public boolean bitstreamIsIncoming(Bitstream bitstream, List<CristinBitstream> ibs) {
        for (CristinBitstream ib : ibs) {
            if (ib.getMd5() != null) {
                if (ib.getMd5().equals(bitstream.getChecksum())) {
                    return true;
                }
            }
            if (ib.getName().equals(bitstream.getName())) {
                return true;
            }
        }
        return false;
    }

    /**
     * Resequence the item's bitstreams in the named bundle so that they match the
     * sequencing of the incoming bitstreams
     *
     * @param context
     * @param item
     * @param bundleName
     * @param ibs
     * @throws SQLException
     * @throws AuthorizeException
     */
    public void sequenceBitstreams(Context context, Item item, String bundleName, List<CristinBitstream> ibs)
            throws SQLException, AuthorizeException {
        // get the bundle we are going to sequence in
        List<Bundle> bundles = itemService.getBundles(item, bundleName);
        Bundle bundle = bundles.size() > 0 ? bundles.get(0) : null;
        if (bundle == null) {
            return;
        }

        // get the existing bitstreams, and prepare the ordering array
        List<Bitstream> bss = bundle.getBitstreams();
        // int[] order = new int[bss.length];
        Map<Integer, UUID> orderMap = new TreeMap<>();

        // go through the incoming bitstreams and find the corresponding
        // DSpace bitstream (this will ignore any incoming bitstreams
        // that didn't find their way into the bundle of interest)
        int offset = 1;
        for (CristinBitstream ib : ibs) {
            if (ib.getMd5() != null) {
                for (Bitstream bitstream : bss) {
                    if (ib.getMd5().equals(bitstream.getChecksum())) {
                        int pos = ib.getOrder();
                        if (pos == -1) {
                            pos = -1 * offset;
                            offset++;
                        }
                        orderMap.put(pos, bitstream.getID());
                    }
                }
            } else {
                for (Bitstream bitstream : bss) {
                    if (ib.getName().equals(bitstream.getName())) {
                        int pos = ib.getOrder();
                        if (pos == -1) {
                            pos = -1 * offset;
                            offset++;
                        }
                        orderMap.put(pos, bitstream.getID());
                    }
                }
            }
        }

        // if there is an order map, then sequence the bitstreams properly
        if (orderMap.size() > 0) {
            Map<Integer, UUID> normalisedOrder = new TreeMap<>();

            // now convert the order map to the correct array format, normalising
            // the numbering as we go
            int idx = 0;
            for (Integer ord : orderMap.keySet()) {
                UUID bsid = orderMap.get(ord);
                if (bsid != null) {
                    // if there is an erroneous bitstream id skip it
                    log.info("Skipping bitstream id of 0!");
                    continue;
                }
                normalisedOrder.put(idx, orderMap.get(ord));
                log.info("Allocating bitstream order " + idx + " to bitstream with id " + orderMap.get(ord));
                idx++;
            }

            // "cast" the order to an array
            UUID[] order = new UUID[normalisedOrder.size()];
            for (Integer ord : normalisedOrder.keySet()) {
                order[ord] = normalisedOrder.get(ord);
            }

            // finally, as the bundle to order the bitstreams
            bundleService.setOrder(context, bundle, order);
        }
    }

    /**
     * Helper method to escape all chaacters that are not part of the canon set
     *
     * @param sourceString source unescaped string
     */
    private String encodeForURL(String sourceString) {
        Character lowalpha[] = {'a', 'b', 'c', 'd', 'e', 'f', 'g', 'h', 'i',
                'j', 'k', 'l', 'm', 'n', 'o', 'p', 'q', 'r',
                's', 't', 'u', 'v', 'w', 'x', 'y', 'z'};
        Character upalpha[] = {'A', 'B', 'C', 'D', 'E', 'F', 'G', 'H', 'I',
                'J', 'K', 'L', 'M', 'N', 'O', 'P', 'Q', 'R',
                'S', 'T', 'U', 'V', 'W', 'X', 'Y', 'Z'};
        Character digit[] = {'0', '1', '2', '3', '4', '5', '6', '7', '8', '9'};
        Character mark[] = {'-', '_', '.', '!', '~', '*', '\'', '(', ')'};

        // reserved
        Character reserved[] = {';', '/', '?', ':', '@', '&', '=', '+', '$', ',', '%', '#'};

        Set<Character> URLcharsSet = new HashSet<Character>();
        URLcharsSet.addAll(Arrays.asList(lowalpha));
        URLcharsSet.addAll(Arrays.asList(upalpha));
        URLcharsSet.addAll(Arrays.asList(digit));
        URLcharsSet.addAll(Arrays.asList(mark));
        URLcharsSet.addAll(Arrays.asList(reserved));

        StringBuilder processedString = new StringBuilder();
        for (int i = 0; i < sourceString.length(); i++) {
            char ch = sourceString.charAt(i);
            if (URLcharsSet.contains(ch)) {
                processedString.append(ch);
            } else {
                processedString.append("%").append(Integer.toHexString((int) ch));
            }
        }

        return processedString.toString();
    }
}
