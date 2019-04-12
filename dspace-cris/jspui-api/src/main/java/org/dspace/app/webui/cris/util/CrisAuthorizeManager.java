package org.dspace.app.webui.cris.util;

import java.sql.SQLException;
import java.util.List;

import org.apache.commons.lang.StringUtils;
import org.dspace.app.cris.model.ACrisObject;
import org.dspace.app.cris.model.jdyna.VisibilityTabConstant;
import org.dspace.app.cris.service.ApplicationService;
import org.dspace.authorize.AuthorizeManager;
import org.dspace.core.ConfigurationManager;
import org.dspace.core.Context;
import org.dspace.eperson.EPerson;
import org.dspace.eperson.Group;

import it.cilea.osd.jdyna.model.AuthorizationContext;
import it.cilea.osd.jdyna.model.PropertiesDefinition;
import it.cilea.osd.jdyna.web.AbstractEditTab;
import it.cilea.osd.jdyna.web.ITabService;

public class CrisAuthorizeManager
{

    public static <A extends AuthorizationContext, T extends ACrisObject, PD extends PropertiesDefinition> boolean authorize(
            Context context, ITabService applicationService, Class<T> clazz, Class<PD> classPD,
            Integer id, A authorizedObject) throws SQLException
    {
        Integer visibility = authorizedObject.getVisibility();
        if (!(authorizedObject instanceof AbstractEditTab) 
        		&& VisibilityTabConstant.HIGH.equals(visibility))
        {
            return true;
        }

        boolean result = false;
        T object = null;
        try
        {
            object = ((ApplicationService) applicationService).get(clazz, id);
        }
        catch (NumberFormatException e)
        {
            throw new RuntimeException(e);
        }

        // check admin authorization
        if (isAdmin(context, object) && !VisibilityTabConstant.LOW.equals(visibility))
        {
        	// admin can see everything except what is reserved to the object owner (LOW)
        	return true;
        }
        
        EPerson currUser = context.getCurrentUser();
        
        boolean isOwner = false;

        if (currUser != null)
        {
            isOwner = object.isOwner(currUser);
        }

        if (visibility == VisibilityTabConstant.LOW || visibility == VisibilityTabConstant.STANDARD
        		|| visibility == VisibilityTabConstant.HIGH) {
        	// if visibility is standard the admin case has been already checked on line 49
        	// visibility == HIGH here only for edit tab, it is assumed to be "standard" as nobody want public edit
        	return isOwner;
        }

        // last case... policy
        if (currUser != null)
        {
            List<PD> listPolicySingle = authorizedObject
                    .getAuthorizedSingle();

            if (listPolicySingle != null && !listPolicySingle.isEmpty())
            {
                for (PD policy : listPolicySingle)
                {
                    String data = object.getMetadata(policy.getShortName());
                    if (StringUtils.isNotBlank(data))
                    {
                        if (currUser.getID() == Integer.parseInt(data))
                        {
                            return true;
                        }
                    }
                }
            }
        }

        List<PD> listPolicyGroup = authorizedObject.getAuthorizedGroup();

        if (listPolicyGroup != null && !listPolicyGroup.isEmpty())
        {
            for (PD policy : listPolicyGroup)
            {
                List<String> policies = object.getMetadataValue(policy.getShortName());
                for (String data : policies)
                {
                    if (StringUtils.isNotBlank(data))
                    {
                        Group group = Group.find(context,
                                Integer.parseInt(data));
                        if (group != null)
                        {
                            if (currUser == null && group.getID() == 0)
                            {
                                boolean isMember = Group.isMember(context, 0);
                                if (isMember)
                                {
                                    return true;
                                }
                            }
                            if (Group.isMember(context, group.getID()))
                            {
                                return true;
                            }
                        }
                    }
                }
            }
        }

        return result;
    }
    
    public static <T extends ACrisObject> boolean isAdmin(
            Context context, T crisObject) throws SQLException 
    {
    	String crisObjectTypeText = crisObject.getTypeText();
    	return isAdmin(context,crisObjectTypeText);
    }

    public static <T extends ACrisObject> boolean isAdmin(
            Context context, String crisObjectTypeText) throws SQLException 
    {
        EPerson currUser = context.getCurrentUser();
        if(currUser==null) 
        {
            return false;
        }
        
        // check admin authorization
        if (AuthorizeManager.isAdmin(context))
        {
            return true;
        }

        String groupName = ConfigurationManager.getProperty("cris", crisObjectTypeText + ".admin");
        if(StringUtils.isBlank(groupName)) {
            groupName = "Administrator "+crisObjectTypeText;
        }
        Group group = Group.findByName(context, groupName);
        if (group != null)
        {
            if (Group.isMember(context, group.getID()))
            {
                return true;
            }
        }
        return false;
    }    
    
    public static <T extends ACrisObject> boolean canEdit(
            Context context, ITabService as, Class<? extends AbstractEditTab> classT, T crisObject) throws SQLException 
    {
        EPerson currUser = context.getCurrentUser();
        if(currUser==null) 
        {
            return false;
        }
        
        // check admin authorization
        if (isAdmin(context, crisObject))
        {
            return true;
        }
    
        List<? extends AbstractEditTab> list = as.getList(classT);
		for (AbstractEditTab t : list) {
        	if(CrisAuthorizeManager.authorize(context, as, crisObject.getCRISTargetClass(), crisObject.getClassPropertiesDefinition(), crisObject.getId(), t)) {
                return true;
            }
        }
        return false;
    }
}
