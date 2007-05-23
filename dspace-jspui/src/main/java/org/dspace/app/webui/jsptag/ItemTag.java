/*
 * ItemTag.java
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

import org.apache.log4j.Logger;
import org.dspace.app.webui.util.UIUtil;

import org.dspace.content.Bitstream;
import org.dspace.content.Bundle;
import org.dspace.content.Collection;
import org.dspace.content.DCDate;
import org.dspace.content.DCValue;
import org.dspace.content.Item;

import org.dspace.core.ConfigurationManager;
import org.dspace.core.Constants;
import org.dspace.core.Utils;

import java.io.IOException;
import java.net.URLEncoder;

import java.sql.SQLException;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;
import java.util.StringTokenizer;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.jsp.JspException;
import javax.servlet.jsp.JspWriter;
import javax.servlet.jsp.jstl.fmt.LocaleSupport;
import javax.servlet.jsp.tagext.TagSupport;

/**
 * <P>
 * JSP tag for displaying an item.
 * </P>
 * <P>
 * The fields that are displayed can be configured in <code>dspace.cfg</code>
 * using the <code>webui.itemdisplay.(style)</code> property. The form is
 * </P>
 * 
 * <PRE>
 * 
 * &lt;schema prefix>.&lt;element&gt;[.&lt;qualifier&gt;|.*][(date)|(link)], ...
 * 
 * </PRE>
 * 
 * <P>
 * For example:
 * </P>
 * 
 * <PRE>
 * 
 * dc.title = Dublin Core element 'title' (unqualified) dc.title.alternative =
 * DC element 'title', qualifier 'alternative' dc.title.* = All fields with
 * Dublin Core element 'title' (any or no qualifier) dc.identifier.uri(link) =
 * DC identifier.uri, render as a link dc.date.issued(date) = DC date.issued,
 * render as a date
 * 
 * </PRE>
 * 
 * <P>
 * If an item has no value for a particular field, it won't be displayed. The
 * name of the field for display will be drawn from the current UI dictionary,
 * using the key:
 * </P>
 * 
 * <PRE>
 * 
 * "metadata.&lt;field&gt;"
 * 
 * e.g. "metadata.dc.title" "metadata.dc.contributor.*"
 * "metadata.dc.date.issued"
 * 
 * </PRE>
 * 
 * <P>
 * You can also specify which collections use which views.
 * </P>
 * 
 * <PRE>
 * 
 * webui.itemdisplay.&lt;style&gt;.collections = &lt;collection handle&gt;, ...
 * 
 * </PRE>
 * 
 * <P>
 * FIXME: This should be more database-driven
 * </P>
 * 
 * <PRE>
 * 
 * webui.itemdisplay.thesis.collections = 123456789/24, 123456789/35
 * 
 * </PRE>
 * 
 * @author Robert Tansley
 * @version $Revision$
 */
public class ItemTag extends TagSupport
{
    /** Item to display */
    private Item item;

    /** Collections this item appears in */
    private Collection[] collections;

    /** The style to use - "default" or "full" */
    private String style;

    /** Whether to show preview thumbs on the item page */
    private boolean showThumbs;

    /** Hashmap of collection Handles to styles to use, from dspace.cfg */
    private static Map collectionStyles;

    /** Default DC fields to display, in absence of configuration */
    private static String defaultFields = "dc.title, dc.title.alternative, dc.contributor.*, dc.subject, dc.date.issued(date), dc.publisher, dc.identifier.citation, dc.relation.ispartofseries, dc.description.abstract, dc.description, dc.identifier.govdoc, dc.identifier.uri(link), dc.identifier.isbn, dc.identifier.issn, dc.identifier.ismn, dc.identifier";

    /** log4j logger */
    private static Logger log = Logger.getLogger(ItemTag.class);

    public ItemTag()
    {
        super();
        getThumbSettings();
        collectionStyles = null;
    }

    public int doStartTag() throws JspException
    {
        try
        {
            if (style == null || style.equals(""))
            {
                // see if we need to use a particular style for a style
                // FIXME?: Trust owning collection
                Collection owner = item.getOwningCollection();
                if (owner != null)
                {
                    getStyleFor(owner);
                }
                else
                {
                    style = "default";
                }
            }

            if (style.equals("full"))
            {
                renderFull();
            }
            else
            {
                render();
            }
        }
        catch (SQLException sqle)
        {
            throw new JspException(sqle);
        }
        catch (IOException ie)
        {
            throw new JspException(ie);
        }

        return SKIP_BODY;
    }

