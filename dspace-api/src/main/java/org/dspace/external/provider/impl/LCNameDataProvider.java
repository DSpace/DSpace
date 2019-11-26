/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.external.provider.impl;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.apache.commons.lang3.StringUtils;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.dspace.content.DCPersonName;
import org.dspace.content.dto.MetadataValueDTO;
import org.dspace.external.model.ExternalDataObject;
import org.dspace.external.provider.ExternalDataProvider;
import org.xml.sax.Attributes;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;
import org.xml.sax.XMLReader;
import org.xml.sax.helpers.DefaultHandler;

/**
 * Sample personal name authority based on Library of Congress Name Authority
 * Also serves as an example of an SRU client as authority.
 *
 * This is tuned for the data in the LC Name Authority test instance, see
 * http://alcme.oclc.org/srw/search/lcnaf
 *
 * WARNING: This is just a proof-of-concept implementation.  It would need
 * WARNING: lots of refinement to be used in production, because it is very
 * WARNING: sloppy about digging through the MARC/XML results.  No doubt
 * WARNING: it is losing a lot of valid results and information.
 * WARNING: Could also do a better job including more info (title, life dates
 * WARNING: etc) in the label instead of just the name.
 *
 * Reads these DSpace Config properties:
 *
 * lcname.url = http://alcme.oclc.org/srw/search/lcnaf
 *
 * @author Larry Stone
 * @version $Revision $
 */
public class LCNameDataProvider implements ExternalDataProvider {
    private static final Logger log = LogManager.getLogger(LCNameDataProvider.class);

    private String url;
    private String sourceIdentifier;

    // NS URI for SRU respones
    protected static final String NS_SRU = "http://www.loc.gov/zing/srw/";

    // NS URI for MARC/XML
    protected static final String NS_MX = "http://www.loc.gov/MARC21/slim";


    public String getSourceIdentifier() {
        return sourceIdentifier;
    }

    public Optional<ExternalDataObject> getExternalDataObject(String id) {

        StringBuilder query = new StringBuilder();
        query.append("local.LCCN = \"").append(id).append("\"");
        List<ExternalDataObject> list = doLookup(0, 10, query);
        if (list.size() > 0) {
            return Optional.of(list.get(0));
        } else {
            return Optional.empty();
        }

    }

    /**
     * Generic getter for the url
     * @return the url value of this LCNameDataProvider
     */
    public String getUrl() {
        return url;
    }

    /**
     * Generic setter for the url
     * @param url   The url to be set on this LCNameDataProvider
     */
    public void setUrl(String url) {
        this.url = url;
    }

    /**
     * Generic setter for the sourceIdentifier
     * @param sourceIdentifier   The sourceIdentifier to be set on this LCNameDataProvider
     */
    public void setSourceIdentifier(String sourceIdentifier) {
        this.sourceIdentifier = sourceIdentifier;
    }

    @Override
    public List<ExternalDataObject> searchExternalDataObjects(String text, int start, int limit) {
        // punt if there is no query text
        if (text == null || text.trim().length() == 0) {
            return Collections.EMPTY_LIST;
        }

        // 1. build CQL query
        DCPersonName pn = new DCPersonName(text);
        StringBuilder query = new StringBuilder();
        query.append("local.FirstName = \"").append(pn.getFirstNames()).
            append("\" and local.FamilyName = \"").append(pn.getLastName()).
                 append("\"");
        return doLookup(start, limit, query);

    }

