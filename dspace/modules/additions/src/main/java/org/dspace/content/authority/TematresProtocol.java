/**
 * 170913 - Dan Shinkai
 * Classe desenvolvida baseada na classe SHERPARMEOProtocol para carregar dados a partir do Tematres.
 */
package org.dspace.content.authority;

import java.io.IOException;
import javax.xml.parsers.SAXParserFactory;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.ParserConfigurationException;
import org.xml.sax.XMLReader;
import org.xml.sax.InputSource;
import org.xml.sax.helpers.DefaultHandler;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;

import org.apache.log4j.Logger;

import org.dspace.core.ConfigurationManager;

import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.commons.httpclient.util.EncodingUtil;
import org.apache.commons.httpclient.NameValuePair;
import org.apache.commons.httpclient.HttpException;

/**
 *    # URL do servidor de servicos
 *    tematres.url = http://[dspace.url]/a/tematres/vocab/services.php
 *          
 *    IMPORTANTE: A classe foi desenvolvida especificamente para ler os dados informados pelo Tematres. As informacoes apresentadas pelo Tematres ou qualquer
 *                outro web service devera, obrigatoriamente, apresentar o numero de ocorrencias retornadas antes da leitura das tags contendo informacoes 
 *                do resultado da busca. No caso do Tematres, essa informacoes esta definida pela tag cant_result, no qual foi alterada para ser apresentada
 *                no inicio das tags.
 *
 */
public abstract class TematresProtocol implements ChoiceAuthority
{
    private static Logger log = Logger.getLogger(TematresProtocol.class);

    // contact URL from configuration
    private static String url = null;

    public TematresProtocol()
    {
        if (url == null)
        {
            url = ConfigurationManager.getProperty("tematres.url");
                        
            // sanity check
            if (url == null)
            {
                throw new IllegalStateException("Missing DSpace configuration keys for Tematres Query");
            }
        }
    }

    // this implements the specific Tematres API args and XML tag naming
    public abstract Choices getMatches(String text, int collection, int start, int limit, String locale);

    public Choices getBestMatch(String field, String text, int collection, String locale)
    {
        return getMatches(field, text, collection, 0, 2, locale);
    }

    // XXX FIXME just punt, returning value, never got around to
    //  implementing a reverse query.
    public String getLabel(String field, String key, String locale)
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
                
                log.debug("Trying Tematres Query, URL="+srUrl);

        try
        {
            int status = hc.executeMethod(get);
            if (status == 200)
            {
                SAXParserFactory spf = SAXParserFactory.newInstance();
                SAXParser sp = spf.newSAXParser();
                XMLReader xr = sp.getXMLReader();
                TematresHandler handler = new TematresHandler(result, label, authority);

                // XXX FIXME: should turn off validation here explicitly, but
                //  it seems to be off by default.
                xr.setFeature("http://xml.org/sax/features/namespaces", true);
                xr.setContentHandler(handler);
                xr.setErrorHandler(handler);
                xr.parse(new InputSource(get.getResponseBodyAsStream()));
                
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
        catch (HttpException e)
        {
            log.error("Tematres query failed: ", e);
            return null;
        }
        catch (IOException e)
        {
            log.error("Tematres query failed: ", e);
            return null;
        }
        catch (ParserConfigurationException  e)
        {
            log.warn("Failed parsing Tematres result: ", e);
            return null;
        }
        catch (SAXException  e)
        {
            log.warn("Failed parsing Tematres result: ", e);
            return null;
        }
        finally
        {
            get.releaseConnection();
        }
        return null;
    }

    // SAX handler to grab Tematres (and eventually other details) from result
    private static class TematresHandler
        extends DefaultHandler
    {
        private Choice result[] = null;
        int rindex = 0; // result index
        int total = 0;

        // name of element containing a result, e.g. <term>
        private String resultElement = null;

        // name of element containing the label e.g. <string>
        private String labelElement = null;

        // name of element containing the authority value e.g. <term_id>
        private String authorityElement = null;

        protected String textValue = null;

        public TematresHandler(String result, String label, String authority)
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
        public void endElement(String namespaceURI, String localName,
                                 String qName)
            throws SAXException
        {
                if (localName.equals("cant_result"))
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