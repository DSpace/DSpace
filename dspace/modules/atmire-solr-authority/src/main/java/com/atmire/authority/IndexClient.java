package com.atmire.authority;


import com.atmire.authority.indexer.FileIndexer;
import com.atmire.authority.indexer.IndexerInterface;

import com.atmire.authority.indexer.LocalIndexer;
import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.response.QueryResponse;
import org.apache.solr.common.params.FacetParams;
import org.dspace.core.Context;
import org.dspace.kernel.ServiceManager;
import org.dspace.utils.DSpace;
import java.util.Map;


public class IndexClient {

    public static void main(String[] args) throws Exception {
        System.out.println("Indexing started...");

        populateSolr();

        System.out.println("Indexing finished with successful...");
    }


    private static void populateSolr() throws Exception {
        //Populate our solr
        Context context = new Context();

        ServiceManager serviceManager=getServiceManager();
        IndexingService indexingService = serviceManager.getServiceByName(IndexingService.class.getName(),IndexingService.class);

        //clean all
        indexingService.cleanIndex();

        fileIndex(serviceManager, indexingService);

        localIndex(serviceManager, indexingService);

        //In the end commit our server
        indexingService.commit();
        context.abort();
    }

    private static void localIndex(ServiceManager serviceManager, IndexingService indexingService) throws Exception {
        IndexerInterface indexerInterface = serviceManager.getServiceByName(null, LocalIndexer.class);


        // clean local authority
        indexingService.cleanIndex(indexerInterface.getSource());
        indexerInterface.init();
        while (indexerInterface.hasMore()) {
            Map<String, String> values = indexerInterface.nextValue();

            System.out.println("");

            if(values != null && values.size() > 0){
                System.out.println("id: " + values.get("id") + " - value: " + values.get("value"));
                indexingService.indexContent(values, true);
            }
        }

        //Close up
        indexerInterface.close();
    }


    private static void fileIndex(ServiceManager serviceManager, IndexingService indexingService) throws Exception {
        IndexerInterface indexerInterface = serviceManager.getServiceByName(null, FileIndexer.class);

        // clean local authority
        indexingService.cleanIndex(indexerInterface.getSource());
        indexerInterface.init();
        while (indexerInterface.hasMore()) {
            Map<String, String> values = indexerInterface.nextValue();

            System.out.println("");

            if(values != null && values.size() > 0){
                System.out.println("INDEXED ==> id: " + values.get("id") + " - value: " + values.get("value"));
                indexingService.indexContent(values, true);
            }
        }

        //Close up
        indexerInterface.close();
    }

    private static boolean isDocumentAlreadyPresent(ServiceManager serviceManager, Map<String, String> values) throws Exception
    {
        SolrQuery query = new SolrQuery();
        query.setQuery("duplicate");
        query.setFacetLimit(1);
        query.setFacetMinCount(1);
        query.setFacet(true);
        query.setParam("value", values.get("value"));

        SearchService searchService = serviceManager.getServiceByName(SearchService.class.getName(),SearchService.class);
        QueryResponse response = searchService.search(query);
        if(response!=null && response.getResults().size() > 0){
            return true;
        }
        return false;
    }

    private static ServiceManager getServiceManager(){
        //Retrieve our service
        DSpace dspace = new DSpace();
        ServiceManager serviceManager = dspace.getServiceManager();
        return serviceManager;
    }
}
