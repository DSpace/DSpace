/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */

package org.dspace.rdf;

import com.hp.hpl.jena.rdf.model.Model;
import java.sql.SQLException;
import java.util.Deque;
import java.util.LinkedList;
import java.util.NoSuchElementException;
import org.apache.log4j.Logger;
import org.dspace.authorize.AuthorizeException;
import org.dspace.content.Bitstream;
import org.dspace.content.Bundle;
import org.dspace.content.DSpaceObject;
import org.dspace.content.Item;
import org.dspace.content.Site;
import org.dspace.content.WorkspaceItem;
import org.dspace.core.Constants;
import org.dspace.core.Context;
import org.dspace.event.Consumer;
import org.dspace.event.Event;

/**
 *
 * @author Pascal-Nicolas Becker (dspace -at- pascal -hyphen- becker -dot- de)
 */
public class RDFConsumer implements Consumer
{
    private static final Logger log = Logger.getLogger(RDFConsumer.class);
    
    protected Deque<DSOIdentifier> toConvert;
    protected Deque<DSOIdentifier> toDelete;

    @Override
    public void consume(Context ctx, Event event)
            throws SQLException
    {
        if (this.toConvert == null)
        {
            log.debug("Initalized first queue.");
            this.toConvert = new LinkedList<>();
        }
        if (this.toDelete == null)
        {
            log.debug("Initalized second queue.");
            this.toDelete = new LinkedList<>();
        }
        
        int sType = event.getSubjectType();
        log.debug(event.getEventTypeAsString() + " for " 
                + event.getSubjectTypeAsString() + ":" + event.getSubjectID());
        switch (sType)
        {
            case (Constants.BITSTREAM) :
            {
                this.consumeBitstream(ctx, event);
            }
            case (Constants.BUNDLE) :
            {
                this.consumeBundles(ctx, event);
                return;
            }
            case (Constants.ITEM) :
            {
                this.consumeCommunityCollectionItem(ctx, event);
                return;
            }
            case (Constants.COLLECTION) :
            {
                this.consumeCommunityCollectionItem(ctx, event);
                return;
            }
            case (Constants.COMMUNITY) :
            {
                this.consumeCommunityCollectionItem(ctx, event);
                return;
            }
            case (Constants.SITE) :
            {
                this.consumeSite(ctx,event);
                return;
            }
            default:
            {
                log.warn("RDFConsumer should not have been given this kind of "
                        + "subject in an event, skipping: " + event.toString());
            }
        }
        
    }
    
    public void consumeBitstream(Context ctx, Event event) throws SQLException
    {
        if (event.getEventType() == Event.MODIFY
                || event.getEventType() == Event.MODIFY_METADATA)
        {
            Bitstream bitstream = Bitstream.find(ctx, event.getSubjectID());
            if (bitstream == null)
            {
                log.debug("Cannot find bitstream " + event.getSubjectID() + "! "
                        + "Ignoring, as it is likely it was deleted "
                        + "and we'll cover it by a REMOVE event on its bundle.");
                return;
            }
            Bundle[] bundles = bitstream.getBundles();
            for (Bundle b : bundles)
            {
                Item[] items = b.getItems();
                for (Item i : items)
                {
                    if (WorkspaceItem.findByItem(ctx, i) != null)
                    {
                        log.debug("Ignoring Item " + i.getID() + " as a corresponding workspace item exists.");
                        continue;
                    }
                    DSOIdentifier id = new DSOIdentifier(i, ctx);
                    if (!this.toDelete.contains(id) && !this.toConvert.contains(id))
                    {
                        this.toConvert.addLast(id);
                    }
                }
                
            }
            return;
        }

        // ignore create and delete event on Bitstreams, as they should be
        // reported as ADD and REMOVE on their bundles as well.
        if (event.getEventType() == Event.CREATE 
                || event.getEventType() == Event.DELETE)
        {
                return;
        }
        
        // Events of type ADD and REMOVE does currently (DSpace 4.1) not exist
        // on a bitstream
        log.warn("Got an unexpected event type (" + event.getEventTypeAsString() 
                + ") for a bitstream. Ignoring.");
    }
    
    public void consumeBundles(Context ctx, Event event) throws SQLException
    {
        if (event.getEventType() == Event.ADD 
                || event.getEventType() == Event.REMOVE
                || event.getEventType() == Event.MODIFY
                || event.getEventType() == Event.MODIFY_METADATA)
        {
            // either a Bitstream was added or removed or the Bundle was changed
            // update its item.
            Bundle bundle = Bundle.find(ctx, event.getSubjectID());
            if (bundle == null)
            {
                log.debug("Cannot find bundle " + event.getSubjectID() + "! "
                        + "Ignoring, as it is likely it was deleted "
                        + "and we'll cover it by a REMOVE event on its item.");
                return;
            }
            Item[] items = bundle.getItems();
            for (Item i : items)
            {
                if (WorkspaceItem.findByItem(ctx, i) != null)
                {
                    log.debug("Ignoring Item " + i.getID() + " as a corresponding workspace item exists.");
                    continue;
                }
                DSOIdentifier id = new DSOIdentifier(i, ctx);
                if (!this.toDelete.contains(id) && !this.toConvert.contains(id))
                {
                    this.toConvert.addLast(id);
                }
            }
        }

        // ignore create and delete event on Bundles, as they should be
        // reported as ADD and REMOVE on their items as well.
        if (event.getEventType() == Event.CREATE
                || event.getEventType() == Event.DELETE)
        {
            return;
        }
        
        log.warn("Got an unexpected event type (" + event.getEventTypeAsString() 
                + ") for a bundle. Ignoring.");
    }
    
