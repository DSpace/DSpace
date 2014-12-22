/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.authority.indexer;

import org.dspace.authority.AuthorityValue;
import org.apache.log4j.Logger;
import org.dspace.content.Item;
import org.dspace.core.Context;
import org.dspace.kernel.ServiceManager;
import org.dspace.utils.DSpace;

import java.sql.SQLException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 *
 * @author Antoine Snyers (antoine at atmire.com)
 * @author Kevin Van de Velde (kevin at atmire dot com)
 * @author Ben Bosman (ben at atmire dot com)
 * @author Mark Diggory (markd at atmire dot com)
 */
public class AuthorityIndexClient {

    private static Logger log = Logger.getLogger(AuthorityIndexClient.class);

    public static void main(String[] args) throws Exception {

        //Populate our solr
        Context context = new ContextNoCaching();
        //Ensure that we can update items if we are altering our authority control
        context.turnOffAuthorisationSystem();
        ServiceManager serviceManager = getServiceManager();


        AuthorityIndexingService indexingService = serviceManager.getServiceByName(AuthorityIndexingService.class.getName(),AuthorityIndexingService.class);
        List<AuthorityIndexerInterface> indexers = serviceManager.getServicesByType(AuthorityIndexerInterface.class);

        if(!isConfigurationValid(indexingService, indexers)){
                    //Cannot index, configuration not valid
            System.out.println("Cannot index authority values since the configuration isn't valid. Check dspace logs for more information.");

            return;
        }
        
        System.out.println("Retrieving all data");
        log.info("Retrieving all data");

        //Get all our values from the input forms
        Map<String, AuthorityValue> toIndexValues = new HashMap<>();
        for (AuthorityIndexerInterface indexerInterface : indexers) {
            log.info("Initialize " + indexerInterface.getClass().getName());
            System.out.println("Initialize " + indexerInterface.getClass().getName());
            indexerInterface.init(context, true);
            while (indexerInterface.hasMore()) {
                AuthorityValue authorityValue = indexerInterface.nextValue();
                if(authorityValue != null){
                    toIndexValues.put(authorityValue.getId(), authorityValue);
                }
            }
            //Close up
            indexerInterface.close();
        }


        log.info("Cleaning the old index");
        System.out.println("Cleaning the old index");
        indexingService.cleanIndex();
        log.info("Writing new data");
        System.out.println("Writing new data");
        for(String id : toIndexValues.keySet()){
            indexingService.indexContent(toIndexValues.get(id), true);
            indexingService.commit();
        }

        context.commit();
        //In the end commit our server
        indexingService.commit();
        context.abort();
        System.out.println("All done !");
        log.info("All done !");
    }

    public static void indexItem(Context context, Item item){
        ServiceManager serviceManager = getServiceManager();

        AuthorityIndexingService indexingService = serviceManager.getServiceByName(AuthorityIndexingService.class.getName(),AuthorityIndexingService.class);
        List<AuthorityIndexerInterface> indexers = serviceManager.getServicesByType(AuthorityIndexerInterface.class);

        if(!isConfigurationValid(indexingService, indexers)){
            //Cannot index, configuration not valid
            return;
        }

        for (AuthorityIndexerInterface indexerInterface : indexers) {

            indexerInterface.init(context , item);
            while (indexerInterface.hasMore()) {
                AuthorityValue authorityValue = indexerInterface.nextValue();
                if(authorityValue != null)
                    indexingService.indexContent(authorityValue, true);
            }
            //Close up
            indexerInterface.close();
        }
        //Commit to our server
        indexingService.commit();
    }

    private static ServiceManager getServiceManager() {
        //Retrieve our service
        DSpace dspace = new DSpace();
        return dspace.getServiceManager();
    }

    private static class ContextNoCaching extends Context
    {

        public ContextNoCaching() throws SQLException {
            super();
        }

        @Override
        public void cache(Object o, int id) {
            //Do not cache any object
        }
    }

    private static boolean isConfigurationValid(AuthorityIndexingService indexingService, List<AuthorityIndexerInterface> indexers){
        if(!indexingService.isConfiguredProperly()){
            return false;
        }

        for (AuthorityIndexerInterface indexerInterface : indexers) {
            if(!indexerInterface.isConfiguredProperly()){
                return false;
            }
        }
        return true;
    }

}
