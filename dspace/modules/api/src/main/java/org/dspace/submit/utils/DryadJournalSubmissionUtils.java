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
import org.dspace.workflow.WorkflowItem;

import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.io.IOException;
import java.sql.SQLException;
import java.util.*;

/**
 * Created by IntelliJ IDEA.
 * User: fabio.bolognesi
 * Date: 9/7/11
 * Time: 9:47 AM
 * To change this template use File | Settings | File Templates.
 */
public class DryadJournalSubmissionUtils {
    private static Logger log = Logger.getLogger(DryadJournalSubmissionUtils.class);

    // Reading DryadJournalSubmission.properties
    public static final String FULLNAME = "fullname";
    public static final String METADATADIR = "metadataDir";
    public static final String INTEGRATED = "integrated";
    public static final String PUBLICATION_BLACKOUT = "publicationBlackout";
    public static final String NOTIFY_ON_REVIEW = "notifyOnReview";
    public static final String NOTIFY_ON_ARCHIVE = "notifyOnArchive";
    public static final String JOURNAL_ID = "journalID";
    public static final String SUBSCRIPTION_PAID = "subscriptionPaid";
    public static final String journalPropFile =  ConfigurationManager.getProperty("solrauthority.searchscheme.prism_publicationName");
    public enum RecommendedBlackoutAction {
        BLACKOUT_TRUE
        , BLACKOUT_FALSE
        , JOURNAL_NOT_INTEGRATED
    }

    public static final java.util.Map<String, Map<String, String>> journalProperties = new HashMap<String, Map<String, String>>();
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
                        journalProperties.put(key, map);
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
        Map<String, String> journalProperties = new HashMap<String, String>();


            try {
                Scheme scheme = Scheme.findByIdentifier(c, ConfigurationManager.getProperty("solrauthority.searchscheme.prism_publicationName"));
                Concept[] concepts = Concept.findByPreferredLabel(c,journal,scheme.getID());
                //todo:add the journal order
                Concept concept = concepts[0];

                    String key = concept.getPreferredLabel();
                    ArrayList<AuthorityMetadataValue> metadataValues = concept.getMetadata();
                    Map<String, String> map = new HashMap<String, String>();
                    for(AuthorityMetadataValue metadataValue : metadataValues){

                        if(metadataValue.qualifier!=null){
                            journalProperties.put(metadataValue.element+'.'+metadataValue.qualifier,metadataValue.value);
                        }
                        else
                        {
                            journalProperties.put(metadataValue.element,metadataValue.value);
                        }

                    }

            }catch (Exception e) {
                log.error("Error while loading journal properties", e);
            }
        return journalProperties;

    }

    public static Boolean shouldEnterBlackoutByDefault(Context context, Item item, Collection collection) throws SQLException {
        RecommendedBlackoutAction action = recommendedBlackoutAction(context, item, collection);
        return (action == RecommendedBlackoutAction.BLACKOUT_TRUE ||
                action == RecommendedBlackoutAction.JOURNAL_NOT_INTEGRATED);
    }

    public static RecommendedBlackoutAction recommendedBlackoutAction(Context context, Item item, Collection collection) throws SQLException {
        // get Journal
        Item dataPackage=item;
        if(!isDataPackage(collection))
            dataPackage = DryadWorkflowUtils.getDataPackage(context, item);
        DCValue[] journalFullNames = dataPackage.getMetadata("prism.publicationName");
        String journalFullName=null;
        if(journalFullNames!=null && journalFullNames.length > 0){
            journalFullName=journalFullNames[0].value;
        }

        Map<String, String> values = journalProperties.get(journalFullName);
        // Ignore blackout setting if journal is not (yet) integrated
        // get journal's blackout setting
        // journal is blacked out if its blackout setting is true or if it has no setting
        String isIntegrated = null;
        String isBlackedOut = null;
        if(values!=null && values.size()>0) {
            isIntegrated = values.get(INTEGRATED);
            isBlackedOut = values.get(PUBLICATION_BLACKOUT);
        }

        if(isIntegrated == null || isIntegrated.equals("false")) {
            // journal is not integrated.  Enter blackout by default
            return RecommendedBlackoutAction.JOURNAL_NOT_INTEGRATED;
        } else if(isBlackedOut==null || isBlackedOut.equals("true")) {
            // journal has a blackout setting and it's set to true
            return RecommendedBlackoutAction.BLACKOUT_TRUE;
        } else {
            // journal is integrated but blackout setting is false or missing
            return RecommendedBlackoutAction.BLACKOUT_FALSE;
        }
    }


    private static boolean isDataPackage(Collection coll) throws SQLException {
        return coll.getHandle().equals(ConfigurationManager.getProperty("submit.publications.collection"));
    }


    public static String findKeyByFullname(String fullname){
        Map<String, String> props = journalProperties.get(fullname);
        if(props!=null)
            return props.get(DryadJournalSubmissionUtils.JOURNAL_ID);

        return null;
    }


    public static Map<String, String> getPropertiesByJournal(String key){
        return journalProperties.get(key);
    }
    /**
     * Replaces invalid filename characters by percent-escaping.  Based on
     * http://stackoverflow.com/questions/1184176/how-can-i-safely-encode-a-string-in-java-to-use-as-a-filename
     *
     * @param filename A filename to escape
     * @return The filename, with special characters escaped with percent
     */
    public static String escapeFilename(String filename) {
        final char fileSep = System.getProperty("file.separator").charAt(0); // e.g. '/'
        final char escape = '%';
        int len = filename.length();
        StringBuilder sb = new StringBuilder(len);
        for (int i = 0; i < len; i++) {
            char ch = filename.charAt(i);
            if (ch < ' ' || ch >= 0x7F || ch == fileSep
                    || (ch == '.' && i == 0) // we don't want to collide with "." or ".."!
                    || ch == escape) {
                sb.append(escape);
                if (ch < 0x10) {
                    sb.append('0'); // Leading zero
                }
                sb.append(Integer.toHexString(ch));
            } else {
                sb.append(ch);
            }
        }
        return sb.toString();
    }

    /**
     * Replaces escaped characters with their original representations
     * @param escaped a filename that has been escaped by
     * {@link #escapeFilename(java.lang.String) }
     * @return The original string, after unescaping
     */
    public static String unescapeFilename(String escaped) {
        StringBuilder sb = new StringBuilder();
        int i;
        while ((i = escaped.indexOf("%")) >= 0) {
            sb.append(escaped.substring(0, i));
            sb.append((char) Integer.parseInt(escaped.substring(i + 1, i + 3), 16));
            escaped = escaped.substring(i + 3);
        }
        sb.append(escaped);
        return sb.toString();
    }

}
