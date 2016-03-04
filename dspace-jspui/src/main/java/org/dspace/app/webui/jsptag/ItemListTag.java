/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.webui.jsptag;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.StringTokenizer;

import javax.imageio.ImageIO;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.jsp.JspException;
import javax.servlet.jsp.JspWriter;
import javax.servlet.jsp.jstl.fmt.LocaleSupport;
import javax.servlet.jsp.tagext.TagSupport;
import org.apache.commons.lang.ArrayUtils;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.dspace.app.webui.util.UIUtil;
import org.dspace.authorize.AuthorizeException;
import org.dspace.browse.BrowseException;
import org.dspace.browse.BrowseIndex;
import org.dspace.browse.CrossLinks;
import org.dspace.content.Bitstream;
import org.dspace.content.DCDate;
import org.dspace.content.Item;
import org.dspace.content.MetadataValue;
import org.dspace.content.Thumbnail;
import org.dspace.content.authority.factory.ContentAuthorityServiceFactory;
import org.dspace.content.authority.service.MetadataAuthorityService;
import org.dspace.content.factory.ContentServiceFactory;
import org.dspace.content.service.BitstreamService;
import org.dspace.content.service.ItemService;
import org.dspace.core.Constants;
import org.dspace.core.Context;
import org.dspace.core.Utils;
import org.dspace.services.ConfigurationService;
import org.dspace.services.factory.DSpaceServicesFactory;
import org.dspace.sort.SortOption;

/**
 * Tag for display a list of items
 *
 * @author Robert Tansley
 * @version $Revision$
 */
public class ItemListTag extends TagSupport
{
    private static final Logger log = Logger.getLogger(ItemListTag.class);

    /** Items to display */
    private List<Item> items;

    /** Row to highlight, -1 for no row */
    private int highlightRow = -1;

    /** Column to emphasise - null, "title" or "date" */
    private String emphColumn;

    /** Config value of thumbnail view toggle */
    private static boolean showThumbs;

    /** Config browse/search width and height */
    private static int thumbItemListMaxWidth;

    private static int thumbItemListMaxHeight;

    /** Config browse/search thumbnail link behaviour */
    private static boolean linkToBitstream = false;

    /** Config to include an edit link */
    private boolean linkToEdit = false;

    /** Config to disable cross links */
    private boolean disableCrossLinks = false;

    /** The default fields to be displayed when listing items */
    private static final String[] DEFAULT_LIST_FIELDS;

    /** The default widths for the columns */
    private static final String[] DEFAULT_LIST_WIDTHS;

    /** The default field which is bound to the browse by date */
    private static String dateField = "dc.date.issued";

    /** The default field which is bound to the browse by title */
    private static String titleField = "dc.title";

    private static String authorField = "dc.contributor.*";

    private int authorLimit = -1;

    private transient SortOption sortOption = null;
    
    private final transient ItemService itemService
            = ContentServiceFactory.getInstance().getItemService();

    private static final long serialVersionUID = 348762897199116432L;
    
    private final transient MetadataAuthorityService metadataAuthorityService
            = ContentAuthorityServiceFactory.getInstance().getMetadataAuthorityService();

    private final transient BitstreamService bitstreamService
            = ContentServiceFactory.getInstance().getBitstreamService();
    
    private final transient ConfigurationService configurationService
             = DSpaceServicesFactory.getInstance().getConfigurationService();

    static
    {
        getThumbSettings();

        if (showThumbs)
        {
            DEFAULT_LIST_FIELDS = new String[] {"thumbnail", "dc.date.issued(date)", "dc.title", "dc.contributor.*"};
            DEFAULT_LIST_WIDTHS = new String[] {"*", "130", "60%", "40%"};
        }
        else
        {
            DEFAULT_LIST_FIELDS = new String[] {"dc.date.issued(date)", "dc.title", "dc.contributor.*"};
            DEFAULT_LIST_WIDTHS = new String[] {"130", "60%", "40%"};
        }

        // get the date and title fields
        String dateLine = DSpaceServicesFactory.getInstance().getConfigurationService().getProperty("webui.browse.index.date");
        if (dateLine != null)
        {
            dateField = dateLine;
        }

        String titleLine = DSpaceServicesFactory.getInstance().getConfigurationService().getProperty("webui.browse.index.title");
        if (titleLine != null)
        {
            titleField = titleLine;
        }

        String authorLine = DSpaceServicesFactory.getInstance().getConfigurationService().getProperty("webui.browse.author-field");
        if (authorLine != null)
        {
            authorField = authorLine;
        }
    }

