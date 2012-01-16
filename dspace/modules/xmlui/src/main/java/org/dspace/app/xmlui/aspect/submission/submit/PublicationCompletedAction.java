package org.dspace.app.xmlui.aspect.submission.submit;

import org.apache.avalon.framework.parameters.Parameters;
import org.apache.cocoon.acting.AbstractAction;
import org.apache.cocoon.environment.ObjectModelHelper;
import org.apache.cocoon.environment.Redirector;
import org.apache.cocoon.environment.Request;
import org.apache.cocoon.environment.SourceResolver;
import org.apache.cocoon.environment.http.HttpEnvironment;
import org.apache.log4j.Logger;
import org.dspace.app.util.Util;
import org.dspace.app.xmlui.utils.ContextUtil;
import org.dspace.content.InProgressSubmission;
import org.dspace.content.WorkspaceItem;
import org.dspace.core.Context;

import javax.servlet.http.HttpServletResponse;
import java.util.HashMap;
import java.util.Map;

/**
 * User: kevin (kevin at atmire.com)
 * Date: 13-jan-2012
 * Time: 16:09:41
 */
public class PublicationCompletedAction extends AbstractAction {

    private static final Logger log = Logger.getLogger(PublicationCompletedAction.class);

    public Map act(Redirector redirector, SourceResolver resolver, Map objectModel,
            String source, Parameters parameters) throws Exception
    {
        System.out.println("Entered publication completed action");
        Request request = ObjectModelHelper.getRequest(objectModel);
        int workspaceId = Util.getIntParameter(request, "workspaceID");
        System.out.println("Publication completed action workspace ID: " + workspaceId);
        if(workspaceId == -1){
            return null;
        }
        Context context = ContextUtil.obtainContext(objectModel);
        WorkspaceItem publication = WorkspaceItem.find(context, workspaceId);
        if(publication == null){
            System.out.println("ERROR: Publication completed action no Workspace item with id: " + workspaceId);
            return null;
        }

        //Check if our publication is completed
        if(0 < publication.getItem().getMetadata("internal", "workflow", "submitted", org.dspace.content.Item.ANY).length){
            final HttpServletResponse httpResponse = (HttpServletResponse) objectModel.get(HttpEnvironment.HTTP_RESPONSE_OBJECT);
            httpResponse.sendRedirect(request.getContextPath() + "/submit-overview?workspaceID=" + publication.getID());
            return new HashMap();
        }else{
            System.out.println("Publication not completed for: " + workspaceId);
        }
        return null;
    }

}
