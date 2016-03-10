package org.dspace;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.datadryad.api.DryadJournalConcept;
import org.datadryad.rest.converters.ManuscriptToLegacyXMLConverter;
import org.datadryad.rest.models.Manuscript;
import org.datadryad.rest.models.Organization;
import org.datadryad.rest.storage.StorageException;
import org.datadryad.rest.storage.StoragePath;
import org.datadryad.rest.storage.rdbms.ManuscriptDatabaseStorageImpl;
import org.datadryad.rest.storage.rdbms.OrganizationDatabaseStorageImpl;
import org.dspace.content.Collection;
import org.dspace.content.DCValue;
import org.dspace.content.Item;
import org.dspace.content.MetadataField;
import org.dspace.content.MetadataSchema;
import org.dspace.content.authority.Concept;
import org.dspace.content.authority.Scheme;
import org.dspace.core.ConfigurationManager;
import org.dspace.core.Context;
import org.dspace.workflow.DryadWorkflowUtils;
import org.dspace.authorize.AuthorizeException;

import java.io.File;
import java.lang.*;
import java.lang.Exception;
import java.sql.SQLException;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
/**
 * User: lantian @ atmire . com
 * Date: 9/12/14
 * Time: 12:18 PM
 */
public class JournalUtils {
    private static Logger log = Logger.getLogger(JournalUtils.class);
    public enum RecommendedBlackoutAction {
        BLACKOUT_TRUE,
        BLACKOUT_FALSE,
        JOURNAL_NOT_INTEGRATED
    }
    private static HashMap<String, DryadJournalConcept> journalConceptHashMapByConceptIdentifier = new HashMap<String, DryadJournalConcept>();

    private static HashMap<String, DryadJournalConcept> journalConceptHashMapByJournalID = new HashMap<String, DryadJournalConcept>();
    private static HashMap<String, DryadJournalConcept> journalConceptHashMapByJournalName = new HashMap<String, DryadJournalConcept>();
    private static HashMap<String, DryadJournalConcept> journalConceptHashMapByCustomerID = new HashMap<String, DryadJournalConcept>();

    static {
        Context context = null;

        try {
            context = new Context();
            Scheme scheme = Scheme.findByIdentifier(context, ConfigurationManager.getProperty("solrauthority.searchscheme.prism_publicationName"));
            Concept[] concepts = scheme.getConcepts(context);
            for (Concept concept : concepts) {
                addJournalConcept(context, concept);
            }
            context.complete();
        } catch (Exception e) {
            if (context!=null) {
                context.abort();
            }
            log.error("Error while loading journal properties", e);
        }
    }

    private static void addJournalConcept(Context context, Concept concept) throws StorageException {
        if (DryadJournalConcept.conceptIsValidJournal(concept)) {
            DryadJournalConcept journalConcept = null;
            try {
                Concept newConcept = Concept.findByIdentifier(context, concept.getIdentifier()).get(0);
                journalConcept = new DryadJournalConcept(context, newConcept);
                context.commit();
            } catch (Exception e) {
                throw new StorageException("couldn't add a journal concept " + concept.getID() + ": " + e.getMessage());
            }
            if (journalConcept != null) {
                // add this concept to the main index if it's not already there.
                if (!journalConceptHashMapByConceptIdentifier.containsValue(journalConcept)) {
                    journalConceptHashMapByConceptIdentifier.put(journalConcept.getIdentifier(), journalConcept);
                    updateDryadJournalConcept(journalConcept);
                }
            }
        }
    }

    public static void addDryadJournalConcept(Context context, DryadJournalConcept journalConcept) throws StorageException {
        if (journalConcept != null) {
            Concept underlyingConcept = journalConcept.getUnderlyingConcept();
            if (underlyingConcept != null) {
                addJournalConcept(context, underlyingConcept);
            }
        }
    }

    public static void removeDryadJournalConcept(Context context, DryadJournalConcept existingConcept) throws SQLException, AuthorizeException {
        if (existingConcept != null) {
            if (journalConceptHashMapByJournalID.containsValue(existingConcept)) {
                journalConceptHashMapByJournalID.remove(existingConcept.getJournalID());
            }
            if (journalConceptHashMapByCustomerID.containsValue(existingConcept)) {
                journalConceptHashMapByCustomerID.remove(existingConcept.getCustomerID());
            }
            if (journalConceptHashMapByJournalName.containsValue(existingConcept)) {
                journalConceptHashMapByJournalName.remove(existingConcept.getFullName());
            }
            journalConceptHashMapByConceptIdentifier.remove(existingConcept.getIdentifier());
            try {
                existingConcept.delete(context);
            } catch (Exception e) {
                log.error("couldn't delete concept " + existingConcept.getConceptID() + ": " + e.getMessage());
            }
        }
    }

