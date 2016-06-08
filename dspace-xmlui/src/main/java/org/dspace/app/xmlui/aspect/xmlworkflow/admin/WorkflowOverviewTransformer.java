/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.xmlui.aspect.xmlworkflow.admin;

import org.apache.cocoon.environment.ObjectModelHelper;
import org.apache.cocoon.environment.Request;
import org.apache.log4j.Logger;
import org.dspace.app.util.Util;
import org.dspace.app.xmlui.cocoon.AbstractDSpaceTransformer;
import org.dspace.app.xmlui.utils.ContextUtil;
import org.dspace.app.xmlui.utils.UIException;
import org.dspace.app.xmlui.wing.Message;
import org.dspace.app.xmlui.wing.WingException;
import org.dspace.app.xmlui.wing.element.*;
import org.dspace.authorize.AuthorizeException;
import org.dspace.authorize.AuthorizeServiceImpl;
import org.dspace.authorize.factory.AuthorizeServiceFactory;
import org.dspace.authorize.service.AuthorizeService;
import org.dspace.content.Collection;
import org.dspace.content.Item;
import org.dspace.content.factory.ContentServiceFactory;
import org.dspace.content.service.CollectionService;
import org.dspace.core.Context;
import org.dspace.core.LogManager;
import org.dspace.handle.HandleServiceImpl;
import org.dspace.handle.factory.HandleServiceFactory;
import org.dspace.handle.service.HandleService;
import org.dspace.xmlworkflow.WorkflowConfigurationException;
import org.dspace.xmlworkflow.XmlWorkflowFactoryImpl;
import org.dspace.xmlworkflow.factory.XmlWorkflowFactory;
import org.dspace.xmlworkflow.factory.XmlWorkflowServiceFactory;
import org.dspace.xmlworkflow.state.Step;
import org.dspace.xmlworkflow.state.Workflow;
import org.dspace.xmlworkflow.storedcomponents.ClaimedTask;
import org.dspace.xmlworkflow.storedcomponents.PoolTask;
import org.dspace.xmlworkflow.storedcomponents.XmlWorkflowItem;
import org.dspace.xmlworkflow.storedcomponents.service.ClaimedTaskService;
import org.dspace.xmlworkflow.storedcomponents.service.PoolTaskService;
import org.dspace.xmlworkflow.storedcomponents.service.XmlWorkflowItemService;
import org.xml.sax.SAXException;

import java.io.IOException;
import java.sql.SQLException;
import java.util.*;
import java.util.List;

/**
 * A transformer that renders all xmlworkflow items
 * and allows for the admin to either delete them or send
 * them back to the submitter
 *
 * @author Bram De Schouwer (bram.deschouwer at dot com)
 * @author Kevin Van de Velde (kevin at atmire dot com)
 * @author Ben Bosman (ben at atmire dot com)
 * @author Mark Diggory (markd at atmire dot com)
 */
public class WorkflowOverviewTransformer extends AbstractDSpaceTransformer {


    private static final Logger log = Logger.getLogger(WorkflowOverviewTransformer.class);
    private static final int[] RESULTS_PER_PAGE_PROGRESSION = {5, 10, 20, 40, 60, 80, 100};


    private static final Message T_dspace_home =
        message("xmlui.general.dspace_home");

    private static final Message T_trail =
        message("xmlui.XMLWorkflow.WorkflowOverviewTransformer.trail");

    private static final Message T_title =
        message("xmlui.XMLWorkflow.WorkflowOverviewTransformer.title");
    private static final Message T_go = message("xmlui.general.go");
    private static final Message T_head = message("xmlui.XMLWorkflow.WorkflowOverviewTransformer.title");
    
    private static final Message T_search_column1 = message("xmlui.XMLWorkflow.WorkflowOverviewTransformer.search_column1");
    private static final Message T_search_column2 = message("xmlui.XMLWorkflow.WorkflowOverviewTransformer.search_column2");
    private static final Message T_search_column3 = message("xmlui.XMLWorkflow.WorkflowOverviewTransformer.search_column3");
    private static final Message T_search_column4 = message("xmlui.XMLWorkflow.WorkflowOverviewTransformer.search_column4");
    private static final Message T_search_column5 = message("xmlui.XMLWorkflow.WorkflowOverviewTransformer.search_column5");
    private static final Message T_button_back_to_submitter = message("xmlui.XMLWorkflow.WorkflowOverviewTransformer.button.submit_submitter");
    private static final Message T_button_delete = message("xmlui.XMLWorkflow.WorkflowOverviewTransformer.button.submit_delete");
    private static final Message T_no_results = message("xmlui.XMLWorkflow.WorkflowOverviewTransformer.button.no_results");

