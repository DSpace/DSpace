/*
 * ItemListTag.java
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

import org.dspace.app.webui.util.UIUtil;

import org.dspace.authorize.AuthorizeManager;

import org.dspace.content.Bitstream;
import org.dspace.content.Bundle;
import org.dspace.content.DCDate;
import org.dspace.content.DCValue;
import org.dspace.content.Item;

import org.dspace.core.ConfigurationManager;
import org.dspace.core.Constants;
import org.dspace.core.Context;
import org.dspace.core.Utils;

import org.dspace.storage.bitstore.BitstreamStorageManager;

import java.awt.image.BufferedImage;

import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;

import java.sql.SQLException;

import javax.imageio.ImageIO;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.jsp.JspException;
import javax.servlet.jsp.JspWriter;
import javax.servlet.jsp.jstl.fmt.LocaleSupport;
import javax.servlet.jsp.tagext.TagSupport;

/**
 * Tag for display a list of items
 * 
 * @author Robert Tansley
 * @version $Revision$
 */
public class ItemListTag extends TagSupport
{
    /** Items to display */
    private Item[] items;

    /** Row to highlight, -1 for no row */
    private int highlightRow = -1;

    /** Column to emphasise - null, "title" or "date" */
    private String emphColumn;

    /** Config value of thumbnail view toggle */
    private boolean showThumbs;

    /** Config browse/search width and height */
    private int thumbItemListMaxWidth;

    private int thumbItemListMaxHeight;

    /** Config browse/search thumbnail link behaviour */
    private boolean linkToBitstream = false;

    public ItemListTag()
    {
        super();
        getThumbSettings();
    }

    public int doStartTag() throws JspException
    {
        JspWriter out = pageContext.getOut();

        boolean emphasiseDate = false;
        boolean emphasiseTitle = false;

        if (emphColumn != null)
        {
            emphasiseDate = emphColumn.equalsIgnoreCase("date");
            emphasiseTitle = emphColumn.equalsIgnoreCase("title");
        }

        try
        {
            out.println("<table align=center class=\"miscTable\">");

            // Write column headings
            out.print("<tr><th class=\"oddRowOddCol\">"
                    + (emphasiseDate ? "<strong>" : "")
                    + LocaleSupport.getLocalizedMessage(pageContext,
                            "org.dspace.app.webui.jsptag.ItemListTag.issueDate")
                    + (emphasiseDate ? "</strong>" : "") + "</th>");

            out.println("<th class=\"oddRowEvenCol\">"
                    + (emphasiseTitle ? "<strong>" : "")
                    + LocaleSupport.getLocalizedMessage(pageContext,
                            "org.dspace.app.webui.jsptag.ItemListTag.title")
                    + (emphasiseTitle ? "</strong>" : "") + "</th>");

            out.println("<th class=\"oddRowOddCol\">"
                    + LocaleSupport.getLocalizedMessage(pageContext,
                            "org.dspace.app.webui.jsptag.ItemListTag.authors")
                    + "</th></tr>");

            // Row: toggles between Odd and Even
            String row = "even";

            for (int i = 0; i < items.length; i++)
            {
                // Title - we just use the first one
                DCValue[] titleArray = items[i].getDC("title", null, Item.ANY);
                String title = LocaleSupport.getLocalizedMessage(pageContext,
                                "jsp.general.untitled");

                if (titleArray.length > 0)
                {
                    title = titleArray[0].value;
                }

                // Authors....
                DCValue[] authors = items[i].getDC("contributor", Item.ANY,
                        Item.ANY);

                // Date issued
                DCValue[] dateIssued = items[i].getDC("date", "issued",
                        Item.ANY);
                DCDate dd = null;

                if (dateIssued.length > 0)
                {
                    dd = new DCDate(dateIssued[0].value);
                }

                // First column is date
                out.print("<tr><td nowrap class=\"");
                out.print((i == highlightRow) ? "highlight" : row);
                out.print("RowOddCol\" align=right>");

                if (emphasiseDate)
                {
                    out.print("<strong>");
                }

                out.print(UIUtil.displayDate(dd, false, false));

                if (emphasiseDate)
                {
                    out.print("</strong>");
                }

                HttpServletRequest hrq = (HttpServletRequest) pageContext
                        .getRequest();

                // display thumbnails if required
                if (showThumbs)
                {
                    out.print(getThumbMarkup(hrq, items[i]));
                }

                // Second column is title
                out.print("</td><td class=\"");
                out.print((i == highlightRow) ? "highlight" : row);
                out.print("RowEvenCol\">");

                if (emphasiseTitle)
                {
                    out.print("<strong>");
                }

                out.print("<A HREF=\"");
                out.print(hrq.getContextPath());
                out.print("/handle/");
                out.print(items[i].getHandle());
                out.print("\">");
                out.print(Utils.addEntities(title));
                out.print("</A>");

                if (emphasiseTitle)
                {
                    out.print("</strong>");
                }

                // Third column is authors
                out.print("</td><td class=\"");
                out.print((i == highlightRow) ? "highlight" : row);
                out.print("RowOddCol\">");

                for (int j = 0; j < authors.length; j++)
                {
                    out.print("<em>" + Utils.addEntities(authors[j].value)
                            + "</em>");

                    if (j < (authors.length - 1))
                    {
                        out.print("; ");
                    }
                }

                out.println("</td></tr>");

                row = (row.equals("odd") ? "even" : "odd");
            }

            out.println("</table>");
        }
        catch (IOException ie)
        {
            throw new JspException(ie);
        }

        return SKIP_BODY;
    }