    public ItemListTag()
    {
        super();
    }

    @Override
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
        String[] browseFields  = null;
        String[] browseWidths = null;

        if (sortOption != null)
        {
            if (ArrayUtils.isEmpty(browseFields))
            {
                browseFields = configurationService.getArrayProperty("webui.itemlist.sort." + sortOption.getName() + ".columns");
                browseWidths  = configurationService.getArrayProperty("webui.itemlist.sort." + sortOption.getName() + ".widths");
            }

            if (ArrayUtils.isEmpty(browseFields))
            {
                browseFields = configurationService.getArrayProperty("webui.itemlist." + sortOption.getName() + ".columns");
                browseWidths  = configurationService.getArrayProperty("webui.itemlist." + sortOption.getName() + ".widths");
            }
        }

        if (ArrayUtils.isEmpty(browseFields))
        {
            browseFields = configurationService.getArrayProperty("webui.itemlist.columns");
            browseWidths  = configurationService.getArrayProperty("webui.itemlist.widths");
        }

        // Have we read a field configration from dspace.cfg?
        if (ArrayUtils.isNotEmpty(browseFields))
        {
            // If thumbnails are disabled, strip out any thumbnail column from the configuration
            if (!showThumbs)
            {
                // check if it contains a thumbnail entry
                // If so, remove it, and the width associated with it
                int thumbnailIndex = ArrayUtils.indexOf(browseFields, "thumbnail");
                if(thumbnailIndex>=0)
                {
                    browseFields = (String[]) ArrayUtils.remove(browseFields, thumbnailIndex);
                    if(ArrayUtils.isNotEmpty(browseWidths))
                    {
                       browseWidths = (String[]) ArrayUtils.remove(browseWidths, thumbnailIndex);
                    }
                }
            }
        }
        else
        {
            browseFields = DEFAULT_LIST_FIELDS;
            browseWidths = DEFAULT_LIST_WIDTHS;
        }

        // Arrays used to hold the information we will require when outputting each row
        boolean isDate[]   = new boolean[browseFields.length];
        boolean emph[]     = new boolean[browseFields.length];
        boolean isAuthor[] = new boolean[browseFields.length];
        boolean viewFull[] = new boolean[browseFields.length];
        String[] browseType = new String[browseFields.length];
        String[] cOddOrEven = new String[browseFields.length];