    /**
     * Get the item this tag should display
     * 
     * @return the item
     */
    public Item getItem()
    {
        return item;
    }

    /**
     * Set the item this tag should display
     * 
     * @param itemIn
     *            the item to display
     */
    public void setItem(Item itemIn)
    {
        item = itemIn;
    }

    /**
     * Get the collections this item is in
     * 
     * @return the collections
     */
    public Collection[] getCollections()
    {
        return collections;
    }

    /**
     * Set the collections this item is in
     * 
     * @param collectionsIn
     *            the collections
     */
    public void setCollections(Collection[] collectionsIn)
    {
        collections = collectionsIn;
    }

    /**
     * Get the style this tag should display
     * 
     * @return the style
     */
    public String getStyle()
    {
        return style;
    }

    /**
     * Set the style this tag should display
     * 
     * @param styleIn
     *            the Style to display
     */
    public void setStyle(String styleIn)
    {
        style = styleIn;
    }

    public void release()
    {
        style = "default";
        item = null;
        collections = null;
    }

    /**
     * Render an item in the given style
     */
    private void render() throws IOException
    {
        JspWriter out = pageContext.getOut();
        HttpServletRequest request = (HttpServletRequest)pageContext.getRequest();
        
        String configLine = ConfigurationManager
                .getProperty("webui.itemdisplay." + style);

        if (configLine == null)
            configLine = defaultFields;

        out.println("<center><table class=\"itemDisplayTable\">");

        /*
         * Break down the configuration into fields and display them
         * 
         * FIXME?: it may be more efficient to do some processing once, perhaps
         * to a more efficient intermediate class, but then it would become more
         * difficult to reload the configuration "on the fly".
         */
        StringTokenizer st = new StringTokenizer(configLine, ",");

        while (st.hasMoreTokens())
        {
        	String field = st.nextToken().trim();
            boolean isDate = false;
            boolean isLink = false;
            boolean isAuthor = isAuthor(field);
            boolean isSubject = isSubject(field);

            // Find out if the field should rendered as a date or link

            if (field.indexOf("(date)") > 0)
            {
                field = field.replaceAll("\\(date\\)", "");
                isDate = true;
            }

            if (field.indexOf("(link)") > 0)
            {
                field = field.replaceAll("\\(link\\)", "");
                isLink = true;
            }
            // Get the separate schema + element + qualifier

            String[] eq = field.split("\\.");
            String schema = eq[0];
            String element = eq[1];
            String qualifier = null;
            if (eq.length > 2 && eq[2].equals("*"))
            {
                qualifier = Item.ANY;
            }
            else if (eq.length > 2)
            {
                qualifier = eq[2];
            }
            // FIXME: Still need to fix for metadata language?
            DCValue[] values = item.getMetadata(schema, element, qualifier, Item.ANY);

            if (values.length > 0)
            {
                out.print("<tr><td class=\"metadataFieldLabel\">");

                out.print(LocaleSupport.getLocalizedMessage(pageContext,
                        "metadata." + field));

                out.print(":&nbsp;</td><td class=\"metadataFieldValue\">");

                for (int j = 0; j < values.length; j++)
                {
                    if (j > 0)
                    {
                        out.print("<br />");
                    }

                    if (isLink)
                    {
                        out.print("<a href=\"" + values[j].value + "\">"
                                + Utils.addEntities(values[j].value) + "</a>");
                    }
                    else if (isDate)
                    {
                        DCDate dd = new DCDate(values[j].value);

                        // Parse the date
                        out.print(UIUtil.displayDate(dd, false, false, (HttpServletRequest)pageContext.getRequest()));
                    }
                    else if (isAuthor)
                    {
                        out.print("<a href=\"" + request.getContextPath() + "/items-by-author?author="
                            + URLEncoder.encode(values[j].value, "UTF-8") + "\">" + values[j].value
                            + "</a>");
                    }
                    else if (isSubject)
                    {
                        out.print("<a href=\"" + request.getContextPath() + "/items-by-subject?subject="
                            + URLEncoder.encode(values[j].value, "UTF-8") + "\">" + values[j].value
                            + "</a>");
                    }
                    else
                    {
                        out.print(Utils.addEntities(values[j].value));
                    }
                }

                out.println("</td></tr>");
            }
        }

        listCollections();

        out.println("</table></center><br/>");

        listBitstreams();

        if (ConfigurationManager
                .getBooleanProperty("webui.licence_bundle.show"))

        {
            out.println("<br/><br/>");
            showLicence();
        }
    }

