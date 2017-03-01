package org.tamu.dspace.extensions;

import java.util.HashMap;
import java.util.Map;

import org.apache.avalon.framework.parameters.Parameters;
import org.apache.cocoon.acting.AbstractAction;
import org.apache.cocoon.environment.ObjectModelHelper;
import org.apache.cocoon.environment.Redirector;
import org.apache.cocoon.environment.Request;
import org.apache.cocoon.environment.SourceResolver;
import org.dspace.app.xmlui.utils.ContextUtil;
import org.dspace.core.Context;

public class RecordContextAction extends AbstractAction 
{
    public Map act(Redirector redirector, SourceResolver resolver, Map objectModel, String source, Parameters parameters) throws Exception
    {
        Context context = ContextUtil.obtainContext(objectModel);
        Request request = ObjectModelHelper.getRequest(objectModel);
        
        //if user is not logged in and we are not at the password login page already
        if(context.getCurrentUser() == null && "GET".equals(request.getMethod()) &&
        	!request.getRequestURI().contains("login") && !request.getRequestURI().contains("favicon"))  
        {
            //generate the interrupt request
            org.dspace.app.xmlui.utils.AuthenticationUtil.interruptRequest(objectModel, "", "", "");
            return new HashMap();
        }

        return null;
    }

}