    protected AuthorizeService authorizeService = AuthorizeServiceFactory.getInstance().getAuthorizeService();
    protected CollectionService collectionService = ContentServiceFactory.getInstance().getCollectionService();
    protected HandleService handleService = HandleServiceFactory.getInstance().getHandleService();
    protected ClaimedTaskService claimedTaskService = XmlWorkflowServiceFactory.getInstance().getClaimedTaskService();
    protected PoolTaskService poolTaskService = XmlWorkflowServiceFactory.getInstance().getPoolTaskService();
    protected XmlWorkflowFactory workflowFactory = XmlWorkflowServiceFactory.getInstance().getWorkflowFactory();
    protected XmlWorkflowItemService xmlWorkflowItemService = XmlWorkflowServiceFactory.getInstance().getXmlWorkflowItemService();

    @Override
    public void addPageMeta(PageMeta pageMeta) throws WingException, SQLException
    {
        pageMeta.addMetadata("title").addContent(T_title);
        pageMeta.addTrailLink(contextPath + "/", T_dspace_home);

        pageMeta.addTrail().addContent(T_trail);
    }

    @Override
    public void addBody(Body body) throws SAXException, WingException, SQLException, IOException, AuthorizeException {
        Context context = ContextUtil.obtainContext(ObjectModelHelper.getRequest(objectModel));
        if(!authorizeService.isAdmin(context)){
            throw new AuthorizeException();
        }
        
        Division div = body.addInteractiveDivision("xmlworkflowoverview", contextPath + "/admin/xmlworkflowoverview", Division.METHOD_POST, "primary");
        this.buildSearchResultsDivision(div);
    }

    /**
     * Attach a division to the given search division named "search-results"
     * which contains results for this search query.
     *
     * @param div The division to contain the results division.
     * @throws java.io.IOException passed through.
     * @throws java.sql.SQLException passed through.
     * @throws org.dspace.app.xmlui.wing.WingException passed through.
     * @throws org.dspace.authorize.AuthorizeException passed through.
     */
    protected void buildSearchResultsDivision(Division div)
            throws IOException, SQLException, WingException, AuthorizeException {
        int pageSize = getParameterRpp();
        int page = getParameterPage();
        try {
            Request request = ObjectModelHelper.getRequest(objectModel);
            UUID collectionIdFilter = Util.getUUIDParameter(request, "filter_collection");

            Collection collection = collectionService.find(context, collectionIdFilter);
            int offset = (page - 1) * pageSize;
            List<XmlWorkflowItem> results = xmlWorkflowItemService.findAllInCollection(context, offset, pageSize, collection);
            Para para = div.addPara("result-query", "result-query");

            int hitCount = xmlWorkflowItemService.countAllInCollection(context, collection);
            para.addContent(message("").parameterize("", hitCount));


            div.setHead(T_head);
            this.buildSearchControls(div);

            Division resultsDiv = div.addDivision("search-results", "primary");

            if (hitCount > 0) {
                // Pagination variables.
                int firstItemIndex = ((page - 1) * pageSize) + 1;
                int lastItemIndex = (page - 1) * pageSize + results.size();
                if (hitCount < lastItemIndex) {
                    lastItemIndex = hitCount;
                }
                int pagesTotal = ((hitCount - 1) / pageSize) + 1;
                Map<String, String> parameters = new HashMap<String, String>();
                parameters.put("page", "{pageNum}");
                String pageURLMask = generateURL(parameters);

                resultsDiv.setMaskedPagination(hitCount, firstItemIndex,
                        lastItemIndex, page, pagesTotal, pageURLMask);


                // Look for any items in the result set.
                Table table = resultsDiv.addTable("workflow-item-overview-table", results.size() + 1, 5);

                Row headerRow = table.addRow(Row.ROLE_HEADER);
                headerRow.addCellContent(T_search_column1);
                headerRow.addCellContent(T_search_column2);
                headerRow.addCellContent(T_search_column3);
                headerRow.addCellContent(T_search_column4);
                headerRow.addCellContent(T_search_column5);


                for (XmlWorkflowItem wfi : results) {
                    Item item = wfi.getItem();
                    Row itemRow = table.addRow();

                    java.util.List<PoolTask> pooltasks = poolTaskService.find(context,wfi);
                    java.util.List<ClaimedTask> claimedtasks = claimedTaskService.find(context, wfi);

                    Message state = message("xmlui.XMLWorkflow.step.unknown");
                    for(PoolTask task: pooltasks){
                        Workflow wf = workflowFactory.getWorkflow(wfi.getCollection());
                        Step step = wf.getStep(task.getStepID());
                        state = message("xmlui.XMLWorkflow." + wf.getID() + "." + step.getId());
                    }
                    for(ClaimedTask task: claimedtasks){
                        Workflow wf = workflowFactory.getWorkflow(wfi.getCollection());
                        Step step = wf.getStep(task.getStepID());
                        state = message("xmlui.XMLWorkflow." + wf.getID() + "." + step.getId());
                    }


                    //Column 0 task Checkbox to delete
                    itemRow.addCell().addCheckBox("workflow_id").addOption(wfi.getID());

                    //Column 1 task Step
                    itemRow.addCellContent(state);
                    //Column 2 Item name
                    itemRow.addCell().addXref(request.getContextPath() + "/admin/display-workflowItem?wfiId=" +wfi.getID(), item.getName() );
                    //Column 3 collection
                    itemRow.addCell().addXref(handleService.resolveToURL(context, wfi.getCollection().getHandle()), wfi.getCollection().getName());
                    //Column 4 submitter
                    itemRow.addCell().addXref("mailto:" + wfi.getSubmitter().getEmail(), wfi.getSubmitter().getFullName());
                }

                Para buttonsPara = resultsDiv.addPara();
                buttonsPara.addButton("submit_submitter").setValue(T_button_back_to_submitter);
                buttonsPara.addButton("submit_delete").setValue(T_button_delete);

            } else {
                resultsDiv.addPara(T_no_results);
            }
        } catch (WorkflowConfigurationException e) {
            log.error(LogManager.getHeader(context, "Error while displaying the admin workflow overview page", ""), e);
        }
    }


