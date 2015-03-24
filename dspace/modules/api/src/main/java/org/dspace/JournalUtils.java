package org.dspace;

import org.apache.log4j.Logger;
import org.dspace.content.Item;
import org.dspace.content.authority.AuthorityMetadataValue;
import org.dspace.content.authority.Concept;
import org.dspace.content.authority.Scheme;
import org.dspace.core.ConfigurationManager;
import org.dspace.core.Context;

import java.sql.SQLException;
import java.util.List;

/**
 * User: lantian @ atmire . com
 * Date: 9/12/14
 * Time: 12:18 PM
 */
public class JournalUtils {

    private static Logger log = Logger.getLogger(JournalUtils.class);


    public static Concept[] getJournalConcepts(Context context) throws SQLException {
        Scheme scheme = Scheme.findByIdentifier(context, ConfigurationManager.getProperty("solrauthority.searchscheme.prism_publicationName"));
        return scheme.getConcepts();
    }

    public static Concept getJournalConceptById(Context context, String authorityId) throws SQLException {
        try
        {
            return Concept.findByIdentifier(context,authorityId).iterator().next();
        }
        catch(Exception e)
        {
            if(log.isDebugEnabled())
                log.error(e.getMessage(),e);
            else
                log.error(e);
        }

        return null;
    }

    public static Concept getJournalConceptByName(Context context, String journalName) throws SQLException {
        Scheme scheme = Scheme.findByIdentifier(context, ConfigurationManager.getProperty("solrauthority.searchscheme.prism_publicationName"));
        try
        {
            return Concept.findByPreferredLabel(context,journalName,scheme.getID())[0];
        }
        catch(Exception e)
        {
            if(log.isDebugEnabled())
                log.error(e.getMessage(),e);
            else
                log.error(e);
        }

        return null;
    }


    public static Concept getJournalConceptByShortID(Context context, String journalShortID) throws SQLException {
        Scheme scheme = Scheme.findByIdentifier(context, ConfigurationManager.getProperty("solrauthority.searchscheme.prism_publicationName"));
        try
        {
            // TODO : a better query would be in Concept and filter at the db level
            for(Concept concept : scheme.getConcepts())
            {
                String shortId = getJournalShortID(concept);
                if(shortId != null && shortId.equals(journalShortID))
                    return concept;
            }
        }
        catch(Exception e)
        {
            if(log.isDebugEnabled())
                log.error(e.getMessage(),e);
            else
                log.error(e);
        }

        return null;
    }

    public static Concept[] getJournalConcept(Context context,String journal) throws SQLException {
        Scheme scheme = Scheme.findByIdentifier(context, ConfigurationManager.getProperty("solrauthority.searchscheme.prism_publicationName"));

        if(journal==null||journal.length()==0)
            return getJournalConcepts(context);
        return Concept.findByPreferredLabel(context,journal,scheme.getID());

    }

    public static String getIntegrated(Concept concept) {
        AuthorityMetadataValue[] vals = concept.getMetadata("journal","integrated",null, Item.ANY);
        if(vals != null && vals.length > 0)
            return vals[0].value;

        return null;
    }

    public static boolean getBooleanIntegrated(Concept concept) {
        AuthorityMetadataValue[] vals = concept.getMetadata("journal","integrated",null, Item.ANY);
        if(vals != null && vals.length > 0)
            return vals[0].value.toLowerCase().equals("true");

        return false;
    }

    public static String getSubscriptionPaid(Concept concept) {
        AuthorityMetadataValue[] vals = concept.getMetadata("journal","subscriptionPaid",null, Item.ANY);
        if(vals != null && vals.length > 0)
            return vals[0].value;

        return null;
    }

    public static String getMetadataDir(Concept concept) {
        try
        {
            AuthorityMetadataValue[] vals = concept.getMetadata("journal","metadataDir",null, Item.ANY);
            if(vals != null && vals.length > 0)
                return vals[0].value;

        }catch(Exception e)
        {
            log.error(e.getMessage(),e);
        }

        return null;
    }


    public static String getFullName(Concept concept) {
        AuthorityMetadataValue[] vals = concept.getMetadata("journal","fullname",null, Item.ANY);
        if(vals != null && vals.length > 0)
            return vals[0].value;

        return null;
    }

    public static String getJournalShortID(Concept concept) {
        AuthorityMetadataValue[] vals = concept.getMetadata("journal","journalID",null, Item.ANY);
        if(vals != null && vals.length > 0)
            return vals[0].value;

        return null;
    }

