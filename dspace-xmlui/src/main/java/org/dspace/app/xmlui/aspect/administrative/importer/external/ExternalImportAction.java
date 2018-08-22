/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.xmlui.aspect.administrative.importer.external;

import java.util.*;
import javax.servlet.http.*;
import org.apache.avalon.framework.parameters.*;
import org.apache.cocoon.acting.*;
import org.apache.cocoon.environment.*;
import org.apache.cocoon.environment.http.*;
import org.apache.log4j.*;
import org.dspace.app.util.*;
import org.dspace.app.xmlui.utils.*;
import org.dspace.content.Collection;
import org.dspace.content.*;
import org.dspace.content.factory.*;
import org.dspace.content.service.*;
import org.dspace.handle.factory.*;
import org.dspace.handle.service.*;
import org.dspace.importer.external.datamodel.*;
import org.dspace.importer.external.metadatamapping.*;
import org.dspace.importer.external.service.*;
import org.dspace.utils.*;
import org.dspace.workflow.factory.*;

/**
 * Created by Philip Vissenaekens (philip at atmire dot com)
 * Date: 06/10/15
 * Time: 10:49
 */
public class ExternalImportAction extends AbstractAction {
    private Map<String, AbstractImportMetadataSourceService> sources = new DSpace().getServiceManager().getServiceByName("ImportServices", HashMap.class);
    private ImportService importService = new DSpace().getServiceManager().getServiceByName("importService", ImportService.class);
    private static Logger log = Logger.getLogger(ExternalImportAction.class);

    protected ItemService itemService = ContentServiceFactory.getInstance().getItemService();
    protected WorkspaceItemService workspaceItemService = ContentServiceFactory.getInstance().getWorkspaceItemService();
    protected InstallItemService installItemService = ContentServiceFactory.getInstance().getInstallItemService();

    @Override
    public Map act(Redirector redirector, SourceResolver resolver, Map objectModel, String source, Parameters parameters) throws Exception {
        Map<String, String> returnValues = new HashMap<String, String>();

        Request request = ObjectModelHelper.getRequest(objectModel);
        org.dspace.core.Context context = ContextUtil.obtainContext(objectModel);
        String buttonPressed = Util.getSubmitButton(request, "");

        if(buttonPressed.equals(ExternalImportSelected.CANCEL_BUTTON)){
            ((HttpServletResponse) objectModel.get(HttpEnvironment.HTTP_RESPONSE_OBJECT)).sendRedirect(request.getContextPath() + "/admin/external-import");
        }

        HashMap<String,SessionRecord> selected = (HashMap<String,SessionRecord>) request.getSession().getAttribute("selected");
        String action = request.getParameter("import-action");
        String collectionHandle = request.getParameter("import-collection");

        HandleService handleService = HandleServiceFactory.getInstance().getHandleService();
        Collection collection = (Collection) handleService.resolveToObject(context, collectionHandle);
        String importSourceString = request.getSession(true).getAttribute("source").toString();
        AbstractImportMetadataSourceService importSource = sources.get(importSourceString);
        for (String eid : selected.keySet()) {
            try {
                ImportRecord record = importService.getRecord(importSource.getImportSource(), "eid(" + eid + ")");

                if (record != null) {

                    WorkspaceItem wi = workspaceItemService.create(context, collection, false);

                    Item item = wi.getItem();

                    if (!action.equals("archive") ) {
                        itemService.addMetadata(context, item, "workflow", "import", "source", null, importSourceString);
                    }

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
                returnValues.put("message", "xmlui.administrative.importer.external.external-import-action.failure");
            }
        }

        if(returnValues.size() == 0){
            returnValues.put("outcome", "success");
            returnValues.put("message", "xmlui.administrative.importer.external.external-import-action.success");
        }

        return returnValues;
    }
}
