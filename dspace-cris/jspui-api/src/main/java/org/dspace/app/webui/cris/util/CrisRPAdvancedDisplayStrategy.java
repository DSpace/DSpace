/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * https://github.com/CILEA/dspace-cris/wiki/License
 */
package org.dspace.app.webui.cris.util;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.sql.SQLException;
import java.util.Locale;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.jsp.JspException;

import org.apache.commons.lang.StringUtils;
import org.dspace.app.cris.integration.CRISAuthority;
import org.dspace.app.webui.util.UIUtil;
import org.dspace.authority.AuthorityValueGenerator;
import org.dspace.content.Item;
import org.dspace.content.MetadataValue;
import org.dspace.content.Metadatum;
import org.dspace.content.authority.ChoiceAuthority;
import org.dspace.content.authority.ChoiceAuthorityManager;
import org.dspace.content.authority.MetadataAuthorityManager;
import org.dspace.core.ConfigurationManager;
import org.dspace.core.I18nUtil;
import org.dspace.core.Utils;

/**
 * @author Luigi Andrea Pascarelli 
 *
 */
public class CrisRPAdvancedDisplayStrategy extends ItemCrisRefDisplayStrategy {

	@Override
	public String getMetadataDisplay(HttpServletRequest hrq, int limit,
			boolean viewFull, String browseType, int colIdx, int itemId,
			String field, Metadatum[] metadataArray, boolean disableCrossLinks,
			boolean emph) throws JspException {
		
	  	String RPOtherMetadata= ConfigurationManager.getProperty("webui.item.displaystrategy.crisrp.tooltipmetadata");
	  	Metadatum[] otherMetadata = null;
		try {
			otherMetadata = Item.find(UIUtil.obtainContext(hrq), itemId).getMetadataValueInDCFormat(RPOtherMetadata);
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	  	
    	String publicPath = null;
    	int minConfidence = -1;
		if (metadataArray.length > 0) {
			ChoiceAuthorityManager cam = ChoiceAuthorityManager.getManager();
			ChoiceAuthority ca = cam.getChoiceAuthority(metadataArray[0].schema, metadataArray[0].element, metadataArray[0].qualifier);
			minConfidence = MetadataAuthorityManager.getManager().getMinConfidence(metadataArray[0].schema, metadataArray[0].element, metadataArray[0].qualifier);
			if (ca != null && ca instanceof CRISAuthority) {
				CRISAuthority crisAuthority = (CRISAuthority) ca;
				publicPath = crisAuthority.getPublicPath();
				if (publicPath == null) {
					publicPath = ConfigurationManager.getProperty("ItemCrisRefDisplayStrategy.publicpath."+field);
					if (publicPath == null) {
						publicPath = metadataArray[0].qualifier;
					}
				}
			}
		}
		
		if (publicPath == null) {
			return "";
		}
		
        String metadata;
        // limit the number of records if this is the author field (if
        // -1, then the limit is the full list)
        boolean truncated = false;
        int loopLimit = metadataArray.length;
        if (limit != -1)
        {
            loopLimit = (limit > metadataArray.length ? metadataArray.length
                    : limit);
            truncated = (limit < metadataArray.length);
        }

        StringBuffer sb = new StringBuffer();
        for (int j = 0; j < loopLimit; j++)
        {
            buildBrowseLink(hrq, viewFull, browseType, metadataArray, minConfidence,
                    otherMetadata,disableCrossLinks, sb, j);
            if (StringUtils.isNotBlank(metadataArray[j].authority) && metadataArray[j].confidence >= minConfidence) {
            	buildAuthority(hrq, metadataArray[j].value, metadataArray[j].authority, publicPath, sb);
            }
            if (j < (loopLimit - 1))
            {
                if (colIdx != -1) // we are showing metadata in a table row
                                  // (browse or item list)
                {
                    sb.append("; ");
                }
                else
                {
                    // we are in the item tag
                    sb.append("<br />");
                }
            }
        }
        if (truncated)
        {
            Locale locale = UIUtil.getSessionLocale(hrq);
        	String etal = I18nUtil.getMessage("itemlist.et-al", locale);
            sb.append(", " + etal);
        }

        if (colIdx != -1) // we are showing metadata in a table row (browse or
                          // item list)
        {
            metadata = (emph ? "<strong><em>" : "<em>") + sb.toString()
                    + (emph ? "</em></strong>" : "</em>");
        }
        else
        {
            // we are in the item tag
            metadata = (emph ? "<strong>" : "") + sb.toString()
                    + (emph ? "</strong>" : "");
        }

        return metadata;
    }

	
    protected void buildBrowseLink(HttpServletRequest hrq, boolean viewFull,
            String browseType, Metadatum[] metadataArray, int minConfidence,
            Metadatum[] otherMetadata,boolean disableCrossLinks, StringBuffer sb, int j)
    {
  
    	
        String startLink = "";
        String endLink = "";
        if (!StringUtils.isEmpty(browseType) && !disableCrossLinks)
        {
            String argument;
            String value;
            String authority = metadataArray[j].authority;
			if (authority != null &&
                    metadataArray[j].confidence >= minConfidence && !(authority.startsWith(AuthorityValueGenerator.GENERATE)))
            {
                argument = "authority";
                value = authority;
            }
            else
            {
                argument = "value";
                value = metadataArray[j].value;
            }
            if (viewFull)
            {
                argument = "vfocus";
            }
            try
            {
                startLink = "<a href=\"" + hrq.getContextPath()
                        + "/browse?type=" + browseType + "&amp;" + argument
                        + "=" + URLEncoder.encode(value, "UTF-8");
            }
            catch (UnsupportedEncodingException e)
            {
                throw new RuntimeException(e.getMessage(), e);
            }

            if (metadataArray[j].language != null)
            {
                try
                {
                    startLink = startLink + "&amp;" + argument + "_lang="
                            + URLEncoder.encode(metadataArray[j].language, "UTF-8")+"\"";
                }
                catch (UnsupportedEncodingException e)
                {
                    throw new RuntimeException(e.getMessage(), e);
                }
            }else
            {
            	startLink+="\"";
            }
            
            if(otherMetadata != null && otherMetadata.length>j){
            	String val = StringUtils.equals(otherMetadata[j].value,MetadataValue.PARENT_PLACEHOLDER_VALUE) ? "N/D": otherMetadata[j].value; 
            	startLink+=" data-toggle=\"tooltip\" data-placement=\"right\" title=\""+ val +"\" ";
            }
            

            if ("authority".equals(argument))
            {
                startLink += " class=\"authority " + browseType + "\">";
            }
            else
            {
                startLink = startLink + ">";
            }
            endLink = "</a>";
        }
        sb.append(startLink);
        sb.append(Utils.addEntities(metadataArray[j].value));
        if(otherMetadata != null && otherMetadata.length>1){
        	sb.append("<strong>*</strong>");
        }
        sb.append(endLink);
    }

		


}