    /**
     * Render full item record
     */
    private void renderFull() throws IOException
    {
        JspWriter out = pageContext.getOut();

        // Get all the metadata
        DCValue[] values = item.getMetadata(Item.ANY, Item.ANY, Item.ANY, Item.ANY);

        out.println("<p align=\"center\">"
                + LocaleSupport.getLocalizedMessage(pageContext,
                        "org.dspace.app.webui.jsptag.ItemTag.full") + "</p>");

        // Three column table - DC field, value, language
        out.println("<center><table class=\"itemDisplayTable\">");
        out.println("<tr><th id=\"s1\" class=\"standard\">"
                + LocaleSupport.getLocalizedMessage(pageContext,
                        "org.dspace.app.webui.jsptag.ItemTag.dcfield")
                + "</th><th id=\"s2\" class=\"standard\">"
                + LocaleSupport.getLocalizedMessage(pageContext,
                        "org.dspace.app.webui.jsptag.ItemTag.value")
                + "</th><th id=\"s3\" class=\"standard\">"
                + LocaleSupport.getLocalizedMessage(pageContext,
                        "org.dspace.app.webui.jsptag.ItemTag.lang")
                + "</th></tr>");

        for (int i = 0; i < values.length; i++)
        {
            boolean hidden = false;

            // Mask description.provenance
            if (values[i].element.equals("description")
                    && ((values[i].qualifier != null) && values[i].qualifier
                            .equals("provenance")))
            {
                hidden = true;
            }

            if (!hidden)
            {
                out
                        .print("<tr><td headers=\"s1\" class=\"metadataFieldLabel\">");
                out.print(values[i].schema);
                out.print("." + values[i].element);

                if (values[i].qualifier != null)
                {
                    out.print("." + values[i].qualifier);
                }

                out
                        .print("</td><td headers=\"s2\" class=\"metadataFieldValue\">");
                out.print(Utils.addEntities(values[i].value));
                out
                        .print("</td><td headers=\"s3\" class=\"metadataFieldValue\">");

                if (values[i].language == null)
                {
                    out.print("-");
                }
                else
                {
                    out.print(values[i].language);
                }

                out.println("</td></tr>");
            }
        }

        listCollections();

        out.println("</table></center><br/>");

        listBitstreams();

        if (ConfigurationManager
                .getBooleanProperty("webui.licence_bundle.show"))
        {
            out.println("<br/><br/>");
            showLicence();
        }
    }

    /**
     * List links to collections if information is available
     */
    private void listCollections() throws IOException
    {
        JspWriter out = pageContext.getOut();
        HttpServletRequest request = (HttpServletRequest) pageContext
                .getRequest();

        if (collections != null)
        {
            out.print("<tr><td class=\"metadataFieldLabel\">");
            if (item.getHandle()==null)  // assume workspace item
            {
                out.print(LocaleSupport.getLocalizedMessage(pageContext,
                        "org.dspace.app.webui.jsptag.ItemTag.submitted"));
            }
            else
            {
                out.print(LocaleSupport.getLocalizedMessage(pageContext,
                          "org.dspace.app.webui.jsptag.ItemTag.appears"));
            }
            out.print("</td><td class=\"metadataFieldValue\">");

            for (int i = 0; i < collections.length; i++)
            {
                out.print("<a href=\"");
                out.print(request.getContextPath());
                out.print("/handle/");
                out.print(collections[i].getHandle());
                out.print("\">");
                out.print(collections[i].getMetadata("name"));
                out.print("</a><br/>");
            }

            out.println("</td></tr>");
        }
    }