    public static void updateDryadJournalConcept(DryadJournalConcept journalConcept) {
        if (journalConceptHashMapByConceptIdentifier.containsValue(journalConcept)) {
            for (String k : journalConceptHashMapByJournalName.keySet()) {
                if (journalConceptHashMapByJournalName.get(k) == journalConcept) {
                    journalConceptHashMapByJournalName.remove(k);
                }
            }
            for (String k : journalConceptHashMapByJournalID.keySet()) {
                if (journalConceptHashMapByJournalID.get(k) == journalConcept) {
                    journalConceptHashMapByJournalID.remove(k);
                }
            }
            for (String k : journalConceptHashMapByCustomerID.keySet()) {
                if (journalConceptHashMapByCustomerID.get(k) == journalConcept) {
                    journalConceptHashMapByCustomerID.remove(k);
                }
            }
            if (!"".equals(journalConcept.getFullName())) {
                journalConceptHashMapByJournalName.put(journalConcept.getFullName().toUpperCase(), journalConcept);
            }
            if (!"".equals(journalConcept.getJournalID())) {
                journalConceptHashMapByJournalID.put(journalConcept.getJournalID().toUpperCase(), journalConcept);
            }
            if (!"".equals(journalConcept.getCustomerID())) {
                journalConceptHashMapByCustomerID.put(journalConcept.getCustomerID(), journalConcept);
            }
        }

    }

    public static DryadJournalConcept createJournalConcept(String journalName) throws StorageException {
        Context context = null;
        try {
            context = new Context();
            DryadJournalConcept journalConcept = new DryadJournalConcept(context, journalName);
            addDryadJournalConcept(context, journalConcept);
            context.complete();
            return journalConcept;
        } catch (Exception e) {
            if (context!=null) {
                context.abort();
            }
            log.error("Error while creating journal concept", e);
        }
        return null;
    }

    public static DryadJournalConcept[] getAllJournalConcepts() {
        ArrayList<DryadJournalConcept> journalConcepts = new ArrayList<DryadJournalConcept>();
        journalConcepts.addAll(journalConceptHashMapByConceptIdentifier.values());
        Collections.sort(journalConcepts);
        return journalConcepts.toArray(new DryadJournalConcept[journalConceptHashMapByConceptIdentifier.size()]);
    }

    public static DryadJournalConcept getJournalConceptByJournalID(String journalID) {
        return journalConceptHashMapByJournalID.get(journalID.toUpperCase());
    }

    public static DryadJournalConcept getJournalConceptByJournalName(String fullName) {
        return journalConceptHashMapByJournalName.get(fullName.toUpperCase());
    }

    public static DryadJournalConcept getJournalConceptByCustomerID(String customerID) {
        return journalConceptHashMapByCustomerID.get(customerID);
    }

    public static String getCanonicalManuscriptID(Manuscript manuscript) {
        return getCanonicalManuscriptID(manuscript.getManuscriptId(), manuscript.getOrganization().organizationCode);
    }

