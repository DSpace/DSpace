/*
 * SHERPARoMEOProtocol.java
 *
 * Version: $Revision: 3705 $
 *
 * Date: $Date: 2009-04-11 13:02:24 -0400 (Sat, 11 Apr 2009) $
 *
 * Copyright (c) 2002-2009, The DSpace Foundation.  All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are
 * met:
 *
 * - Redistributions of source code must retain the above copyright
 * notice, this list of conditions and the following disclaimer.
 *
 * - Redistributions in binary form must reproduce the above copyright
 * notice, this list of conditions and the following disclaimer in the
 * documentation and/or other materials provided with the distribution.
 *
 * - Neither the name of the DSpace Foundation nor the names of its
 * contributors may be used to endorse or promote products derived from
 * this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 * ``AS IS'' AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 * LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR
 * A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT
 * HOLDERS OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT,
 * INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING,
 * BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS
 * OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 * ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR
 * TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE
 * USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH
 * DAMAGE.
 */
package org.dspace.content.authority;

import java.io.IOException;
import java.io.FileNotFoundException;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.FileReader;
import java.io.BufferedReader;
import java.util.Enumeration;
import java.util.List;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import javax.xml.parsers.SAXParserFactory;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.ParserConfigurationException;
import org.xml.sax.XMLReader;
import org.xml.sax.InputSource;
import org.xml.sax.helpers.DefaultHandler;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.SAXNotRecognizedException;
import org.xml.sax.SAXParseException;

import org.apache.log4j.Logger;

import org.dspace.core.ConfigurationManager;

import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.commons.httpclient.util.EncodingUtil;
import org.apache.commons.httpclient.NameValuePair;
import org.apache.commons.httpclient.HttpException;

/**
 * Choice Authority based on SHERPA/RoMEO - for Publishers and Journals
 * See the subclasses  SHERPARoMEOPublisher and SHERPARoMEOJournalTitle
 * for actual choice plugin implementations.  This is a superclass
 * containing all the common prototcol logic.
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
 * @see SHERPARoMEOPublisher, SHERPARoMEOJournalTitle
 * @author Larry Stone
 * @version $Revision $
 */
public abstract class SHERPARoMEOProtocol implements ChoiceAuthority
{
    private static Logger log = Logger.getLogger(SHERPARoMEOProtocol.class);

    // contact URL from configuration
    private static String url = null;

    public SHERPARoMEOProtocol()
    {
        if (url == null)
        {
            url = ConfigurationManager.getProperty("sherpa.romeo.url");

            // sanity check
            if (url == null)
                throw new IllegalStateException("Missing DSpace configuration keys for SHERPA/RoMEO Query");
        }
    }

    // this implements the specific RoMEO API args and XML tag naming
    public abstract Choices getMatches(String text, int collection, int start, int limit, String locale);

    public Choices getBestMatch(String text, int collection, String locale)
    {
        return getMatches(text, collection, 0, 2, locale);
    }

    // XXX FIXME just punt, returning value, never got around to
    //  implementing a reverse query.
    public String getLabel(String key, String locale)
    {
        return key;
    }

    // NOTE - ignore limit and start for now
    protected Choices query(String result, String label, String authority,
                            NameValuePair[] args, int start, int limit)
    {
        HttpClient hc = new HttpClient();
        String srUrl = url + "?" + EncodingUtil.formUrlEncode(args, "UTF8");
        GetMethod get = new GetMethod(srUrl);

        log.debug("Trying SHERPA/RoMEO Query, URL="+srUrl);

        try
        {
            int status = hc.executeMethod(get);
            if (status == 200)
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
                xr.parse(new InputSource(get.getResponseBodyAsStream()));
                int confidence;
                if (handler.total == 0)
                    confidence = Choices.CF_NOTFOUND;
                else if (handler.total == 1)
                    confidence = Choices.CF_UNCERTAIN;
                else
                    confidence = Choices.CF_AMBIGUOUS;
                return new Choices(handler.result, start, handler.total, confidence, false);
            }
        }
        catch (HttpException e)
        {
            log.error("SHERPA/RoMEO query failed: ", e);
            return null;
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
        public void characters(char[] ch, int start, int length)
            throws SAXException
        {
            String newValue = new String(ch, start, length);
            if (newValue.length() > 0)
            {
                if (textValue == null)
                    textValue = newValue;
                else
                    textValue += newValue;
            }
        }

        // if this was the FIRST "numhits" element, it's size of results:
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

            // after start of result element, get next hit ready
            else if (localName.equals(resultElement))
            {
                if (++rindex < result.length)
                    result[rindex] = new Choice();
            }

            // plug in label value
            else if (localName.equals(labelElement) && textValue != null)
                result[rindex].value =
                result[rindex].label = textValue.trim();

            // plug in authority value
            else if (authorityElement != null &&
                     localName.equals(authorityElement) && textValue != null)
                result[rindex].authority = textValue.trim();

            // error message
            else if (localName.equals("message") && textValue != null)
                log.warn("SHERPA/RoMEO response error message: "+textValue.trim());
        }

        // subclass overriding this MUST call it with super()
        public void startElement(String namespaceURI, String localName,
                                 String qName, Attributes atts)
            throws SAXException
        {
            textValue = null;
        }

        public void error(SAXParseException exception)
            throws SAXException
        {
            throw new SAXException(exception);
        }

        public void fatalError(SAXParseException exception)
            throws SAXException
        {
            throw new SAXException(exception);
        }
    }
}