    /**
     * List bitstreams in the item
     */
    private void listBitstreams() throws IOException
    {
        JspWriter out = pageContext.getOut();
        HttpServletRequest request = (HttpServletRequest) pageContext
                .getRequest();

        out.print("<table align=\"center\" class=\"miscTable\"><tr>");
        out.println("<td class=\"evenRowEvenCol\"><p><strong>"
                + LocaleSupport.getLocalizedMessage(pageContext,
                        "org.dspace.app.webui.jsptag.ItemTag.files")
                + "</strong></p>");

        try
        {
        	Bundle[] bundles = item.getBundles("ORIGINAL");

        	if (bundles.length == 0)
        	{
        		out.println("<p>"
        				+ LocaleSupport.getLocalizedMessage(pageContext,
                            "org.dspace.app.webui.jsptag.ItemTag.files.no")
                            + "</p>");
        	}
        	else
        	{
        		boolean html = false;
        		String handle = item.getHandle();
        		Bitstream primaryBitstream = null;

        		Bundle[] bunds = item.getBundles("ORIGINAL");
        		Bundle[] thumbs = item.getBundles("THUMBNAIL");

        		// if item contains multiple bitstreams, display bitstream
        		// description
        		boolean multiFile = false;
        		Bundle[] allBundles = item.getBundles();

        		for (int i = 0, filecount = 0; (i < allBundles.length)
                    	&& !multiFile; i++)
        		{
        			filecount += allBundles[i].getBitstreams().length;
        			multiFile = (filecount > 1);
        		}

        		// check if primary bitstream is html
        		if (bunds[0] != null)
        		{
        			Bitstream[] bits = bunds[0].getBitstreams();

        			for (int i = 0; (i < bits.length) && !html; i++)
        			{
        				if (bits[i].getID() == bunds[0].getPrimaryBitstreamID())
        				{
        					html = bits[i].getFormat().getMIMEType().equals(
        							"text/html");
        					primaryBitstream = bits[i];
        				}
        			}
        		}

        		out
                    .println("<table cellpadding=\"6\"><tr><th id=\"t1\" class=\"standard\">"
                            + LocaleSupport.getLocalizedMessage(pageContext,
                                    "org.dspace.app.webui.jsptag.ItemTag.file")
                            + "</th>");

        		if (multiFile)
        		{

        			out
                        .println("<th id=\"t2\" class=\"standard\">"
                                + LocaleSupport
                                        .getLocalizedMessage(pageContext,
                                                "org.dspace.app.webui.jsptag.ItemTag.description")
                                + "</th>");
        		}

        		out.println("<th id=\"t3\" class=\"standard\">"
                    + LocaleSupport.getLocalizedMessage(pageContext,
                            "org.dspace.app.webui.jsptag.ItemTag.filesize")
                    + "</th><th id=\"t4\" class=\"standard\">"
                    + LocaleSupport.getLocalizedMessage(pageContext,
                            "org.dspace.app.webui.jsptag.ItemTag.fileformat")
                    + "</th></tr>");

            	// if primary bitstream is html, display a link for only that one to
            	// HTMLServlet
            	if (html)
            	{
            		// If no real Handle yet (e.g. because Item is in workflow)
            		// we use the 'fake' Handle db-id/1234 where 1234 is the
            		// database ID of the item.
            		if (handle == null)
            		{
            			handle = "db-id/" + item.getID();
            		}

            		out.print("<tr><td headers=\"t1\" class=\"standard\">");
                    out.print("<a target=\"_blank\" href=\"");
                    out.print(request.getContextPath());
                    out.print("/html/");
                    out.print(handle + "/");
                    out
                        .print(UIUtil.encodeBitstreamName(primaryBitstream
                                .getName(), Constants.DEFAULT_ENCODING));
                    out.print("\">");
                    out.print(primaryBitstream.getName());
                    out.print("</a>");
                    
                    
            		if (multiFile)
            		{
            			out.print("</td><td headers=\"t2\" class=\"standard\">");

            			String desc = primaryBitstream.getDescription();
            			out.print((desc != null) ? desc : "");
            		}

            		out.print("</td><td headers=\"t3\" class=\"standard\">");
                    out.print(UIUtil.formatFileSize(primaryBitstream.getSize()));
                    out.print("</td><td headers=\"t4\" class=\"standard\">");
            		out.print(primaryBitstream.getFormatDescription());
            		out
                        .print("</td><td class=\"standard\"><a target=\"_blank\" href=\"");
            		out.print(request.getContextPath());
            		out.print("/html/");
            		out.print(handle + "/");
            		out
                        .print(UIUtil.encodeBitstreamName(primaryBitstream
                                .getName(), Constants.DEFAULT_ENCODING));
            		out.print("\">"
                        + LocaleSupport.getLocalizedMessage(pageContext,
                                "org.dspace.app.webui.jsptag.ItemTag.view")
                        + "</a></td></tr>");
            	}	
            	else
            	{
            		for (int i = 0; i < bundles.length; i++)
            		{
            			Bitstream[] bitstreams = bundles[i].getBitstreams();

            			for (int k = 0; k < bitstreams.length; k++)
            			{
            				// Skip internal types
            				if (!bitstreams[k].getFormat().isInternal())
            				{

                                // Work out what the bitstream link should be
                                // (persistent
                                // ID if item has Handle)
                                String bsLink = "<a target=\"_blank\" href=\""
                                        + request.getContextPath();

                                if ((handle != null)
                                        && (bitstreams[k].getSequenceID() > 0))
                                {
                                    bsLink = bsLink + "/bitstream/"
                                            + item.getHandle() + "/"
                                            + bitstreams[k].getSequenceID() + "/";
                                }
                                else
                                {
                                    bsLink = bsLink + "/retrieve/"
                                            + bitstreams[k].getID() + "/";
                                }

                                bsLink = bsLink
                                        + UIUtil.encodeBitstreamName(bitstreams[k]
                                            .getName(),
                                            Constants.DEFAULT_ENCODING) + "\">";

            					out
                                    .print("<tr><td headers=\"t1\" class=\"standard\">");
                                out.print(bsLink);
            					out.print(bitstreams[k].getName());
                                out.print("</a>");
                                

            					if (multiFile)
            					{
            						out
                                        .print("</td><td headers=\"t2\" class=\"standard\">");

            						String desc = bitstreams[k].getDescription();
            						out.print((desc != null) ? desc : "");
            					}

            					out
                                    .print("</td><td headers=\"t3\" class=\"standard\">");
                                out.print(UIUtil.formatFileSize(bitstreams[k].getSize()));
            					out
                                .print("</td><td headers=\"t4\" class=\"standard\">");
            					out.print(bitstreams[k].getFormatDescription());
            					out
                                    .print("</td><td class=\"standard\" align=\"center\">");

            					// is there a thumbnail bundle?
            					if ((thumbs.length > 0) && showThumbs)
            					{
            						String tName = bitstreams[k].getName() + ".jpg";
                                    String tAltText = LocaleSupport.getLocalizedMessage(pageContext, "org.dspace.app.webui.jsptag.ItemTag.thumbnail");
            						Bitstream tb = thumbs[0]
                                        .	getBitstreamByName(tName);

            						if (tb != null)
            						{
            							String myPath = request.getContextPath()
                                            	+ "/retrieve/"
                                            	+ tb.getID()
                                            	+ "/"
                                            	+ UIUtil.encodeBitstreamName(tb
                                            			.getName(),
                                            			Constants.DEFAULT_ENCODING);

            							out.print(bsLink);
            							out.print("<img src=\"" + myPath + "\" ");
            							out.print("alt=\"" + tAltText
            									+ "\" /></a><br />");
            						}
            					}

            					out
                                    .print(bsLink
                                            + LocaleSupport
                                                    .getLocalizedMessage(
                                                            pageContext,
                                                            "org.dspace.app.webui.jsptag.ItemTag.view")
                                            + "</a></td></tr>");
            				}
            			}
            		}
            	}

            	out.println("</table>");
        	}
        }
        catch(SQLException sqle)
        {
        	throw new IOException(sqle.getMessage());
        }

        out.println("</td></tr></table>");
    }

