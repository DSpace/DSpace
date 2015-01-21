/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */

package org.dspace.authority.indexer;

import org.apache.log4j.Logger;
import org.dspace.content.DSpaceObject;
import org.dspace.content.Item;
import org.dspace.core.Context;
import org.dspace.event.Consumer;
import org.dspace.event.Event;

import java.util.HashSet;
import java.util.Set;

/**
 * Consumer that takes care of the indexing of authority controlled metadata fields for installed/updated items
 *
 * @author Antoine Snyers (antoine at atmire.com)
 * @author Kevin Van de Velde (kevin at atmire dot com)
 * @author Ben Bosman (ben at atmire dot com)
 * @author Mark Diggory (markd at atmire dot com)
 */
public class AuthorityConsumer implements Consumer {

    private static final Logger log = Logger.getLogger(AuthorityConsumer.class);

    /** A set of all item IDs installed which need their authority updated **/
    private Set<Integer> itemsToUpdateAuthority = null;

    /** A set of item IDs who's metadata needs to be reindexed **/
    private Set<Integer> itemsToReindex = null;

    public void initialize() throws Exception {

    }

    public void consume(Context ctx, Event event) throws Exception {
        if(itemsToUpdateAuthority == null){
            itemsToUpdateAuthority = new HashSet<Integer>();
            itemsToReindex = new HashSet<Integer>();
        }

        DSpaceObject dso = event.getSubject(ctx);
        if(dso instanceof Item){
            Item item = (Item) dso;
            if(item.isArchived()){
                if(!itemsToReindex.contains(item.getID()))
                    itemsToReindex.add(item.getID());
            }

            if(("ARCHIVED: " + true).equals(event.getDetail())){
                itemsToUpdateAuthority.add(item.getID());
            }

        }
    }

    public void end(Context ctx) throws Exception {
        if(itemsToUpdateAuthority == null)
            return;

        try{
            ctx.turnOffAuthorisationSystem();
            for (Integer id : itemsToUpdateAuthority) {
                Item item = Item.find(ctx, id);
                AuthorityIndexClient.indexItem(ctx, item);
            }
            //Loop over our items which need to be re indexed
            for (Integer id : itemsToReindex) {
                Item item = Item.find(ctx, id);
                AuthorityIndexClient.indexItem(ctx, item);
                //Commit our DB connection in case new UUID were generated.
                ctx.getDBConnection().commit();

            }
        } catch (Exception e){
            log.error("Error while consuming the authority consumer", e);

        } finally {
            itemsToUpdateAuthority = null;
            itemsToReindex = null;
            ctx.restoreAuthSystemState();
        }
    }

    public void finish(Context ctx) throws Exception {

    }
}