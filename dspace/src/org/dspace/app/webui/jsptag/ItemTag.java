/*
 * ItemTag.java
 *
 * Version: $Revision$
 *
 * Date: $Date$
 *
 * Copyright (c) 2001, Hewlett-Packard Company and Massachusetts
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
import java.util.Iterator;
import java.util.List;
import java.util.LinkedList;
import javax.servlet.ServletException;
import javax.servlet.jsp.tagext.TagSupport;
import javax.servlet.jsp.JspException;
import javax.servlet.jsp.JspWriter;
import javax.servlet.http.HttpServletRequest;

import org.dspace.app.webui.util.UIUtil;
import org.dspace.content.Bitstream;
import org.dspace.content.Bundle;
import org.dspace.content.Collection;
import org.dspace.content.Community;
import org.dspace.content.DCDate;
import org.dspace.content.DCValue;
import org.dspace.content.Item;

/**
 * Tag for displaying an item
 *
 * @author  Robert Tansley
 * @version $Revision$
 */
public class ItemTag extends TagSupport
{
    /** Item to display */
    private Item item;
    
    /** Collections this item appears in */
    private Collection[] collections;
    
    /**
     * Communities corresponding to the collections, such that collections[n] is
     * contained by communities[n].  This must have the same number of elements
     * as collections.
     */
    private Community[] communities;

    /** The style to use - "default" or "full" */
    private String style;