    protected int getParameterPage() {
        try {
            return Integer.parseInt(ObjectModelHelper.getRequest(objectModel).getParameter("page"));
        }
        catch (Exception e) {
            return 1;
        }
    }

    protected int getParameterRpp() {
        try {
            return Integer.parseInt(ObjectModelHelper.getRequest(objectModel).getParameter("rpp"));
        }
        catch (Exception e) {
            return 10;
        }
    }

    protected int getParameterSortBy() {
        try {
            return Integer.parseInt(ObjectModelHelper.getRequest(objectModel).getParameter("sort_by"));
        }
        catch (Exception e) {
            return 0;
        }
    }

    protected String getParameterOrder() {
        String s = ObjectModelHelper.getRequest(objectModel).getParameter("order");
        return s != null ? s : "DESC";
    }

    /**
     * Generate a URL to the simple search url.
     * @param parameters search parameters.
     * @return the URL.
     * @throws org.dspace.app.xmlui.utils.UIException passed through.
     */
    protected String generateURL(Map<String, String> parameters)
            throws UIException {
        if (parameters.get("page") == null) {
            parameters.put("page", String.valueOf(getParameterPage()));
        }

        if (parameters.get("rpp") == null) {
            parameters.put("rpp", String.valueOf(getParameterRpp()));
        }
        if(parameters.get("filter_collection")== null) {
            parameters.put("filter_collection", ObjectModelHelper.getRequest(objectModel).getParameter("filter_collection"));
        }

        return super.generateURL("xmlworkflowoverview", parameters);
    }

    protected void buildSearchControls(Division div)
            throws WingException, SQLException {

        Request request = ObjectModelHelper.getRequest(objectModel);
        Table controlsTable = div.addTable("search-controls", 1, 3);
        Row controlsRow = controlsTable.addRow(Row.ROLE_DATA);

        // Create a control for the number of records to display
        Cell rppCell = controlsRow.addCell();
        rppCell.addContent("pagesize");
        Select rppSelect = rppCell.addSelect("rpp");
        for (int i : RESULTS_PER_PAGE_PROGRESSION) {
            rppSelect.addOption((i == getParameterRpp()), i, Integer.toString(i));
        }

        Cell filterCell = controlsRow.addCell();

        // Create a drop down of the different sort columns available
        UUID selectedCollectionId = Util.getUUIDParameter(request, "filter_collection");
        filterCell.addContent("Collection filter:");
        Select sortSelect = filterCell.addSelect("filter_collection");
        sortSelect.addOption(null== selectedCollectionId,-1, "None");
        List<Collection> collections = collectionService.findAll(context);
        for (Collection collection : collections) {
            sortSelect.addOption(collection.getID().equals(selectedCollectionId), collection.getID().toString(), collection.getName());
        }

        controlsRow.addCell().addButton("submit_search_controls").setValue(T_go);
    }

}