    private void getThumbSettings()
    {
        showThumbs = ConfigurationManager
                .getBooleanProperty("webui.item.thumbnail.show");
    }

    /**
     * Link to the item licence
     */
    private void showLicence() throws IOException
    {
        JspWriter out = pageContext.getOut();
        HttpServletRequest request = (HttpServletRequest) pageContext
                .getRequest();

        Bundle[] bundles = null;
        try
        {
        	bundles = item.getBundles("LICENSE");
        }
        catch(SQLException sqle)
        {
        	throw new IOException(sqle.getMessage());
        }

        out.println("<table align=\"center\" class=\"attentionTable\"><tr>");

        out.println("<td class=\"attentionCell\"><p><strong>"
                + LocaleSupport.getLocalizedMessage(pageContext,
                        "org.dspace.app.webui.jsptag.ItemTag.itemprotected")
                + "</strong></p>");

        for (int i = 0; i < bundles.length; i++)
        {
            Bitstream[] bitstreams = bundles[i].getBitstreams();

            for (int k = 0; k < bitstreams.length; k++)
            {
                out.print("<div align=\"center\" class=\"standard\">");
                out.print("<strong><a target=\"_blank\" href=\"");
                out.print(request.getContextPath());
                out.print("/retrieve/");
                out.print(bitstreams[k].getID() + "/");
                out.print(UIUtil.encodeBitstreamName(bitstreams[k].getName(),
                        Constants.DEFAULT_ENCODING));
                out
                        .print("\">"
                                + LocaleSupport
                                        .getLocalizedMessage(pageContext,
                                                "org.dspace.app.webui.jsptag.ItemTag.viewlicence")
                                + "</a></strong></div>");
            }
        }

        out.println("</td></tr></table>");
    }

