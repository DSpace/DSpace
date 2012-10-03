package com.atmire.authority;


import com.atmire.authority.indexer.FileIndexer;
import com.atmire.authority.indexer.IndexerInterface;

import com.atmire.authority.indexer.LocalIndexer;
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

        localIndex(serviceManager, indexingService);

        fileIndex(serviceManager, indexingService);

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
                indexingService.indexContent(values, true);
            }
        }

        //Close up
        indexerInterface.close();
    }

    private static ServiceManager getServiceManager(){
         //Retrieve our service
        DSpace dspace = new DSpace();
        ServiceManager serviceManager = dspace.getServiceManager();
        return serviceManager;
    }
}
