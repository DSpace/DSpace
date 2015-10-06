/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.content.authority;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.List;
import java.util.ArrayList;
import javax.xml.parsers.SAXParserFactory;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.ParserConfigurationException;

import org.dspace.content.Collection;
import org.xml.sax.XMLReader;
import org.xml.sax.InputSource;
import org.xml.sax.helpers.DefaultHandler;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;

import org.apache.log4j.Logger;

import org.dspace.core.ConfigurationManager;
import org.dspace.content.DCPersonName;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.impl.client.DefaultHttpClient;

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
 *      lcname.url = http://alcme.oclc.org/srw/search/lcnaf
 *
 *  TODO: make # of results to ask for (and return) configurable.
 *
 * @author Larry Stone
 * @version $Revision $
 */
public class LCNameAuthority implements ChoiceAuthority
{
    private static final Logger log = Logger.getLogger(LCNameAuthority.class);

    // get these from configuration
    protected static String url = null;

    // NS URI for SRU respones
    protected static final String NS_SRU = "http://www.loc.gov/zing/srw/";

    // NS URI for MARC/XML
    protected static final String NS_MX = "http://www.loc.gov/MARC21/slim";

    // constructor does static init too..
    public LCNameAuthority()
    {
        if (url == null)
        {
            url = ConfigurationManager.getProperty("lcname.url");

            // sanity check
            if (url == null)
            {
                throw new IllegalStateException("Missing DSpace configuration keys for LCName Query");
            }
        }
    }

    // punt!  this is a poor implementation..
    @Override
    public Choices getBestMatch(String field, String text, Collection collection, String locale)
    {
        return getMatches(field, text, collection, 0, 2, locale);
    }

    /**
     * Match a proposed value against name authority records
     * Value is assumed to be in "Lastname, Firstname" format.
     */
    @Override
    public Choices getMatches(String field, String text, Collection collection, int start, int limit, String locale)
    {
        Choices result = queryPerson(text, start, limit);
        if (result == null)
        {
            result = new Choices(true);
        }
        
        return result;
    }

    // punt; supposed to get the canonical display form of a metadata authority key
    // XXX FIXME implement this with a query on the authority key, cache results
    @Override
    public String getLabel(String field, String key, String locale)
    {
        return key;
    }

    /**
     * Guts of the implementation, returns a complete Choices result, or
     * null for a failure.
     */
    private Choices queryPerson(String text, int start, int limit)
    {
        // punt if there is no query text
        if (text == null || text.trim().length() == 0)
        {
            return new Choices(true);
        }

        // 1. build CQL query
        DCPersonName pn = new DCPersonName(text);
        StringBuilder query = new StringBuilder();
        query.append("local.FirstName = \"").append(pn.getFirstNames()).
          append("\" and local.FamilyName = \"").append(pn.getLastName()).
          append("\"");

        // XXX arbitrary default limit - should be configurable?
        if (limit == 0)
        {
            limit = 50;
        }

        URI sruUri;
        try {
            URIBuilder builder = new URIBuilder(url);
            builder.addParameter("operation", "searchRetrieve");
            builder.addParameter("version", "1.1");
            builder.addParameter("recordSchema", "info:srw/schema/1/marcxml-v1.1");
            builder.addParameter("query", query.toString());
            builder.addParameter("maximumRecords", String.valueOf(limit));
            builder.addParameter("startRecord", String.valueOf(start+1));
            sruUri = builder.build();
        } catch (URISyntaxException e) {
            log.error("SRU query failed: ", e);
            return new Choices(true);
        }
        HttpGet get = new HttpGet(sruUri);

        log.debug("Trying SRU query, URL=" + sruUri);

        // 2. web request
        try
        {
            HttpClient hc = new DefaultHttpClient();
            HttpResponse response = hc.execute(get);
            if (response.getStatusLine().getStatusCode() == 200)
            {
                SAXParserFactory spf = SAXParserFactory.newInstance();
                SAXParser sp = spf.newSAXParser();
                XMLReader xr = sp.getXMLReader();
                SRUHandler handler = new SRUHandler();

                // XXX FIXME: should turn off validation here explicitly, but
                //  it seems to be off by default.
                xr.setFeature("http://xml.org/sax/features/namespaces", true);
                xr.setContentHandler(handler);
                xr.setErrorHandler(handler);
                HttpEntity responseBody = response.getEntity();
                xr.parse(new InputSource(responseBody.getContent()));

                // this probably just means more results available..
                if (handler.hits != handler.result.size())
                {
                    log.warn("Discrepency in results, result.length=" + handler.result.size() +
                            ", yet expected results=" + handler.hits);
                }
                boolean more = handler.hits > (start + handler.result.size());

                // XXX add non-auth option; perhaps the UI should do this?
                // XXX it's really a policy matter if they allow unauth result.
                   // XXX good, stop it.
                // handler.result.add(new Choice("", text, "Non-Authority: \""+text+"\""));

                int confidence;
                if (handler.hits == 0)
                {
                    confidence = Choices.CF_NOTFOUND;
                }
                else if (handler.hits == 1)
                {
                    confidence = Choices.CF_UNCERTAIN;
                }
                else
                {
                    confidence = Choices.CF_AMBIGUOUS;
                }
                return new Choices(handler.result.toArray(new Choice[handler.result.size()]),
                                   start, handler.hits, confidence, more);
            }
        }
        catch (IOException e)
        {
            log.error("SRU query failed: ", e);
            return new Choices(true);
        }
        catch (ParserConfigurationException  e)
        {
            log.warn("Failed parsing SRU result: ", e);
            return new Choices(true);
        }
        catch (SAXException  e)
        {
            log.warn("Failed parsing SRU result: ", e);
            return new Choices(true);
        }
        finally
        {
            get.releaseConnection();
        }
        return new Choices(true);
    }

