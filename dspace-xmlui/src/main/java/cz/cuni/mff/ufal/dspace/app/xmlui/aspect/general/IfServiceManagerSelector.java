package cz.cuni.mff.ufal.dspace.app.xmlui.aspect.general;

import java.sql.SQLException;
import java.util.List;
import java.util.Map;

import org.apache.avalon.framework.logger.AbstractLogEnabled;
import org.apache.avalon.framework.parameters.Parameters;
import org.apache.cocoon.environment.ObjectModelHelper;
import org.apache.cocoon.environment.Request;
import org.apache.cocoon.selection.Selector;
import org.apache.log4j.Logger;
import org.dspace.app.xmlui.utils.ContextUtil;
import org.dspace.app.xmlui.utils.HandleUtil;
import org.dspace.authorize.AuthorizeManager;
import org.dspace.content.Bitstream;
import org.dspace.content.Bundle;
import org.dspace.content.DSpaceObject;
import org.dspace.content.Item;
import org.dspace.core.Constants;
import org.dspace.core.Context;
import org.dspace.eperson.EPerson;
import org.dspace.eperson.Group;

/**
 * This simple selector operates on the authenticated DSpace user and selects
 * if a user is member of group "Service Managers" but not an admin
 * 
 * <map:selector name="IfServiceManagerSelector" src="cz.cuni.mff.ufal.dspace.app.xmlui.aspect.general.IfServiceManagerSelector"/>
 * 
 * 
 * 
 * <map:select type="IfServiceManagerSelector"> 
 *   <map:when test="servicemanager">
 *     ...
 *   </map:when> 
 *   <map:otherwise> 
 *     ...
 *   </map:otherwise> 
 * </map:select>
 * 
 * There is only one defined test expressions: "servicemanager".
 *
 * based on class by Scott Phillips
 * modified for LINDAT/CLARIN
 */

public class IfServiceManagerSelector extends AbstractLogEnabled implements
        Selector
{

    private static Logger log = Logger.getLogger(IfServiceManagerSelector.class);

    /** Test expressions */
    public static final String SERVICEMANAGER = "servicemanager";

    /**
     * Determine if the authenticated eperson matches the given expression.
     */
    public boolean select(String expression, Map objectModel,
            Parameters parameters)
    {
        try
        {
            Context context = ContextUtil.obtainContext(objectModel);
            EPerson eperson = context.getCurrentUser();

            if(expression.equals(SERVICEMANAGER)) {
            	return isNonAdminServiceManager(context, eperson);
            }

            // Otherwise return false;
            return false;

        }
        catch (Exception e)
        {
            // Log it and returned no match.
            log.error("Error selecting based on authentication status: " + e.getMessage());
            return false;
        }
    }
    
    public static boolean isNonAdminServiceManager(Context context, EPerson eperson) throws SQLException {
    	try {
    		Group sm_group = Group.findByName(context, "Service Managers");
    		if(sm_group != null) {
    			if(sm_group.isMember(eperson) && !AuthorizeManager.isAdmin(context)) {
    				return true;
    			}
    		}
    	}catch(Exception e) {
    		return false;
    	}
    	return false;
    }

}
