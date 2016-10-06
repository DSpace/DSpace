/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * https://github.com/CILEA/dspace-cris/wiki/License
 */
package org.dspace.app.webui.cris.util;

import java.text.MessageFormat;
import java.util.Locale;
import java.util.MissingResourceException;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.jsp.JspException;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.dspace.app.cris.model.ACrisObject;
import org.dspace.app.cris.service.ApplicationService;
import org.dspace.app.webui.util.IDisplayMetadataValueStrategy;
import org.dspace.app.webui.util.UIUtil;
import org.dspace.browse.BrowseDSpaceObject;
import org.dspace.browse.BrowseItem;
import org.dspace.content.Item;
import org.dspace.content.Metadatum;
import org.dspace.core.ConfigurationManager;
import org.dspace.core.I18nUtil;
import org.dspace.core.Utils;
import org.dspace.discovery.IGlobalSearchResult;
import org.dspace.utils.DSpace;

public class CrisI18nLabel implements IDisplayMetadataValueStrategy
{

	/**
	 * log4j category
	 */
    public static final Log log = LogFactory
            .getLog(CrisI18nLabel.class);

    private ApplicationService applicationService = new DSpace()
            .getServiceManager()
			.getServiceByName("applicationService", ApplicationService.class);
    @Override
    public String getMetadataDisplay(HttpServletRequest hrq, int limit,
            boolean viewFull, String browseType, int colIdx, String field,
            Metadatum[] metadataArray, BrowseItem item,
            boolean disableCrossLinks, boolean emph)
    {
        ACrisObject crisObject = (ACrisObject) ((BrowseDSpaceObject) item)
                .getBrowsableDSpaceObject();
		String metadata = internalDisplay(hrq, metadataArray, crisObject);
		return metadata;
	}
    @Override
    public String getMetadataDisplay(HttpServletRequest hrq, int limit,
            boolean viewFull, String browseType, int colIdx, String field,
            Metadatum[] metadataArray, Item item, boolean disableCrossLinks,
            boolean emph)
    {
        if (metadataArray != null && metadataArray.length > 0)
        {
            String authority = metadataArray[0].authority;
            if (StringUtils.isNotBlank(authority))
            {
                ACrisObject entityByCrisId = applicationService
                        .getEntityByCrisId(authority);
                return internalDisplay(hrq, metadataArray, entityByCrisId);
            } else {
                return metadataArray[0].value;
            }
	    }
		return "N/D";
	}
    @Override
    public String getExtraCssDisplay(HttpServletRequest hrq, int limit,
            boolean b, String browseType, int colIdx, String field,
            Metadatum[] metadataArray, Item item, boolean disableCrossLinks,
            boolean emph) throws JspException
    {
		return null;
	}

	@Override
    public String getExtraCssDisplay(HttpServletRequest hrq, int limit,
            boolean b, String browseType, int colIdx, String field,
            Metadatum[] metadataArray, BrowseItem browseItem,
            boolean disableCrossLinks, boolean emph)
                    throws JspException
    {
		return null;
	}

	@Override
    public String getMetadataDisplay(HttpServletRequest hrq, int limit,
            boolean viewFull, String browseType, int colIdx, String field,
            Metadatum[] metadataArray, IGlobalSearchResult item,
            boolean disableCrossLinks, boolean emph)
                    throws JspException
    {

		ACrisObject crisObject = (ACrisObject) item;
		String metadata = internalDisplay(hrq, metadataArray, crisObject);
		return metadata;
	}
	
    private String internalDisplay(HttpServletRequest hrq,
            Metadatum[] metadataArray, ACrisObject crisObject)
    {
        
        Locale locale = UIUtil.getSessionLocale(hrq); 

        String metadata = "";
        if (metadataArray != null && metadataArray.length > 0)
        {
            String publicPath = crisObject.getAuthorityPrefix();
            for (Metadatum metadatum : metadataArray)
            {
                try
                {
                    String authority = "";
                    if(metadataArray.length==1) {
                        authority = crisObject.getCrisID();
                    }
                    else {
                        authority = metadatum.authority;
                    }

                    String target = ConfigurationManager
                            .getBooleanProperty("cris",
                                    publicPath
                                            + ".ref.display.strategy.target.blank",
                                    false) ? "target=\"_blank\" " : "";
                    String startLink = "<a " + target + "href=\""
                            + hrq.getContextPath() + "/cris/" + publicPath + "/"
                            + authority;
                    startLink += "\" class=\"authority\">";
                    String endLink = "</a>";

                    // perhaps this is to avoid a lazyloader exception?
                    ACrisObject cris = applicationService.getEntityByCrisId(
                            authority, crisObject.getClass());

                    String icon = "";
                    try
                    {
                        String type = cris.getMetadata(ConfigurationManager
                                .getProperty("cris", publicPath
                                        + ".ref.display.strategy.metadata.icon"));

                        if (!cris.getStatus())
                        {
                            startLink = "&nbsp;";
                            endLink = "";
                        }
                        String title = I18nUtil.getMessage("CrisI18nLabel."
                                + publicPath + "." + type + ".title");
                        icon = MessageFormat.format(
                                I18nUtil.getMessage("CrisI18nLabel."
                                        + publicPath + "." + type + ".icon"),
                                title);
                    }
                    catch (Exception e)
                    {
                        log.debug(
                                "Error when build icon (perhaps missing this configuration: on cris module key:"
                                        + publicPath
                                        + ".ref.display.strategy.metadata.icon)",
                                e);

                        String title;
                        try
                        {
                            title = I18nUtil.getMessage("CrisI18nLabel."
                                    + publicPath + "." + ".title", true);
                        }
                        catch (MissingResourceException e2)
                        {
                            title = I18nUtil.getMessage(
                                    "CrisI18nLabel." + publicPath + ".title");
                        }

                        try
                        {
                            icon = MessageFormat.format(
                                    I18nUtil.getMessage("CrisI18nLabel."
                                            + publicPath + ".icon", true),
                                    title);
                        }
                        catch (MissingResourceException e2)
                        {
                            icon = I18nUtil
                                    .getMessage("CrisI18nLabel.default.icon");
                        }

                    }

                    metadata += startLink;

                    String metadataLocale = cris.getMetadataFieldName(locale);
                    String value = cris.getMetadata(metadataLocale);
                    if (StringUtils.isNotBlank(value))
                    {
                        metadata += Utils.addEntities(value);
                    }
                    else
                    {
                        metadata += Utils.addEntities(cris.getName());
                    }

                    metadata += "&nbsp;";
                    metadata += icon;
                    metadata += endLink;
                    metadata += "&nbsp;";
                }
                catch (Exception ex)
                {
                    log.error(ex.getMessage(), ex);
                }
            }
        }
        return metadata;
    }

}
