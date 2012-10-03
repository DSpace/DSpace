package org.dspace.content.authority;

import com.atmire.authority.SearchService;
import com.atmire.authority.SolrDocumentFields;
import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.response.QueryResponse;
import org.apache.solr.common.SolrDocument;
import org.apache.solr.common.SolrDocumentList;
import org.apache.solr.common.params.CommonParams;
import org.dspace.core.ConfigurationManager;
import org.dspace.utils.DSpace;

import java.util.ArrayList;

/**
 * User: kevin (kevin at atmire.com)
 * Date: 6-dec-2010
 * Time: 13:37:50
 */
public class SolrAuthority implements SolrChoiceAuthority, SolrDocumentFields{

    public Choices getMatches(String field, String text, int collection, int start, int limit, String locale) {
         return getMatches(field, text, collection, start, limit, locale, true);
    }

    @Override
    public Choices getBestMatch(String fieldKey, String query, int collection, String locale) {
        return getMatches(fieldKey, query, collection, 0, 1, locale,false);
    }


    public Choices getMatches(String text, int collection, int start, int limit, String locale, boolean bestMatch) {
         return getMatches(null, text, collection, start, limit, locale, bestMatch);
    }

    public Choices getMatches(String field, String text, int collection, int start, int limit, String locale, boolean bestMatch) {
        if(limit == 0)
            limit = 20;

        SolrQuery queryArgs = new SolrQuery();
        if(text == null || text.trim().equals("")){
            queryArgs.setQuery("*:*");
        }else{
            //queryArgs.setQuery("value:" + text + "* or value:\"" + text +"\"");
            queryArgs.setQuery("full-text:" + text.toLowerCase() + "* or full-text:\"" + text.toLowerCase() +"\"");
        }

        if(field != null)
            queryArgs.addFilterQuery("field:" + field.replace("_","."));

        queryArgs.set(CommonParams.START, start);
        //We add one to our facet limit so that we know if there are more matches
        queryArgs.set(CommonParams.ROWS, limit + 1);

        Choices result;
        try {
            int max = 0;
            boolean hasMore = false;
            QueryResponse searchResponse = getSearchService().search(queryArgs);
            SolrDocumentList authDocs = searchResponse.getResults();
            ArrayList<Choice> choices = new ArrayList<Choice>();
            if(authDocs != null){
                max = (int) searchResponse.getResults().getNumFound();
                int maxDocs = authDocs.size();
                if(limit < maxDocs)
                    maxDocs = limit;
                for (int i = 0; i < maxDocs; i++) {
                    SolrDocument solrDocument = authDocs.get(i);
                    if(solrDocument != null){
                        //choices.add(new Choice(val.getId(), val.getValue(), val.getValue()));
                        choices.add(new Choice((String)solrDocument.get(DOC_ID), (String)solrDocument.get(DOC_VALUE), (String)solrDocument.get(DOC_DISPLAY_VALUE)));
                    }
                }

                hasMore = (authDocs.size() == (limit + 1));
            }


            int confidence;
            if (choices.size() == 0)
                confidence = Choices.CF_NOTFOUND;
            else if (choices.size() == 1)
                confidence = Choices.CF_UNCERTAIN;
            else
                confidence = Choices.CF_AMBIGUOUS;

            result = new Choices(choices.toArray(new Choice[choices.size()]), start, hasMore ? max : choices.size()+start, confidence, hasMore);
        } catch (Exception e) {
            result = new Choices(true);
        }

        return result;  //To change body of implemented methods use File | Settings | File Templates.
    }

    public Choices getMatches(String text, int collection, int start, int limit, String locale) {
        return getMatches(text, collection, start, limit, locale,true);
    }


    public Choices getBestMatch(String text, int collection, String locale) {
        return getMatches(text, collection, 0, 1, locale,false);
    }


    public String getLabel(String field, String key, String locale) {
        return key;
    }

    public String getLabel(String key, String locale) {
        return key;
    }


    private SearchService getSearchService(){
        DSpace dspace = new DSpace();

        org.dspace.kernel.ServiceManager manager = dspace.getServiceManager() ;

        return manager.getServiceByName(SearchService.class.getName(),SearchService.class);
    }
}
