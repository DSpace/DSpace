/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.content.authority;

import org.apache.log4j.Logger;
import org.dspace.content.DCValue;
import org.dspace.content.Item;
import org.dspace.core.ConfigurationManager;
import org.dspace.core.Constants;
import org.dspace.core.Context;
import org.dspace.event.Consumer;
import org.dspace.event.Event;
import org.dspace.utils.DSpace;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Consumer for updating Editable Authority when DSpace Objects change.
 *
 * @author Mark Diggory (markd at atmire dot com)
 * @author Lantian Gai (lantian at atmire dot com)
 */
public class AuthorityItemConsumer implements Consumer {
    /**
     * log4j logger
     */
    private static Logger log = Logger.getLogger(AuthorityItemConsumer.class);

    // collect Items, Collections, Communities that need indexing
    private Set<Item> itemsToUpdate=null;

    // handles to delete since IDs are not useful by now.
    private Set<String> handlesToDelete = null;

    public void initialize() throws Exception {
        // No-op

    }
    DSpace dspace = new DSpace();
    EditableAuthorityIndexingService indexer = dspace.getServiceManager().getServiceByName(EditableAuthorityIndexingService.class.getName(),EditableAuthorityIndexingService.class);

    public void finish(Context ctx) throws Exception {
        // No-op
    }


    public void consume(Context ctx, Event event) throws Exception {
        int st = event.getSubjectType();
        int et = event.getEventType();

        if(itemsToUpdate==null)
        {
            itemsToUpdate = new HashSet<Item>();
        }
        try {

            switch (st) {
                case Constants.ITEM: {
                    if (et == Event.MODIFY_METADATA||et == Event.INSTALL) {
                        Item item = (Item) event.getSubject(ctx);
                        if(item.isArchived()){
                            itemsToUpdate.add(item);
                        }
                    }
                    break;
                }
            }
        }
        catch (Exception e) {
            ctx.abort();
        }

    }

    private void addAuthority(Item item){

        try{

            //solrauthority.searchfieldtype = full-text
            //solrauthority.searchscheme.prism_publicationName = Journal
            //solrauthority.sortfieldtype = display-value

            if (item.isArchived())
            {
                List<String> keys = new ArrayList<String>();

                for(DCValue dcValue : item.getMetadata(Item.ANY, Item.ANY, Item.ANY, Item.ANY)){

                    String key = ChoiceAuthorityManager.makeFieldKey(dcValue.schema,dcValue.element,dcValue.qualifier);
                    String schemeId = ConfigurationManager.getProperty("solrauthority.searchscheme." + key);

                    if(schemeId != null)
                        keys.add(dcValue.schema + "." + dcValue.element + (dcValue.qualifier != null ? "." + dcValue.qualifier : ""));

                }

                Context context = new Context();
                context.turnOffAuthorisationSystem();

                for(String key : keys)
                {
                    DCValue[] vals = item.getMetadata(key);

                    if(vals != null && vals.length > 0 && vals[0] != null)
                    {
                        item.clearMetadata(vals[0].schema, vals[0].element, vals[0].qualifier,Item.ANY);

                        for(DCValue dcValue : vals){

                            String schemeId = ConfigurationManager.getProperty("solrauthority.searchscheme." + ChoiceAuthorityManager.makeFieldKey(dcValue.schema,dcValue.element,dcValue.qualifier));

                            if(schemeId != null)
                            {
                                Scheme scheme = Scheme.findByIdentifier(context, schemeId);

                                if (scheme!=null) {

                                    Concept newConcept = null;

                                    if(dcValue.authority != null)
                                    {
                                        List<Concept> newConcepts = Concept.findByIdentifier(context, dcValue.authority);
                                        if(newConcepts != null && newConcepts.size() > 0&&newConcepts.get(0).getPreferredLabel().equals(dcValue.value))
                                            newConcept = newConcepts.get(0);
                                    }
                                    else
                                    {
                                        log.info("item:"+item.getHandle()+" has a unsaved concept :"+dcValue.value);
                                    }

                                    if(newConcept == null)
                                    {
                                        Concept newConcepts[] = Concept.findByPreferredLabel(context,dcValue.value,scheme.getID());
                                        if(newConcepts!=null && newConcepts.length>0){
                                            newConcept = newConcepts[0];
                                        }
                                    }

                                    if(newConcept==null){
                                        newConcept = scheme.createConcept(context);
                                        newConcept.setStatus(context, Concept.Status.ACCEPTED.name());
                                        Term term = newConcept.createTerm(context, dcValue.value,Term.prefer_term);
                                        context.commit();
                                    }

                                    item.addMetadata(dcValue.schema,
                                            dcValue.element,
                                            dcValue.qualifier,
                                            dcValue.language,
                                            dcValue.value,
                                            newConcept.getIdentifier(),
                                            Choices.CF_ACCEPTED);


                                }

                            }
                        }
                    }
                }

                context.complete();
            }

        }catch (Exception e)
        {
            log.error(e.getMessage());
        }

    }



    public void end(Context ctx) throws Exception {
        try{
            if(itemsToUpdate!=null&&itemsToUpdate.size()>0)
            {
                ctx.turnOffAuthorisationSystem();
                for(Item item : itemsToUpdate)
                {
                    addAuthority(item);
                    item.update();
                }

                ctx.getDBConnection().commit();
                ctx.restoreAuthSystemState();
            }


        }catch (Exception e)
        {
            log.error(e.getMessage());
        }
        itemsToUpdate = null;
    }

}