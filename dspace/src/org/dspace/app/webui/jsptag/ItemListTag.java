/*
 * ItemListTag.java
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
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.LinkedList;
import javax.servlet.ServletException;
import javax.servlet.jsp.tagext.TagSupport;
import javax.servlet.jsp.JspException;
import javax.servlet.jsp.JspWriter;
import javax.servlet.http.HttpServletRequest;

import org.dspace.app.webui.util.UIUtil;
import org.dspace.content.DCDate;
import org.dspace.content.DCValue;
import org.dspace.content.Item;
import org.dspace.handle.HandleManager;

/**
 * Tag for display a list of items
 *
 * @author  Robert Tansley
 * @version $Revision$
 */
public class ItemListTag extends TagSupport
{
    /** Items to display */
    private Item[] items;
    
    /** Corresponding handles */
    private String[] handles;
    
    /** Row to highlight, -1 for no row */
    private int highlightRow;
    
    /** Column to emphasise - null, "title" or "date" */
    private String emphColumn;
    

    public ItemListTag()
    {
        super();
    }
    

    public int doStartTag()
        throws JspException
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

            // Row: toggles between Odd and Even
            String row = "odd";

            for (int i = 0; i < items.length; i++)
            {
                // Title - we just use the first one
                DCValue[] titleArray = items[i].getDC("title", null, Item.ANY);
                String title = "Untitled";
                if (titleArray.length > 0)
                {
                    title = titleArray[0].value;
                }

                // Authors....
                DCValue[] authors = items[i].getDC("contributor",
                    "author",
                    Item.ANY);

                // Date issued
                DCValue[] dateIssued = items[i].getDC("date",
                    "issued",
                    Item.ANY);
                DCDate dd = null;
                if(dateIssued.length > 0)
                {
                    dd = new DCDate(dateIssued[0].value);
                }
                
                // First column is date
                out.print("<tr><td nowrap class=\"");
                out.print(i == highlightRow ? "highlight" : row);
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
                
                // Second column is title
                out.print("</td><td class=\"");
                out.print(i == highlightRow ? "highlight" : row);
                out.print("RowEvenCol\">");
                
                if (emphasiseTitle)
                {
                    out.print("<strong>");
                }
                
                out.print("<A HREF=\"item/");
                out.print(handles[i]);
                out.print("\">");
                out.print(title);
                out.print("</A>");
                
                if (emphasiseTitle)
                {
                    out.print("</strong>");
                }
                
                // Third column is authors
                out.print("</td><td class=\"" + row + "RowOddCol\">");

                for (int j = 0; j < authors.length; j++)
                {
                    out.print("<em>" + authors[j].value + "</em>");
                    
                    if (j < authors.length - 1)
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
    public List getItems()
    {
        return Arrays.asList(items);
    }
    

    /**
     * Set the items to list
     * 
     * @param  itemsIn  the items
     */
    public void setItems(List itemsIn)
    {
        items = new Item[itemsIn.size()];
        items = (Item[]) itemsIn.toArray(items);
    }


    /**
     * Get the corresponding handles
     *
     * @return the handles
     */
    public String[] getHandles()
    {
        return handles;
    }
    

    /**
     * Set the handles corresponding to items
     * 
     * @param  handlesIn  the handles 
     */
    public void setHandles(String[] handlesIn)
    {
        handles = handlesIn;
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
     * @param  highlightRowIn  the row to highlight or -1 for no highlight
     */
    public void setHighlightrow(String highlightRowIn)
    {
        if (highlightRowIn == null)
        {
            highlightRow = -1;
        }
        else
        {
            try
            {
                highlightRow = Integer.parseInt(highlightRowIn);
            }
            catch(NumberFormatException nfe)
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
     * @param  emphcolumnIn  column to emphasise
     */
    public void setEmphcolumn(String emphColumnIn)
    {
        emphColumn = emphColumnIn;
    }
}