        try
        {
            // Get the interlinking configuration too
            CrossLinks cl = new CrossLinks();

            // Get a width for the table
            String tablewidth = configurationService.getProperty("webui.itemlist.tablewidth");

            // If we have column widths, output a fixed layout table - faster for browsers to render
            // but not if we have to add an 'edit item' button - we can't know how big it will be
            if (ArrayUtils.isNotEmpty(browseWidths) && browseWidths.length == browseFields.length && !linkToEdit)
            {
                // If the table width has been specified, we can make this a fixed layout
                if (!StringUtils.isEmpty(tablewidth))
                {
                    out.println("<table style=\"width: " + tablewidth + "; table-layout: fixed;\" align=\"center\" class=\"table\" summary=\"This table browses all dspace content\">");
                }
                else
                {
                    // Otherwise, don't constrain the width
                    out.println("<table align=\"center\" class=\"table\" summary=\"This table browses all dspace content\">");
                }

                // Output the known column widths
                out.print("<colgroup>");

                for (int w = 0; w < browseWidths.length; w++)
                {
                    out.print("<col width=\"");

                    // For a thumbnail column of width '*', use the configured max width for thumbnails
                    if (browseFields[w].equals("thumbnail") && browseWidths[w].equals("*"))
                    {
                        out.print(thumbItemListMaxWidth);
                    }
                    else
                    {
                        out.print(StringUtils.isEmpty(browseWidths[w]) ? "*" : browseWidths[w]);
                    }

                    out.print("\" />");
                }

                out.println("</colgroup>");
            }
            else if (!StringUtils.isEmpty(tablewidth))
            {
                out.println("<table width=\"" + tablewidth + "\" align=\"center\" class=\"table\" summary=\"This table browses all dspace content\">");
            }
            else
            {
                out.println("<table align=\"center\" class=\"table\" summary=\"This table browses all dspace content\">");
            }

            // Output the table headers
            out.println("<tr>");

            for (int colIdx = 0; colIdx < browseFields.length; colIdx++)
            {
                String field = browseFields[colIdx].toLowerCase().trim();
                cOddOrEven[colIdx] = (((colIdx + 1) % 2) == 0 ? "Odd" : "Even");

                // find out if the field is a date
                if (field.indexOf("(date)") > 0)
                {
                    field = field.replaceAll("\\(date\\)", "");
                    isDate[colIdx] = true;
                }

                // Cache any modifications to field
                browseFields[colIdx] = field;

                // find out if this is the author column
                if (field.equals(authorField))
                {
                    isAuthor[colIdx] = true;
                }

                // find out if this field needs to link out to other browse views
                if (cl.hasLink(field))
                {
                    browseType[colIdx] = cl.getLinkType(field);
                    viewFull[colIdx] = BrowseIndex.getBrowseIndex(browseType[colIdx]).isItemIndex();
                }

                // find out if we are emphasising this field
                if (field.equals(emphColumn))
                {
                    emph[colIdx] = true;
                }
                else if ((field.equals(dateField) && emphasiseDate) ||
                        (field.equals(titleField) && emphasiseTitle))
                {
                    emph[colIdx] = true;
                }

                // prepare the strings for the header
                String id = "t" + Integer.toString(colIdx + 1);
                String css = "oddRow" + cOddOrEven[colIdx] + "Col";
                String message = "itemlist." + field;

                String markClass = "";
                if (field.startsWith("mark_"))
                {
                	markClass = " "+field+"_th";
                }

                // output the header
                out.print("<th id=\"" + id +  "\" class=\"" + css + markClass +"\">"
                        + (emph[colIdx] ? "<strong>" : "")
                        + LocaleSupport.getLocalizedMessage(pageContext, message)
                        + (emph[colIdx] ? "</strong>" : "") + "</th>");
            }

            if (linkToEdit)
            {
                String id = "t" + Integer.toString(cOddOrEven.length + 1);
                String css = "oddRow" + cOddOrEven[cOddOrEven.length - 2] + "Col";

                // output the header
                out.print("<th id=\"" + id +  "\" class=\"" + css + "\">"
                        + (emph[emph.length - 2] ? "<strong>" : "")
                        + "&nbsp;" //LocaleSupport.getLocalizedMessage(pageContext, message)
                        + (emph[emph.length - 2] ? "</strong>" : "") + "</th>");
            }

            out.print("</tr>");

            // now output each item row
            for (Item item : items)
            {
                // now prepare the XHTML frag for this division
            	out.print("<tr>"); 

                for (int colIdx = 0; colIdx < browseFields.length; colIdx++)
                {
                    String field = browseFields[colIdx];

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

                    // first get hold of the relevant metadata for this column
                    List<MetadataValue> metadataArray;
                    if (qualifier.equals("*"))
                    {
                        metadataArray = itemService.getMetadata(item, schema, element, Item.ANY, Item.ANY);
                    }
                    else if (qualifier.equals(""))
                    {
                        metadataArray = itemService.getMetadata(item, schema, element, null, Item.ANY);
                    }
                    else
                    {
                        metadataArray = itemService.getMetadata(item, schema, element, qualifier, Item.ANY);
                    }

                    // save on a null check which would make the code untidy
                    if (metadataArray == null)
                    {
                        metadataArray = new ArrayList<>();
                    }

                    // now prepare the content of the table division
                    String metadata = "-";
                    if (field.equals("thumbnail"))
                    {
                        metadata = getThumbMarkup(hrq, item);
                    }
                    else  if (field.startsWith("mark_"))
                    {
                        metadata = UIUtil.getMarkingMarkup(hrq, item, field);
                    }
                    if (metadataArray.size() > 0)
                    {
                        // format the date field correctly
                        if (isDate[colIdx])
                        {
                            DCDate dd = new DCDate(metadataArray.get(0).getValue());
                            metadata = UIUtil.displayDate(dd, false, false, hrq);
                        }
                        // format the title field correctly for withdrawn items (ie. don't link)
                        else if (field.equals(titleField) && item.isWithdrawn())
                        {
                            metadata = Utils.addEntities(metadataArray.get(0).getValue());
                        }
                        // format the title field correctly
                        else if (field.equals(titleField))
                        {
                            metadata = "<a href=\"" + hrq.getContextPath() + "/handle/"
                            + item.getHandle() + "\">"
                            + Utils.addEntities(metadataArray.get(0).getValue())
                            + "</a>";
                        }
                        // format all other fields
                        else
                        {
                            // limit the number of records if this is the author field (if
                            // -1, then the limit is the full list)
                            boolean truncated = false;
                            int loopLimit = metadataArray.size();
                            if (isAuthor[colIdx])
                            {
                                int fieldMax = (authorLimit > 0 ? authorLimit : metadataArray.size());
                                loopLimit = (fieldMax > metadataArray.size() ? metadataArray.size() : fieldMax);
                                truncated = (fieldMax < metadataArray.size());
                                log.debug("Limiting output of field " + field + " to " + Integer.toString(loopLimit) + " from an original " + Integer.toString(metadataArray.size()));
                            }

                            StringBuffer sb = new StringBuffer();
                            for (int j = 0; j < loopLimit; j++)
                            {
                                String startLink = "";
                                String endLink = "";
                                if (!StringUtils.isEmpty(browseType[colIdx]) && !disableCrossLinks)
                                {
                                    String argument;
                                    String value;
                                    if (metadataArray.get(j).getAuthority() != null &&
                                            metadataArray.get(j).getConfidence() >= metadataAuthorityService
                                                .getMinConfidence(metadataArray.get(j).getMetadataField()))
                                    {
                                        argument = "authority";
                                        value = metadataArray.get(j).getAuthority();
                                    }
                                    else
                                    {
                                        argument = "value";
                                        value = metadataArray.get(j).getValue();
                                    }
                                    if (viewFull[colIdx])
                                    {
                                        argument = "vfocus";
                                    }
                                    startLink = "<a href=\"" + hrq.getContextPath() + "/browse?type=" + browseType[colIdx] + "&amp;" +
                                        argument + "=" + URLEncoder.encode(value,"UTF-8");

                                    if (metadataArray.get(j).getLanguage() != null)
                                    {
                                        startLink = startLink + "&amp;" +
                                            argument + "_lang=" + URLEncoder.encode(metadataArray.get(j).getLanguage(), "UTF-8");
                                    }

                                    if ("authority".equals(argument))
                                    {
                                        startLink += "\" class=\"authority " +browseType[colIdx] + "\">";
                                    }
                                    else
                                    {
                                        startLink = startLink + "\">";
                                    }
                                    endLink = "</a>";
                                }
                                sb.append(startLink);
                                sb.append(Utils.addEntities(metadataArray.get(j).getValue()));
                                sb.append(endLink);
                                if (j < (loopLimit - 1))
                                {
                                    sb.append("; ");
                                }
                            }
                            if (truncated)
                            {
                                String etal = LocaleSupport.getLocalizedMessage(pageContext, "itemlist.et-al");
                                sb.append(", ").append(etal);
                            }
                            metadata = "<em>" + sb.toString() + "</em>";
                        }
                    }
                    //In case title has no value, replace it with "undefined" so as the user has something to
                	//click in order to access the item page
                    else if (field.equals(titleField)){
                    	String undefined = LocaleSupport.getLocalizedMessage(pageContext, "itemlist.title.undefined");
                    	if (item.isWithdrawn())
                        {
                            metadata = "<span style=\"font-style:italic\">("+undefined+")</span>";
                        }
                        // format the title field correctly (as long as the item isn't withdrawn, link to it)
                        else
                        {
                            metadata = "<a href=\"" + hrq.getContextPath() + "/handle/"
                            + item.getHandle() + "\">"
                            + "<span style=\"font-style:italic\">("+undefined+")</span>"
                            + "</a>";
                        }
                    }

                    // prepare extra special layout requirements for dates
                    String extras = "";
                    if (isDate[colIdx])
                    {
                        extras = "nowrap=\"nowrap\" align=\"right\"";
                    }

                    String markClass = "";
                    if (field.startsWith("mark_"))
                    {
                    	markClass = " "+field+"_tr";
                    }

                    
                    String id = "t" + Integer.toString(colIdx + 1);
                    out.print("<td headers=\"" + id + "\" "
                    	+  extras + ">"
                        + (emph[colIdx] ? "<strong>" : "") + metadata + (emph[colIdx] ? "</strong>" : "")
                        + "</td>");
                }

                // Add column for 'edit item' links
                if (linkToEdit)
                {
                    String id = "t" + Integer.toString(cOddOrEven.length + 1);

                        out.print("<td headers=\"" + id + "\" "
                            + "nowrap=\"nowrap\">"
                            + "<form method=\"get\" action=\"" + hrq.getContextPath() + "/tools/edit-item\">"
                            + "<input type=\"hidden\" name=\"handle\" value=\"" + item.getHandle() + "\" />"
                            + "<input type=\"submit\" value=\"Edit Item\" /></form>"
                            + "</td>");
                    }

                out.println("</tr>");
            }

            // close the table
            out.println("</table>");
        }
        catch (IOException ie)
        {
            throw new JspException(ie);
        }
        catch (BrowseException e)
        {
            throw new JspException(e);
        }

