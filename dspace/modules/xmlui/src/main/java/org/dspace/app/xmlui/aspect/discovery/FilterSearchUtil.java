package org.dspace.app.xmlui.aspect.discovery;

import org.apache.log4j.Logger;
import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.response.QueryResponse;
import org.dspace.app.xmlui.utils.ContextUtil;
import org.dspace.content.Collection;
import org.dspace.content.Community;
import org.dspace.content.DSpaceObject;
import org.dspace.core.ConfigurationManager;
import org.dspace.core.Constants;
import org.dspace.core.Context;
import org.dspace.core.LogManager;
import org.dspace.discovery.SearchService;
import org.dspace.discovery.SearchUtils;
import org.dspace.utils.DSpace;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Created by IntelliJ IDEA.
 * User: fabio.bolognesi
 * Date: 2/23/12
 * Time: 9:40 AM
 * To change this template use File | Settings | File Templates.
 */
public class FilterSearchUtil {


    private static final Logger log = Logger.getLogger(FilterSearchUtil.class);

    private SolrQuery queryArgs;

    private Context context;

    private QueryResponse queryResults;


    public FilterSearchUtil(Context context) {
        this.context = context;
    }

    public SolrQuery prepareDefaultFilters(String scope, String ...filterQueries) throws SQLException {

         queryArgs = new SolrQuery();

         SearchUtils.SolrFacetConfig[] facets = SearchUtils.getFacetsForType(scope);

         log.info("facets for scope, " + scope + ": " + (facets != null ? facets.length : null));



         //Set the default limit to 10
         int max = 10;
         try{
             max = SearchUtils.getConfig().getInteger("search.facet.max");
         }catch (Exception e){
             //Ignore, only occurs if property isn't set in the config, default will be used then
         }
         //Add one to our facet limit to make sure that if we have more then the shown facets that we show our show more url
         max++;

         if (facets != null){
             queryArgs.setFacetLimit(max);
             queryArgs.setFacetMinCount(1);
             queryArgs.setFacet(true);
         }

         /** enable faceting of search results */
         if (facets != null){
             for (SearchUtils.SolrFacetConfig facet : facets) {


                 if(facet.isDate()){

                     String dateFacet = facet.getFacetField();
                     try{
                         //Get a range query so we can create facet queries ranging from out first to our last date
                         //Attempt to determine our oldest & newest year by checking for previously selected filters
                         int oldestYear = -1;
                         int newestYear = -1;
                         for (String filterQuery : filterQueries) {
                             if(filterQuery.startsWith(facet.getFacetField() + ":")){
                                 //Check for a range
                                 Pattern pattern = Pattern.compile("\\[(.*? TO .*?)\\]");
                                 Matcher matcher = pattern.matcher(filterQuery);
                                 boolean hasPattern = matcher.find();
                                 if(hasPattern){
                                     filterQuery = matcher.group(0);
                                     //We have a range
                                     //Resolve our range to a first & endyear
                                     int tempOldYear = Integer.parseInt(filterQuery.split(" TO ")[0].replace("[", "").trim());
                                     int tempNewYear = Integer.parseInt(filterQuery.split(" TO ")[1].replace("]", "").trim());

                                     //Check if we have a further filter (or a first one found)
                                     if(tempNewYear < newestYear || oldestYear < tempOldYear || newestYear == -1){
                                         oldestYear = tempOldYear;
                                         newestYear = tempNewYear;
                                     }

                                 }else{
                                     if(filterQuery.indexOf(" OR ") != -1){
                                         //Should always be the case
                                         filterQuery = filterQuery.split(" OR ")[0];
                                     }
                                     //We should have a single date
                                     oldestYear = Integer.parseInt(filterQuery.split(":")[1].trim());
                                     newestYear = oldestYear;
                                     //No need to look further
                                     break;
                                 }
                             }
                         }
                         //Check if we have found a range, if not then retrieve our first & last year by using solr
                         if(oldestYear == -1 && newestYear == -1){

                             SolrQuery yearRangeQuery = new SolrQuery();
                             yearRangeQuery.addFilterQuery(SearchUtils.getDefaultFilters(scope));
                             yearRangeQuery.setRows(1);
                             //Set our query to anything that has this value
                             yearRangeQuery.setQuery(facet.getFacetField() + ":[* TO *]");
                             //Set sorting so our last value will appear on top
                             yearRangeQuery.setSortField(dateFacet, SolrQuery.ORDER.asc);
                             yearRangeQuery.addFilterQuery(filterQueries);
                             QueryResponse lastYearResult = getSearchService().search(context, yearRangeQuery);

                             // Exception: this is returning an ArrayList that fails to be cast to an Integer
                             if(lastYearResult.getResults() != null && 0 < lastYearResult.getResults().size() && lastYearResult.getResults().get(0).getFieldValue(dateFacet) != null){
                                 Object obj = lastYearResult.getResults().get(0).get(dateFacet);

                                 if (obj instanceof Integer) {
                                     oldestYear = (Integer) obj;
                                 }
                                 else if (obj instanceof ArrayList) {
                                     oldestYear = (Integer) ((ArrayList<?>) obj).get(0);
                                 }
                                 else {
                                     throw new ClassCastException("Cannot cast " + obj.getClass().getName() + " as Integer");
                                 }
                             }
                             //Now get the first year
                             yearRangeQuery.setSortField(dateFacet, SolrQuery.ORDER.desc);
                             QueryResponse firstYearResult = getSearchService().search(context, yearRangeQuery);

                             if(firstYearResult.getResults() != null && 0 < firstYearResult.getResults().size() && firstYearResult.getResults().get(0).getFieldValue(dateFacet) != null){
                                 Object obj = firstYearResult.getResults().get(0).get(dateFacet);

                                 if (obj instanceof Integer) {
                                     newestYear = (Integer) obj;
                                 }
                                 else if (obj instanceof ArrayList) {
                                     newestYear = (Integer) ((ArrayList<?>) obj).get(0);
                                 }
                                 else {
                                     throw new ClassCastException("Cannot cast " + obj.getClass().getName() + " as Integer");
                                 }
                             }
                             //No values found!
                             if(newestYear == -1 || oldestYear == -1)
                             {
                                 continue;
                             }

                         }

                         int gap = 1;
                         //Attempt to retrieve our gap by the algorithm below
                         int yearDifference = newestYear - oldestYear;
                         if(yearDifference != 0){
                             while (10 < ((double)yearDifference / gap)){
                                 gap *= 10;
                             }
                         }
                         // We need to determine our top year so we can start our count from a clean year
                         // Example: 2001 and a gap from 10 we need the following result: 2010 - 2000 ; 2000 - 1990 hence the top year
                         int topYear = (int) (Math.ceil((float) (newestYear)/gap)*gap);

                         if(gap == 1){
                             //We need a list of our years
                             //We have a date range add faceting for our field
                             //The faceting will automatically be limited to the 10 years in our span due to our filterquery
                             queryArgs.addFacetField(facet.getFacetField());
                         }else{
                             java.util.List<String> facetQueries = new ArrayList<String>();
                             //Create facet queries but limit then to 11 (11 == when we need to show a show more url)
                             for(int year = topYear; year > oldestYear && (facetQueries.size() < max); year-=gap){
                                 //Add a filter to remove the last year only if we aren't the last year
                                 int bottomYear = year - gap;
                                 //Make sure we don't go below our last year found
                                 if(bottomYear < oldestYear)
                                 {
                                     bottomYear = oldestYear;
                                 }

                                 //Also make sure we don't go above our newest year
                                 int currentTop = year;
                                 if((year == topYear))
                                 {
                                     currentTop = newestYear;
                                 }
                                 else
                                 {
                                     //We need to do -1 on this one to get a better result
                                     currentTop--;
                                 }
                                 facetQueries.add(dateFacet + ":[" + bottomYear + " TO " + currentTop + "]");
                             }
                             for (String facetQuery : facetQueries) {
                                 queryArgs.addFacetQuery(facetQuery);
                             }
                         }
                     }catch (Exception e){
                         log.error(LogManager.getHeader(this.context, "Error in discovery while setting up date facet range", "date facet: " + dateFacet), e);
                     }

                 }else{

                     queryArgs.addFacetField(facet.getFacetField());

                 }
             }
         }
         //Add the default filters
         queryArgs.addFilterQuery(SearchUtils.getDefaultFilters(scope));

         return queryArgs;
     }



    public SearchService getSearchService(){
       DSpace dspace = new DSpace();
       org.dspace.kernel.ServiceManager manager = dspace.getServiceManager() ;
       return manager.getServiceByName(SearchService.class.getName(),SearchService.class);
   }



}
