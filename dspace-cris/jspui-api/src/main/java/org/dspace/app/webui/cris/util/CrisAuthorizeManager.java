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
import it.cilea.osd.jdyna.web.ITabService;

public class CrisAuthorizeManager
{

    public static <A extends AuthorizationContext, T extends ACrisObject, PD extends PropertiesDefinition> boolean authorize(
            Context context, ITabService applicationService, Class<T> clazz, Class<PD> classPD,
            Integer id, A authorizedObject) throws SQLException
    {
        Integer visibility = authorizedObject.getVisibility();
        if (VisibilityTabConstant.HIGH.equals(visibility))
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

        EPerson currUser = context.getCurrentUser();

        if (visibility != VisibilityTabConstant.POLICY)
        {
            boolean isOwner = false;

            if (currUser != null)
            {
                isOwner = object.isOwner(currUser);
            }

            // check admin authorization
            if (AuthorizeManager.isAdmin(context))
            {
                if (VisibilityTabConstant.ADMIN.equals(visibility)
                        || VisibilityTabConstant.STANDARD.equals(visibility))
                {
                    return true;
                }
                if (isOwner)
                {
                    return true;
                }
                return false;
            }
            if (VisibilityTabConstant.LOW.equals(visibility)
                    || VisibilityTabConstant.STANDARD.equals(visibility))
            {
                if (isOwner)
                {
                    return true;
                }
            }

        }

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

        String crisObjectTypeText = crisObject.getTypeText();
        
        String groupName = ConfigurationManager.getProperty("cris", "admin" + crisObjectTypeText);
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
}
