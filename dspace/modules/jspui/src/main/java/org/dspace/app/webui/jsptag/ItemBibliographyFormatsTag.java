/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package org.dspace.app.webui.jsptag;

import java.io.IOException;
import java.util.Map;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.jsp.JspException;
import javax.servlet.jsp.JspWriter;
import javax.servlet.jsp.jstl.fmt.LocaleSupport;
import javax.servlet.jsp.tagext.TagSupport;
import org.apache.log4j.Logger;
import org.dspace.content.Collection;
import org.dspace.content.Item;

/**
 * <P>
 * JSP tag for displaying buttons to export an item bibliography in several 
 * formats.
 * </P>
 * <P>
 * This functionality is based in Andres Quast BibFormatTag for DSpace 1.7.2
 * </P>
 *
 * @author David Andrés Maznzano Herrera <damanzano>
 */
public class ItemBibliographyFormatsTag extends TagSupport{
    /** Item to display */
    private transient Item item;

    /** Items to display, it is used to generate multiple item bibliography */
    private transient Item[] items;

    /** log4j logger */
    private static Logger log = Logger.getLogger(ItemBibliographyFormatsTag.class);
    
    private static final long serialVersionUID = -5535762797556685631L;

    public ItemBibliographyFormatsTag() {
        super();
    }

    public int doStartTag() throws JspException
    {
        try
        {
            HttpServletRequest request = (HttpServletRequest) pageContext.getRequest();
            JspWriter out = pageContext.getOut();
            
            if(item!=null){
                out.println(itemButtonsMarkup(request, item));
            }else{
                // The tag is called in order to generate several records
            }
            
        }
        catch (IOException ie)
        {
            throw new JspException(ie);
        }

        return SKIP_BODY;
    }
    
    public String itemButtonsMarkup(HttpServletRequest request, Item item){
            //create a link for displaying bibliographic result
       
        String mendeleyLink= "<button class=\"col-md-12 col-sm-12 col-xs-12 btn btn-default\" onclick=\"MendeleyImporterApi.open()\">Mendeley<img src=\"" +request.getContextPath()+ "/image/registered.png"+"\" style=\"margin-top:-8px;\" height=\"10\" width=\"10\"/></button>";
        String endNoteLink = "<a class=\"col-md-12 col-sm-12 col-xs-12 btn btn-default\" href=\"" + request.getContextPath() + "/bibliography?bib=en&handle_item=" + item.getHandle() + "\" target=\"_blank\" alt=\"Download EndNote record\" id=\"en\">EndNote<img src=\"" +request.getContextPath()+ "/image/registered.png"+"\" style=\"margin-top:-8px;\" height=\"10\" width=\"10\"/></a>";
        String bibTextLink = "<a class=\"col-md-12 col-sm-12 col-xs-12 btn btn-default\" href=\"" + request.getContextPath() + "/bibliography?bib=tex&handle_item=" + item.getHandle() + "\" target=\"_blank\" alt=\"Download BibText record\" id=\"tex\">BibTex</a>";
        String RisLink = "<a class=\"col-md-12 col-sm-12 col-xs-12 btn btn-default\" href=\"" + request.getContextPath() + "/bibliography?bib=ris&handle_item=" + item.getHandle() + "\" target=\"_blank\" alt=\"Download Ris record\" id=\"ris\">RIS</a>";
        String CSVLink=  "<a class=\"col-md-12 col-sm-12 col-xs-12 btn btn-default\" href=\"" + request.getContextPath() + "/bibliography?bib=csv&handle_item=" + item.getHandle() + "\" target=\"_blank\" alt=\"Download CSV record\" id=\"csv\">CSV</a>";
        String TSVLink=  "<a class=\"col-md-12 col-sm-12 col-xs-12 btn btn-default\" href=\"" + request.getContextPath() + "/bibliography?bib=tsv&handle_item=" + item.getHandle() + "\" target=\"_blank\" alt=\"Download TSV record\" id=\"tsv\">TSV</a>";
       
        StringBuffer sbLink = new StringBuffer();
        sbLink.append(mendeleyLink);
        sbLink.append(endNoteLink);
        sbLink.append(bibTextLink);
        sbLink.append(RisLink);
        sbLink.append(CSVLink);
        sbLink.append(TSVLink);
        
        return sbLink.toString();          
    }

    /**
     * Set the item to list
     * 
     * @param item
     */
    public void setItem(Item item) {
        this.item = item;
    }
 
    /**
     * Set the items to list
     * 
     * @param items
     *            the items
     */
    public void setItems(Item[] items)
    {
        this.items = items;
    }
    
    public void release()
    {
        item = null;
        items = null;
    }

}