    /**
     * Get the items to list
     * 
     * @return the items
     */
    public Item[] getItems()
    {
        return items;
    }

    /**
     * Set the items to list
     * 
     * @param itemsIn
     *            the items
     */
    public void setItems(Item[] itemsIn)
    {
        items = itemsIn;
    }

    /**
     * Get the row to highlight - null or -1 for no row
     * 
     * @return the row to highlight
     */
    public String getHighlightrow()
    {
        return String.valueOf(highlightRow);
    }

    /**
     * Set the row to highlight
     * 
     * @param highlightRowIn
     *            the row to highlight or -1 for no highlight
     */
    public void setHighlightrow(String highlightRowIn)
    {
        if ((highlightRowIn == null) || highlightRowIn.equals(""))
        {
            highlightRow = -1;
        }
        else
        {
            try
            {
                highlightRow = Integer.parseInt(highlightRowIn);
            }
            catch (NumberFormatException nfe)
            {
                highlightRow = -1;
            }
        }
    }

    /**
     * Get the column to emphasise - "title", "date" or null
     * 
     * @return the column to emphasise
     */
    public String getEmphcolumn()
    {
        return emphColumn;
    }

    /**
     * Set the column to emphasise - "title", "date" or null
     * 
     * @param emphcolumnIn
     *            column to emphasise
     */
    public void setEmphcolumn(String emphColumnIn)
    {
        emphColumn = emphColumnIn;
    }

    public void release()
    {
        highlightRow = -1;
        emphColumn = null;
        items = null;
    }

    /* get the required thumbnail config items */
    private void getThumbSettings()
    {
        showThumbs = ConfigurationManager
                .getBooleanProperty("webui.browse.thumbnail.show");

        if (showThumbs)
        {
            thumbItemListMaxHeight = ConfigurationManager
                    .getIntProperty("webui.browse.thumbnail.maxheight");

            if (thumbItemListMaxHeight == 0)
            {
                thumbItemListMaxHeight = ConfigurationManager
                        .getIntProperty("thumbnail.maxheight");
            }

            thumbItemListMaxWidth = ConfigurationManager
                    .getIntProperty("webui.browse.thumbnail.maxwidth");

            if (thumbItemListMaxWidth == 0)
            {
                thumbItemListMaxWidth = ConfigurationManager
                        .getIntProperty("thumbnail.maxwidth");
            }
        }

        String linkBehaviour = ConfigurationManager
                .getProperty("webui.browse.thumbnail.linkbehaviour");

        if (linkBehaviour != null)
        {
            if (linkBehaviour.equals("bitstream"))
            {
                linkToBitstream = true;
            }
        }
    }

