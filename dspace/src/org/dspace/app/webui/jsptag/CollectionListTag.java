/*
 * CollectionListTag.java
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
import org.dspace.content.Collection;
import org.dspace.handle.HandleManager;

/**
 * Tag for display a list of collections
 *
 * @author  Robert Tansley
 * @version $Revision$
 */
public class CollectionListTag extends TagSupport
{
    /** Collections to display */
    private Collection[] collections;
    
    /** Corresponding handles */
    private String[] handles;
    
    /** Row to highlight, -1 for no row */
    private int highlightRow = -1;
    
    /** Column to emphasise - null, "title" or "date" */
    private String emphColumn;
    

    public CollectionListTag()
    {
        super();
    }
    

    public int doStartTag()
        throws JspException
    {
        JspWriter out = pageContext.getOut();

// ignore emphasis        
//        boolean emphasiseDate  = false;
//        boolean emphasiseTitle = false;
//        
//        if (emphColumn != null)
//        {
//            emphasiseDate  = emphColumn.equalsIgnoreCase("date");
//            emphasiseTitle = emphColumn.equalsIgnoreCase("title");
//        }

        try
        {
            out.println("<table align=center class=\"miscTable\">");

            // Write column headings
            out.print("<tr><th class=\"oddRowOddCol\">" +
//                (emphasiseDate ? "<strong>" : "") +
                "Collection Name" +
//                (emphasiseDate ? "</strong>" : "") + 
                "</th></tr>");

            // Row: toggles between Odd and Even
            String row = "even";

            for (int i = 0; i < collections.length; i++)
            {
                // name
                String name = collections[i].getMetadata("name");
                
                // first and only column is 'name'
                out.print("</td><td class=\"");
                out.print(i == highlightRow ? "highlight" : row);
                out.print("RowEvenCol\">");
                
//                if (emphasiseTitle)
//                {
//                    out.print("<strong>");
//                }
                
                out.print("<A HREF=\"collection/");
                out.print(handles[i]);
                out.print("\">");
                out.print(name);
                out.print("</A>");
                
//                if (emphasiseTitle)
//                {
//                    out.print("</strong>");
//                }
                
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
     * Get the collections to list
     *
     * @return the collections
     */
    public Collection[] getCollections()
    {
        return collections;
    }
    

    /**
     * Set the collections to list
     * 
     * @param  collectionsIn  the collections
     */
    public void setCollections(Collection[] collectionsIn)
    {
        collections = collectionsIn;
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
     * Set the handles corresponding to collections
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
        if (highlightRowIn == null || highlightRowIn.equals(""))
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


    public void release()
    {
        highlightRow = -1;
        emphColumn   = null;
        collections  = null;
        handles      = null;
    }
}