    private List<ExternalDataObject> doLookup(int start, int limit, StringBuilder query) {
        // XXX arbitrary default limit - should be configurable?
        if (limit == 0) {
            limit = 50;
        }

        HttpGet get = constructHttpGet(query, start, limit);
        // 2. web request
        try {
            HttpClient hc = new DefaultHttpClient();
            HttpResponse response = hc.execute(get);
            if (response.getStatusLine().getStatusCode() == 200) {
                SRUHandler handler = parseResponseToSRUHandler(response);

                // this probably just means more results available..
                if (handler.hits != handler.result.size()) {
                    log.warn("Discrepency in results, result.length=" + handler.result.size() +
                                 ", yet expected results=" + handler.hits);
                }
                return handler.result;
            }
        } catch (IOException e) {
            log.error("SRU query failed: ", e);
            return Collections.EMPTY_LIST;
        } catch (ParserConfigurationException e) {
            log.warn("Failed parsing SRU result: ", e);
            return Collections.EMPTY_LIST;
        } catch (SAXException e) {
            log.warn("Failed parsing SRU result: ", e);
            return Collections.EMPTY_LIST;
        } finally {
            get.releaseConnection();
        }
        return Collections.EMPTY_LIST;
    }

    public boolean supports(String source) {
        return StringUtils.equalsIgnoreCase(sourceIdentifier, source);
    }

    private HttpGet constructHttpGet(StringBuilder query, int start, int limit) {
        URI sruUri;
        try {
            URIBuilder builder = new URIBuilder(url);
            builder.addParameter("operation", "searchRetrieve");
            builder.addParameter("version", "1.1");
            builder.addParameter("recordSchema", "info:srw/schema/1/marcxml-v1.1");
            builder.addParameter("query", query.toString());
            builder.addParameter("maximumRecords", String.valueOf(limit));
            builder.addParameter("startRecord", String.valueOf(start + 1));
            sruUri = builder.build();
        } catch (URISyntaxException e) {
            log.error("SRU query failed: ", e);
            return null;
        }
        HttpGet get = new HttpGet(sruUri);

        log.debug("Trying SRU query, URL=" + sruUri);
        return get;
    }

    @Override
    public int getNumberOfResults(String query) {
        // punt if there is no query text
        if (query == null || query.trim().length() == 0) {
            return 0;
        }

        // 1. build CQL query
        DCPersonName pn = new DCPersonName(query);
        StringBuilder queryStringBuilder = new StringBuilder();
        queryStringBuilder.append("local.FirstName = \"").append(pn.getFirstNames()).
            append("\" and local.FamilyName = \"").append(pn.getLastName()).
                              append("\"");

        HttpGet get = constructHttpGet(queryStringBuilder, 0, 1);

        // 2. web request
        try {
            HttpClient hc = new DefaultHttpClient();
            HttpResponse response = hc.execute(get);
            if (response.getStatusLine().getStatusCode() == 200) {
                SRUHandler handler = parseResponseToSRUHandler(response);
                return handler.hits;
            }
        } catch (IOException | ParserConfigurationException | SAXException e) {
            log.warn("Failed parsing SRU result: ", e);
            return 0;
        } finally {
            get.releaseConnection();
        }
        return 0;


    }

    private SRUHandler parseResponseToSRUHandler(HttpResponse response)
        throws ParserConfigurationException, SAXException, IOException {
        SAXParserFactory spf = SAXParserFactory.newInstance();
        SAXParser sp = spf.newSAXParser();
        XMLReader xr = sp.getXMLReader();
        SRUHandler handler = new SRUHandler(sourceIdentifier);

        // XXX FIXME: should turn off validation here explicitly, but
        //  it seems to be off by default.
        xr.setFeature("http://xml.org/sax/features/namespaces", true);
        xr.setContentHandler(handler);
        xr.setErrorHandler(handler);
        HttpEntity responseBody = response.getEntity();
        xr.parse(new InputSource(responseBody.getContent()));

        return handler;
    }

