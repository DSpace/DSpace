/*
 * ChoicesXMLGenerator.java
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

import org.dspace.content.authority.Choices;
import org.dspace.content.authority.Choice;

import org.xml.sax.ContentHandler;
import org.xml.sax.SAXException;
import org.xml.sax.ext.LexicalHandler;
import org.xml.sax.Attributes;
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
    private static final String NS_URI = "http://www.w3.org/1999/xhtml";
    private static final String NS_NAME = "";

    public static void generate(Choices result, String format, ContentHandler contentHandler)
        throws SAXException
    {
        Attributes noAtts = new AttributesImpl();
        AttributesImpl resultAtts = new AttributesImpl();
        if (result.more)
            resultAtts.addAttribute("", "more", "more", "boolean", "true");
        if (result.isError())
            resultAtts.addAttribute("", "error", "error", "boolean", "true");
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
                    va.addAttribute("", "selected", "selected", "boolean", "");
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
                    va.addAttribute("", "selected", "selected", "boolean", "");
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
                if (result.defaultSelected == i)
                    va.addAttribute("", "selected", "selected", "boolean", "");
                contentHandler.startElement(NS_URI, NS_NAME, "Choice", va);
                  contentHandler.characters(mdav.label.toCharArray(), 0, mdav.label.length());
                contentHandler.endElement(NS_URI, NS_NAME, "Choice");
            }
            contentHandler.endElement(NS_URI, NS_NAME, "Choices");
        }
        contentHandler.endDocument();
    }
}
