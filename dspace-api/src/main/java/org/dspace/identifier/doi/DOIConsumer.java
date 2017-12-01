/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.identifier.doi;

import java.util.HashSet;
import java.util.Set;
import org.apache.log4j.Logger;
import org.dspace.content.DSpaceObject;
import org.dspace.content.Item;
import org.dspace.core.Constants;
import org.dspace.core.Context;
import org.dspace.event.Consumer;
import org.dspace.event.Event;
import org.dspace.identifier.DOIIdentifierProvider;
import org.dspace.identifier.IdentifierException;
import org.dspace.identifier.IdentifierNotFoundException;
import org.dspace.search.SearchConsumer;
import org.dspace.utils.DSpace;

/**
 *
 * @author Pascal-Nicolas Becker (p dot becker at tu hyphen berlin dot de)
 */
public class DOIConsumer implements Consumer
{
    /** log4j logger */
    private static Logger log = Logger.getLogger(DOIConsumer.class);

    @Override
    public void initialize() throws Exception {
        // nothing to do
        // we can ask spring to give as a properly setuped instance of
        // DOIIdentifierProvider. Doing so we don't have to configure it and
        // can load it in consume method as this is not very expensive.
        
    }
    
    // as we use asynchronous metadata update, our updates are not very expensive.
    // so we can do everything in the consume method.
    @Override
    public void consume(Context ctx, Event event) throws Exception {
        if (event.getSubjectType() != Constants.ITEM)
        {
            log.warn("DOIConsumer should not have been given this kind of "
                    + "subject in an event, skipping: " + event.toString());
            return;
        }
        if (Event.MODIFY_METADATA != event.getEventType())
        {
            log.warn("DOIConsumer should not have been given this kind of "
                    + "event type, skipping: " + event.toString());
            return;
        }
        
        DSpaceObject dso = event.getSubject(ctx);
        //FIXME
        if (!(dso instanceof Item))
        {
            log.warn("DOIConsumer got an event whose subject was not an item, "
                    + "skipping: " + event.toString());
        }
        Item item = (Item) dso;
        
        DOIIdentifierProvider provider = new DSpace().getSingletonService(
                DOIIdentifierProvider.class);
        
        String doi = null;
        try {
            doi = provider.lookup(ctx, dso);
        }
        catch (IdentifierNotFoundException ex)
        {
            log.warn("DOIConsumer cannot handles items without DOIs, skipping: "
                    + event.toString());
        }
        try
        {
            provider.updateMetadata(ctx, dso, doi);
        }
        catch (IllegalArgumentException ex)
        {
            // should not happen, as we got the DOI from the DOIProvider
            log.warn("DOIConsumer caught an IdentifierException.", ex);
        }
        catch (IdentifierException ex)
        {
            log.warn("DOIConsumer cannot update metadata for Item with ID "
                    + item.getID() + " and DOI " + doi + ".", ex);
        }
     }

    @Override
    public void end(Context ctx) throws Exception {


    }

    @Override
    public void finish(Context ctx) throws Exception {
        // nothing to do
    }
    
}
