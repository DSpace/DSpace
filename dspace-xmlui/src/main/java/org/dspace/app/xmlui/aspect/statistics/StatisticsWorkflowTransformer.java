/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.xmlui.aspect.statistics;

import org.apache.cocoon.ProcessingException;
import org.apache.cocoon.environment.ObjectModelHelper;
import org.apache.cocoon.environment.Request;
import org.dspace.app.util.Util;
import org.dspace.app.xmlui.utils.HandleUtil;
import org.dspace.app.xmlui.utils.UIException;
import org.dspace.app.xmlui.wing.Message;
import org.dspace.app.xmlui.wing.WingException;
import org.dspace.app.xmlui.wing.element.*;
import org.dspace.authorize.AuthorizeException;
import org.dspace.content.DSpaceObject;
import org.dspace.statistics.content.DatasetTypeGenerator;
import org.dspace.statistics.content.StatisticsDataWorkflow;
import org.dspace.statistics.content.StatisticsTable;
import org.dspace.statistics.content.filter.StatisticsSolrDateFilter;
import org.xml.sax.SAXException;

import java.io.IOException;
import java.sql.SQLException;

/**
 * @author Kevin Van de Velde (kevin at atmire dot com)
 * @author Ben Bosman (ben at atmire dot com)
 * @author Mark Diggory (markd at atmire dot com)
 */
public class StatisticsWorkflowTransformer extends AbstractStatisticsDataTransformer {

    private static final Message T_dspace_home = message("xmlui.general.dspace_home");
    private static final Message T_trail = message("xmlui.statistics.trail-workflow");
    private static final Message T_head_title = message("xmlui.statistics.workflow.title");
    private static final Message T_title = message("xmlui.statistics.workflow.title");
    private static final Message T_retrieval_error = message("xmlui.statistics.workflow.error");
    private static final Message T_no_results = message("xmlui.statistics.workflow.no-results");
    private static final Message T_workflow_head = message("xmlui.statistics.workflow.head");
    private static final Message T_workflow_head_dso = message("xmlui.statistics.workflow.head-dso");

    /**
     * Add a page title and trail links
     */
    public void addPageMeta(PageMeta pageMeta) throws SAXException, WingException, UIException, SQLException, IOException, AuthorizeException {
        //Try to find our dspace object
        DSpaceObject dso = HandleUtil.obtainHandle(objectModel);

        pageMeta.addTrailLink(contextPath + "/",T_dspace_home);

        if(dso != null)
        {
            HandleUtil.buildHandleTrail(context, dso, pageMeta, contextPath);
        }
        pageMeta.addTrailLink(contextPath + (dso != null && dso.getHandle() != null ? "/handle/" + dso.getHandle() : "") + "/workflow-statistics", T_trail);

        // Add the page title
        pageMeta.addMetadata("title").addContent(T_head_title);
    }


    @Override
    public void addBody(Body body) throws SAXException, WingException, SQLException, IOException, AuthorizeException, ProcessingException {
        //Try to find our dspace object
        DSpaceObject dso = HandleUtil.obtainHandle(objectModel);
        Request request = ObjectModelHelper.getRequest(objectModel);
        String selectedTimeFilter = request.getParameter("time_filter");


        StringBuilder actionPath = new StringBuilder().append(contextPath);
        if(dso != null){
            actionPath.append("/handle/").append(dso.getHandle());
        }
        actionPath.append("/workflow-statistics");

        Division mainDivision = body.addInteractiveDivision("workflow-statistics", actionPath.toString(), Division.METHOD_POST, null);
        if(dso != null){
            mainDivision.setHead(T_workflow_head_dso.parameterize(dso.getName()));
        }else{
            mainDivision.setHead(T_workflow_head);
        }
        try {

            //Add the time filter box
            Division workflowTermsDivision = mainDivision.addDivision("workflow-terms");
            workflowTermsDivision.setHead(T_title);
            addTimeFilter(workflowTermsDivision);

            //Retrieve the optional time filter
            StatisticsSolrDateFilter dateFilter = getDateFilter(selectedTimeFilter);



            int time_filter = -1;
            if(request.getParameter("time_filter") != null && !"".equals(request.getParameter("time_filter"))){
                //Our time filter is a negative value if present
                time_filter = Math.abs(Util.getIntParameter(request, "time_filter"));

            }
            StatisticsTable statisticsTable = new StatisticsTable(new StatisticsDataWorkflow(dso, time_filter));

            DatasetTypeGenerator queryGenerator = new DatasetTypeGenerator();
            //Set our type to previousworkflow step (indicates our performed actions !)
            queryGenerator.setType("previousWorkflowStep");
            queryGenerator.setMax(10);
            statisticsTable.addDatasetGenerator(queryGenerator);
            if(dateFilter != null){
                statisticsTable.addFilter(dateFilter);
            }

            addDisplayTable(workflowTermsDivision, statisticsTable, true, new String[]{"xmlui.statistics.display.table.workflow.step."});
        } catch (Exception e) {
            mainDivision.addPara().addContent(T_retrieval_error);

        }
    }

    @Override
    protected Message getNoResultsMessage() {
        return T_no_results;
    }
}