    public ItemTag()
    {
        super();
    }
    
    
    public int doStartTag()
        throws JspException
    {
        try
        {
            if (style != null && style.equals("full"))
            {
                renderFull();
            }
            else
            {
                renderDefault();
            }
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
     * @param  itemIn  the item to display
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
     * @param  collectionsIn  the collections
     */
    public void setCollections(Collection[] collectionsIn)
    {
        collections = collectionsIn;
    }


    /**
     * Get the communities corresponding to collections
     *
     * @return the communities
     */
    public Community[] getCommunities()
    {
        return communities;
    }
    

    /**
     * Set the communities corresponding to collections
     * 
     * @param  communitiesIn  the communities
     */
    public void setCommunities(Community[] communitiesIn)
    {
        communities = communitiesIn;
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
     * @param  styleIn  the Style to display
     */
    public void setStyle(String styleIn)
    {
        style = styleIn;
    }


    public void release()
    {
        style = "default";
        item = null;
        communities = null;
        collections = null;
    }
    
    
    /**
     * Render an item in the default style
     */
    private void renderDefault()
        throws IOException
    {
        JspWriter out = pageContext.getOut();

        // Build up a list of things to display.
        // To display a DC field from the item, do
        //   fields.add(new String[] {"Display Name", "element", "qualifier"});
        //      (or "qualifier" as null for unqualified)
        // to display an actual value without getting it from the item, do
        //   fields.add(new String[] {"Display Name", "The value to display")
        List fields = new LinkedList();

        // Title - special case, if there is no title, use "Untitled"
        DCValue[] titleDC = item.getDC("title", null, Item.ANY);
        if (titleDC.length == 0)
        {
            fields.add(new String[] {"Title", "Untitled"});
        }
        else
        {
            fields.add(new String[] {"Title", "title", null});
        }

        fields.add(new String[] {"Other Titles", "title", "alternative"});
        fields.add(new String[] {"Authors", "contributor", "author"});
        fields.add(new String[] {"Keywords", "subject", null});

        // Date issued
        DCValue[] dateIssued = item.getDC("date",
            "issued",
            Item.ANY);
        DCDate dd = null;
        if(dateIssued.length > 0)
        {
            dd = new DCDate(dateIssued[0].value);
        }
        String displayDate = UIUtil.displayDate(dd, false, false);

        fields.add(new String[] {"Issue Date", displayDate});

        fields.add(new String[] {"Series/Report no.", "relation", "ispartofseries"});
        fields.add(new String[] {"Abstract", "description", "abstract"});
        fields.add(new String[] {"Description", "description", null});
        fields.add(new String[] {"Gov't Doc # ", "identifier", "govdoc"});
        fields.add(new String[] {"URI", "identifier", "uri"});
        fields.add(new String[] {"ISBN", "identifier", "isbn"});
        fields.add(new String[] {"ISSN", "identifier", "issn"});
        fields.add(new String[] {"ISMN", "identifier", "ismn"});
        fields.add(new String[] {"Other Identifiers", "identifier", null});
        
        out.println("<center><table class=\"itemDisplayTable\">");
        
        Iterator fieldIterator = fields.iterator();
        while (fieldIterator.hasNext())
        {
            String[] fieldData = (String[]) fieldIterator.next();
            DCValue[] values;

            if (fieldData.length == 2)
            {
                // Value direct from field data
                DCValue v = new DCValue();
                v.value = fieldData[1];
                values = new DCValue[1];
                values[0] = v;
            }
            else
            {
                // Grab the value from the item
                values = item.getDC(fieldData[1], fieldData[2], Item.ANY);
            }

            // Only display the field if we have an actual value
            if (values.length > 0)
            {
                out.print("<tr><td class=\"metadataFieldLabel\">");
                out.print(fieldData[0]);
                out.print(":&nbsp;</td><td class=\"metadataFieldValue\">");
                out.print(values[0].value);

                for (int j = 1; j < values.length; j++)
                {
                    out.print("<br>");
                    out.print(values[j].value);
                }
                out.println("</td></tr>");
            }
        }
        
        listCollections();
        
        out.println("</table></center><br>");
        
        listBitstreams();
    }
    
    
    /**
     * Render full item record
     */
    private void renderFull()
        throws IOException
    {
        JspWriter out = pageContext.getOut();

        // Get all the metadata
        DCValue[] values = item.getDC(Item.ANY, Item.ANY, Item.ANY);
    
        out.println("<P align=center>Full metadata record</P>");

        // Three column table - DC field, value, language
        out.println("<center><table class=\"itemDisplayTable\">");
        out.println("<tr><th class=\"standard\">DC Field</th><th class=\"standard\">Value</th><th class=\"standard\">Language</th></tr>");
        
        for (int i = 0; i < values.length; i++)
        {
            boolean hidden = false;
            
            // Mask description.provenance
            if (values[i].element.equals("description") &&
                    (values[i].qualifier != null &&
                     values[i].qualifier.equals("provenance")))
            {
                hidden = true;
            }

            if (!hidden)
            {
                out.print("<tr><td class=\"metadataFieldLabel\">");
                out.print(values[i].element);
                if (values[i].qualifier != null)
                {
                    out.print("." + values[i].qualifier);
                }
                out.print("</td><td class=\"metadataFieldValue\">");
                out.print(values[i].value);
                out.print("</td><td class=\"metadataFieldValue\">");

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
        
        out.println("</table></center><br>");
        
        listBitstreams();
    }


    /**
     * List links to collections if information is available
     */
    private void listCollections()
        throws IOException
    {
        JspWriter out = pageContext.getOut();
        HttpServletRequest request =
            (HttpServletRequest) pageContext.getRequest();
    
        if (collections != null)
        {
            out.print("<tr><td class=\"metadataFieldLabel\">" +
                "Appears in Collections:</td><td class=\"metadataFieldValue\">");

            for (int i = 0; i < collections.length; i++)
            {
                out.print("<A HREF=\"");
                out.print(request.getContextPath());
                out.print("/communities/");
                out.print(communities[i].getID());
                out.print("/collections/");
                out.print(collections[i].getID());
                out.print("/\">");
                out.print(collections[i].getMetadata("name"));
                out.print("</A><BR>");
            }

            out.println("</td></tr>");
        }
    }


    /**
     * List bitstreams in the item
     */
    private void listBitstreams()
        throws IOException
    {
        JspWriter out = pageContext.getOut();
        HttpServletRequest request =
            (HttpServletRequest) pageContext.getRequest();

        out.print("<table align=center class=\"miscTable\"><tr>");
        out.println("<td class=evenRowEvenCol><P><strong>Files:</strong></P>");

        Bundle[] bundles = item.getBundles();
    
        if (bundles.length == 0)
        {
            out.println("<P>There are no files associated with this item.</P>");
        }
        else
        {        
            out.println("<UL>");

            for (int i = 0; i < bundles.length; i++)
            {
                Bitstream[] bitstreams = bundles[i].getBitstreams();

                for (int k = 0; k < bitstreams.length ; k++)
                {
                    // Skip internal types
                    if (!bitstreams[k].getFormat().isInternal())
                    {
                        out.print("<LI><A TARGET=_blank HREF=\"");
                        out.print(request.getContextPath());
                        out.print("/retrieve/");
                        out.print(bitstreams[k].getID());
                        out.print("\">");
                        out.print(bitstreams[k].getName());
                        out.print("</A> - ");
                        out.print(bitstreams[k].getSize());
                        out.print(" bytes; " + bitstreams[k].getFormatDescription());
                        out.println("</LI>");
                    }
                }
            }
        
            out.println("</UL>");
        }
        
        out.println("</td></tr></table>");
    }
}
