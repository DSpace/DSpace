package org.dspace.app.webui.cris.util;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.jsp.JspException;

import org.dspace.app.cris.model.ACrisObject;
import org.dspace.app.cris.model.CrisConstants;
import org.dspace.app.cris.service.ApplicationService;
import org.dspace.app.webui.util.IDisplayMetadataValueStrategy;
import org.dspace.browse.BrowseDSpaceObject;
import org.dspace.browse.BrowseItem;
import org.dspace.content.Item;
import org.dspace.content.Metadatum;
import org.dspace.core.ConfigurationManager;
import org.dspace.discovery.IGlobalSearchResult;
import org.dspace.utils.DSpace;

import it.cilea.osd.jdyna.web.tag.JDynATagLibraryFunctions;

public class CrisPictureDisplayStrategy
        implements IDisplayMetadataValueStrategy
{
    /** Config value of thumbnail view toggle */
    private boolean showThumbs;

    /** Config browse/search width and height */
    private int thumbItemListMaxWidth;

    private int thumbItemListMaxHeight;

    private ApplicationService applicationService = new DSpace()
            .getServiceManager()
            .getServiceByName("applicationService", ApplicationService.class);
    
    public CrisPictureDisplayStrategy()
    {
        /* get the required thumbnail config items */
        showThumbs = ConfigurationManager
                .getBooleanProperty("webui.browse.thumbnail.show");

        if (showThumbs)
        {
            thumbItemListMaxHeight = ConfigurationManager
                    .getIntProperty("webui.browse.thumbnail.maxheight");

            if (thumbItemListMaxHeight == 0)
            {
                thumbItemListMaxHeight = ConfigurationManager
                        .getIntProperty("thumbnail.maxheight");
            }

            thumbItemListMaxWidth = ConfigurationManager
                    .getIntProperty("webui.browse.thumbnail.maxwidth");

            if (thumbItemListMaxWidth == 0)
            {
                thumbItemListMaxWidth = ConfigurationManager
                        .getIntProperty("thumbnail.maxwidth");
            }
        }

    }

    public String getMetadataDisplay(HttpServletRequest hrq, int limit,
            boolean viewFull, String browseType, int colIdx, String field,
            Metadatum[] metadataArray, BrowseItem item,
            boolean disableCrossLinks, boolean emph) throws JspException
    {
        ACrisObject crisObject = (ACrisObject) ((BrowseDSpaceObject) item)
                .getBrowsableDSpaceObject();
        return getThumbMarkup(hrq, crisObject);
    }

    public String getMetadataDisplay(HttpServletRequest hrq, int limit,
            boolean viewFull, String browseType, int colIdx, String field,
            Metadatum[] metadataArray, Item item, boolean disableCrossLinks,
            boolean emph) throws JspException
    {
        return null;
    }

    public String getExtraCssDisplay(HttpServletRequest hrq, int limit,
            boolean b, String string, int colIdx, String field,
            Metadatum[] metadataArray, BrowseItem browseItem,
            boolean disableCrossLinks, boolean emph) throws JspException
    {
        return null;
    }

    public String getExtraCssDisplay(HttpServletRequest hrq, int limit,
            boolean b, String browseType, int colIdx, String field,
            Metadatum[] metadataArray, Item item, boolean disableCrossLinks,
            boolean emph) throws JspException
    {
        return null;
    }

    /* generate the (X)HTML required to show the thumbnail */
    private String getThumbMarkup(HttpServletRequest hrq, ACrisObject crisObject) throws JspException
    {
            
            StringBuffer thumbFrag = new StringBuffer();
            
            String crispicture = null;            
            String servletPath = null;
            switch (crisObject.getType())
            {
            case 9:
                servletPath = ConfigurationManager.getProperty(CrisConstants.CFG_MODULE, "researcherpage.jdynafile.servlet.name");
                crispicture = ConfigurationManager.getProperty(CrisConstants.CFG_MODULE, "researcherpage.thumbnail.picture");
                break;
            case 10:
                servletPath = ConfigurationManager.getProperty(CrisConstants.CFG_MODULE, "project.jdynafile.servlet.name");
                crispicture = ConfigurationManager.getProperty(CrisConstants.CFG_MODULE, "project.thumbnail.picture");
                break;
            case 11:
                servletPath = ConfigurationManager.getProperty(CrisConstants.CFG_MODULE, "organizationunit.jdynafile.servlet.name");
                crispicture = ConfigurationManager.getProperty(CrisConstants.CFG_MODULE, "organizationunit.thumbnail.picture");                
                break;
            default:
                servletPath = ConfigurationManager.getProperty(CrisConstants.CFG_MODULE, "otherresearchobject.jdynafile.servlet.name");
                crispicture = ConfigurationManager.getProperty(CrisConstants.CFG_MODULE, "otherresearchobject."+crisObject.getPublicPath().trim()+".thumbnail.picture");
                break;
            }

            String displayObject = crisObject.getMetadata(crispicture);
            String fileName = JDynATagLibraryFunctions.getFileName(displayObject);
            String img = hrq.getContextPath() + "/"+ servletPath +"/" + JDynATagLibraryFunctions.getFileFolder(displayObject) + "?filename=" + fileName;            
           
            String alt = fileName;
            String title = "A preview " + fileName + " picture"; 
            
            thumbFrag.append("<img class=\"cris-thubmnail img-thumbnail\" src=\"").append(img).append("\" alt=\"")
                    .append(alt + "\" title=\"")
                    .append(title + "\" ")
                    .append("/ border=\"0\"/>");
            
            return thumbFrag.toString();
    }

    @Override
    public String getMetadataDisplay(HttpServletRequest hrq, int limit,
            boolean viewFull, String browseType, int colIdx, String field,
            Metadatum[] metadataArray, IGlobalSearchResult item,
            boolean disableCrossLinks, boolean emph) throws JspException
    {
        ACrisObject crisObject = (ACrisObject)item;
        return getThumbMarkup(hrq, crisObject);
    }
}