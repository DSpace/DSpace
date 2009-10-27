/*
 * SFXLinkTag.java
 *
 * Version: $Revision$
 *
 * Date: $Date$
 *
 * Copyright (c) 2002-2005, Hewlett-Packard Company and Massachusetts
 * Institute of Technology.  All rights reserved.
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
 * - Neither the name of the Hewlett-Packard Company nor the name of the
 * Massachusetts Institute of Technology nor the names of their
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
package org.dspace.app.webui.jsptag;

import java.io.IOException;
import java.net.URLEncoder;

import javax.servlet.jsp.JspException;
import javax.servlet.jsp.tagext.TagSupport;

import org.dspace.content.DCPersonName;
import org.dspace.content.DCValue;
import org.dspace.content.Item;
import org.dspace.core.ConfigurationManager;
import org.dspace.core.Constants;

/**
 * Renders an SFX query link. Takes one attribute - "item" which must be an Item
 * object.
 * 
 * @author Robert Tansley
 * @version $Revision$
 */
public class SFXLinkTag extends TagSupport
{
    /** Item to display SFX link for */
    private Item item;

    public SFXLinkTag()
    {
        super();
    }

    public int doStartTag() throws JspException
    {
        try
        {
            String sfxServer = ConfigurationManager
                    .getProperty("sfx.server.url");

            if (sfxServer == null)
            {
                // No SFX server - SFX linking switched off
                return SKIP_BODY;
            }

            String sfxQuery = "";

            DCValue[] titles = item.getDC("title", null, Item.ANY);

            if (titles.length > 0)
            {
                sfxQuery = sfxQuery
                        + "&title="
                        + URLEncoder.encode(titles[0].value,
                                Constants.DEFAULT_ENCODING);
            }

            DCValue[] authors = item.getDC("contributor", "author", Item.ANY);

            if (authors.length > 0)
            {
                DCPersonName dpn = new DCPersonName(authors[0].value);
                sfxQuery = sfxQuery
                        + "&aulast="
                        + URLEncoder.encode(dpn.getLastName(),
                                Constants.DEFAULT_ENCODING);
                sfxQuery = sfxQuery
                        + "&aufirst="
                        + URLEncoder.encode(dpn.getFirstNames(),
                                Constants.DEFAULT_ENCODING);
            }

            DCValue[] isbn = item.getDC("identifier", "isbn", Item.ANY);

            if (isbn.length > 0)
            {
                sfxQuery = sfxQuery
                        + "&isbn="
                        + URLEncoder.encode(isbn[0].value,
                                Constants.DEFAULT_ENCODING);
            }

            DCValue[] issn = item.getDC("identifier", "issn", Item.ANY);

            if (issn.length > 0)
            {
                sfxQuery = sfxQuery
                        + "&issn="
                        + URLEncoder.encode(issn[0].value,
                                Constants.DEFAULT_ENCODING);
            }

            DCValue[] dates = item.getDC("date", "issued", Item.ANY);

            if (dates.length > 0)
            {
                String fullDate = dates[0].value;

                // Remove the time if there is one - day is greatest granularity
                // for SFX
                if (fullDate.length() > 10)
                {
                    fullDate = fullDate.substring(0, 10);
                }

                sfxQuery = sfxQuery
                        + "&date="
                        + URLEncoder.encode(fullDate,
                                Constants.DEFAULT_ENCODING);
            }

            // Remove initial &, if any
            if (sfxQuery.startsWith("&"))
            {
                sfxQuery = sfxQuery.substring(1);
            }

            pageContext.getOut().print(sfxServer + sfxQuery);
        }
        catch (IOException ie)
        {
            throw new JspException(ie);
        }

        return SKIP_BODY;
    }

    /**
     * Get the item this tag should display SFX Link for
     * 
     * @return the item
     */
    public Item getItem()
    {
        return item;
    }

    /**
     * Set the item this tag should display SFX Link for
     * 
     * @param itemIn
     *            the item
     */
    public void setItem(Item itemIn)
    {
        item = itemIn;
    }
}
