package org.dspace.app.xmlui.aspect.discovery.administrative;

import org.apache.cocoon.environment.Request;
import org.apache.log4j.Logger;
import org.apache.solr.common.SolrDocument;
import org.apache.solr.common.SolrDocumentList;
import org.dspace.app.xmlui.aspect.discovery.BrowseFacet;
import org.dspace.app.xmlui.aspect.discovery.SimpleSearch;
import org.dspace.app.xmlui.utils.ContextUtil;
import org.dspace.app.xmlui.utils.UIException;
import org.dspace.app.xmlui.wing.Message;
import org.dspace.app.xmlui.wing.WingException;
import org.dspace.app.xmlui.wing.element.Division;
import org.dspace.app.xmlui.wing.element.PageMeta;
import org.dspace.app.xmlui.wing.element.ReferenceSet;
import org.dspace.authorize.AuthorizeException;
import org.dspace.authorize.AuthorizeManager;
import org.dspace.content.DSpaceObject;
import org.dspace.content.Item;
import org.dspace.discovery.SearchServiceException;
import org.dspace.discovery.SearchUtils;

import java.io.IOException;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;

/**
 * User: kevin (kevin at atmire.com)
 * Date: 15-nov-2011
 * Time: 8:48:59
 */
public class NonArchivedDiscovery extends SimpleSearch {

    private static final Logger log = Logger.getLogger(NonArchivedDiscovery.class);

    private static final Message T_no_results = message("xmlui.ArtifactBrowser.AbstractSearch.no_results");
    private static final Message T_VIEW_MORE = message("xmlui.discovery.AbstractFiltersTransformer.filters.view-more");
    protected static final Message T_title =
        message("xmlui.Submission.Submissions.title");

    @Override
    public void addPageMeta(PageMeta pageMeta) throws WingException, SQLException, AuthorizeException {
        pageMeta.addMetadata("title").addContent(T_title);

        if(!AuthorizeManager.isAdmin(ContextUtil.obtainContext(objectModel))){
            throw new AuthorizeException();
        }
    }

    @Override
    protected void buildSearchResultsDivision(Division search) throws IOException, SQLException, WingException, SearchServiceException {
        try {
            if (queryResults == null || queryResults.getResults() == null) {

                DSpaceObject scope = getScope();
                this.performSearch(scope);
            }
        }
        catch (RuntimeException e) {
            log.error(e.getMessage(), e);
            queryResults = null;
        }
        catch (Exception e) {
            log.error(e.getMessage(), e);
            queryResults = null;
        }

//        if (queryResults != null) {
//            search.addPara("result-query", "result-query")
//                    .addContent(T_result_query.parameterize(getQuery(), queryResults.getResults().getNumFound()));
//        }

        Division results = search.addDivision("search-results", "primary");

        DSpaceObject searchScope = getScope();
        if (queryResults != null &&
                queryResults.getResults().getNumFound() > 0) {

            SolrDocumentList solrResults = queryResults.getResults();

            // Pagination variables.
            int itemsTotal = (int) solrResults.getNumFound();
            int firstItemIndex = (int) solrResults.getStart() + 1;
            int lastItemIndex = (int) solrResults.getStart() + solrResults.size();

            //if (itemsTotal < lastItemIndex)
            //    lastItemIndex = itemsTotal;
            int currentPage = (int) (solrResults.getStart() / this.queryArgs.getRows()) + 1;
            int pagesTotal = (int) ((solrResults.getNumFound() - 1) / this.queryArgs.getRows()) + 1;
            Map<String, String> parameters = new HashMap<String, String>();
            parameters.put("page", "{pageNum}");
            String pageURLMask = generateURL(parameters);
            //Check for facet queries ? If we have any add them
            String[] fqs = getParameterFilterQueries();
            if(fqs != null) {
                StringBuilder maskBuilder = new StringBuilder(pageURLMask);
                for (String fq : fqs) {
                    maskBuilder.append("&fq=").append(fq);
                }

                pageURLMask = maskBuilder.toString();
            }

            results.setMaskedPagination(itemsTotal, firstItemIndex,
                    lastItemIndex, currentPage, pagesTotal, pageURLMask);

            ReferenceSet referenceSet = results.addReferenceSet("search-results-repository",
                    ReferenceSet.TYPE_SUMMARY_LIST, null, "repository-search-results");


            for (SolrDocument doc : solrResults) {

                DSpaceObject resultDSO = SearchUtils.findDSpaceObject(context, doc);

                if (resultDSO instanceof Item) {
                    referenceSet.addReference(resultDSO);
                }
            }

        } else {
            results.addPara(T_no_results);
        }
        //}// Empty query
    }

    public void addViewMoreUrl(org.dspace.app.xmlui.wing.element.List facet, DSpaceObject dso, Request request, String fieldName) throws WingException {
        String parameters = retrieveParameters(request);
        facet.addItem().addXref(
                contextPath +
                        (dso == null ? "" : "/handle/" + dso.getHandle()) +
                        "/non-archived-search-filter?" + parameters + BrowseFacet.FACET_FIELD + "=" + fieldName,
                T_VIEW_MORE

        );
    }

    public Message getHead() {
        return T_title;
    }

    @Override
    protected String getDiscoverUrl(){
        return "non-archived-discovery";
    }

    @Override
    public String getView() {
        return "nonarchived";
    }
}
