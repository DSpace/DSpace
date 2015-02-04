/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.webui.util;

import java.lang.StringBuilder;
import java.io.IOException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.jsp.JspException;
import javax.servlet.jsp.PageContext;
import javax.servlet.jsp.jstl.fmt.LocaleSupport;

import org.apache.log4j.Logger;
import org.dspace.browse.BrowseItem;
import org.dspace.content.DCValue;
import org.dspace.content.Item;
import org.dspace.content.BitstreamFormat;
import org.dspace.content.Bitstream;
import org.dspace.content.BitstreamList;
import org.dspace.core.SelfNamedPlugin;
import org.dspace.core.ConfigurationManager;
import org.dspace.core.Constants;

public class BitstreamDisplayStrategy extends SelfNamedPlugin implements
        IDisplayMetadataValueStrategy
{
    /** log4j category */
    private static final Logger log = Logger.getLogger(BitstreamDisplayStrategy.class);

    private static final boolean showFullBitstream;
    static
    {
        showFullBitstream = ConfigurationManager
                .getBooleanProperty("webui.browse.bitstream.showfull");
    }

    public String getMetadataDisplay(HttpServletRequest hrq,
            int limit, boolean viewFull, String browseType, int colIdx,
            String field, DCValue[] metadataArray, boolean disableCrossLinks,
            boolean emph, PageContext pageContext) throws JspException
    {
        // StringBuilder debugMsg = new StringBuilder();
        BitstreamList bss = (BitstreamList) hrq.getAttribute("itemlist.bitstream");
        // debugMsg.append("Bitstream list using simple version ...\nitemlist.bitstream.length = ");
        // debugMsg.append(bss.length);
        Bitstream bs = bss.getNextBS();

        if(bs != null){
            BitstreamFormat bsFormat = bs.getFormat();
            if((!bsFormat.isInternal()) || bsFormat.getMIMEType().equals("text/html"))
                return LocaleSupport.getLocalizedMessage(pageContext,"itemlist.bitstream.yes");
            log.debug("\nbitstream " + bs.getName() + " not null but format " + bsFormat.getMIMEType() + " is internal or not equal to text/html\n");
            // debugMsg.append("\nbitstream " + bs.getName() + " not null but format " + bsFormat.getMIMEType() + " is internal or not equal to text/html\n");
        }
        // debugMsg.append("\nbitstream[");
        // debugMsg.append(bss.getCursor());
        // debugMsg.append("] is null");
        // log.debug(debugMsg.toString());

        return LocaleSupport.getLocalizedMessage(pageContext,"itemlist.bitstream.no");
    }

    public String getMetadataDisplay(HttpServletRequest hrq, int limit,
            boolean viewFull, String browseType, int colIdx, String field,
            DCValue[] metadataArray, BrowseItem item,
            boolean disableCrossLinks, boolean emph, PageContext pageContext)
            throws JspException
    {
        return getMetadataDisplay(hrq, limit, viewFull, browseType, colIdx,
                field, metadataArray, disableCrossLinks, emph, pageContext);
    }

    public String getMetadataDisplay(HttpServletRequest hrq, int limit,
            boolean viewFull, String browseType, int colIdx, String field,
            DCValue[] metadataArray, Item item, boolean disableCrossLinks,
            boolean emph, PageContext pageContext) throws JspException
    {
        if(!showFullBitstream)
            return getMetadataDisplay(hrq, limit, viewFull, browseType, colIdx,
                field, metadataArray, disableCrossLinks, emph, pageContext);
        // StringBuilder debugMsg = new StringBuilder();
        BitstreamList bss = (BitstreamList) hrq.getAttribute("itemlist.bitstream");
        // debugMsg.append("Bitstream list using full version ...\nitemlist.bitstream.length = ");
        // debugMsg.append(bss.length);
        Bitstream bs = bss.getNextBS();

        if(bs == null)
            return LocaleSupport.getLocalizedMessage(pageContext,"itemlist.bitstream.no");

        String bsLink;
        String itemHandle = item.getHandle();
        BitstreamFormat bsFormat = bs.getFormat();
        Object[] bsData;

        try{
            if(bsFormat.getMIMEType().equals("text/html"))
                bsLink =
                    hrq.getContextPath() + "/html/" +
                    (itemHandle == null ? "db-id/" + item.getID() : itemHandle) +
                    "/" + UIUtil.encodeBitstreamName(bs.getName(), Constants.DEFAULT_ENCODING);
            else if(!bsFormat.isInternal()){
                if ((itemHandle != null) && (bs.getSequenceID() > 0))
                    bsLink =
                        hrq.getContextPath() + "/bitstream/" +
                        item.getHandle() + "/" + bs.getSequenceID() + "/";
                else
                    bsLink = hrq.getContextPath() + "/retrieve/" + bs.getID() + "/";

                bsLink = bsLink + UIUtil.encodeBitstreamName(bs.getName(), Constants.DEFAULT_ENCODING);
            }
            else{
                log.debug("\nbitstream " + bs.getName() + " not null but format " + bsFormat.getMIMEType() + " is internal or not equal to text/html\n");
                return LocaleSupport.getLocalizedMessage(pageContext,"itemlist.bitstream.no");
            }
        }catch(IOException ie){
            throw new JspException(ie);
        }

        bsData = new Object[2];
        bsData[0] = bsLink;
        bsData[1] = bs.getName();
        // debugMsg.append("\nbitstream " + bs.getName() + " not null and format " + bsFormat.getMIMEType() + "bitstream returning full info{link: '" + bsLink + "', name: '" + bs.getName() + "'");
        // log.debug(debugMsg.toString());
        return LocaleSupport.getLocalizedMessage(pageContext,"itemlist.bitstream.yes.full",bsData);
    }

    public String getExtraCssDisplay(HttpServletRequest hrq, int limit,
            boolean b, String browseType, int colIdx, String field,
            DCValue[] metadataArray, boolean disableCrossLinks, boolean emph,
            PageContext pageContext) throws JspException
    {
        return null;
    }

    public String getExtraCssDisplay(HttpServletRequest hrq, int limit,
            boolean b, String browseType, int colIdx, String field,
            DCValue[] metadataArray, BrowseItem browseItem,
            boolean disableCrossLinks, boolean emph, PageContext pageContext)
            throws JspException
    {
        return getExtraCssDisplay(hrq, limit, b, browseType, colIdx, field,
                metadataArray, disableCrossLinks, emph, pageContext);
    }

    public String getExtraCssDisplay(HttpServletRequest hrq, int limit,
            boolean b, String browseType, int colIdx, String field,
            DCValue[] metadataArray, Item item, boolean disableCrossLinks,
            boolean emph, PageContext pageContext) throws JspException
    {
        return getExtraCssDisplay(hrq, limit, b, browseType, colIdx, field,
                metadataArray, disableCrossLinks, emph, pageContext);
    }

}
