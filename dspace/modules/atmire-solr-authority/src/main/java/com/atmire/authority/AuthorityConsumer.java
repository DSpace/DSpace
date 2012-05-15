package com.atmire.authority;

import com.atmire.authority.indexer.IndexerInterface;
import com.atmire.authority.indexer.LocalIndexer;
import org.apache.log4j.Logger;
import org.dspace.content.DCValue;
import org.dspace.content.DSpaceObject;
import org.dspace.content.Item;
import org.dspace.core.Constants;
import org.dspace.core.Context;
import org.dspace.event.Consumer;
import org.dspace.event.Event;
import org.dspace.kernel.ServiceManager;
import org.dspace.utils.DSpace;

import java.sql.SQLException;
import java.util.Map;
import java.util.Set;

/**
 * Created by IntelliJ IDEA.
 * User: fabio.bolognesi
 * Date: 5/15/12
 * Time: 12:39 PM
 * To change this template use File | Settings | File Templates.
 */
public class AuthorityConsumer implements Consumer {
    /**
     * log4j logger
     */
    private static Logger log = Logger.getLogger(AuthorityConsumer.class);

    // collect Items, Collections, Communities that need indexing
    private Set<DSpaceObject> objectsToUpdate = null;

    // handles to delete since IDs are not useful by now.
    private Set<String> handlesToDelete = null;

    public void initialize() throws Exception {
        // No-op

    }


    public void finish(Context ctx) throws Exception {
        // No-op
    }

    public void end(Context ctx) throws Exception {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    public void consume(Context ctx, Event event) throws Exception {
        int st = event.getSubjectType();
        int et = event.getEventType();

        try {
            ctx = new Context();
            ctx.turnOffAuthorisationSystem();
            switch (st) {

                // Versioning: when the new version is archived, set previousVersion.inArchive=false(if there is one.)
                case Constants.ITEM: {
                    if (et == Event.MODIFY_METADATA) {
                        Item item = (Item) event.getSubject(ctx);
                        addAuthority(item);
                        ctx.commit();   
                    }
                    break;
                }                              
            }
        }
        catch (Exception e) {
            ctx.abort();
        }
        finally {
            ctx.complete();
        }

    }

    private void addAuthority(Item item){
        DCValue[] vals = item.getMetadata("prism.publicationName");
        if (vals.length > 0) {
            String journal = vals[0].value;

            ServiceManager serviceManager=getServiceManager();
            IndexerInterface local = serviceManager.getServiceByName(null, LocalIndexer.class);
            IndexingService solrIndexer = serviceManager.getServiceByName(IndexingService.class.getName(),IndexingService.class);

            Map<String, String>values =  local.createHashMap("prism.publicationName", journal);
            if(values != null && values.size() > 0){
                solrIndexer.indexContent(values, true);
            }
            solrIndexer.commit();
        }
    }

    private static ServiceManager getServiceManager(){
        //Retrieve our service
        DSpace dspace = new DSpace();
        ServiceManager serviceManager = dspace.getServiceManager();
        return serviceManager;
    }




}