    public void consumeCommunityCollectionItem(Context ctx, Event event) throws SQLException
    {
        if (event.getSubjectType() != Constants.COMMUNITY
                && event.getSubjectType() != Constants.COLLECTION
                && event.getSubjectType() != Constants.ITEM)
        {
            log.error("Called on an unexpected Event with subject type " 
                    + event.getSubjectTypeAsString() + " and event type " 
                    + event.getEventTypeAsString() + ", ignoring.");
            return;
        }
        
        if (event.getEventType() == Event.DELETE)
        {
            DSOIdentifier id = new DSOIdentifier(event.getSubjectType(), 
                    event.getSubjectID(), event.getDetail(), event.getIdentifiers());
            
            if (this.toConvert.contains(id))
            {
                this.toConvert.remove(id);
            }
            
            if (!this.toDelete.contains(id))
            {
                this.toDelete.addLast(id);
            }
            return;
        }
        
        if (event.getEventType() == Event.MODIFY
                || event.getEventType() == Event.MODIFY_METADATA
                || event.getEventType() == Event.ADD
                || event.getEventType() == Event.REMOVE
                || event.getEventType() == Event.CREATE)
        {
            // we have to find the dso as the handle is set as detail only
            // if the event type is delete.
            DSpaceObject dso = event.getSubject(ctx);
            if (dso == null)
            {
                log.debug("Cannot find " + event.getSubjectTypeAsString() + " " 
                        + event.getSubjectID() + "! " + "Ignoring, as it is "
                        + "likely it was deleted and we'll cover it by another "
                        + "event with the type REMOVE.");
                return;
            }
            
            // ignore unfinished submissions here. Every unfinished submission
            // has an workspace item. The item flag "in_archive" doesn't help us
            // here as this is also set to false if a newer version was submitted.
            if (dso instanceof Item
                    && WorkspaceItem.findByItem(ctx, (Item) dso) != null)
            {
                log.debug("Ignoring Item " + dso.getID() + " as a corresponding workspace item exists.");
                return;
            }

            DSOIdentifier id = new DSOIdentifier(dso, ctx);
            // If an item gets withdrawn, a MODIFIY event is fired. We have to
            // delete the item from the triple store instead of converting it.
            // we don't have to take care for reinstantions of items as they can
            // be processed as normal modify events.
            if (dso instanceof Item 
                    && event.getDetail() != null
                    && event.getDetail().equals("WITHDRAW"))
            {
                if (this.toConvert.contains(id))
                {
                    this.toConvert.remove(id);
                }
                if (!this.toDelete.contains(id))
                {
                    this.toDelete.add(id);
                    return;
                }
            }

            if (!this.toDelete.contains(id) 
                    && !this.toConvert.contains(id))
            {
                this.toConvert.addLast(id);
            }
        }
    }
    
    public void consumeSite(Context ctx, Event event)
    {
        // in case a top level community was added or remove.
        // event type remove won't be thrown until DS-1966 is fixed (f.e. by
        // merging PR #517).
        if (event.getEventType() == Event.ADD
                || event.getEventType() == Event.REMOVE)
        {
            DSOIdentifier id = new DSOIdentifier(Constants.SITE,
                    Site.SITE_ID, Site.getSiteHandle(), new String[] {Site.getSiteHandle()});
            if (!this.toConvert.contains(id)) this.toConvert.add(id);
            return;
        }
        log.warn("Got an unexpected Event for the SITE. Event type is " 
                + event.getEventTypeAsString() + ", ignoring.");
    }

    @Override
    public void end(Context ctx) throws Exception {
        log.debug("Started processing of queued events.");
        // create a new context, to be sure to work as anonymous user
        // we don't want to store private data in a triplestore with public
        // SPARQL endpoint.
        ctx = new Context(Context.READ_ONLY);
        if (toDelete == null) 
        {
            log.debug("Deletion queue does not exists, creating empty queue.");
            this.toDelete = new LinkedList<>();
        }
        if (toConvert != null)
        {
            log.debug("Starting conversion of DSpaceObjects.");
            while (true)
            {
                DSOIdentifier id;
                try { id = toConvert.removeFirst(); }
                catch (NoSuchElementException ex) { break; }

                if (toDelete.contains(id))
                {
                    log.debug("Skipping " + Constants.typeText[id.type] + " " 
                            + Integer.toString(id.id) + " as it is marked for "
                            + "deletion as well.");
                    continue;
                }
                log.debug("Converting " + Constants.typeText[id.type] + " " 
                            + Integer.toString(id.id) + ".");
                convert(ctx, id);
            }
            log.debug("Conversion ended.");
        }
        log.debug("Starting to delete data from the triple store...");
        while (true)
        {
            DSOIdentifier id;
            try { id = toDelete.removeFirst(); }
            catch (NoSuchElementException ex) { break; }
            
            log.debug("Going to delete data from " +
                    Constants.typeText[id.type] + " " 
                    + Integer.toString(id.id) + ".");
            delete(ctx, id);
        }
        ctx.abort();
        log.debug("Deletion finished.");
    }

