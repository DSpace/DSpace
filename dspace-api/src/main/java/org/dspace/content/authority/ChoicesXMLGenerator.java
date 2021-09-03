/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.content.authority;

import org.xml.sax.ContentHandler;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.AttributesImpl;


/**
 * Record class to hold a set of Choices returned by an authority in response
 * to a search.
 *
 * @author Larry Stone
 * @see Choice
 */
public class ChoicesXMLGenerator
{
    // use the XHTML NS, even though this is  a fragment.
    protected static final String NS_URI = "http://www.w3.org/1999/xhtml";
    protected static final String NS_NAME = "";

    public static void generate(Choices result, String format, ContentHandler contentHandler)
        throws SAXException
    {
        AttributesImpl resultAtts = new AttributesImpl();
        if (result.more)
        {
            resultAtts.addAttribute("", "more", "more", "boolean", "true");
        }
        if (result.isError())
        {
            resultAtts.addAttribute("", "error", "error", "boolean", "true");
        }
        resultAtts.addAttribute("", "start", "start", "int", String.valueOf(result.start));
        resultAtts.addAttribute("", "total", "total", "int", String.valueOf(result.total));

        contentHandler.startDocument();

        // "select" HTML format for DSpace popup
        if (format != null && format.equalsIgnoreCase("select"))
        {
            contentHandler.startElement(NS_URI, NS_NAME, "select", resultAtts);
            for (int i = 0; i < result.values.length; ++i)
            {
                Choice mdav = result.values[i];
                AttributesImpl va = new AttributesImpl();
                va.addAttribute("", "authority", "authority", "string", mdav.authority == null ? "":mdav.authority);
                va.addAttribute("", "value", "value", "string", mdav.value);
                if (result.defaultSelected == i)
                {
                    va.addAttribute("", "selected", "selected", "boolean", "");
                }
                if(mdav.extras != null){
                    for(String extraLabel : mdav.extras.keySet()){
                        va.addAttribute("", extraLabel, extraLabel, "string", mdav.extras.get(extraLabel));
                    }
                }
                contentHandler.startElement(NS_URI, NS_NAME, "option", va);
                  contentHandler.characters(mdav.label.toCharArray(), 0, mdav.label.length());
                contentHandler.endElement(NS_URI, NS_NAME, "option");
            }
            contentHandler.endElement(NS_URI, NS_NAME, "select");
        }

        // "ul" HTML format (required by Scriptactulous autocomplete)
        else if (format != null && format.equalsIgnoreCase("ul"))
        {
            AttributesImpl classLabel = new AttributesImpl();
            classLabel.addAttribute("", "class", "class", "string", "label");
            AttributesImpl classValue = new AttributesImpl();
            classValue.addAttribute("", "class", "class", "string", "value");
            contentHandler.startElement(NS_URI, NS_NAME, "ul", resultAtts);
            for (int i = 0; i < result.values.length; ++i)
            {
                Choice mdav = result.values[i];
                AttributesImpl va = new AttributesImpl();
                va.addAttribute("", "authority", "authority", "string", mdav.authority == null ? "":mdav.authority);
                if (result.defaultSelected == i)
                {
                    va.addAttribute("", "selected", "selected", "boolean", "");
                }
                if(mdav.extras != null){
                    for(String extraLabel : mdav.extras.keySet()){
                        va.addAttribute("", extraLabel, extraLabel, "string", mdav.extras.get(extraLabel));
                    }
                }                
                contentHandler.startElement(NS_URI, NS_NAME, "li", va);
                  contentHandler.startElement(NS_URI, NS_NAME, "span", classLabel);
                    contentHandler.characters(mdav.label.toCharArray(), 0, mdav.label.length());
                  contentHandler.endElement(NS_URI, NS_NAME,   "span");
                  contentHandler.startElement(NS_URI, NS_NAME, "span", classValue);
                    contentHandler.characters(mdav.value.toCharArray(), 0, mdav.value.length());
                  contentHandler.endElement(NS_URI, NS_NAME,   "span");
                contentHandler.endElement(NS_URI, NS_NAME, "li");
            }
            contentHandler.endElement(NS_URI, NS_NAME, "ul");
        }

        // default is XML format, Choices/Choice
        else
        {
            contentHandler.startElement(NS_URI, NS_NAME, "Choices", resultAtts);
            for (int i = 0; i < result.values.length; ++i)
            {
                Choice mdav = result.values[i];
                AttributesImpl va = new AttributesImpl();
                va.addAttribute("", "authority", "authority", "string", mdav.authority == null ? "":mdav.authority);
                va.addAttribute("", "value", "value", "string", mdav.value);
                if(mdav.extras != null){
                    for(String extraLabel : mdav.extras.keySet()){
                        va.addAttribute("", extraLabel, extraLabel, "string", mdav.extras.get(extraLabel));
                    }
                }

                if (result.defaultSelected == i)
                {
                    va.addAttribute("", "selected", "selected", "boolean", "");
                }
                contentHandler.startElement(NS_URI, NS_NAME, "Choice", va);
                  contentHandler.characters(mdav.label.toCharArray(), 0, mdav.label.length());
                contentHandler.endElement(NS_URI, NS_NAME, "Choice");
            }
            contentHandler.endElement(NS_URI, NS_NAME, "Choices");
        }
        contentHandler.endDocument();
    }
}
