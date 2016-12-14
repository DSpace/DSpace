/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * https://github.com/CILEA/dspace-cris/wiki/License
 */
package org.dspace.app.webui.cris.util;

import java.text.MessageFormat;
import java.util.MissingResourceException;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.jsp.JspException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.dspace.app.cris.model.ACrisObject;
import org.dspace.app.cris.model.jdyna.ACrisNestedObject;
import org.dspace.app.cris.service.ApplicationService;
import org.dspace.app.webui.util.IDisplayMetadataValueStrategy;
import org.dspace.browse.BrowseItem;
import org.dspace.content.Item;
import org.dspace.content.Metadatum;
import org.dspace.core.ConfigurationManager;
import org.dspace.core.I18nUtil;
import org.dspace.core.Utils;
import org.dspace.discovery.IGlobalSearchResult;
import org.dspace.utils.DSpace;

import it.cilea.osd.jdyna.model.ANestedPropertiesDefinition;
import it.cilea.osd.jdyna.model.ANestedProperty;
import it.cilea.osd.jdyna.model.ATypeNestedObject;
import it.cilea.osd.jdyna.model.PropertiesDefinition;
import it.cilea.osd.jdyna.model.Property;

public abstract class ACrisRefDisplayStrategy<P extends Property<TP>, TP extends PropertiesDefinition, NP extends ANestedProperty<NTP>, NTP extends ANestedPropertiesDefinition, ACNO extends ACrisNestedObject<NP, NTP, P, TP>, ATNO extends ATypeNestedObject<NTP>> implements IDisplayMetadataValueStrategy
{

    /**
     * log4j category
     */
    public static final Log log = LogFactory
            .getLog(ACrisRefDisplayStrategy.class);

    private ApplicationService applicationService = new DSpace()
            .getServiceManager()
            .getServiceByName("applicationService", ApplicationService.class);

    @Override
    public String getMetadataDisplay(HttpServletRequest hrq, int limit,
            boolean viewFull, String browseType, int colIdx, String field,
            Metadatum[] metadataArray, BrowseItem item,
            boolean disableCrossLinks, boolean emph)
    {
        String metadata = "N/A";
        metadata = internalDisplay(hrq, metadataArray, metadata);
        return metadata;
    }

    @Override
    public String getMetadataDisplay(HttpServletRequest hrq, int limit,
            boolean viewFull, String browseType, int colIdx, String field,
            Metadatum[] metadataArray, Item item, boolean disableCrossLinks,
            boolean emph)
    {
        // not used
        return null;
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
    public String getMetadataDisplay(HttpServletRequest hrq, int limit, boolean viewFull, String browseType,
            int colIdx, String field, Metadatum[] metadataArray, IGlobalSearchResult item, boolean disableCrossLinks,
            boolean emph) throws JspException 
    {       
        String metadata = "N/A";
        metadata = internalDisplay(hrq, metadataArray, metadata);
        return metadata;
    }
    
    private String internalDisplay(HttpServletRequest hrq,
            Metadatum[] metadataArray, String metadata)
    {
        if (metadataArray.length > 0)
        {

            String publicPath = getPublicPath();
            metadata = "";
            for (Metadatum metadatum : metadataArray)
            {
                String authority = metadatum.authority;
                String target = ConfigurationManager
                        .getBooleanProperty("cris", "researcher.cris." + publicPath
                                + ".ref.display.strategy.target.blank", false)?"target=\"_blank\" ":"";
                String startLink = "&nbsp;<a "+target+ "href=\""
                        + hrq.getContextPath() + "/cris/" + publicPath + "/"
                        + authority;
                startLink += "\" class=\"authority\">";
                String endLink = "</a>";
                
                String icon = "";
                try
                {
                    ACrisObject rp = applicationService.getEntityByCrisId(authority, getClassName());
                    String type = rp.getMetadata(ConfigurationManager
                            .getProperty("cris", "researcher.cris." + publicPath
                                    + ".ref.display.strategy.metadata.icon"));

                    if(!rp.getStatus() || (type!=null && type.equals("exstaff"))) {
                        startLink = "&nbsp;";
                        endLink = "";
                   }
                    String title = I18nUtil
                            .getMessage("ItemCrisRefDisplayStrategy."
                                    + publicPath + "." + type + ".title");
                    icon = MessageFormat.format(
                            I18nUtil.getMessage("ItemCrisRefDisplayStrategy."
                                    + publicPath + "." + type + ".icon"),
                            title);
                }
                catch (Exception e)
                {
                    log.warn(
                            "Error when build icon (perhaps missing this configuration: on cris module key:researcher.cris."
                                    + publicPath
                                    + ".ref.display.strategy.metadata.icon)");
                    try
                    {
                        icon = I18nUtil.getMessage("ItemCrisRefDisplayStrategy."
                                + publicPath + ".icon");
                    }
                    catch (MissingResourceException e2)
                    {
                        icon = I18nUtil.getMessage(
                                "ItemCrisRefDisplayStrategy.default.icon");
                    }
                }
                metadata += startLink;
                metadata += Utils.addEntities(metadatum.value);
                metadata += "&nbsp;";
                metadata += icon;
                metadata += endLink;
            }

        }
        return metadata;
    }

    abstract public <ACO extends ACrisObject<P, TP, NP, NTP, ACNO, ATNO>> Class<ACO> getClassName();

    abstract public String getPublicPath();

}
