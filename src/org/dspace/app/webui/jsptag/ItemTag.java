/*
 * ItemTag.java
 *
 * Version: $Revision: 1.27 $
 *
 * Date: $Date: 2005/03/29 03:11:14 $
 *
 * Copyright (c) 2002, Hewlett-Packard Company and Massachusetts
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

import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import java.util.regex.Pattern;
import java.util.regex.Matcher;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.jsp.JspException;
import javax.servlet.jsp.JspWriter;
import javax.servlet.jsp.tagext.TagSupport;

/**
 * Tag for displaying an item
 * 
 * @author Robert Tansley
 * @version $Revision: 1.27 $
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

    public ItemTag()
    {
        super();
        getThumbSettings();
    }

    public int doStartTag() throws JspException
    {
        try
        {
            if ((style != null) && style.equals("full"))
            {
                renderFull();
            }
            else
            {
                renderDefault();
            }
        }
        catch (java.sql.SQLException e)
        {
            throw new JspException(e);
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
     * Render an item in the default style
     */
    private void renderDefault() throws IOException, java.sql.SQLException
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
            fields.add(new String[] { "Title", "Untitled" });
        }
        else
        {
            fields.add(new String[] { "Title", "title", null });
        }

        fields.add(new String[] { "Other Titles", "title", "alternative" });
        fields.add(new String[] {"Authors", "contributor", "author"});
        fields.add(new String[] {"Advisors", "contributor", "advisor"});
        fields.add(new String[] {"Editors", "contributor", "editor"});
        fields.add(new String[] {"Illustrators", "contributor", "illustrator"});
        fields.add(new String[] {"Contributors", "contributor", "other"});
        fields.add(new String[] {"Contributors", "contributor", null});
        fields.add(new String[] {"Department/Program", "contributor", "department"});
        fields.add(new String[] {"Type", "type", null});
        fields.add(new String[] {"Sponsors", "contributor", "publisher"});
        fields.add(new String[] { "Keywords", "subject", null });

        // Date issued
        DCValue[] dateIssued = item.getDC("date", "issued", Item.ANY);
        DCDate dd = null;

        if (dateIssued.length > 0)
        {
            dd = new DCDate(dateIssued[0].value);
        }

        String displayDate = UIUtil.displayDate(dd, false, false);

        fields.add(new String[] { "Issue Date", displayDate });

        fields.add(new String[] { "Publisher", "publisher", null });
        fields.add(new String[] { "Citation", "identifier", "citation" });
        fields.add(new String[] { "Series/Report no.", "relation",
                "ispartofseries" });

        // Truncate abstract
        DCValue[] abstrDC = item.getDC("description", "abstract", Item.ANY);

        if (abstrDC.length > 0)
        {
            String abstr = abstrDC[0].value;

            if (abstr.length() > 1000)
            {
                abstr = abstr.substring(0, 1000) + "...";
            }

            fields.add(new String[] { "Abstract", abstr });
        }

        fields.add(new String[] { "Required Publisher Statement", "rights", "license" });
        fields.add(new String[] { "Description", "description", null });
        fields.add(new String[] { "Gov't Doc # ", "identifier", "govdoc" });
        fields.add(new String[] { "URI", "identifier", "uri" });
        fields.add(new String[] { "ISBN", "identifier", "isbn" });
        fields.add(new String[] { "ISSN", "identifier", "issn" });
        fields.add(new String[] { "ISMN", "identifier", "ismn" });
        fields.add(new String[] { "Other Identifiers", "identifier", null });

        HttpServletRequest request = (HttpServletRequest) pageContext
                .getRequest();

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
                out.print(getDisplay(values[0].value));

                for (int j = 1; j < values.length; j++)
                {
                    out.print("<br>");
                    out.print(getDisplay(values[j].value));
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
    private void renderFull() throws IOException, java.sql.SQLException
    {
        JspWriter out = pageContext.getOut();

        // Get all the metadata
        DCValue[] values = item.getDC(Item.ANY, Item.ANY, Item.ANY);

        out.println("<P align=center>Full metadata record</P>");

        HttpServletRequest request = (HttpServletRequest) pageContext
                .getRequest();

        // Three column table - DC field, value, language
        out.println("<center><table class=\"itemDisplayTable\">");
        out
                .println("<tr><th class=\"standard\">DC Field</th><th class=\"standard\">Value</th><th class=\"standard\">Language</th></tr>");

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
                out.print("<tr><td class=\"metadataFieldLabel\">");
                out.print(values[i].element);

                if (values[i].qualifier != null)
                {
                    out.print("." + values[i].qualifier);
                }

                out.print("</td><td class=\"metadataFieldValue\">");
                out.print(getDisplay(values[i].value));
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
     * Make a displayable version of the field value.  html
     * encode the data and wrap http urls with an html anchor.
     */

    static Pattern pUrl = Pattern.compile("(.*?)(http://[^\\s]*)(.*)", Pattern.DOTALL);

    public static String getDisplay(String strOriginal) {
	StringBuffer sbRet = new StringBuffer();

	// Keep track of remaining text
	String strRemaining = strOriginal;

	// Look for the next url
	Matcher m = pUrl.matcher(strRemaining);
	while(m.matches()) {
	    // Before the url
	    sbRet.append(Utils.addEntities(m.group(1)));

	    // url with anchor
	    sbRet.append("<a href=\"");
	    sbRet.append(m.group(2));
	    sbRet.append("\">");
	    sbRet.append(Utils.addEntities(m.group(2)));
	    sbRet.append("</a>");

	    strRemaining = m.group(3);
	    m = pUrl.matcher(strRemaining);
	}	    

	sbRet.append(Utils.addEntities(strRemaining));

	return sbRet.toString();
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
            out
                    .print("<tr><td class=\"metadataFieldLabel\">"
                            + "Appears in Collections:</td><td class=\"metadataFieldValue\">");

            for (int i = 0; i < collections.length; i++)
            {
                out.print("<A HREF=\"");
                out.print(request.getContextPath());
                out.print("/handle/");
                out.print(collections[i].getHandle());
                out.print("\">");
                out.print(collections[i].getMetadata("name"));
                out.print("</A><BR>");
            }

            out.println("</td></tr>");
        }
    }


    /**
     * List bitstreams in the item
     */
  private void listBitstreams() throws IOException, java.sql.SQLException
    {
        JspWriter out = pageContext.getOut();
        HttpServletRequest request = (HttpServletRequest) pageContext
                .getRequest();

        out.print("<table align=center class=\"miscTable\"><tr>");
        out
                .println("<td class=evenRowEvenCol><P><strong>Files in This Item:</strong></P>");

        Bundle[] bundles = item.getBundles("ORIGINAL");

        if (bundles.length == 0)
        {
            out.println("<P>There are no files associated with this item.</P>");
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
            boolean multiFile = true;
            //Bundle[] allBundles = item.getBundles();
	    //
            //for (int i = 0, filecount = 0; (i < allBundles.length)
            //	      && !multiFile; i++)
            //{
            //	  filecount += allBundles[i].getBitstreams().length;
            //	  multiFile = (filecount > 1);
            //}

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
                    .println("<table cellpadding=6><tr><th class=\"standard\">File</th>");

            if (multiFile)
            {
                out.println("<th class=\"standard\">Description</th>");
            }

            out
	      .println("<th class=\"standard\">Size</th class=\"standard\"><th class=\"standard\">Format</th><th class=\"standard\">No. of Downloads</th></tr>");

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

                out.print("<tr><td class=\"standard\">");
                out.print(primaryBitstream.getName());

                if (multiFile)
                {
                    out.print("</td><td class=\"standard\">");

                    String desc = primaryBitstream.getDescription();
                    out.print((desc != null) ? desc : "");
                }

                out.print("</td><td class=\"standard\">");
                out.print(primaryBitstream.getSize() / 1024);
                out.print("Kb</td><td class=\"standard\">");
                out.print(primaryBitstream.getFormatDescription());
                out
                        .print("</td><td class=\"standard\"><A TARGET=_blank HREF=\"");
                out.print(request.getContextPath());
                out.print("/html/");
                out.print(handle + "/");
                out.print(UIUtil.encodeBitstreamName(primaryBitstream.getName(),
                        Constants.DEFAULT_ENCODING));
                out.print("\">View/Open</A></td></tr>");
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
                            out.print("<tr><td class=\"standard\">");
                            out.print(bitstreams[k].getName());

                            if (multiFile)
                            {
                                out.print("</td><td class=\"standard\">");

                                String desc = bitstreams[k].getDescription();

                                if (bitstreams[k].isETDEmbargo()) {
                                  desc = 
                                    "RESTRICTED ACCESS" + 
                                    (desc==null ? "" : "; " + desc);
                                }

                                out.print((desc != null) ? desc : "");
                            }

                            out.print("</td><td class=\"standard\">");
                            out.print(bitstreams[k].getSize() / 1024);
                            out.print("Kb</td><td class=\"standard\">");
                            out.print(bitstreams[k].getFormatDescription());
                            out.print("</td><td class=\"standard\" align=\"center\">");
                            out.print(bitstreams[k].getIntMetadata("views"));
                            out.print("</td><td class=\"standard\" align=\"center\">");

                            // Work out what the bitstream link should be
                            // (persistent
                            // ID if item has Handle)
                            String bsLink = "<A TARGET=_blank HREF=\""
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
                                    + UIUtil.encodeBitstreamName(
                                            bitstreams[k].getName(),
                                            Constants.DEFAULT_ENCODING) + "\">";

                            // is there a thumbnail bundle?
                            if ((thumbs.length > 0) && showThumbs)
                            {
                                String tName = bitstreams[k].getName() + ".jpg";
                                Bitstream tb = thumbs[0]
                                        .getBitstreamByName(tName);

                                if (tb != null)
                                {
                                    String myPath = request.getContextPath()
                                            + "/retrieve/"
                                            + tb.getID()
                                            + "/"
                                            + UIUtil.encodeBitstreamName(tb.getName(),
                                                    Constants.DEFAULT_ENCODING);

                                    out.print(bsLink);
                                    out.print("<img src=\"" + myPath + "\" ");
                                    out.print("alt=\"" + tName + "\"></A><BR>");
                                }
                            }

                            out.print(bsLink + "View/Open</A></td></tr>");
                        }
                    }
                }
            }

            out.println("</table>");
        }

        out.println("</td></tr></table>");
    }

    private void getThumbSettings()
    {
        showThumbs = ConfigurationManager
                .getBooleanProperty("webui.item.thumbnail.show");
    }
}




