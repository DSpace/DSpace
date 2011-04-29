/*
 * AJAXMenuGenerator.java
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

package org.dspace.app.xmlui.cocoon;

import java.io.IOException;
import java.sql.SQLException;

import org.apache.cocoon.ProcessingException;
import org.apache.cocoon.ResourceNotFoundException;
import org.apache.cocoon.environment.ObjectModelHelper;
import org.apache.cocoon.environment.Request;
import org.apache.cocoon.generation.AbstractGenerator;
import org.dspace.app.xmlui.objectmanager.AbstractAdapter;
import org.dspace.app.xmlui.objectmanager.ContainerAdapter;
import org.dspace.app.xmlui.objectmanager.ItemAdapter;
import org.dspace.app.xmlui.objectmanager.RepositoryAdapter;
import org.dspace.app.xmlui.utils.ContextUtil;
import org.dspace.app.xmlui.wing.WingException;
import org.dspace.content.Collection;
import org.dspace.content.Community;
import org.dspace.content.DSpaceObject;
import org.dspace.content.Item;
import org.dspace.content.authority.Choices;
import org.dspace.content.authority.Choice;
import org.dspace.content.authority.ChoiceAuthorityManager;
import org.dspace.content.authority.ChoicesXMLGenerator;
import org.dspace.content.crosswalk.CrosswalkException;
import org.dspace.core.ConfigurationManager;
import org.dspace.core.Context;
import org.dspace.handle.HandleManager;

import org.xml.sax.ContentHandler;
import org.xml.sax.SAXException;
import org.xml.sax.ext.LexicalHandler;
import org.xml.sax.Attributes;
import org.xml.sax.helpers.AttributesImpl;

import org.apache.log4j.Logger;

/**
 * Generate XML description of Choice values to be used by
 * AJAX UI client.
 *
 * @author Larry Stone
 */
public class AJAXMenuGenerator extends AbstractGenerator
{
    private static Logger log = Logger.getLogger(AJAXMenuGenerator.class);

    /**
     * Generate the AJAX response Document.
     *
     * Looks for request paraemters:
     *  field - MD field key, i.e. form key, REQUIRED.
     *  start - index to start from, default 0.
     *  limit - max number of lines, default 1000.
     *  format - opt. result XML/XHTML format: "select", "ul", "xml"(default)
     *  locale - explicit locale, pass to choice plugin
     */
    public void generate()
        throws IOException, SAXException, ProcessingException
    {
        int start = 0;
        int limit = 1000;
        int collection = -1;
        String format = parameters.getParameter("format",null);
        String field = parameters.getParameter("field",null);
        String sstart = parameters.getParameter("start",null);
        if (sstart != null && sstart.length() > 0)
            start = Integer.parseInt(sstart);
        String slimit = parameters.getParameter("limit",null);
        if (slimit != null && slimit.length() > 0)
            limit = Integer.parseInt(slimit);
        String scoll = parameters.getParameter("collection",null);
        if (scoll != null && scoll.length() > 0)
            collection = Integer.parseInt(scoll);
        String query = parameters.getParameter("query",null);

        // localization
        String locale = parameters.getParameter("locale",null);
        log.debug("AJAX menu generator: field="+field+", query="+query+", start="+sstart+", limit="+slimit+", format="+format+", field="+field+", query="+query+", start="+sstart+", limit="+slimit+", format="+format+", locale = "+locale);

        Choices result =
            ChoiceAuthorityManager.getManager().getMatches(field, query, collection, start, limit, locale);

        log.debug("Result count = "+result.values.length+", default="+result.defaultSelected);

        ChoicesXMLGenerator.generate(result, format, contentHandler);
    }
}
