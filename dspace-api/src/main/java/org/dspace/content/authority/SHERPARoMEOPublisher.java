/*
 * SHERPARoMEOPublisher.java
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
 * Sample Publisher name authority based on SHERPA/RoMEO
 *
 *
 * WARNING: This is a very crude and incomplete implementation, done mainly
 *  as a proof-of-concept.  Any site that actually wants to use it will
 *  probably have to refine it (and give patches back to dspace.org).
 *
 * @see SHERPARoMEOProtocol
 * @author Larry Stone
 * @version $Revision $
 */
public class SHERPARoMEOPublisher extends SHERPARoMEOProtocol
{
    private static final String RESULT = "publisher";
    private static final String LABEL = "name";
    // note: the publisher records have nothing we can use as authority code.
    private static final String AUTHORITY = null;

    public SHERPARoMEOPublisher()
    {
        super();
    }

    public Choices getMatches(String text, int collection, int start, int limit, String locale)
    {
        // punt if there is no query text
        if (text == null || text.trim().length() == 0)
            return new Choices(true);

        // query args to add to SHERPA/RoMEO request URL
        NameValuePair args[] = new NameValuePair[2];
        args[0] = new NameValuePair("pub", text);
        args[1] = new NameValuePair("qtype","all"); // OR: starts, exact

        Choices result = query(RESULT, LABEL, AUTHORITY, args, start, limit);
        if (result == null)
        {
            result =  new Choices(true);
        }
        return result;
    }
}
