/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.xmlui.aspect.administrative.importer.external.scidir;

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
import org.dspace.content.Collection;
import org.dspace.content.Item;
import org.dspace.content.WorkspaceItem;
import org.dspace.content.factory.ContentServiceFactory;
import org.dspace.content.service.InstallItemService;
import org.dspace.content.service.ItemService;
import org.dspace.content.service.WorkspaceItemService;
import org.dspace.core.ConfigurationManager;
import org.dspace.handle.factory.HandleServiceFactory;
import org.dspace.handle.service.HandleService;
import org.dspace.importer.external.datamodel.ImportRecord;
import org.dspace.importer.external.metadatamapping.MetadatumDTO;
import org.dspace.importer.external.service.ImportService;
import org.dspace.utils.DSpace;
import org.dspace.workflow.factory.WorkflowServiceFactory;

import javax.servlet.http.HttpServletResponse;
import java.util.HashMap;
import java.util.Map;

/**
 * Created by Philip Vissenaekens (philip at atmire dot com)
 * Date: 06/10/15
 * Time: 10:49
 */
public class LiveImportAction extends AbstractAction {
    private String url = ConfigurationManager.getProperty("elsevier-sciencedirect", "api.scidir.url");
    ImportService importService = new DSpace().getServiceManager().getServiceByName("importService", ImportService.class);
    private static Logger log = Logger.getLogger(LiveImportAction.class);

    protected ItemService itemService = ContentServiceFactory.getInstance().getItemService();
    protected WorkspaceItemService workspaceItemService = ContentServiceFactory.getInstance().getWorkspaceItemService();
    protected InstallItemService installItemService = ContentServiceFactory.getInstance().getInstallItemService();

    @Override
    public Map act(Redirector redirector, SourceResolver resolver, Map objectModel, String source, Parameters parameters) throws Exception {
        Map<String, String> returnValues = new HashMap<String, String>();

        Request request = ObjectModelHelper.getRequest(objectModel);
        org.dspace.core.Context context = ContextUtil.obtainContext(objectModel);
        String buttonPressed = Util.getSubmitButton(request, "");

        if(buttonPressed.equals(LiveImportSelected.CANCEL_BUTTON)){
            ((HttpServletResponse) objectModel.get(HttpEnvironment.HTTP_RESPONSE_OBJECT)).sendRedirect(request.getContextPath() + "/liveimport");
        }

        HashMap<String,SessionRecord> selected = (HashMap<String,SessionRecord>) request.getSession().getAttribute("selected");
        String action = request.getParameter("import-action");
        String collectionHandle = request.getParameter("import-collection");

        HandleService handleService = HandleServiceFactory.getInstance().getHandleService();
        Collection collection = (Collection) handleService.resolveToObject(context, collectionHandle);

        for (String eid : selected.keySet()) {
            try {
                ImportRecord record = importService.getRecord(url, eid);

                if (record != null) {

                    WorkspaceItem wi = workspaceItemService.create(context, collection, false);

                    Item item = wi.getItem();

                    for (MetadatumDTO metadatum : record.getValueList()) {
                        itemService.addMetadata(context,item,metadatum.getSchema(), metadatum.getElement(), metadatum.getQualifier(), metadatum.getLanguage(), metadatum.getValue());
                    }

                    if (action.equals("workflow")) {
                        WorkflowServiceFactory.getInstance().getWorkflowService().start(context, wi);
                    } else if (action.equals("archive")) {
                        try {
                            installItemService.installItem(context, wi);
                        } catch (Exception e) {
                            workspaceItemService.deleteAll(context, wi);
                            log.error("Exception after install item, try to revert...", e);
                            throw e;
                        }
                    }

                    itemService.update(context,item);
                    context.dispatchEvents();
                }
            }
            catch (Exception e){
                log.error(e.getMessage(), e);

                returnValues.put("outcome", "failure");
                returnValues.put("message", "xmlui.scidir.live-import-action.failure");
            }
        }

        if(returnValues.size() == 0){
            returnValues.put("outcome", "success");
            returnValues.put("message", "xmlui.scidir.live-import-action.success");
        }

        return returnValues;
    }
}
