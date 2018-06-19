/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */

package org.dspace.authority.indexer;

import org.apache.log4j.Logger;
import org.dspace.authority.factory.AuthorityServiceFactory;
import org.dspace.authority.service.AuthorityService;
import org.dspace.content.DSpaceObject;
import org.dspace.content.Item;
import org.dspace.content.factory.ContentServiceFactory;
import org.dspace.content.service.ItemService;
import org.dspace.core.Context;
import org.dspace.event.Consumer;
import org.dspace.event.Event;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

/**
 * Consumer that takes care of the indexing of authority controlled metadata fields for installed/updated items
 *
 * @author Antoine Snyers (antoine at atmire.com)
 * @author Kevin Van de Velde (kevin at atmire dot com)
 * @author Ben Bosman (ben at atmire dot com)
 * @author Mark Diggory (markd at atmire dot com)
 */
public class AuthorityConsumer implements Consumer {

    private final Logger log = Logger.getLogger(AuthorityConsumer.class);

    /** A set of all item IDs installed which need their authority updated **/
    protected Set<UUID> itemsToUpdateAuthority = null;

    /** A set of item IDs who's metadata needs to be reindexed **/
    protected Set<UUID> itemsToReindex = null;

    protected ItemService itemService;

    protected AuthorityService authorityService;

    @Override
    public void initialize() throws Exception {
        authorityService = AuthorityServiceFactory.getInstance().getAuthorityService();
        itemService = ContentServiceFactory.getInstance().getItemService();

    }

    @Override
    public void consume(Context ctx, Event event) throws Exception {
        if(itemsToUpdateAuthority == null){
            itemsToUpdateAuthority = new HashSet<>();
            itemsToReindex = new HashSet<>();
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

    @Override
    public void end(Context ctx) throws Exception {
        if(itemsToUpdateAuthority == null)
            return;

        try{
            ctx.turnOffAuthorisationSystem();
            for (UUID id : itemsToUpdateAuthority) {
                Item item = itemService.find(ctx, id);
                authorityService.indexItem(ctx, item);
            }
            //Loop over our items which need to be re indexed
            for (UUID id : itemsToReindex) {
                Item item = itemService.find(ctx, id);
                authorityService.indexItem(ctx, item);

            }
        } catch (Exception e){
            log.error("Error while consuming the authority consumer", e);

        } finally {
            itemsToUpdateAuthority = null;
            itemsToReindex = null;
            ctx.restoreAuthSystemState();
        }
    }

    @Override
    public void finish(Context ctx) throws Exception {

    }
}