    public static String getPublicationBlackout(Concept concept) {
        AuthorityMetadataValue[] vals = concept.getMetadata("journal","publicationBlackout",null, Item.ANY);
        if(vals != null && vals.length > 0)
            return vals[0].value;

        return null;
    }

    public static boolean getBooleanPublicationBlackout(Concept concept) {
        AuthorityMetadataValue[] vals = concept.getMetadata("journal","publicationBlackout",null, Item.ANY);
        if(vals != null && vals.length > 0)
            return vals[0].value.toLowerCase().equals("true");

        return false;
    }
    public static String getNotifyOnReview(Concept concept) {
        AuthorityMetadataValue[] vals = concept.getMetadata("journal","notifyOnReview",null, Item.ANY);
        if(vals != null && vals.length > 0)
            return vals[0].value;

        return null;
    }
    public static String[] getListNotifyOnReview(Concept concept) {
        AuthorityMetadataValue[] vals = concept.getMetadata("journal","notifyOnReview",null, Item.ANY);
        if(vals != null && vals.length > 0)
            return vals[0].value.split(",");

        return null;
    }
    public static String getNotifyOnArchive(Concept concept) {
        AuthorityMetadataValue[] vals = concept.getMetadata("journal","notifyOnArchive",null, Item.ANY);
        if(vals != null && vals.length > 0)
            return vals[0].value;

        return null;
    }

    public static String[] getListNotifyOnArchive(Concept concept) {
        AuthorityMetadataValue[] vals = concept.getMetadata("journal","notifyOnArchive",null, Item.ANY);
        if(vals != null && vals.length > 0)
            return vals[0].value.split(",");

        return null;
    }
    public static String getNotifyWeekly(Concept concept) {
        AuthorityMetadataValue[] vals = concept.getMetadata("journal","notifyWeekly",null, Item.ANY);
        if(vals != null && vals.length > 0)
            return vals[0].value;

        return null;
    }
    public static String[] getListNotifyWeekly(Concept concept) {
        AuthorityMetadataValue[] vals = concept.getMetadata("journal","notifyWeekly",null, Item.ANY);
        if(vals != null && vals.length > 0)
            return vals[0].value.split(",");

        return null;
    }
    public static String getParsingScheme(Concept concept) {
        AuthorityMetadataValue[] vals = concept.getMetadata("journal","parsingScheme",null, Item.ANY);
        if(vals != null && vals.length > 0)
            return vals[0].value;

        return null;
    }


    public static String getEmbargoAllowed(Concept concept) {
        AuthorityMetadataValue[] vals = concept.getMetadata("journal","embargoAllowed",null, Item.ANY);
        if(vals != null && vals.length > 0)
            return vals[0].value;

        return null;
    }
    public static boolean getBooleanEmbargoAllowed(Concept concept) {
        AuthorityMetadataValue[] vals = concept.getMetadata("journal","embargoAllowed",null, Item.ANY);
        if(vals != null && vals.length > 0)
            return vals[0].value.toLowerCase().equals("true");

        return false;
    }

    public static String getAllowReviewWorkflow(Concept concept) {
        AuthorityMetadataValue[] vals = concept.getMetadata("journal","allowReviewWorkflow",null, Item.ANY);
        if(vals != null && vals.length > 0)
            return vals[0].value;

        return null;
    }

    public static boolean getBooleanAllowReviewWorkflow(Concept concept) {
        AuthorityMetadataValue[] vals = concept.getMetadata("journal","allowReviewWorkflow",null, Item.ANY);
        if(vals != null && vals.length > 0)
            return vals[0].value.toLowerCase().equals("true");

        return false;
    }

    public static String getSponsorName(Concept concept) {
        AuthorityMetadataValue[] vals = concept.getMetadata("journal","sponsorName",null, Item.ANY);
        if(vals != null && vals.length > 0)
            return vals[0].value;

        return null;
    }

    public static String getDescription(Concept concept) {
        AuthorityMetadataValue[] vals = concept.getMetadata("journal","description",null, Item.ANY);
        if(vals != null && vals.length > 0)
            return vals[0].value;

        return null;
    }

    public static String getMemberName(Concept concept) {
        AuthorityMetadataValue[] vals = concept.getMetadata("journal","memberName",null, Item.ANY);
        if(vals != null && vals.length > 0)
            return vals[0].value;

        return null;
    }

    /**
     * Replaces escaped characters with their original representations
     * @param escaped a filename that has been escaped by
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