    /*
     * Get the (X)HTML width and height attributes. As the browser is being used
     * for scaling, we only scale down otherwise we'll get hideously chunky
     * images. This means the media filter should be run with the maxheight and
     * maxwidth set greater than or equal to the size of the images required in
     * the search/browse
     */
    private String getScalingAttr(HttpServletRequest hrq, Bitstream bitstream)
            throws JspException
    {
        BufferedImage buf;

        try
        {
            Context c = UIUtil.obtainContext(hrq);

            InputStream is = BitstreamStorageManager.retrieve(c, bitstream
                    .getID());

            //AuthorizeManager.authorizeAction(bContext, this, Constants.READ);
            // 	read in bitstream's image
            buf = ImageIO.read(is);
        }
        catch (SQLException sqle)
        {
            throw new JspException(sqle.getMessage());
        }
        catch (IOException ioe)
        {
            throw new JspException(ioe.getMessage());
        }

        // now get the image dimensions
        float xsize = (float) buf.getWidth(null);
        float ysize = (float) buf.getHeight(null);

        // scale by x first if needed
        if (xsize > (float) thumbItemListMaxWidth)
        {
            // calculate scaling factor so that xsize * scale = new size (max)
            float scale_factor = (float) thumbItemListMaxWidth / xsize;

            // now reduce x size and y size
            xsize = xsize * scale_factor;
            ysize = ysize * scale_factor;
        }

        // scale by y if needed
        if (ysize > (float) thumbItemListMaxHeight)
        {
            float scale_factor = (float) thumbItemListMaxHeight / ysize;

            // now reduce x size
            // and y size
            xsize = xsize * scale_factor;
            ysize = ysize * scale_factor;
        }

        StringBuffer sb = new StringBuffer("width=\"").append(xsize).append(
                "\" height=\"").append(ysize).append("\"");

        return sb.toString();
    }

    /* generate the (X)HTML required to show the thumbnail */
    private String getThumbMarkup(HttpServletRequest hrq, Item item)
            throws JspException
    {
        Bundle[] original = item.getBundles("ORIGINAL");
        boolean html = false;

        // if multiple bitstreams, check if the primary one is HTML
        if (original[0].getBitstreams().length > 1)
        {
            Bitstream[] bitstreams = original[0].getBitstreams();

            for (int i = 0; (i < bitstreams.length) && !html; i++)
            {
                if (bitstreams[i].getID() == original[0]
                        .getPrimaryBitstreamID())
                {
                    html = bitstreams[i].getFormat().getMIMEType().equals(
                            "text/html");
                }
            }
        }

        Bundle[] thumbs = item.getBundles("THUMBNAIL");

        // if there are thumbs and we're not dealing with an HTML item
        // then show the thumbnail
        if ((thumbs.length > 0) && !html)
        {
            try
            {
                Context c = UIUtil.obtainContext(hrq);

                Bitstream thumbnailBitstream;
                Bitstream originalBitstream;

                if ((original[0].getBitstreams().length > 1)
                        && (original[0].getPrimaryBitstreamID() > -1))
                {
                    originalBitstream = Bitstream.find(c, original[0]
                            .getPrimaryBitstreamID());
                    thumbnailBitstream = thumbs[0]
                            .getBitstreamByName(originalBitstream.getName()
                                    + ".jpg");
                }
                else
                {
                    originalBitstream = original[0].getBitstreams()[0];
                    thumbnailBitstream = thumbs[0].getBitstreams()[0];
                }

                if ((thumbnailBitstream != null)
                        && (AuthorizeManager.authorizeActionBoolean(c,
                                thumbnailBitstream, Constants.READ)))
                {
                    StringBuffer thumbLink;

                    if (linkToBitstream)
                    {
                        thumbLink = new StringBuffer(
                                "<br/><a target=_blank href=\"").append(
                                hrq.getContextPath()).append("/bitstream/")
                                .append(item.getHandle()).append("/").append(
                                        originalBitstream.getSequenceID())
                                .append("/").append(
                                		UIUtil.encodeBitstreamName(originalBitstream
                                                .getName(),
                                                Constants.DEFAULT_ENCODING));
                    }
                    else
                    {
                        thumbLink = new StringBuffer("<br/><a href=\"").append(
                                hrq.getContextPath()).append("/handle/")
                                .append(item.getHandle());
                    }

                    thumbLink.append("\"><img src=\"").append(
                            hrq.getContextPath()).append("/retrieve/").append(
                            thumbnailBitstream.getID()).append("/").append(
                            		UIUtil.encodeBitstreamName(thumbnailBitstream.getName(),
                                    Constants.DEFAULT_ENCODING)).append(
                            "\" alt=\"").append(thumbnailBitstream.getName())
                            .append("\" ").append(
                                    getScalingAttr(hrq, thumbnailBitstream))
                            .append("/></a>");

                    return thumbLink.toString();
                }
            }
            catch (SQLException sqle)
            {
                throw new JspException(sqle.getMessage());
            }
            catch (UnsupportedEncodingException e)
            {
                throw new JspException(
                        "Server does not support DSpace's default encoding. ",
                        e);
            }
        }

        return "";
    }
}