    /**
     * XXX FIXME TODO: Very sloppy MARC/XML parser.
     * This only reads subfields 010.a (for LCCN, to use as key)
     * and 100.a (for "established personal name")
     * Maybe look at Indicator on 100 too.
     * Should probably read other 100 subfields to build a more detailed label.
     */
    private static class SRUHandler
        extends DefaultHandler
    {
        private List<Choice> result = new ArrayList<Choice>();
        private int hits = -1;
        private String textValue = null;
        private String name = null;
        private String lccn = null;
        private String lastTag = null;
        private String lastCode = null;

        // NOTE:  text value MAY be presented in multiple calls, even if
        // it all one word, so be ready to splice it together.
        // BEWARE:  subclass's startElement method should call super()
        // to null out 'value'.  (Don't you miss the method combination
        // options of a real object system like CLOS?)
        @Override
        public void characters(char[] ch, int start, int length)
            throws SAXException
        {
            String newValue = new String(ch, start, length);
            if (newValue.length() > 0)
            {
                if (textValue == null)
                {
                    textValue = newValue;
                }
                else
                {
                    textValue += newValue;
                }
            }
        }

        @Override
        public void endElement(String namespaceURI, String localName,
                                 String qName)
            throws SAXException
        {
            if (localName.equals("numberOfRecords") &&
                     namespaceURI.equals(NS_SRU))
            {
                hits = Integer.parseInt(textValue.trim());
                if (hits > 0)
                {
                    name = null;
                    lccn = null;
                    log.debug("Expecting "+hits+" records in results.");
                }
            }

            // after record get next hit ready
            else if (localName.equals("record") &&
                     namespaceURI.equals(NS_SRU))
            {
                if (name != null && lccn != null)
                {
                    // HACK: many LC name entries end with ',' ...trim it.
                    if (name.endsWith(","))
                    {
                        name = name.substring(0, name.length() - 1);
                    }

                    // XXX DEBUG
                    // log.debug("Got result, name="+name+", lccn="+lccn);
                    result.add(new Choice(lccn, name, name));
                }
                else
                {
                    log.warn("Got anomalous result, at least one of these null: lccn=" + lccn + ", name=" + name);
                }
                name = null;
                lccn = null;
            }

            else if (localName.equals("subfield") && namespaceURI.equals(NS_MX))
            {
                if (lastTag != null && lastCode != null)
                {
                    if (lastTag.equals("010") && lastCode.equals("a"))
                    {
                        // 010.a is lccn, "authority code"
                        lccn = textValue;
                    }
                    else if (lastTag.equals("100") && lastCode.equals("a"))
                    {
                        // 100.a is the personal name
                        name = textValue;
                    }

                    if (lastTag.equals("100") && lastCode.equals("d") && (name != null))
                    {
                        name = name + "  " + textValue;
                    }
                }
            }
        }

        // subclass overriding this MUST call it with super()
        @Override
        public void startElement(String namespaceURI, String localName,
                                 String qName, Attributes atts)
            throws SAXException
        {
            textValue = null;

            if (localName.equals("datafield") &&
                     namespaceURI.equals(NS_MX))
            {
                lastTag = atts.getValue("tag");
                if (lastTag == null)
                {
                    log.warn("MARC datafield without tag attribute!");
                }
            }
            else if (localName.equals("subfield") &&
                     namespaceURI.equals(NS_MX))
            {
                lastCode = atts.getValue("code");
                if (lastCode == null)
                {
                    log.warn("MARC subfield without code attribute!");
                }
            }
        }

        @Override
        public void error(SAXParseException exception)
            throws SAXException
        {
            throw new SAXException(exception);
        }

        @Override
        public void fatalError(SAXParseException exception)
            throws SAXException
        {
            throw new SAXException(exception);
        }
    }
}
