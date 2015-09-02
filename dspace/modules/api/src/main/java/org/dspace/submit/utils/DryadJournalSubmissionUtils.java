package org.dspace.submit.utils;

import org.apache.log4j.Logger;
import org.dspace.content.Collection;
import org.dspace.content.DCValue;
import org.dspace.content.Item;
import org.dspace.content.authority.AuthorityMetadataValue;
import org.dspace.content.authority.Concept;
import org.dspace.content.authority.Scheme;
import org.dspace.core.ConfigurationManager;
import org.dspace.core.Context;
import org.dspace.workflow.DryadWorkflowUtils;
import org.dspace.JournalUtils;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

/**
 * Created by IntelliJ IDEA.
 * User: fabio.bolognesi
 * Date: 9/7/11
 * Time: 9:47 AM
 * To change this template use File | Settings | File Templates.
 */
public class DryadJournalSubmissionUtils {
    private static Logger log = Logger.getLogger(DryadJournalSubmissionUtils.class);

    static{
        Context context = null;

        try {
            context = new Context();
            Scheme scheme = Scheme.findByIdentifier(context, ConfigurationManager.getProperty("solrauthority.searchscheme.prism_publicationName"));
            Concept[] concepts = scheme.getConcepts();
            //todo:add the journal order
            //String journalTypes = properties.getProperty("journal.order");
            for(Concept concept:concepts){
                String key = concept.getPreferredLabel();
                ArrayList<AuthorityMetadataValue> metadataValues = concept.getMetadata();
                Map<String, String> map = new HashMap<String, String>();
                for(AuthorityMetadataValue metadataValue : metadataValues){
                    if(metadataValue.qualifier==null){
                        map.put(metadataValue.element,metadataValue.value);
                    }
                    else
                    {
                        map.put(metadataValue.element+'.'+metadataValue.qualifier,metadataValue.value);
                    }
                    if(key!=null&&key.length()>0){
                        JournalUtils.journalProperties.put(key, map);
                    }
                }
            }
            context.complete();
        }catch (Exception e) {
            if(context!=null)
            {
                context.abort();
            }
            log.error("Error while loading journal properties", e);
        }
    }

    public static Map<String, String> findJournalProperties(Context c, String journal){
        Map<String, String> myJournalProperties = new HashMap<String, String>();


            try {
                String publicationNameProp = ConfigurationManager.getProperty("solrauthority.searchscheme.prism_publicationName");
                Scheme scheme = Scheme.findByIdentifier(c, publicationNameProp);
                int schemeID = scheme.getID();
                Concept[] concepts = Concept.findByPreferredLabel(c,journal, schemeID);
                log.debug("journal lookup: name = " + journal + ", publicationNameProp = " + publicationNameProp + ", ID  = " + schemeID);
                //todo:add the journal order
                Concept concept = concepts[0];

                    String key = concept.getPreferredLabel();
                    ArrayList<AuthorityMetadataValue> metadataValues = concept.getMetadata();
                    Map<String, String> map = new HashMap<String, String>();
                    for(AuthorityMetadataValue metadataValue : metadataValues){

                        if(metadataValue.qualifier!=null){
                            myJournalProperties.put(metadataValue.element + '.' + metadataValue.qualifier, metadataValue.value);
                        }
                        else
                        {
                            myJournalProperties.put(metadataValue.element, metadataValue.value);
                        }

                    }

            }catch (Exception e) {
                log.error("Error while loading journal properties", e);
            }
        return myJournalProperties;

    }

    public static Boolean shouldEnterBlackoutByDefault(Context context, Item item, Collection collection) throws SQLException {
        JournalUtils.RecommendedBlackoutAction action = JournalUtils.recommendedBlackoutAction(context, item, collection);
        return (action == JournalUtils.RecommendedBlackoutAction.BLACKOUT_TRUE ||
                action == JournalUtils.RecommendedBlackoutAction.JOURNAL_NOT_INTEGRATED);
    }



}
