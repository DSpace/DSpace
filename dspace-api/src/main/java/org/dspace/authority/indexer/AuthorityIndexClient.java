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
import org.dspace.authority.factory.AuthorityServiceFactory;
import org.dspace.authority.service.AuthorityService;
import org.dspace.core.Context;

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

    protected static final AuthorityService authorityService = AuthorityServiceFactory.getInstance().getAuthorityService();
    protected static final AuthorityIndexingService indexingService = AuthorityServiceFactory.getInstance().getAuthorityIndexingService();
    protected static final List<AuthorityIndexerInterface> indexers = AuthorityServiceFactory.getInstance().getAuthorityIndexers();

    public static void main(String[] args) throws Exception {

        //Populate our solr
        Context context = new Context();
        //Ensure that we can update items if we are altering our authority control
        context.turnOffAuthorisationSystem();



        if(!authorityService.isConfigurationValid()){
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

        //In the end commit our server
        indexingService.commit();
        context.complete();
        System.out.println("All done !");
        log.info("All done !");
    }
}