    /**
     * Find the style to use for a particular collection from dspace.cfg
     */
    private void getStyleFor(Collection c)
    {
        if (collectionStyles == null)
        {
            readCollectionStyleConfig();
        }

        String collStyle = (String) collectionStyles.get(c.getHandle());

        if (collStyle == null)
        {
            // No specific style specified for this collection
            style = "default";
            return;
        }

        // Specific style specified. Check style exists
        if (ConfigurationManager.getProperty("webui.itemdisplay." + collStyle) == null)
        {
            log
                    .warn("dspace.cfg specifies undefined item metadata display style '"
                            + collStyle
                            + "' for collection "
                            + c.getHandle()
                            + ".  Using default");
            style = "default";
            return;
        }

        // Style specified & exists
        style = collStyle;
    }

    private static void readCollectionStyleConfig()
    {
        collectionStyles = new HashMap();

        Enumeration e = ConfigurationManager.propertyNames();

        while (e.hasMoreElements())
        {
            String key = (String) e.nextElement();

            if (key.startsWith("webui.itemdisplay.")
                    && key.endsWith(".collections"))
            {
                String styleName = key.substring("webui.itemdisplay.".length(),
                        key.length() - ".collections".length());

                String[] collections = ConfigurationManager.getProperty(key)
                        .split(",");

                for (int i = 0; i < collections.length; i++)
                {
                    collectionStyles.put(collections[i].trim(), styleName
                            .toLowerCase());
                }

            }
        }
    }
    
    /**
     * Is the given field name an Author field? 
     * 
     * If undefined in dspace.cfg (webui.browse.index.author) it defaults
     * to using any field containing 'creator'.
     * 
     * @param field
     * @return Whether or not the given String is an author 
     */
    private boolean isAuthor(String field)
    {
        // Does the user want to link to authors?
        if (ConfigurationManager.getBooleanProperty("webui.authorlinks.enable", true) == false)
        {
           return false; 
        }
        
        //Check whether a given metadata field should be considered an author field.
        String authorField = ConfigurationManager.getProperty("webui.browse.index.author");
        if (authorField == null)
        {
            if (field.indexOf("contributor") > 0 || field.indexOf("creator") > 0)
                return true;
            else
                return false;
        }
        else
        {
            StringTokenizer st = new StringTokenizer(authorField, ",");
            String aField;

            while (st.hasMoreTokens())
            {
                aField = st.nextToken().trim();
                // does dspace.cfg allow all qualifiers for this element?
                if (aField.endsWith(".*"))
                {
                    // does the field have a qualifier?
                    int i = field.lastIndexOf(".");
                    if (i != field.indexOf("."))
                    {
                        // lop off qualifier
                        field = field.substring(0, i);
                    }
                }
                // check field against dspace.cfg
                if (aField.indexOf(field) >= 0)
                    return true;
            }
            //no match found
            return false;
        }
    }
    
    /**
     * Is the given field name a Subject field? 
     * 
     * If undefined in dspace.cfg (webui.browse.index.subject) it defaults
     * to using any field containing 'subject'.
     * 
     * @param field
     * @return Whether or not the given String is a subject 
     */
    private boolean isSubject(String field)
    {
        // Does the user want to link to subjects?
        if (ConfigurationManager.getBooleanProperty("webui.subjectlinks.enable", false) == false)
        {
           return false; 
        }
        
        // Check whether a given metadata field should be considered a subject field
        String subjectField = ConfigurationManager.getProperty("webui.browse.index.subject");
        
        if (subjectField == null)
        {
            if (field.indexOf("subject") > 0)
            {
                return true;
            }
            else
            {
                return false;
            }
        }
        else
        {
            StringTokenizer st = new StringTokenizer(subjectField, ",");
            String sField;
            
            while (st.hasMoreTokens())
            {
                sField = st.nextToken().trim();
                // does dspace.cfg allow all qualifiers for this element?
                if (sField.endsWith(".*"))
                {
                    // does the field have a qualifier?
                    int i = field.lastIndexOf(".");
                    if (i != field.indexOf("."))
                    {
                        // lop off qualifier
                        field = field.substring(0, i);
                    }
                }

                // check field against dspace.cfg
                if (sField.indexOf(field) >= 0)
                {
                    return true;
                }
            }
            
            //no match found
            return false;
        }
    }
}