    /**
     * XXX FIXME TODO: Very sloppy MARC/XML parser.
     * This only reads subfields 010.a (for LCCN, to use as key)
     * and 100.a (for "established personal name")
     * Maybe look at Indicator on 100 too.
     * Should probably read other 100 subfields to build a more detailed label.
     */
    private static class SRUHandler
        extends DefaultHandler {
        private String sourceIdentifier;
        private List<ExternalDataObject> result = new ArrayList<ExternalDataObject>();
        private int hits = -1;
        private String textValue = null;
        private String name = null;
        private String birthDate = null;
        private String lccn = null;
        private String lastTag = null;
        private String lastCode = null;

        public SRUHandler(String sourceIdentifier) {
            super();
            this.sourceIdentifier = sourceIdentifier;
        }

        // NOTE:  text value MAY be presented in multiple calls, even if
        // it all one word, so be ready to splice it together.
        // BEWARE:  subclass's startElement method should call super()
        // to null out 'value'.  (Don't you miss the method combination
        // options of a real object system like CLOS?)
        @Override
        public void characters(char[] ch, int start, int length)
            throws SAXException {
            String newValue = new String(ch, start, length);
            if (newValue.length() > 0) {
                if (textValue == null) {
                    textValue = newValue;
                } else {
                    textValue += newValue;
                }
            }
        }

        @Override
        public void endElement(String namespaceURI, String localName,
                               String qName)
            throws SAXException {
            if (localName.equals("numberOfRecords") &&
                namespaceURI.equals(NS_SRU)) {
                hits = Integer.parseInt(textValue.trim());
                if (hits > 0) {
                    name = null;
                    lccn = null;
                    log.debug("Expecting " + hits + " records in results.");
                }
            } else if (localName.equals("record") &&
                namespaceURI.equals(NS_SRU)) {
                // after record get next hit ready
                if (name != null && lccn != null) {
                    // HACK: many LC name entries end with ',' ...trim it.
                    if (name.endsWith(",")) {
                        name = name.substring(0, name.length() - 1);
                    }

                    ExternalDataObject externalDataObject = new ExternalDataObject(sourceIdentifier);
                    externalDataObject.setDisplayValue(name);
                    externalDataObject.setValue(name);
                    externalDataObject.setId(lccn);
                    String[] names = name.split(", ");
                    String familyName = names[0];
                    String givenName = names.length > 1 ? names[1] : null;
                    if (StringUtils.isNotBlank(familyName)) {
                        externalDataObject
                            .addMetadata(new MetadataValueDTO("person", "familyName", null, null, familyName));
                    }
                    if (StringUtils.isNotBlank(givenName)) {
                        externalDataObject
                            .addMetadata(new MetadataValueDTO("person", "givenName", null, null, givenName));
                    }
                    if (StringUtils.isNotBlank(birthDate)) {
                        externalDataObject
                            .addMetadata(new MetadataValueDTO("person", "date", "birth", null, birthDate));
                    }
                    externalDataObject.addMetadata(new MetadataValueDTO("person", "identifier", "lccn", null, lccn));
                    result.add(externalDataObject);
                } else {
                    log.warn("Got anomalous result, at least one of these null: lccn=" + lccn + ", name=" + name);
                }
                name = null;
                lccn = null;
                birthDate = null;
            } else if (localName.equals("subfield") && namespaceURI.equals(NS_MX)) {
                if (lastTag != null && lastCode != null) {
                    if (lastTag.equals("010") && lastCode.equals("a")) {
                        // 010.a is lccn, "authority code"
                        lccn = textValue;
                    } else if (lastTag.equals("100") && lastCode.equals("a")) {
                        // 100.a is the personal name
                        name = textValue;
                    }
                    if (lastTag.equals("100") && lastCode.equals("d")) {
                        birthDate = textValue;
                    }
                }
            }
        }

        // subclass overriding this MUST call it with super()
        @Override
        public void startElement(String namespaceURI, String localName,
                                 String qName, Attributes atts)
            throws SAXException {
            textValue = null;

            if (localName.equals("datafield") &&
                namespaceURI.equals(NS_MX)) {
                lastTag = atts.getValue("tag");
                if (lastTag == null) {
                    log.warn("MARC datafield without tag attribute!");
                }
            } else if (localName.equals("subfield") &&
                namespaceURI.equals(NS_MX)) {
                lastCode = atts.getValue("code");
                if (lastCode == null) {
                    log.warn("MARC subfield without code attribute!");
                }
            }
        }

        @Override
        public void error(SAXParseException exception)
            throws SAXException {
            throw new SAXException(exception);
        }

        @Override
        public void fatalError(SAXParseException exception)
            throws SAXException {
            throw new SAXException(exception);
        }
    }
}
