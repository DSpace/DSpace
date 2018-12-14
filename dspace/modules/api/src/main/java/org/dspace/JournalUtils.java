package org.dspace;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.module.SimpleModule;
import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.datadryad.api.DryadDataPackage;
import org.datadryad.api.DryadJournalConcept;
import org.datadryad.rest.converters.ManuscriptToLegacyXMLConverter;
import org.datadryad.rest.models.*;
import org.datadryad.rest.storage.StorageException;
import org.datadryad.rest.storage.StoragePath;
import org.datadryad.rest.storage.rdbms.ManuscriptDatabaseStorageImpl;
import org.dspace.authorize.AuthorizeException;
import org.dspace.content.Collection;
import org.dspace.content.DCValue;
import org.dspace.content.Item;
import org.dspace.content.MetadataField;
import org.dspace.content.authority.Concept;
import org.dspace.content.authority.Scheme;
import org.dspace.core.ConfigurationManager;
import org.dspace.core.Context;
import org.dspace.storage.rdbms.DatabaseManager;
import org.dspace.storage.rdbms.TableRow;
import org.dspace.storage.rdbms.TableRowIterator;
import org.dspace.workflow.ApproveRejectReviewItemException;
import org.dspace.workflow.DryadWorkflowUtils;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.net.URL;
import java.sql.SQLException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
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

    public final static String crossRefApiRoot = "https://api.crossref.org/";
    public final static String nsfApiRoot = "https://api.nsf.gov/services/v1/awards/";

    public static final String archivedDataPackageIds    = "SELECT * FROM ArchivedPackageItemIdsByJournal(?,?);";
    public static final String archivedDataPackageIdsCol =               "archivedpackageitemidsbyjournal";

    public static final String fmtDateView = "yyyy-MM-dd";
    public static final String dcDateAccessioned = "dc.date.accessioned";
    private final static SimpleDateFormat fmt = new SimpleDateFormat(fmtDateView);

    private static HashMap<Integer, DryadJournalConcept> journalConceptHashMapByConceptIdentifier = new HashMap<Integer, DryadJournalConcept>();

    private static HashMap<String, DryadJournalConcept> journalConceptHashMapByJournalID = new HashMap<String, DryadJournalConcept>();
    private static HashMap<String, DryadJournalConcept> journalConceptHashMapByJournalName = new HashMap<String, DryadJournalConcept>();
    private static HashMap<String, DryadJournalConcept> journalConceptHashMapByCustomerID = new HashMap<String, DryadJournalConcept>();
    private static HashMap<String, DryadJournalConcept> journalConceptHashMapByISSN = new HashMap<String, DryadJournalConcept>();
    public static HashSet<DryadJournalConcept> recentlyIntegratedJournals = new HashSet<>();

    static {
        initializeJournalConcepts();
    }

    private static void initializeJournalConcepts() {
        Context context = null;

        try {
            context = new Context();
            Scheme scheme = Scheme.findByIdentifier(context, ConfigurationManager.getProperty("solrauthority.searchscheme.prism_publicationName"));
            Concept[] concepts = scheme.getConcepts(context);
            for (Concept newConcept : concepts) {
                DryadJournalConcept journalConcept = new DryadJournalConcept(context, newConcept);
                // add this concept to the main index if it's not already there.
                if (!journalConceptHashMapByConceptIdentifier.containsValue(journalConcept)) {
                    journalConceptHashMapByConceptIdentifier.put(journalConcept.getIdentifier(), journalConcept);
                    updateDryadJournalConcept(journalConcept);
                }
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
                // add this concept to the main index if it's not already there.
                if (!journalConceptHashMapByConceptIdentifier.containsValue(journalConcept)) {
                    journalConceptHashMapByConceptIdentifier.put(journalConcept.getIdentifier(), journalConcept);
                    updateDryadJournalConcept(journalConcept);
                }
            } catch (Exception e) {
                throw new StorageException("couldn't add a journal concept " + concept.getID() + ": " + e.getMessage());
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
            if (journalConceptHashMapByISSN.containsValue(existingConcept)) {
                journalConceptHashMapByISSN.remove(existingConcept.getISSN());
            }
            if (recentlyIntegratedJournals.contains(existingConcept)) {
                recentlyIntegratedJournals.remove(existingConcept);
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
        if (journalConceptHashMapByConceptIdentifier.containsKey(journalConcept.getConceptID())) {
            for (String k : journalConceptHashMapByJournalName.keySet()) {
                if (journalConceptHashMapByJournalName.get(k).compareTo(journalConcept) == 0) {
                    journalConceptHashMapByJournalName.remove(k);
                    break;
                }
            }
            if (!"".equals(journalConcept.getFullName())) {
                journalConceptHashMapByJournalName.put(journalConcept.getFullName().toUpperCase(), journalConcept);
            }

            for (String k : journalConceptHashMapByJournalID.keySet()) {
                if (journalConceptHashMapByJournalID.get(k).compareTo(journalConcept) == 0) {
                    journalConceptHashMapByJournalID.remove(k);
                    break;
                }
            }
            if (!"".equals(journalConcept.getJournalID())) {
                journalConceptHashMapByJournalID.put(journalConcept.getJournalID().toUpperCase(), journalConcept);
            }

            for (String k : journalConceptHashMapByCustomerID.keySet()) {
                if (journalConceptHashMapByCustomerID.get(k).compareTo(journalConcept) == 0) {
                    journalConceptHashMapByCustomerID.remove(k);
                    break;
                }
            }
            if (!"".equals(journalConcept.getCustomerID())) {
                journalConceptHashMapByCustomerID.put(journalConcept.getCustomerID(), journalConcept);
            }

            for (String k : journalConceptHashMapByISSN.keySet()) {
                if (journalConceptHashMapByISSN.get(k).compareTo(journalConcept) == 0) {
                    journalConceptHashMapByISSN.remove(k);
                    break;
                }
            }
            if (journalConcept.getISSNs().size() > 0) {
                for (String issn : journalConcept.getISSNs()) {
                    journalConceptHashMapByISSN.put(issn, journalConcept);
                }
            }

            for (DryadJournalConcept concept : recentlyIntegratedJournals) {
                if (concept.getConceptID() == journalConcept.getConceptID()) {
                    recentlyIntegratedJournals.remove(concept);
                    break;
                }
            }
            if (journalConcept.getRecentlyIntegrated()) {
                recentlyIntegratedJournals.add(journalConcept);
            }
        }
        writeJournalLookupJSON();
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
        initializeJournalConcepts();
        ArrayList<DryadJournalConcept> journalConcepts = new ArrayList<DryadJournalConcept>();
        journalConcepts.addAll(journalConceptHashMapByConceptIdentifier.values());
        Collections.sort(journalConcepts);
        return journalConcepts.toArray(new DryadJournalConcept[journalConceptHashMapByConceptIdentifier.size()]);
    }

    public static DryadJournalConcept getJournalConceptByJournalID(String journalID) {
        if (journalID == null) {
            return null;
        }
        return journalConceptHashMapByJournalID.get(journalID.toUpperCase());
    }

    public static DryadJournalConcept getJournalConceptByJournalName(String fullName) {
        if (fullName == null) {
            return null;
        }
        return journalConceptHashMapByJournalName.get(fullName.toUpperCase());
    }

    public static DryadJournalConcept getJournalConceptByCustomerID(String customerID) {
        if (customerID == null) {
            return null;
        }
        return journalConceptHashMapByCustomerID.get(customerID);
    }

    public static DryadJournalConcept getJournalConceptByISSN(String ISSN) {
        if (ISSN == null) {
            return null;
        }
        return journalConceptHashMapByISSN.get(ISSN);
    }

    public static String getCanonicalManuscriptID(Manuscript manuscript) throws ParseException {
        return getCanonicalManuscriptID(manuscript.getManuscriptId(), manuscript.getJournalConcept());
    }

    public static String getCanonicalManuscriptID(String manuscriptId, DryadJournalConcept journalConcept) {
        String canonicalID = manuscriptId;
        try {
            if (journalConcept != null && journalConcept.getCanonicalManuscriptNumberPattern() != null && !journalConcept.getCanonicalManuscriptNumberPattern().equals("")) {
                Matcher manuscriptMatcher = Pattern.compile(journalConcept.getCanonicalManuscriptNumberPattern()).matcher(canonicalID);
                if (manuscriptMatcher.find()) {
                    canonicalID = manuscriptMatcher.group(1);
                } else {
                    log.info("Manuscript " + manuscriptId + " does not match with the regex provided for " + journalConcept.getFullName() + ": '" + journalConcept.getCanonicalManuscriptNumberPattern() + "'");
                }
            } else {
                // there is no regex specified, just use the manuscript.
                canonicalID = manuscriptId;
            }
        } catch(Exception e) {
            log.error("error in getting canonical msid for " + manuscriptId + ": " + e.getMessage(),e);
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

    public static Manuscript writeManuscriptToDB(Manuscript manuscript) throws StorageException {
        StoragePath storagePath = StoragePath.createManuscriptPath(manuscript.getJournalConcept().getISSN(), manuscript.getManuscriptId());
        ManuscriptDatabaseStorageImpl manuscriptStorage = new ManuscriptDatabaseStorageImpl();
        List<Manuscript> manuscripts = getManuscriptsMatchingID(manuscript.getJournalConcept(), manuscript.getManuscriptId());
        // if there isn't a manuscript already in the db, create it. Otherwise, update.
        if (manuscripts.size() == 0) {
            try {
                manuscriptStorage.create(storagePath, manuscript);
            } catch (StorageException ex) {
                log.error("Exception creating manuscript", ex);
                throw ex;
            }
        } else {
            try {
                manuscriptStorage.update(storagePath, manuscript);
            } catch (StorageException ex) {
                log.error("Exception updating manuscript", ex);
                throw ex;
            }
        }
        return manuscript;
    }

    public static List<Manuscript> getStoredManuscriptsMatchingManuscript(Manuscript manuscript) throws ParseException {
        ManuscriptDatabaseStorageImpl manuscriptStorage = new ManuscriptDatabaseStorageImpl();
        try {
            if (!"".equals(manuscript.getManuscriptId())) {
                manuscript.setManuscriptId(getCanonicalManuscriptID(manuscript));
            }
            return manuscriptStorage.getManuscriptsMatchingManuscript(manuscript);
        } catch (ParseException e) {
            throw e;
        } catch (StorageException e) {
            log.error("Exception getting manuscripts" , e);
        }
        return null;
    }

    public static boolean manuscriptIsKnownFormerManuscriptNumber(DryadDataPackage dryadDataPackage, Manuscript manuscript) {
        if (manuscript.getManuscriptId() == null) {
            return false;
        }
        // is this manuscript one of this package's former msids?
        List<String> formerMSIDs = dryadDataPackage.getFormerManuscriptNumbers();
        for (String formerMSID : formerMSIDs) {
            if (formerMSID.equalsIgnoreCase(manuscript.getManuscriptId())) {
                return true;
            }
        }
        return false;
    }

    // NOTE: identifier can be either journalCode or ISSN
    private static List<Manuscript> getManuscriptsMatchingID(DryadJournalConcept journalConcept, String manuscriptId) {
        ArrayList<Manuscript> manuscripts = new ArrayList<Manuscript>();
        try {
            StoragePath storagePath = StoragePath.createManuscriptPath(journalConcept.getISSN(), getCanonicalManuscriptID(manuscriptId, journalConcept));
            ManuscriptDatabaseStorageImpl manuscriptStorage = new ManuscriptDatabaseStorageImpl();
            manuscripts.addAll(manuscriptStorage.getManuscriptsMatchingPath(storagePath, 10));
        } catch (StorageException e) {
            log.error("Exception getting manuscripts", e);
        }
        return manuscripts;
    }

    public static Manuscript getManuscriptFromManuscriptStorage (String manuscriptNumber, DryadJournalConcept journalConcept) {
        Manuscript result = null;
        if (journalConcept == null) {
            throw new RuntimeException ("no journal " + journalConcept.getFullName());
        }
        try {
            // canonicalize the manuscriptNumber:
            manuscriptNumber = JournalUtils.getCanonicalManuscriptID(manuscriptNumber, journalConcept);
            // first, look for a matching manuscript in the database:
            List<Manuscript> manuscripts = JournalUtils.getManuscriptsMatchingID(journalConcept, manuscriptNumber);
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
            log.error("Error getting parameters for invalid journal " + journalConcept.getFullName(), e);
        }
        if (result == null) {
            result = new Manuscript(journalConcept);
            result.setManuscriptId(manuscriptNumber);
            result.setMessage("Invalid manuscript number");
        }
        return result;
    }


    /**
     * Return a sorted map of archived data packages (Item objects) for the journal
     * associated with this object. The data packages are sorted according to
     * date-accessioned, with most recently accessioned package first.
     * @param max total number of items to return
     * @return List<org.dspace.content.Item> data packages
     * @throws SQLException
     */
    public static LinkedHashMap<Item,String> getArchivedPackagesSortedRecent(Context context, String journalName, int max)
            throws SQLException
    {
        LinkedHashMap<Item,String> dataPackages = new LinkedHashMap<Item,String>(max);
        try {
            TableRowIterator tri = DatabaseManager.query(context, archivedDataPackageIds, journalName, max);
            while (tri.hasNext() && dataPackages.size() < max) {
                int itemId = tri.next().getIntColumn(archivedDataPackageIdsCol);
                Item dso = Item.find(context, itemId);
                DCValue[] dateAccessioned = dso.getMetadata(dcDateAccessioned);
                String dateStr = fmt.format(fmt.parse(dateAccessioned[0].value));
                dataPackages.put(dso, dateStr);
            }
        } catch (Exception e)  {
            throw new SQLException(e.getMessage());
        }
        return dataPackages;
    }

    /**
     * Return a sorted map of archived packages for a journal, starting with a particular item as a keyset
     * @param context
     * @param journalConcept
     * @param keyset
     * @return
     * @throws SQLException
     */
    public static TreeMap<Integer, Date> getArchivedPackagesFromKeyset(Context context, DryadJournalConcept journalConcept, int keyset) throws SQLException {
        TreeMap<Integer, Date> items = new TreeMap<Integer, Date>();
        try {
            log.error("starting search");
            int pubNameFieldID = MetadataField.findByElement("prism.publicationName").getFieldID();
            int dateAccFieldID = MetadataField.findByElement("dc.date.accessioned").getFieldID();
            String querystring = "select * from ArchivedPackagesForJournal(?, ?, ?)";
            TableRowIterator tri = DatabaseManager.query(context, querystring, journalConcept.getFullName(), pubNameFieldID, dateAccFieldID);
            while (tri.hasNext()) {
                TableRow tableRow = tri.next();
                int itemId = tableRow.getIntColumn("item_id");
                String rawDate = tableRow.getStringColumn("mdv_date");
                Matcher dateMatcher = Pattern.compile("(\\d+-\\d+-\\d+).?").matcher(rawDate);
                Date date = null;
                if (dateMatcher.find()) {
                    SimpleDateFormat dateShort = new SimpleDateFormat("yyyy-MM-dd");
                    date = dateShort.parse(dateMatcher.group(1));
                }
                items.put(itemId, date);
            }
            log.error("ending search");
        } catch (Exception e)  {
            throw new SQLException(e.getMessage());
        }
        return items;
    }

    public static boolean isJournalConceptListedInCrossref(DryadJournalConcept journalConcept) throws RESTModelException {
        if (journalConcept.getISSN().isEmpty()) {
            throw new RESTModelException("journal concept " + journalConcept.getConceptID() + " doesn't have an ISSN");
        } else {
            for (String issn : journalConcept.getISSNs()) {
                String journalTitle = getCrossRefJournalForISSN(issn);
                if (journalTitle != null) {
                    if (!journalTitle.equalsIgnoreCase(journalConcept.getFullName())) {
                        log.debug("journal concept " + journalConcept.getFullName() + " (" + journalConcept.getConceptID() + ") lists ISSN " + issn + ", but that belongs to a journal titled " + journalTitle);
                    }
                    return true;
                } else {
                    throw new RESTModelException("journal concept " + journalConcept.getConceptID() + " has an invalid crossref ISSN");
                }
            }
        }
        return false;
    }

    public static String getCrossRefJournalForISSN(String issn) throws RESTModelException {
        String crossRefURL = crossRefApiRoot + "journals/" + issn;
        try {
            URL url = new URL(crossRefURL.replaceAll("\\s+", "") + "?mailto=" + ConfigurationManager.getProperty("alert.recipient"));
            ObjectMapper m = new ObjectMapper();
            JsonNode rootNode = m.readTree(url.openStream());
            JsonNode titleNode = rootNode.path("message").path("title");
            if (titleNode != null) {
                return titleNode.textValue();
            } else {
                return null;
            }
        } catch (Exception e) {
            throw new RESTModelException("couldn't query crossref for ISSN " + issn + ": " + e.getMessage());
        }
    }

    public static Manuscript getCrossRefManuscriptMatchingManuscript(DryadDataPackage dryadDataPackage, StringBuilder resultString) throws RESTModelException {
        ArrayList<String> crossRefURLs = new ArrayList<>();
        if (resultString == null) {
            resultString = new StringBuilder();
        }
        StringBuilder queryString = new StringBuilder();
        String pubDOI = dryadDataPackage.getPublicationDOI();
        DryadJournalConcept dryadJournalConcept = JournalUtils.getJournalConceptByJournalName(dryadDataPackage.getPublicationName());
        if (pubDOI != null && (!"".equals(StringUtils.stripToEmpty(pubDOI).replaceAll("null","")))) {
            crossRefURLs.add(crossRefApiRoot + "works/" + dryadDataPackage.getPublicationDOI() +
                             "?mailto=" + ConfigurationManager.getProperty("alert.recipient"));
        } else {
            // make a query string of the authors' last names
            ArrayList<String> lastNames = new ArrayList<String>();
            for (Author a : dryadDataPackage.getAuthors()) {
                // replace any hyphens in the last names with spaces for tokenizing.
                lastNames.add(a.getNormalizedFamilyName().replaceAll("-"," "));
            }
            queryString.append(StringUtils.join(lastNames.toArray(), " ").replaceAll("[^a-zA-Z\\s]", ""));
            queryString.append(" ");

            // append the title to the query
            queryString.append(dryadDataPackage.getTitle().replaceAll("[^a-zA-Z\\s]", "").replaceAll("\\s+", " "));
            for (String issn : dryadJournalConcept.getISSNs()) {
                crossRefURLs.add(crossRefApiRoot + "journals/" + issn + "/works?sort=score&order=desc&query=" +
                                 queryString.toString().replaceAll("\\s+", "+") + "?mailto=" + ConfigurationManager.getProperty("alert.recipient"));
            }
        }

        for (String crossRefURL : crossRefURLs) {
            Manuscript currentMatch = null;
            try {
                resultString.append("\n\tcrossref url was ").append(crossRefURL).append("\n");
                URL url = new URL(crossRefURL.replaceAll("\\s+", ""));
                ObjectMapper m = new ObjectMapper();
                JsonNode rootNode = m.readTree(url.openStream());
                JsonNode itemsNode = rootNode.path("message").path("items");
                if (itemsNode != null && itemsNode.isArray()) {
                    JsonNode bestItem = itemsNode.get(0);
                    float score = bestItem.path("score").floatValue();
                    if (score > 3.0) {
                        currentMatch = manuscriptFromCrossRefJSON(bestItem);
                    }
                } else {
                    itemsNode = rootNode.path("message");
                    currentMatch = manuscriptFromCrossRefJSON(itemsNode);
                }
            } catch (JsonParseException e) {
                log.debug("Couldn't find JSON matching URL " + crossRefURL);
            } catch (NullPointerException e) {
                log.debug("No matches returned for URL " + crossRefURL);
            } catch (FileNotFoundException e) {
                log.debug("CrossRef does not have data for URL " + crossRefURL);
            } catch (RESTModelException e) {
                throw e;
            } catch (Exception e) {
                StringBuilder sb = new StringBuilder();
                for (StackTraceElement element : e.getStackTrace()) {
                    sb.append(element.toString());
                    sb.append("\n");
                }
                String s = sb.toString();
                log.error("Exception of type " + e.getClass().getName() + ", message is: " + e.getMessage() + ": url is " + crossRefURL + "\n" + s);
            }

            // sanity check:
            if (currentMatch != null) {
                if (!dryadJournalConcept.getISSN().equals(currentMatch.getJournalISSN())) {
                    throw new RESTModelException("publication DOI listed for item does not belong to the correct journal");
                }
                // for now, scores greater than 0.5 seem to be a match. Keep an eye on this.
                if (compareTitleToManuscript(dryadDataPackage.getTitle(), currentMatch, 0.5, resultString)) {
                    return currentMatch;
                }
            }
        }
        return null;
    }

    public static boolean compareTitleToManuscript(String title, Manuscript matchedManuscript, double threshold, StringBuilder resultString) {
        double matchScore = getHamrScore(title.toLowerCase(), matchedManuscript.getTitle().toLowerCase());
        matchedManuscript.optionalProperties.put("crossref-score", String.valueOf(matchScore));
        if (matchScore < threshold) {
            resultString.append("BAD MATCH: \"" + title + "\" matched \"" + matchedManuscript.getTitle() + "\" with score " + matchScore);
            return false;
        } else {
            resultString.append("GOOD MATCH: \"" + title + "\" matched \"" + matchedManuscript.getTitle() + "\" with score " + matchScore);
        }
        return true;
    }

    public static int comparePackageAuthorsToManuscript(DryadDataPackage dryadDataPackage, Manuscript manuscript, StringBuilder result) {
        int numMatched = 0;
        // count number of authors and number of matched authors: if equal, this is a match.
        List<Author> authorList = dryadDataPackage.getAuthors();
        result.append("package " + dryadDataPackage.getIdentifier() + " has " + authorList.size() + " authors while manuscript has " + manuscript.getAuthorList().size() + " authors\n");
        for (Author itemAuthor : authorList) {
            for (Author msAuthor : manuscript.getAuthorList()) {
                double score = JournalUtils.getHamrScore(itemAuthor.getNormalizedFullName().toLowerCase().replaceAll("[^a-zA-Z ]", ""), msAuthor.getNormalizedFullName().toLowerCase().replaceAll("[^a-zA-Z ]", ""));
                result.append("item author " + itemAuthor.getNormalizedFullName() + " matched ms author " + msAuthor.getUnicodeFullName() + " with a score of " + score + "\n");
                if (score > 0.8) {
                    result.append("  matched\n");
                    numMatched++;
                    break;
                }
            }
        }

        result.append(numMatched).append(" authors matched");
        return numMatched;
    }

    private static Manuscript manuscriptFromCrossRefJSON(JsonNode jsonNode) throws RESTModelException {
        // manuscripts should only be returned if the crossref match is of type "journal-article"
        if (!jsonNode.path("type").isMissingNode()) {
            if (!"journal-article".equals(jsonNode.path("type").textValue())) {
                throw new RESTModelException("Crossref result is not of type journal-article: " + jsonNode.path("type").textValue());
            }
        }

        Manuscript manuscript = new Manuscript();
        if (!jsonNode.path("DOI").isMissingNode()) {
            manuscript.setPublicationDOI(jsonNode.path("DOI").textValue());
        }

        JsonNode authorsNode = jsonNode.path("author");
        if (!authorsNode.isMissingNode() && authorsNode.isArray()) {
            for (JsonNode authorNode : authorsNode) {
                manuscript.addAuthor(new Author(authorNode.path("family").textValue(),authorNode.path("given").textValue()));
            }
        }

        JsonNode dateNode = jsonNode.path("issued");
        if (!dateNode.isMissingNode()) {
            Calendar today = new GregorianCalendar();
            today.setTime(new Date());

            // Assume that any unspecified portion of the date is equivalent to today,
            // e.g. if it's October 26, 2018, and the json date says Sept 2017, assume that Sept 26, 2017 is the date.
            // This way, if the json date says Oct 2018, we would agree that Oct 26, 2018, is not in the future,
            // but if the json date says Nov 2018, we would agree that Nov 26, 2018, is in the future.
            Calendar msDate = new GregorianCalendar();
            msDate.setTime(new Date());

            try {
                JsonNode dateParts = dateNode.path("date-parts").get(0);
                msDate.set(Calendar.YEAR, dateParts.get(0).asInt());
                if (dateParts.has(1)) {
                    msDate.set(Calendar.MONTH, dateParts.get(1).asInt() - 1); // Calendar month is 0-based
                }
                if (dateParts.has(2)) {
                    msDate.set(Calendar.DATE, dateParts.get(2).asInt());
                }
                if (msDate.after(today)) {
                    throw new RESTModelException("CrossRef match has publication date in the future: " + msDate.get(Calendar.YEAR) + "-" + msDate.get(Calendar.MONTH)+1 + "-" + msDate.get(Calendar.DATE));
                }
                manuscript.setPublicationDate(msDate.getTime());
            } catch (Exception e) {
                log.error("exception calculating date: " + e.getClass().getName() + ", " + e.getMessage());
            }
        }
        JsonNode titleNode = jsonNode.path("title");
        if (titleNode.isArray()) {
            String trimmedTitle = titleNode.elements().next().textValue().replace("\n", " ").replaceAll("\\s+", " ");
            manuscript.setTitle(trimmedTitle);
        }
        if (!jsonNode.path("publisher").isMissingNode()) {
            manuscript.setPublisher(jsonNode.path("publisher").textValue());
        }
        if (!jsonNode.path("volume").isMissingNode()) {
            manuscript.setJournalVolume(jsonNode.path("volume").textValue());
        }
        if (!jsonNode.path("article-number").isMissingNode()) {
            manuscript.setPages(jsonNode.path("article-number").textValue());
        }
        if (!jsonNode.path("page").isMissingNode() ) {
            manuscript.setPages(jsonNode.path("page").textValue());
        }
        if (!jsonNode.path("issue").isMissingNode()) {
            manuscript.setJournalNumber(jsonNode.path("issue").textValue());
        }
        if (!jsonNode.path("ISSN").isMissingNode() && jsonNode.path("ISSN").isArray()) {
            for (JsonNode issnNode : jsonNode.path("ISSN")) {
                DryadJournalConcept dryadJournalConcept = getJournalConceptByISSN(issnNode.textValue());
                if (dryadJournalConcept != null) {
                    manuscript.setJournalConcept(dryadJournalConcept);
                    break;
                }
            }
        }

        // sanity checks for manuscript format:
        if (manuscript.getJournalConcept() == null) {
            throw new RESTModelException("Couldn't find journal concept");
        }
        if (manuscript.getPublicationDate() == null) {
            throw new RESTModelException("CrossRef match does not have a publication date");
        }
        if (manuscript.getAuthorList() == null || manuscript.getAuthorList().size() == 0) {
            throw new RESTModelException("CrossRef match does not have authors");
        }
        manuscript.setStatus(Manuscript.STATUS_PUBLISHED);
        return manuscript;
    }

    public static boolean isValidNSFGrantNumber(String grantInfo) {
        if ("".equals(StringUtils.stripToEmpty(grantInfo))) {
            return false;
        }
        Matcher matcher = Pattern.compile("^.*?(\\d+).*$").matcher(grantInfo);
        if (matcher.find()) {
            grantInfo = matcher.group(1);
        }

        String nsfAPIURL = nsfApiRoot + grantInfo + ".json";
        log.error("checking " + nsfAPIURL);
        try {
            URL url = new URL(nsfAPIURL.replaceAll("\\s+", ""));
            ObjectMapper m = new ObjectMapper();
            JsonNode responseNode = m.readTree(url.openStream()).path("response");
            if (responseNode != null) {
                JsonNode awardNode = responseNode.path("award");
                if (awardNode != null && awardNode.isArray()) {
                    Iterator<JsonNode> awards = awardNode.elements();
                    while (awards.hasNext()) {
                        JsonNode award = awards.next();
                        if (award.has("agency") && "NSF".equals(award.get("agency").textValue())) {
                            if (award.has("id") && grantInfo.equals(award.get("id").textValue())) {
                                log.error("valid grant number");
                                return true;
                            }
                        }
                    }
                }
            }
        } catch (Exception e) {
            log.debug("Couldn't find JSON matching URL " + nsfAPIURL);

        }
        return false;
    }

    public static void writeJournalLookupJSON() {
        ObjectMapper mapper = new ObjectMapper();
        try {
            mapper.registerModule(new SimpleModule().addSerializer(Journal.class, new Journal.JournalLookupSerializer()));
            StringBuffer sb = new StringBuffer();
            ArrayList<String> journalJSONs = new ArrayList<String>();
            ArrayList<DryadJournalConcept> journalConcepts = new ArrayList<DryadJournalConcept>();
            journalConcepts.addAll(journalConceptHashMapByISSN.values());
            journalConcepts.sort(Comparator.naturalOrder());
            for (DryadJournalConcept journalConcept : journalConcepts) {
                journalJSONs.add(mapper.writerWithDefaultPrettyPrinter().writeValueAsString(new Journal(journalConcept)));
            }
            sb.append("{\n\"data\" : [\n");
            sb.append(String.join(",", journalJSONs));
            sb.append("\n]\n}");

            String jsonFilePath = ConfigurationManager.getProperty("dspace.dir") + "/webapps/xmlui/static/json/journal-lookup.json";
            File jsonFile = new File(jsonFilePath);
            BufferedWriter bw = new BufferedWriter(new FileWriter(jsonFile));
            bw.write(sb.toString());
            bw.close();

        } catch (Exception e) {
        }
    }

    public static Manuscript getStoredManuscriptForPackage(Context c, DryadDataPackage dryadDataPackage) {
        Manuscript manuscript = new Manuscript(dryadDataPackage);
        try {
            c.turnOffAuthorisationSystem();
            List<Manuscript> storedManuscripts = getStoredManuscriptsMatchingManuscript(manuscript);
            if (storedManuscripts != null && storedManuscripts.size() > 0) {
                Manuscript storedManuscript = storedManuscripts.get(0);
                log.info("found stored manuscript " + storedManuscript.getManuscriptId() + " with status " + storedManuscript.getLiteralStatus());
                if (!manuscriptIsKnownFormerManuscriptNumber(dryadDataPackage, storedManuscript)) {
                    return storedManuscript;
                } else {
                    log.info("stored manuscript match was a known former manuscript number");
                }
            }
            c.restoreAuthSystemState();
        } catch (Exception e) {
            log.error("couldn't process workflowitem " + dryadDataPackage.getIdentifier() + ": " + e.getMessage());
        }
        return null;
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
