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

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.sql.SQLException;
import java.util.StringTokenizer;

import javax.imageio.ImageIO;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.jsp.JspException;
import javax.servlet.jsp.JspWriter;
import javax.servlet.jsp.jstl.fmt.LocaleSupport;
import javax.servlet.jsp.tagext.TagSupport;

import org.apache.log4j.Logger;

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
    
    /** The default fields to be displayed when listing items */
    private static String listFields = "dc.date.issued(date), dc.title, dc.contributor.*";
    
    /** The default field which is bound to the browse by date */
    private static String dateField = "dc.date.issued";
    
    /** The default field which is bound to the browse by title */
    private static String titleField = "dc.title";
    
    public ItemListTag()
    {
        super();
        getThumbSettings();
    }

    public int doStartTag() throws JspException
    {
        JspWriter out = pageContext.getOut();
        HttpServletRequest hrq = (HttpServletRequest) pageContext.getRequest();
        
        boolean emphasiseDate = false;
        boolean emphasiseTitle = false;

        if (emphColumn != null)
        {
            emphasiseDate = emphColumn.equalsIgnoreCase("date");
            emphasiseTitle = emphColumn.equalsIgnoreCase("title");
        }
        
        // get the elements to display
        String configLine = ConfigurationManager.getProperty("webui.itemlist.columns");
        if (configLine != null)
        {
            listFields = configLine;
        }
        
        // get the date and title fields
        String dateLine = ConfigurationManager.getProperty("webui.browse.index.date");
        if (dateLine != null)
        {
            dateField = dateLine;
        }
        
        String titleLine = ConfigurationManager.getProperty("webui.browse.index.title");
        if (titleLine != null)
        {
            titleField = titleLine;
        }
        
        StringTokenizer st = new StringTokenizer(listFields, ",");
        
//      make an array to hold all the frags that we will use
        int columns = st.countTokens();
        String[] frags = new String[columns * items.length];
        
        try 
        {
            out.println("<table align=\"center\" class=\"miscTable\" summary=\"This table browse all dspace content\">");
            out.println("<tr>");
            
            //      Write the column headings
            int colCount = 1;
            boolean isDate = false;
            boolean emph = false;
            
            while (st.hasMoreTokens())
            {
                String field = st.nextToken().toLowerCase().trim();
                String cOddOrEven = ((colCount % 2) == 0 ? "Odd" : "Even");
                
                // find out if the field is a date
                if (field.indexOf("(date)") > 0)
                {
                    field = field.replaceAll("\\(date\\)", "");
                    isDate = true;
                }
                
                // get the schema and the element qualifier pair
                // (Note, the schema is not used for anything yet)
                // (second note, I hate this bit of code.  There must be
                // a much more elegant way of doing this.  Tomcat has
                // some weird problems with variations on this code that 
                // I tried, which is why it has ended up the way it is)
                StringTokenizer eq = new StringTokenizer(field, ".");
                
                String[] tokens = { "", "", "" };
                int k = 0;
                while(eq.hasMoreTokens())
                {
                    tokens[k] = eq.nextToken().toLowerCase().trim();
                    k++;
                }
                String schema = tokens[0];
                String element = tokens[1];
                String qualifier = tokens[2];
                
                // find out if we are emphasising this field
                if ((field.equals(dateField) && emphasiseDate) || 
                        (field.equals(titleField) && emphasiseTitle))
                {
                    emph = true;
                }
                
                // prepare the strings for the header
                String id = "t" + Integer.toString(colCount);
                String css = "oddRow" + cOddOrEven + "Col";
                String message = "itemlist." + field;
                
                // output the header
                out.print("<th id=\"" + id +  "\" class=\"" + css + "\">"
                        + (emph ? "<strong>" : "")
                        + LocaleSupport.getLocalizedMessage(pageContext, message)
                        + (emph ? "</strong>" : "") + "</th>");
                
                // now prepare the frags for each of the table elements
                for (int i = 0; i < items.length; i++)
                {
                    // first get hold of the relevant metadata for this column
                    DCValue[] metadataArray;
                    if (qualifier.equals("*"))
                    {
                        metadataArray = items[i].getMetadata(schema, element, Item.ANY, Item.ANY);
                    }
                    else if (qualifier.equals(""))
                    {
                        metadataArray = items[i].getMetadata(schema, element, null, Item.ANY);
                    }
                    else
                    {
                        metadataArray = items[i].getMetadata(schema, element, qualifier, Item.ANY);
                    }
                    
                    // now prepare the content of the table division
                    String metadata = "-";
                    if (metadataArray.length > 0)
                    {
                        // format the date field correctly
                        if (isDate)
                        {
                            // this is to be consistent with the existing setup.
                            // seems like an odd place to put it though (FIXME)
                            String thumbs = "";
                            if (showThumbs)
                            {
                                thumbs = getThumbMarkup(hrq, items[i]);
                            }
                            DCDate dd = new DCDate(metadataArray[0].value);
                            metadata = UIUtil.displayDate(dd, false, false, (HttpServletRequest)pageContext.getRequest()) + thumbs;
                        }
                        // format the title field correctly                        
                        else if (field.equals(titleField))
                        {
                            metadata = "<a href=\"" 
                            + items[i].getIdentifier().getURL().toString() + "\">" 
                            + Utils.addEntities(metadataArray[0].value)
                            + "</a>";
                        }
                        // format all other fields
                        else
                        {
                            StringBuffer sb = new StringBuffer();
                            for (int j = 0; j < metadataArray.length; j++)
                            {
                                sb.append(Utils.addEntities(metadataArray[j].value));
                                if (j < (metadataArray.length - 1))
                                {
                                    sb.append("; ");
                                }
                            }
                            metadata = "<em>" + sb.toString() + "</em>";
                        }
                    }
                    
                    // now prepare the XHTML frag for this division
                    String rOddOrEven;
                    if (i == highlightRow)
                    {
                        rOddOrEven = "highlight";
                    }
                    else
                    {
                        rOddOrEven = ((i % 2) == 1 ? "odd" : "even");
                    }
                    
                    // prepare extra special layout requirements for dates
                    String extras = "";
                    if (isDate)
                    {
                        extras = "nowrap=\"nowrap\" align=\"right\"";
                    }
                    
                    int idx = ((i + 1) * columns) - columns + colCount - 1;
                    frags[idx] = "<td headers=\"" + id + "\" class=\"" 
                    	+ rOddOrEven + "Row" + cOddOrEven + "Col\" " + extras + ">"
                    	+ (emph ? "<strong>" : "") + metadata + (emph ? "</strong>" : "")
                    	+ "</td>";
                    
                }
                
                colCount++;
                isDate = false;
                emph = false;
            }
            
            out.println("</tr>");
            
            // now output all the frags in the right order for the page
            for (int i = 0; i < frags.length; i++)
            {
                if ((i + 1) % columns == 1)
                {
                    out.println("<tr>");
                }
                out.println(frags[i]);
                if ((i + 1) % columns == 0)
                {
                    out.println("</tr>");
                }
            }
            
            // close the table
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
     * @param emphColumnIn
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
            is.close();
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

        if (original.length == 0)
        {
        	return "";
        }
        
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

        try
        {
        	Bundle[] thumbs = item.getBundles("THUMBNAIL");

        	// if there are thumbs and we're not dealing with an HTML item
        	// then show the thumbnail
        	if ((thumbs.length > 0) && !html)
        	{
                Context c = UIUtil.obtainContext(hrq);

                Bitstream thumbnailBitstream;
                Bitstream originalBitstream;

                if ((original[0].getBitstreams().length > 1)
                        && (original[0].getPrimaryBitstreamID() > -1))
                {
                    originalBitstream = original[0].getPrimaryBitstream();
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
                    String thumbLink;

                    if (linkToBitstream)
                    {
                        thumbLink = "<br/><a target=\"_blank\" href=\"" +
                            hrq.getContextPath() + "/bitstream/" +
                            item.getIdentifier().getCanonicalForm() +
                            "/" + originalBitstream.getSequenceID() + "/" +
                            UIUtil.encodeBitstreamName(originalBitstream.getName(),
                                    Constants.DEFAULT_ENCODING);
                    }
                    else
                    {
                        thumbLink = "<br/><a href=\"" +
                            item.getIdentifier().getURL().toString();
                    }

                    thumbLink = thumbLink + "\"><img src=\"" +
                            hrq.getContextPath() + "/retrieve/" +
                            thumbnailBitstream.getID() + "/" +
                            UIUtil.encodeBitstreamName(thumbnailBitstream.getName(),
                                Constants.DEFAULT_ENCODING) +
                            "\" alt=\"" + thumbnailBitstream.getName() +
                            "\" " + getScalingAttr(hrq, thumbnailBitstream) +
                            "/></a>";

                    return thumbLink;
                }
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

        return "";
    }
}
