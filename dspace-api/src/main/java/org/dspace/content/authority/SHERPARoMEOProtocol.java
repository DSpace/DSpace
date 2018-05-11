/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.content.authority;

import java.io.IOException;
import java.util.List;
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

import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.utils.URLEncodedUtils;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;

/**
 * Choice Authority based on SHERPA/RoMEO - for Publishers and Journals
 * See the subclasses  SHERPARoMEOPublisher and SHERPARoMEOJournalTitle
 * for actual choice plugin implementations.  This is a superclass
 * containing all the common protocol logic.
 *
 * Reads these DSpace Config properties:
 *
 *    # contact URL for server
 *    sherpa.romeo.url = http://www.sherpa.ac.uk/romeoapi11.php
 *
 * WARNING: This is a very crude and incomplete implementation, done mainly
 *  as a proof-of-concept.  Any site that actually wants to use it will
 *  probably have to refine it (and give patches back to dspace.org).
 *
 * @see SHERPARoMEOPublisher
 * @see SHERPARoMEOJournalTitle
 * @author Larry Stone
 * @version $Revision $
 */
public abstract class SHERPARoMEOProtocol implements ChoiceAuthority
{
    private static final Logger log = Logger.getLogger(SHERPARoMEOProtocol.class);

    // contact URL from configuration
    private static String url = null;

    public SHERPARoMEOProtocol()
    {
        if (url == null)
        {
            url = ConfigurationManager.getProperty("sherpa.romeo.url");

            // sanity check
            if (url == null)
            {
                throw new IllegalStateException("Missing DSpace configuration keys for SHERPA/RoMEO Query");
            }
        }
    }

    // this implements the specific RoMEO API args and XML tag naming
    public abstract Choices getMatches(String text, Collection collection, int start, int limit, String locale);

    @Override
    public Choices getBestMatch(String field, String text, Collection collection, String locale)
    {
        return getMatches(field, text, collection, 0, 2, locale);
    }

    // XXX FIXME just punt, returning value, never got around to
    //  implementing a reverse query.
    @Override
    public String getLabel(String field, String key, String locale)
    {
        return key;
    }

    // NOTE - ignore limit and start for now
    protected Choices query(String result, String label, String authority,
                            List<BasicNameValuePair> args, int start, int limit)
    {
        HttpClient hc = new DefaultHttpClient();
        String srUrl = url + "?" + URLEncodedUtils.format(args, "UTF8");
        HttpGet get = new HttpGet(srUrl);

        log.debug("Trying SHERPA/RoMEO Query, URL=" + srUrl);

        try
        {
            HttpResponse response = hc.execute(get);
            if (response.getStatusLine().getStatusCode() == 200)
            {
                SAXParserFactory spf = SAXParserFactory.newInstance();
                SAXParser sp = spf.newSAXParser();
                XMLReader xr = sp.getXMLReader();
                SRHandler handler = new SRHandler(result, label, authority);

                // XXX FIXME: should turn off validation here explicitly, but
                //  it seems to be off by default.
                xr.setFeature("http://xml.org/sax/features/namespaces", true);
                xr.setContentHandler(handler);
                xr.setErrorHandler(handler);
                xr.parse(new InputSource(response.getEntity().getContent()));
                int confidence;
                if (handler.total == 0)
                {
                    confidence = Choices.CF_NOTFOUND;
                }
                else if (handler.total == 1)
                {
                    confidence = Choices.CF_UNCERTAIN;
                }
                else
                {
                    confidence = Choices.CF_AMBIGUOUS;
                }
                return new Choices(handler.result, start, handler.total, confidence, false);
            }
        }
        catch (IOException e)
        {
            log.error("SHERPA/RoMEO query failed: ", e);
            return null;
        }
        catch (ParserConfigurationException  e)
        {
            log.warn("Failed parsing SHERPA/RoMEO result: ", e);
            return null;
        }
        catch (SAXException  e)
        {
            log.warn("Failed parsing SHERPA/RoMEO result: ", e);
            return null;
        }
        finally
        {
            get.releaseConnection();
        }
        return null;
    }

    // SAX handler to grab SHERPA/RoMEO (and eventually other details) from result
    private static class SRHandler
        extends DefaultHandler
    {
        private Choice result[] = null;
        int rindex = 0; // result index
        int total = 0;

        // name of element containing a result, e.g. <journal>
        private String resultElement = null;

        // name of element containing the label e.g. <name>
        private String labelElement = null;

        // name of element containing the authority value e.g. <issn>
        private String authorityElement = null;

        protected String textValue = null;

        public SRHandler(String result, String label, String authority)
        {
            super();
            resultElement = result;
            labelElement = label;
            authorityElement = authority;
        }

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

        // if this was the FIRST "numhits" element, it's size of results:
        @Override
        public void endElement(String namespaceURI, String localName,
                                 String qName)
            throws SAXException
        {
            if (localName.equals("numhits"))
            {
                String stotal = textValue.trim();
                if (stotal.length() > 0)
                {
                    total = Integer.parseInt(stotal);
                    result = new Choice[total];
                    if (total > 0)
                    {
                        result[0] = new Choice();
                        log.debug("Got "+total+" records in results.");
                    }
                }
            }
            else if (localName.equals(resultElement))
            {
                // after start of result element, get next hit ready
                if (++rindex < result.length)
                {
                    result[rindex] = new Choice();
                }
            }
            else if (localName.equals(labelElement) && textValue != null)
            {
                // plug in label value
                result[rindex].value = textValue.trim();
                result[rindex].label = result[rindex].value; 
            }
            else if (authorityElement != null && localName.equals(authorityElement) && textValue != null)
            {
                // plug in authority value
                result[rindex].authority = textValue.trim();
            }
            else if (localName.equals("message") && textValue != null)
            {
                // error message
                log.warn("SHERPA/RoMEO response error message: " + textValue.trim());
            }
        }

        // subclass overriding this MUST call it with super()
        @Override
        public void startElement(String namespaceURI, String localName,
                                 String qName, Attributes atts)
            throws SAXException
        {
            textValue = null;
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