    void convert(Context ctx, DSOIdentifier id) throws SQLException
    {
        Model m = null;
        try
        {
            if (id.type == Constants.SITE)
            {
                m = RDFUtil.convertAndStore(ctx, Site.find(ctx, 0));
                return;
            }

            DSpaceObject dso = DSpaceObject.find(ctx, id.type, id.id);
            if (dso == null)
            {
                log.error("Cannot find " + Constants.typeText[id.type] 
                        + " " + id.id + " unexpectedly! Will delete all "
                        + "information about it in the triple store.");
                toDelete.add(id);
                return;
            }
            m = RDFUtil.convertAndStore(ctx, dso);
        }
        catch(AuthorizeException ex)
        {
            log.debug(Constants.typeText[id.type] + " " + 
                    Integer.toString(id.id) + " couldn't be converted: "
                    + "anonymous user doesn't have read permsission. " 
                    + ex.getMessage());
            toDelete.add(id);
        }
        catch (IllegalArgumentException ex)
        {
            log.error("Ignoring an unexpected IllegalArgumentException: " 
                    + ex.getMessage(), ex);
        }
        catch (ItemNotArchivedException ex)
        {
            log.info("Anonymous user cannot read " 
                    + Constants.typeText[id.type] + " " 
                    + Integer.toString(id.id) 
                    + ": deleting it from the triplestore.");
            toDelete.add(id);
        }
        catch (ItemNotDiscoverableException ex)
        {
            log.info("Item " + Integer.toString(id.id) + " is not "
                    + "discoverable: deleting it from the triplestore.");
            toDelete.add(id);
        }
        catch (ItemWithdrawnException ex)
        {
            log.info("Item " + Integer.toString(id.id) + " is withdrawn: "
                    + "deleting it from the triplestore.");
            toDelete.add(id);
        }
        catch (RDFMissingIdentifierException ex)
        {
            log.warn("Cannot convert " + Constants.typeText[id.type] 
                    + " " + Integer.toString(id.id) + ", as no RDF "
                    + "identifier could be generated: "
                    + ex.getMessage(), ex);
        }
        finally
        {
            if (m != null)
            {
                m.close();
            }
        }
    }
    
    void delete(Context context, DSOIdentifier id)
            throws SQLException {
        try
        {
            RDFUtil.delete(context, id.type, id.id, id.handle, id.identifiers);
        }
        catch (RDFMissingIdentifierException ex)
        {
            log.warn("Cannot delete " + Constants.typeText[id.type] + " " 
                    + Integer.toString(id.id) + ": " 
                    + ex.getMessage(), ex);
        }
    }
    
    @Override
    public void finish(Context ctx) throws Exception {
    }

    @Override
    public void initialize() throws Exception {
    }
    
    class DSOIdentifier
    {
        int type;
        int id;
        String handle;
        String[] identifiers;
        
        DSOIdentifier(int type, int id, String handle, String[] identifiers)
        {
            this.type = type;
            this.id = id;
            this.handle = handle;
            this.identifiers = identifiers;
        }
        
        DSOIdentifier(DSpaceObject dso, Context ctx)
        {
            if (dso.getType() != Constants.SITE 
                    && dso.getType() != Constants.COMMUNITY
                    && dso.getType() != Constants.COLLECTION
                    && dso.getType() != Constants.ITEM)
            {
                throw new IllegalArgumentException("Provided DSpaceObject does"
                        + " not have a handle!");
            }
            this.type = dso.getType();
            this.id = dso.getID();
            this.handle = dso.getHandle();
            this.identifiers = dso.getIdentifiers(ctx);
        }
        
        @Override
        public boolean equals(Object o)
        {
            if (!(o instanceof DSOIdentifier)) return false;
            DSOIdentifier dsoId = (DSOIdentifier) o;
            
            /*
            log.warn("Testing if " + Constants.typeText[this.type] + " " 
                    + Integer.toString(this.id) + " and " 
                    + Constants.typeText[dsoId.type] + " " 
                    + Integer.toString(dsoId.id) + " are equal.");
            */
            return (this.type == dsoId.type && this.id == dsoId.id);
        }
        
        @Override
        public int hashCode()
        {
            /* log.debug("Created hash " + Integer.toString(this.type + (10*this.id)));*/

            // as at least up to DSpace version 4.1 DSpaceObjectType is a 
            // one-digit number, this should produce an distinct hash.
            return this.type + (10*this.id);
        }
    }
}