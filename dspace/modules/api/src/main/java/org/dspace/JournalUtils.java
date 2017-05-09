package org.dspace;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.datadryad.api.DryadJournalConcept;
import org.datadryad.rest.converters.ManuscriptToLegacyXMLConverter;
import org.datadryad.rest.models.Author;
import org.datadryad.rest.models.Manuscript;
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
import org.dspace.workflow.DryadWorkflowUtils;

import java.io.File;
import java.io.FileNotFoundException;
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

    public final static String crossRefApiRoot = "http://api.crossref.org/";
    public final static String nsfApiRoot = "http://api.nsf.gov/services/v1/awards/";

    public static final String archivedDataPackageIds    = "SELECT * FROM ArchivedPackageItemIdsByJournal(?,?);";
    public static final String archivedDataPackageIdsCol =               "archivedpackageitemidsbyjournal";

    public static final String fmtDateView = "yyyy-MM-dd";
    public static final String dcDateAccessioned = "dc.date.accessioned";
    private final static SimpleDateFormat fmt = new SimpleDateFormat(fmtDateView);

    private static HashMap<String, DryadJournalConcept> journalConceptHashMapByConceptIdentifier = new HashMap<String, DryadJournalConcept>();

    private static HashMap<String, DryadJournalConcept> journalConceptHashMapByJournalID = new HashMap<String, DryadJournalConcept>();
    private static HashMap<String, DryadJournalConcept> journalConceptHashMapByJournalName = new HashMap<String, DryadJournalConcept>();
    private static HashMap<String, DryadJournalConcept> journalConceptHashMapByCustomerID = new HashMap<String, DryadJournalConcept>();
    private static HashMap<String, DryadJournalConcept> journalConceptHashMapByISSN = new HashMap<String, DryadJournalConcept>();

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
            if (journalConceptHashMapByISSN.containsValue(existingConcept)) {
                journalConceptHashMapByISSN.remove(existingConcept.getISSN());
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
            for (String k : journalConceptHashMapByISSN.keySet()) {
                if (journalConceptHashMapByISSN.get(k) == journalConcept) {
                    journalConceptHashMapByISSN.remove(k);
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
            if (!"".equals(journalConcept.getISSN())) {
                journalConceptHashMapByISSN.put(journalConcept.getISSN(), journalConcept);
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

    public static String getCanonicalManuscriptID(String manuscriptId, DryadJournalConcept journalConcept) throws ParseException {
        String canonicalID = manuscriptId;
        try {
            if (journalConcept != null && journalConcept.getCanonicalManuscriptNumberPattern() != null && !journalConcept.getCanonicalManuscriptNumberPattern().equals("")) {
                Matcher manuscriptMatcher = Pattern.compile(journalConcept.getCanonicalManuscriptNumberPattern()).matcher(canonicalID);
                if (manuscriptMatcher.find()) {
                    canonicalID = manuscriptMatcher.group(1);
                } else {
                    log.error("Manuscript " + manuscriptId + " does not match with the regex provided for " + journalConcept.getFullName() + ": '" + journalConcept.getCanonicalManuscriptNumberPattern() + "'");
                    throw new ParseException("'" + manuscriptId + "' does not match regex '" + journalConcept.getCanonicalManuscriptNumberPattern() + "'", 0);
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

    // NOTE: identifier can be either journalCode or ISSN
    public static List<Manuscript> getManuscriptsMatchingID(DryadJournalConcept journalConcept, String manuscriptId) {
        ArrayList<Manuscript> manuscripts = new ArrayList<Manuscript>();
        try {
            StoragePath storagePath = StoragePath.createManuscriptPath(journalConcept.getISSN(), getCanonicalManuscriptID(manuscriptId, journalConcept));
            ManuscriptDatabaseStorageImpl manuscriptStorage = new ManuscriptDatabaseStorageImpl();
            manuscripts.addAll(manuscriptStorage.getManuscriptsMatchingPath(storagePath, 10));
        } catch (ParseException e) {
            log.error(e.getMessage());
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
        SimpleDateFormat dateIso = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'");
        try {
            log.error("starting search");
            int pubNameFieldID = MetadataField.findByElement(context,"prism.publicationName").getFieldID();
            int dateAccFieldID = MetadataField.findByElement(context,"dc.date.accessioned").getFieldID();
            String querystring = "select * from ArchivedPackagesForJournal(?, ?, ?)";
            TableRowIterator tri = DatabaseManager.query(context, querystring, journalConcept.getFullName(), pubNameFieldID, dateAccFieldID);
            while (tri.hasNext()) {
                TableRow tableRow = tri.next();
                int itemId = tableRow.getIntColumn("item_id");
                Date date = dateIso.parse(tableRow.getStringColumn("mdv_date"));
                items.put(itemId, date);
            }
            log.error("ending search");
        } catch (Exception e)  {
            throw new SQLException(e.getMessage());
        }
        return items;
    }

    public static Manuscript getCrossRefManuscriptMatchingManuscript(Manuscript queryManuscript, StringBuilder resultString) {
        String crossRefURL = null;
        if (resultString == null) {
            resultString = new StringBuilder();
        }
        String pubDOI = queryManuscript.getPublicationDOI();
        if (pubDOI != null && (!"".equals(StringUtils.stripToEmpty(pubDOI).replaceAll("null","")))) {
            crossRefURL = crossRefApiRoot + "works/" + queryManuscript.getPublicationDOI();
        } else {
            StringBuilder queryString = new StringBuilder();

            // make a query string of the authors' last names
            ArrayList<String> lastNames = new ArrayList<String>();
            for (Author a : queryManuscript.getAuthorList()) {
                // replace any hyphens in the last names with spaces for tokenizing.
                lastNames.add(a.getNormalizedFamilyName().replaceAll("-"," "));
            }
            queryString.append(StringUtils.join(lastNames.toArray(), " ").replaceAll("[^a-zA-Z\\s]", ""));
            queryString.append(" ");

            // append the title to the query
            queryString.append(queryManuscript.getTitle().replaceAll("[^a-zA-Z\\s]", "").replaceAll("\\s+", " "));
            crossRefURL = crossRefApiRoot + "journals/" + queryManuscript.getJournalISSN() + "/works?sort=score&order=desc&query=" + queryString.toString().replaceAll("\\s+", "+");
        }

        Manuscript matchedManuscript = null;
        try {
            resultString.append("crossref url was " + crossRefURL + "\n");
            URL url = new URL(crossRefURL.replaceAll("\\s+", ""));
            ObjectMapper m = new ObjectMapper();
            JsonNode rootNode = m.readTree(url.openStream());
            JsonNode itemsNode = rootNode.path("message").path("items");
            if (itemsNode != null && itemsNode.isArray()) {
                JsonNode bestItem = itemsNode.get(0);
                float score = bestItem.path("score").floatValue();
                if (score > 3.0) {
                    matchedManuscript = manuscriptFromCrossRefJSON(bestItem, queryManuscript.getJournalConcept());
                }
            } else {
                itemsNode = rootNode.path("message");
                matchedManuscript = manuscriptFromCrossRefJSON(itemsNode, queryManuscript.getJournalConcept());
            }
        } catch (JsonParseException e) {
            log.debug("Couldn't find JSON matching URL " + crossRefURL);
        } catch (NullPointerException e) {
            log.debug("No matches returned for URL " + crossRefURL);
        } catch (FileNotFoundException e) {
            log.debug("CrossRef does not have data for URL " + crossRefURL);
        } catch (Exception e) {
            StringBuilder sb = new StringBuilder();
            for (StackTraceElement element : e.getStackTrace()) {
                sb.append(element.toString());
                sb.append("\n");
            }
            String s = sb.toString();
            log.error("Exception of type " + e.getClass().getName()+ ", message is: " + e.getMessage() + ": url is " + crossRefURL + "\n" + s);
        }

        // sanity check:
        if (matchedManuscript != null) {
            double matchScore = getHamrScore(queryManuscript.getTitle().toLowerCase(), matchedManuscript.getTitle().toLowerCase());
            matchedManuscript.optionalProperties.put("crossref-score", String.valueOf(matchScore));
            // for now, scores greater than 0.5 seem to be a match. Keep an eye on this.
            if (matchScore < 0.5) {
                resultString.append("\"" + queryManuscript.getTitle() + "\" matched \"" + matchedManuscript.getTitle() + "\" with score " + matchScore);
                return null;
            }
        }
        return matchedManuscript;
    }

    public static boolean compareItemAuthorsToManuscript(Item item, Manuscript manuscript, StringBuilder result) {
        boolean matched = false;
        // count number of authors and number of matched authors: if equal, this is a match.
        DCValue[] itemAuthors = item.getMetadata("dc", "contributor", "author", Item.ANY);
        result.append("item has " + itemAuthors.length + " authors while manuscript has " + manuscript.getAuthorList().size() + " authors\n");
        if (manuscript.getAuthorList().size() == itemAuthors.length) {
            int numMatched = 0;
            for (int j = 0; j < itemAuthors.length; j++) {
                for (Author a : manuscript.getAuthorList()) {
                    double score = JournalUtils.getHamrScore(Author.normalizeName(itemAuthors[j].value).toLowerCase(), a.getNormalizedFullName().toLowerCase());
                    result.append("author " + itemAuthors[j].value + " matched " + a.getUnicodeFullName() + " with a score of " + score + "\n");
                    if (score > 0.6) {
                        numMatched++;
                        break;
                    }
                }
            }

            if (numMatched == itemAuthors.length) {
                matched = true;
                result.append("matched " + item.getID() + " by authors");
            }
        }
        return matched;
    }

    public static Manuscript manuscriptFromCrossRefJSON(JsonNode jsonNode, DryadJournalConcept dryadJournalConcept) {
        Manuscript manuscript = new Manuscript();
        if (jsonNode.path("DOI") != null) {
            manuscript.setPublicationDOI(jsonNode.path("DOI").textValue());
        }

        JsonNode authorsNode = jsonNode.path("author");
        if (authorsNode.isArray()) {
            for (JsonNode authorNode : authorsNode) {
                manuscript.addAuthor(new Author(authorNode.path("family").textValue(),authorNode.path("given").textValue()));
            }
        }

        JsonNode dateNode = jsonNode.path("created");
        if (dateNode != null) {
            //2016-04-11T17:53:39Z
            SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");
            try {
                if (dateNode.path("date-time") != null) {
                    Date date = dateFormat.parse(dateNode.path("date-time").textValue());
                    manuscript.setPublicationDate(date);
                }
            } catch (ParseException e) {
                log.error("couldn't parse date: " + dateNode.path("date-time").textValue());
            }
        }
        JsonNode titleNode = jsonNode.path("title");
        if (titleNode.isArray()) {
            manuscript.setTitle(titleNode.elements().next().textValue());
        }
        if (jsonNode.path("publisher") != null) {
            manuscript.setPublisher(jsonNode.path("publisher").textValue());
        }
        if (jsonNode.path("volume") != null) {
            manuscript.setJournalVolume(jsonNode.path("volume").textValue());
        }
        if (jsonNode.path("page") != null) {
            manuscript.setPages(jsonNode.path("page").textValue());
        }
        if (jsonNode.path("issue") != null) {
            manuscript.setJournalNumber(jsonNode.path("issue").textValue());
        }

        if (dryadJournalConcept != null) {
            manuscript.setJournalConcept(dryadJournalConcept);
        }
        manuscript.setStatus(Manuscript.STATUS_PUBLISHED);
        if (jsonNode.path("score") != null) {
            manuscript.optionalProperties.put("crossref-score", String.valueOf(jsonNode.path("score").floatValue()));
        }
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