        return SKIP_BODY;
    }

    public int getAuthorLimit()
    {
        return authorLimit;
    }

    public void setAuthorLimit(int al)
    {
        authorLimit = al;
    }

    public boolean getLinkToEdit()
    {
        return linkToEdit;
    }

    public void setLinkToEdit(boolean edit)
    {
        this.linkToEdit = edit;
    }

    public boolean getDisableCrossLinks()
    {
        return disableCrossLinks;
    }

    public void setDisableCrossLinks(boolean links)
    {
        this.disableCrossLinks = links;
    }

    public SortOption getSortOption()
    {
        return sortOption;
    }

    public void setSortOption(SortOption so)
    {
        sortOption = so;
    }

    /**
     * Get the items to list
     *
     * @return the items
     */
    public List<Item> getItems()
    {
        return items;
    }

    /**
     * Set the items to list
     *
     * @param itemsIn
     *            the items
     */
    public void setItems(List<Item> itemsIn)
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

    @Override
    public void release()
    {
        highlightRow = -1;
        emphColumn = null;
        items = null;
    }

    /* get the required thumbnail config items */
    private static void getThumbSettings()
    {
        ConfigurationService configurationService =
                DSpaceServicesFactory.getInstance().getConfigurationService();
        
        showThumbs = configurationService
                .getBooleanProperty("webui.browse.thumbnail.show");

        if (showThumbs)
        {
            thumbItemListMaxHeight = configurationService
                    .getIntProperty("webui.browse.thumbnail.maxheight");

            if (thumbItemListMaxHeight == 0)
            {
                thumbItemListMaxHeight = configurationService
                        .getIntProperty("thumbnail.maxheight");
            }

            thumbItemListMaxWidth = configurationService
                    .getIntProperty("webui.browse.thumbnail.maxwidth");

            if (thumbItemListMaxWidth == 0)
            {
                thumbItemListMaxWidth = configurationService
                        .getIntProperty("thumbnail.maxwidth");
            }
        }

        String linkBehaviour = configurationService
                .getProperty("webui.browse.thumbnail.linkbehaviour");

        if ("bitstream".equals(linkBehaviour))
        {
            linkToBitstream = true;
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

            InputStream is = bitstreamService.retrieve(c, bitstream);

            //AuthorizeManager.authorizeAction(bContext, this, Constants.READ);
            // 	read in bitstream's image
            buf = ImageIO.read(is);
            is.close();
        }
        catch (IOException | AuthorizeException | SQLException ex)
        {
            throw new JspException(ex.getMessage(), ex);
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
        try
        {
            Context c = UIUtil.obtainContext(hrq);
            Thumbnail thumbnail = itemService.getThumbnail(c, item, linkToBitstream);

            if (thumbnail == null)
            {
                return "";
            }
            StringBuffer thumbFrag = new StringBuffer();

            if (linkToBitstream)
            {
                Bitstream original = thumbnail.getOriginal();
                String link = hrq.getContextPath() + "/bitstream/" + item.getHandle() + "/" + original.getSequenceID() + "/" +
                                UIUtil.encodeBitstreamName(original.getName(), Constants.DEFAULT_ENCODING);
                thumbFrag.append("<a target=\"_blank\" href=\"" + link + "\" />");
            }
            else
            {
                String link = hrq.getContextPath() + "/handle/" + item.getHandle();
                thumbFrag.append("<a href=\"" + link + "\" />");
            }

            Bitstream thumb = thumbnail.getThumb();
            String img = hrq.getContextPath() + "/retrieve/" + thumb.getID() + "/" +
                        UIUtil.encodeBitstreamName(thumb.getName(), Constants.DEFAULT_ENCODING);
            String alt = thumb.getName();
            String scAttr = getScalingAttr(hrq, thumb);
            thumbFrag.append("<img src=\"")
                    .append(img)
                    .append("\" alt=\"").append(alt).append("\" ")
                     .append(scAttr)
                     .append("/ border=\"0\"></a>");

            return thumbFrag.toString();
        }
        catch (SQLException sqle)
        {
            throw new JspException(sqle.getMessage(), sqle);
        }
        catch (UnsupportedEncodingException e)
        {
            throw new JspException("Server does not support DSpace's default encoding. ", e);
        }
	}
}