    public static String getCanonicalManuscriptID(String manuscriptId, String journalID) {
        String canonicalID = manuscriptId;
        String regex = null;
        try {
            DryadJournalConcept journalConcept = getJournalConceptByJournalID(journalID);
            regex = journalConcept.getCanonicalManuscriptNumberPattern();
            if (regex != null && !regex.equals("")) {
                Matcher manuscriptMatcher = Pattern.compile(regex).matcher(canonicalID);
                if (manuscriptMatcher.find()) {
                    canonicalID = manuscriptMatcher.group(1);
                } else {
                    log.error("Manuscript " + manuscriptId + " does not match with the regex provided for " + journalID);
                }
            } else {
                // there is no regex specified, just use the manuscript.
                canonicalID = manuscriptId;
            }
        } catch(Exception e) {
            log.error(e.getMessage(),e);
        }
        return canonicalID;
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

        DryadJournalConcept journalConcept = getJournalConceptByJournalName(journalFullName);
        Boolean isIntegrated = false;
        Boolean isBlackedOut = false;
        if (journalConcept != null) {
            isIntegrated = journalConcept.getIntegrated();
            isBlackedOut = journalConcept.getPublicationBlackout();
        }
        if(isIntegrated == false) {
            // journal is not integrated.  Enter blackout by default
            return RecommendedBlackoutAction.JOURNAL_NOT_INTEGRATED;
        } else if (isBlackedOut == true) {
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

    public static Boolean shouldEnterBlackoutByDefault(Context context, Item item, Collection collection) throws SQLException {
        JournalUtils.RecommendedBlackoutAction action = JournalUtils.recommendedBlackoutAction(context, item, collection);
        return (action == JournalUtils.RecommendedBlackoutAction.BLACKOUT_TRUE ||
                action == JournalUtils.RecommendedBlackoutAction.JOURNAL_NOT_INTEGRATED);
    }

    public static void writeManuscriptToDB(Manuscript manuscript) throws StorageException {
        String journalCode = cleanJournalCode(manuscript.getOrganization().organizationCode).toUpperCase();
        StoragePath storagePath = StoragePath.createManuscriptPath(journalCode, manuscript.getManuscriptId());

        ManuscriptDatabaseStorageImpl manuscriptStorage = new ManuscriptDatabaseStorageImpl();
        List<Manuscript> manuscripts = getManuscriptsMatchingID(journalCode, manuscript.getManuscriptId());
        // if there isn't a manuscript already in the db, create it. Otherwise, update.
        if (manuscripts.size() == 0) {
            try {
                manuscriptStorage.create(storagePath, manuscript);
            } catch (StorageException ex) {
                log.error("Exception creating manuscript", ex);
            }
        } else {
            try {
                manuscriptStorage.update(storagePath, manuscript);
            } catch (StorageException ex) {
                log.error("Exception updating manuscript", ex);
            }
        }
    }

    public static List<Manuscript> getManuscriptsMatchingID(String journalCode, String manuscriptId) {
        journalCode = cleanJournalCode(journalCode);
        ArrayList<Manuscript> manuscripts = new ArrayList<Manuscript>();
        StoragePath storagePath = StoragePath.createManuscriptPath(journalCode, manuscriptId);

        try {
            OrganizationDatabaseStorageImpl organizationStorage = new OrganizationDatabaseStorageImpl();
            List<DryadJournalConcept> journalConceptList = organizationStorage.getResults(storagePath, journalCode, 0);
            if (journalConceptList.size() > 0) {
                ManuscriptDatabaseStorageImpl manuscriptStorage = new ManuscriptDatabaseStorageImpl();
                manuscripts.addAll(manuscriptStorage.getManuscriptsMatchingPath(storagePath, 10));
            }
        } catch (StorageException e) {
            log.error("Exception getting manuscripts", e);
        }
        return manuscripts;
    }

    public static Manuscript getManuscriptFromManuscriptStorage (String manuscriptNumber, DryadJournalConcept journalConcept) {
        Manuscript result = null;
        String journalID = journalConcept.getJournalID();
        if (journalConcept == null) {
            throw new RuntimeException ("no journalID " + journalID);
        }
        try {
            // canonicalize the manuscriptNumber:
            manuscriptNumber = JournalUtils.getCanonicalManuscriptID(manuscriptNumber, journalID);
            // first, look for a matching manuscript in the database:
            List<Manuscript> manuscripts = JournalUtils.getManuscriptsMatchingID(journalID, manuscriptNumber);
            if (manuscripts.size() > 0) {
                log.info("found manuscript " + manuscriptNumber + " in database");
                result = manuscripts.get(0);
            } else {
                // if nothing, use the deprecated metadata files (this can be deleted after we completely move away from them):
                File file = new File(journalConcept.getMetadataDir() + File.separator + manuscriptNumber + ".xml");
                try {
                    result = ManuscriptToLegacyXMLConverter.convertInternalXMLToManuscript(file);
                } catch (Exception e) {
                    log.error("Error getting data from metadata file " + file.getPath() + ": " + e.getMessage());
                }
                log.info("found manuscript " + manuscriptNumber + " in file");
            }
        } catch (Exception e) {
            //invalid journalID
            log.error("Error getting parameters for invalid JournalID: " + journalID, e);
        }
        if (result == null) {
            result = new Manuscript();
            result.setJournalConcept(journalConcept);
            result.setManuscriptId(manuscriptNumber);
            result.setMessage("Invalid manuscript number");
        }
        return result;
    }

    public static String cleanJournalCode(String journalCode) {
        return journalCode.replaceAll("[^a-zA-Z0-9]", "").toUpperCase();
    }

    // getHamrScore compares two author names to each other.
    // In practice, it seems that a score of 0.7 or higher generally indicates a good match.
    public static double getHamrScore(String name1, String name2) {
        int maxlen = Math.max(name1.length(), name2.length());
        int editlen = StringUtils.getLevenshteinDistance(name1, name2);

        return (double)(maxlen-editlen)/(double)maxlen;
    }
}
