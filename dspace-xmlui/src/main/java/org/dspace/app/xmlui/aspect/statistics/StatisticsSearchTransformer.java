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
import org.dspace.app.xmlui.utils.HandleUtil;
import org.dspace.app.xmlui.wing.Message;
import org.dspace.app.xmlui.wing.WingException;
import org.dspace.app.xmlui.wing.element.Body;
import org.dspace.app.xmlui.wing.element.Division;
import org.dspace.app.xmlui.wing.element.PageMeta;
import org.dspace.authorize.AuthorizeException;
import org.dspace.content.DSpaceObject;
import org.dspace.statistics.content.DatasetSearchGenerator;
import org.dspace.statistics.content.StatisticsDataSearches;
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
public class StatisticsSearchTransformer extends AbstractStatisticsDataTransformer {

    private static final Message T_dspace_home = message("xmlui.general.dspace_home");
    private static final Message T_search_terms_head = message("xmlui.statistics.StatisticsSearchTransformer.search-terms.head");
    private static final Message T_search_total_head = message("xmlui.statistics.StatisticsSearchTransformer.search-total.head");
    private static final Message T_trail = message("xmlui.statistics.trail-search");
    private static final Message T_head_title = message("xmlui.statistics.search.title");
    private static final Message T_retrieval_error = message("xmlui.statistics.search.error");
    private static final Message T_search_head = message("xmlui.statistics.search.head");
    private static final Message T_search_head_dso = message("xmlui.statistics.search.head-dso");
    private static final Message T_no_results = message("xmlui.statistics.search.no-results");

    /**
     * Add a page title and trail links
     */
    public void addPageMeta(PageMeta pageMeta) throws SAXException, WingException, SQLException, IOException, AuthorizeException {
        //Try to find our dspace object
        DSpaceObject dso = HandleUtil.obtainHandle(objectModel);

        pageMeta.addTrailLink(contextPath + "/",T_dspace_home);

        if(dso != null)
        {
            HandleUtil.buildHandleTrail(context, dso, pageMeta, contextPath);
        }
        pageMeta.addTrailLink(contextPath + (dso != null && dso.getHandle() != null ? "/handle/" + dso.getHandle() : "") + "/search-statistics", T_trail);

        // Add the page title
        pageMeta.addMetadata("title").addContent(T_head_title);
    }


    @Override
    public void addBody(Body body) throws SAXException, WingException, SQLException, IOException, AuthorizeException, ProcessingException {
            //Try to find our dspace object
            DSpaceObject dso = HandleUtil.obtainHandle(objectModel);
            Request request = ObjectModelHelper.getRequest(objectModel);
            String selectedTimeFilter = request.getParameter("time_filter");

            StringBuilder actionPath = new StringBuilder().append(request.getContextPath());
            if(dso != null){
                actionPath.append("/handle/").append(dso.getHandle());
            }
            actionPath.append("/search-statistics");

            Division mainDivision = body.addInteractiveDivision("search-statistics", actionPath.toString(), Division.METHOD_POST, null);
            if(dso != null){
                mainDivision.setHead(T_search_head_dso.parameterize(dso.getName()));
            }else{
                mainDivision.setHead(T_search_head);
            }
        try {
            //Add the time filter box
            Division searchTermsDivision = mainDivision.addDivision("search-terms");
            searchTermsDivision.setHead(T_search_terms_head);
            addTimeFilter(searchTermsDivision);

            //Retrieve the optional time filter
            StatisticsSolrDateFilter dateFilter = getDateFilter(selectedTimeFilter);


            StatisticsTable statisticsTable = new StatisticsTable(new StatisticsDataSearches(dso));

            DatasetSearchGenerator queryGenerator = new DatasetSearchGenerator();
            queryGenerator.setType("query");
            queryGenerator.setMax(10);
            queryGenerator.setMode(DatasetSearchGenerator.Mode.SEARCH_OVERVIEW);
            queryGenerator.setPercentage(true);
            queryGenerator.setRetrievePageViews(true);
            statisticsTable.addDatasetGenerator(queryGenerator);
            if(dateFilter != null){
                statisticsTable.addFilter(dateFilter);
            }

            addDisplayTable(searchTermsDivision, statisticsTable, true, null);


            Division totalDivision = mainDivision.addDivision("search-total");
            totalDivision.setHead(T_search_total_head);
            statisticsTable = new StatisticsTable(new StatisticsDataSearches(dso));

            queryGenerator = new DatasetSearchGenerator();
            queryGenerator.setMode(DatasetSearchGenerator.Mode.SEARCH_OVERVIEW_TOTAL);
            queryGenerator.setPercentage(true);
            queryGenerator.setRetrievePageViews(true);
            statisticsTable.addDatasetGenerator(queryGenerator);
            if(dateFilter != null){
                statisticsTable.addFilter(dateFilter);
            }

            addDisplayTable(totalDivision, statisticsTable, false, null);
        } catch (Exception e) {
            mainDivision.addPara().addContent(T_retrieval_error);
        }
    }

    @Override
    protected Message getNoResultsMessage() {
        return T_no_results;
    }
}
