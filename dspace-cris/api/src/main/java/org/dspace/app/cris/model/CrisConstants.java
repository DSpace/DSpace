/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * https://github.com/CILEA/dspace-cris/wiki/License
 */
package org.dspace.app.cris.model;

import org.apache.log4j.Logger;
import org.dspace.app.cris.model.jdyna.DynamicObjectType;
import org.dspace.app.cris.service.ApplicationService;
import org.dspace.content.Collection;
import org.dspace.content.Community;
import org.dspace.content.DSpaceObject;
import org.dspace.content.Item;
import org.dspace.core.Constants;

public class CrisConstants {
    
    /** log4j logger */
    private static Logger log = Logger
            .getLogger(CrisConstants.class);
    
    private static final String PREFIX_TYPE = "CRIS";

    private static ApplicationService applicationService;
    
    public static final String CFG_MODULE = "cris";
    public static final String CFG_NETWORK_MODULE = "network";
	public static final int CRIS_TYPE_ID_START = 9;
	public static final int RP_TYPE_ID = 9;
	public static final int PROJECT_TYPE_ID = 10;
	public static final int OU_TYPE_ID = 11;
    public static final int NRP_TYPE_ID = 109;
    public static final int NPROJECT_TYPE_ID = 110;
    public static final int NOU_TYPE_ID = 111;
    public static final Integer CRIS_DYNAMIC_TYPE_ID_START = 1000;
    public static final Integer CRIS_NDYNAMIC_TYPE_ID_START = 10000;

    public static final String[] typeText = { "CRISRP", "CRISPJ", "CRISOU"};
	
	public static <T extends DSpaceObject> Integer getEntityType(T crisObject) {
	    return crisObject.getType();
	}
	
	public static <T extends DSpaceObject> Integer getEntityType(Class<T> clazz) throws InstantiationException, IllegalAccessException {
	    if(Item.class.isAssignableFrom(clazz)) {
            return Constants.ITEM;
        }
        else if(Collection.class.isAssignableFrom(clazz)) {
            return Constants.COLLECTION;
        }
        else if(Community.class.isAssignableFrom(clazz)) {
            return Constants.COMMUNITY;
        }
        else if(ResearchObject.class.isAssignableFrom(clazz)) {
            log.warn("Impossible to retrieve exact type definition from a dynamic object class, returned "+CRIS_DYNAMIC_TYPE_ID_START+" - the start placeholder");
            return CRIS_DYNAMIC_TYPE_ID_START;
        }
        return CrisConstants.getEntityType(clazz.newInstance());
    }
	
	public static <T extends ACrisObject> String getAuthorityPrefix(T crisObject) {
        return crisObject.getAuthorityPrefix();
    }

	public static String getEntityTypeText(Integer type) {
	    if(type >= CrisConstants.CRIS_DYNAMIC_TYPE_ID_START) {
	        return (PREFIX_TYPE+getApplicationService().get(DynamicObjectType.class, type-CRIS_DYNAMIC_TYPE_ID_START).getShortName()).toLowerCase();
	    }
	    else if(type >= CrisConstants.CRIS_TYPE_ID_START) {
            return typeText[type-CRIS_TYPE_ID_START].toLowerCase();         
        }
        else {
            return Constants.typeText[type].toLowerCase();
        }
	}

    public static ApplicationService getApplicationService()
    {
        return applicationService;
    }

    public void setApplicationService(ApplicationService applicationService)
    {
        this.applicationService = applicationService;
    }
  
    
}
