/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * https://github.com/CILEA/dspace-cris/wiki/License
 */
package org.dspace.app.webui.cris.util;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.jsp.JspException;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.dspace.app.cris.model.ACrisObject;
import org.dspace.app.cris.service.ApplicationService;
import org.dspace.app.webui.util.IDisplayMetadataValueStrategy;
import org.dspace.browse.BrowseDSpaceObject;
import org.dspace.browse.BrowseItem;
import org.dspace.content.Item;
import org.dspace.content.Metadatum;
import org.dspace.discovery.IGlobalSearchResult;
import org.dspace.utils.DSpace;

import it.cilea.osd.jdyna.model.PropertiesDefinition;
import it.cilea.osd.jdyna.web.tag.JDynATagLibraryFunctions;
import it.cilea.osd.jdyna.widget.WidgetCheckRadio;

public class CrisDropdownDisplayStrategy implements
        IDisplayMetadataValueStrategy
{

    /**
     * log4j category
     */
    public static final Log log = LogFactory
            .getLog(CrisDropdownDisplayStrategy.class);
    
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
        String metadata = internalDisplay(hrq, metadataArray, crisObject, field);
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
                return internalDisplay(hrq, metadataArray, entityByCrisId, field);
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
        String metadata = internalDisplay(hrq, metadataArray, crisObject, field);
        return metadata;
    }
    
    private String internalDisplay(HttpServletRequest hrq,
            Metadatum[] metadataArray, ACrisObject crisObject, String field)
    {
        String metadata = "";
        if (metadataArray != null && metadataArray.length > 0)
        {
            try 
            {
                PropertiesDefinition pd = applicationService.findPropertiesDefinitionByShortName(crisObject.getClassPropertiesDefinition(), field.split("\\.")[1]);
                for(Metadatum mm : metadataArray) {
                    metadata += JDynATagLibraryFunctions.getCheckRadioDisplayValue((((WidgetCheckRadio)pd.getRendering()).getStaticValues()), mm.value);
                }
            }
            catch (Exception ex)
            {
                log.error(ex.getMessage(), ex);
                //failover insert the stored value
                for(Metadatum mm : metadataArray) {
                    metadata += mm.value;
                }                
            }
        }
        return metadata;
    }
}

