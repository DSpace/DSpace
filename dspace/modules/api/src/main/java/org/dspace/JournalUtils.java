package org.dspace;

import org.apache.commons.lang.StringEscapeUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.datadryad.rest.models.Author;
import org.datadryad.rest.models.Manuscript;
import org.datadryad.rest.models.Organization;
import org.datadryad.rest.storage.StorageException;
import org.datadryad.rest.storage.StoragePath;
import org.datadryad.rest.storage.rdbms.ManuscriptDatabaseStorageImpl;
import org.datadryad.rest.storage.rdbms.OrganizationDatabaseStorageImpl;
import org.dspace.content.Collection;
import org.dspace.content.DCValue;
import org.dspace.content.Item;
import org.dspace.content.authority.AuthorityMetadataValue;
import org.dspace.content.authority.Concept;
import org.dspace.content.authority.Scheme;
import org.dspace.core.ConfigurationManager;
import org.dspace.core.Context;
import org.dspace.submit.bean.PublicationBean;
import org.dspace.workflow.DryadWorkflowUtils;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.sql.SQLException;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.w3c.dom.*;
import javax.xml.parsers.*;
/**
 * User: lantian @ atmire . com
 * Date: 9/12/14
 * Time: 12:18 PM
 */
public class JournalUtils {

    private static Logger log = Logger.getLogger(JournalUtils.class);
    public static final String FULLNAME = "fullname";
    public static final String METADATADIR = "metadataDir";
    public static final String INTEGRATED = "integrated";
    public static final String PUBLICATION_BLACKOUT = "publicationBlackout";
    public static final String NOTIFY_ON_REVIEW = "notifyOnReview";
    public static final String NOTIFY_ON_ARCHIVE = "notifyOnArchive";
    public static final String JOURNAL_ID = "journalID";
    public static final String SUBSCRIPTION_PAID = "subscriptionPaid";
    public static final String SUBSCRIPTION_PLAN = "SUBSCRIPTION";
    public static final String PREPAID_PLAN = "PREPAID";
    public static final String DEFERRED_PLAN = "DEFERRED";

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

    public static Concept getJournalConceptByCustomerID(Context context, String customerID) throws SQLException {
        try {
            List<Concept> concepts = Concept.findByConceptMetadata(context, customerID, "journal", "customerID");
            if (concepts.size() > 0) {
                Concept concept = concepts.get(0);
                return concept;
            }
        }
        catch(Exception e) {
                log.error("unable to find journal by AssociationAnywhere customer ID",e);
        }

        return null;
    }


    public static Concept getJournalConceptByShortID(Context context, String journalShortID) throws SQLException {
        try {
            List<Concept> concepts = Concept.findByConceptMetadata(context, journalShortID.toUpperCase(), "journal", "journalID");
            if (concepts.size() > 0) {
                Concept concept = concepts.get(0);
                return concept;
            }
        }
        catch(Exception e) {
                log.error("unable to find journal by journalID",e);
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
        AuthorityMetadataValue[] vals = concept.getMetadata("journal", "integrated", null, Item.ANY);
        if(vals != null && vals.length > 0)
            return vals[0].value;

        return null;
    }

    public static boolean getBooleanIntegrated(Concept concept) {
        AuthorityMetadataValue[] vals = concept.getMetadata("journal", "integrated", null, Item.ANY);
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

    public static String getCanonicalManuscriptID(Context context, Manuscript manuscript) {
        return getCanonicalManuscriptID(context, manuscript.manuscriptId, manuscript.organization.organizationCode);
    }

    public static String getCanonicalManuscriptID(Context context, String manuscriptId, String journalCode) {
        String canonicalID = manuscriptId;
        String regex = null;
        try {
            Concept concept = getJournalConceptByShortID(context, journalCode);
            AuthorityMetadataValue[] vals = concept.getMetadata("journal","canonicalManuscriptNumberPattern",null, Item.ANY);
            if(vals != null && vals.length > 0) {
                regex = vals[0].getValue();
                Matcher manuscriptMatcher = Pattern.compile(regex).matcher(canonicalID);
                if (manuscriptMatcher.find()) {
                    canonicalID = manuscriptMatcher.group(1);
                } else {
                    log.error("Manuscript " + manuscriptId + " does not match with the regex provided for " + journalCode);
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

    public static String getPaymentPlanType(Concept concept) {
        AuthorityMetadataValue[] vals = concept.getMetadata("journal","paymentPlanType",null, Item.ANY);
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
        AuthorityMetadataValue[] vals = concept.getMetadata("journal", "description", null, Item.ANY);
        if(vals != null && vals.length > 0)
            return vals[0].value;

        return null;
    }

    public static String getMemberName(Concept concept) {
        AuthorityMetadataValue[] vals = concept.getMetadata("journal", "memberName", null, Item.ANY);
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
    public static String findKeyByFullname(String fullname){
        Map<String, String> props = journalProperties.get(fullname);
        if(props!=null)
            return props.get(JOURNAL_ID);

        return null;
    }


    public static Map<String, String> getPropertiesByJournal(String key){
        return journalProperties.get(key);
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

        } catch (Exception e) {
            log.error("Error while loading journal properties for " + journal, e);
        }
        return myJournalProperties;

    }

    public static Boolean shouldEnterBlackoutByDefault(Context context, Item item, Collection collection) throws SQLException {
        JournalUtils.RecommendedBlackoutAction action = JournalUtils.recommendedBlackoutAction(context, item, collection);
        return (action == JournalUtils.RecommendedBlackoutAction.BLACKOUT_TRUE ||
                action == JournalUtils.RecommendedBlackoutAction.JOURNAL_NOT_INTEGRATED);
    }

    public static void writeManuscriptToDB(Context context, Manuscript manuscript) throws StorageException {
        String journalCode = cleanJournalCode(manuscript.organization.organizationCode).toUpperCase();
        StoragePath storagePath = StoragePath.createManuscriptPath(journalCode, manuscript.manuscriptId);

        ManuscriptDatabaseStorageImpl manuscriptStorage = new ManuscriptDatabaseStorageImpl();
        List<Manuscript> manuscripts = getManuscriptsMatchingID(journalCode, manuscript.manuscriptId);
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
            List<Organization> orgs = organizationStorage.getResults(storagePath, journalCode, 0);
            if (orgs.size() > 0) {
                ManuscriptDatabaseStorageImpl manuscriptStorage = new ManuscriptDatabaseStorageImpl();
                manuscripts.addAll(manuscriptStorage.getResults(storagePath, manuscriptId, 10));
            }
        } catch (StorageException e) {
            log.error("Exception getting manuscripts", e);
        }
        return manuscripts;
    }

    public static PublicationBean getPublicationBeanFromManuscript(Manuscript manuscript) {
        PublicationBean pBean = new PublicationBean();
        pBean.setManuscriptNumber(manuscript.manuscriptId);
        pBean.setJournalID(cleanJournalCode(manuscript.organization.organizationCode));
        pBean.setTitle(manuscript.title);
        pBean.setStatus(manuscript.getStatus());

        ArrayList<String> authorstrings = new ArrayList<String>();
        for (Author a : manuscript.authors.author) {

            authorstrings.add(a.familyName + ", " + a.givenNames);
        }
        pBean.setAuthors(authorstrings);

        // the rest of the fields are optional:
        if (manuscript.organization.organizationName != null) {
            pBean.setJournalName(manuscript.organization.organizationName);
        }
        if (manuscript.manuscript_abstract != null) {
            pBean.setAbstract(manuscript.manuscript_abstract);
        }
        if (manuscript.correspondingAuthor.author != null) {
            pBean.setCorrespondingAuthor(manuscript.correspondingAuthor.author.givenNames + " " + manuscript.correspondingAuthor.author.familyName);
        }
        if (manuscript.correspondingAuthor.email != null) {
            pBean.setEmail(manuscript.correspondingAuthor.email);
        }
        if (manuscript.optionalProperties != null) {
            String issn = manuscript.optionalProperties.get("ISSN");
            if (issn != null) {
                pBean.setJournalISSN(issn);
            }
        }
        ArrayList<String> subjectKeywords = new ArrayList<String>();
        for (String keyword : manuscript.keywords) {
            subjectKeywords.add(keyword);
        }
        pBean.setSubjectKeywords(subjectKeywords);

        if (manuscript.isSubmitted()) {
            pBean.setSkipReviewStep(false);
        } else if (manuscript.isAccepted() || manuscript.isRejected()) {
            pBean.setSkipReviewStep(true);
        }
        return pBean;
    }

    public static PublicationBean getPublicationBeanFromManuscriptStorage (String manuscriptNumber, String selectedJournalId) {
        PublicationBean pBean = null;
        Context context = null;
        try {
            context = new Context();
            Concept journalConcept = JournalUtils.getJournalConceptByShortID(context, selectedJournalId);
            // canonicalize the manuscriptNumber:
            manuscriptNumber = JournalUtils.getCanonicalManuscriptID(context, manuscriptNumber, selectedJournalId);
            // first, look for a matching manuscript in the database:
            List<Manuscript> manuscripts = JournalUtils.getManuscriptsMatchingID(selectedJournalId,manuscriptNumber);
            if (manuscripts.size() > 0) {
                log.info("found manuscript " + manuscriptNumber + " in database");
                pBean = JournalUtils.getPublicationBeanFromManuscript(manuscripts.get(0));
            } else {
                // if nothing, look in the metadata directory:
                String journalPath = "";
                journalPath = JournalUtils.getMetadataDir(journalConcept);
                pBean = JournalUtils.getDataFromPublisherFile(manuscriptNumber, selectedJournalId, journalPath);
                log.info("found manuscript " + manuscriptNumber + " in file");
            }
            context.complete();
        } catch (Exception e) {
            if (context != null) {
                context.abort();
            }
            //invalid journalID
            log.error("Error getting parameters for invalid JournalID: " + selectedJournalId, e);
        }
        return pBean;
    }

    /**
     * Initialize a publication bean from a publisher's data file.
     * This method can be removed when we finally deprecate the XML file system.
     */
    private static PublicationBean getDataFromPublisherFile(String manuscriptNumber, String journalID, String journalPath)
    {
        // TODO: This method has very rudimentary XML parsing. It would be much better to actually run it through the Java XML parser.

        log.debug("getting data for manu " + manuscriptNumber + " from journal " + journalID + " at path " + journalPath);

        // read data from file and add it to bean;
        PublicationBean pbean = new PublicationBean();
        pbean.setMetadataFromJournal(true);
        File f = new File(journalPath + File.separator + manuscriptNumber + ".xml");
        // for a List object for a multipe node
        List<String> authors = new ArrayList<String>();
        List<String> keywords = new ArrayList<String>();

        pbean.setJournalID(journalID);
        // read the metadata xml by DOM level 3 LSParser
        // InputStream in = new FileInputStream(f);

        try {
            List<String>  xmlTagNameList = Arrays.asList(
                    "Journal", "ISSN", "Manuscript", "Article_Title",
                    "Article_Type", "Author", "Email", "Corresponding_Author",
                    "keyword", "Abstract", "Article_Status", "Citation_Title",
                    "Citation_Authors", "Publication_DOI"
            );

            DocumentBuilder builder =
                    DocumentBuilderFactory.newInstance().newDocumentBuilder();

            Document doc = builder.parse(f);

            Element root = doc.getDocumentElement();

            for (String tag: xmlTagNameList){
                NodeList nl = root.getElementsByTagName(tag);
                log.debug(tag+"(how many nodes="+nl.getLength()+"):");

                for (int i=0;i<nl.getLength();i++){
                    String text = nl.item(i).getTextContent();
                    log.debug(text);

                    if (tag.equals("Corresponding_Author")){
                        pbean.setCorrespondingAuthor(StringEscapeUtils.unescapeXml(text));
                    }

                    if (tag.equals("Article_Title")){
                        pbean.setTitle(StringEscapeUtils.unescapeXml(text));
                    }

                    if (tag.equals("Journal")){
                        pbean.setJournalName(StringEscapeUtils.unescapeXml(text));
                    }

                    if (tag.equals("Manuscript")){
                        pbean.setManuscriptNumber(StringEscapeUtils.unescapeXml(text));
                    }
                    if (tag.equals("ISSN")){
                        pbean.setJournalISSN(StringEscapeUtils.unescapeXml(text));
                    }

                    if (tag.equals("Abstract")){
                        pbean.setAbstract(StringEscapeUtils.unescapeXml(text));
                    }

                    if (tag.equals("Author")){
                        authors.add(StringEscapeUtils.unescapeXml(text).trim());
                    }
                    if (tag.equals("keyword")){
                        keywords.add(StringEscapeUtils.unescapeXml(text).trim());
                    }
                    if(tag.equalsIgnoreCase("Email")){
                        pbean.setEmail(StringEscapeUtils.unescapeXml(text).trim());
                    }
                    if(tag.equals("Article_Status")){
                        String ttext = text.trim().toLowerCase();

                        pbean.setStatus(ttext);


                        if(ttext.equals("submitted") ||
                                ttext.equals("in review")   ||
                                ttext.equals("under review")  ||
                                ttext.equals("revision in review") ||
                                ttext.equals("revision under review")
                                ) {
                            pbean.setSkipReviewStep(false);
                        } else if(ttext.equals("accepted") ||
                                ttext.startsWith("reject") ||
                                ttext.equals("open reject") ||
                                ttext.equals("transferred") ||
                                ttext.equals("needs revision")) {
                            pbean.setSkipReviewStep(true);
                        } else {
                            log.error("unexpected article status " + text.trim());
                        }

                    }
                    if(tag.equals("Article_Type")) {
                        pbean.setArticleType(StringEscapeUtils.unescapeXml(text).trim());
                    }
                    if(tag.equals("Citation_Title")) {
                        pbean.setCitationTitle(StringEscapeUtils.unescapeXml(text).trim());
                    }
                    if(tag.equals("Citation_Authors")) {
                        pbean.setCitationAuthors(StringEscapeUtils.unescapeXml(text).trim());
                    }
                    if(tag.equals("Publication_DOI")) {
                        String doi = StringEscapeUtils.unescapeXml(text).trim();
                        if (doi != null && doi.length() > 0) {
                            doi = doi.trim();
                            if (doi.startsWith("http://dx.doi.org/")) {
                                doi = doi.replace("http://dx.doi.org/", "doi:");
                            }
                            if (doi.startsWith("10.")) {
                                doi = "doi:" + doi;
                            }
                        }
                        pbean.setDOI(doi);
                    }

                }

            }
        }
        catch(FileNotFoundException ex)
        {
            log.error("unable to find manuscript file " + f, ex);
            pbean.setMessage("Invalid manuscript number");
        }
        catch(IOException ex)
        {
            log.error("unable to read from manuscript file " + f, ex);
            pbean.setMessage("Internal Error: Cannot import publication description. Please notify the Dryad administrators at help@datadryad.org");

        }
        catch (Exception e) {
            log.fatal("Exception occurred while parsing:", e);
            pbean.setMessage("Internal Error: Cannot import publication description because it contains invalid xml. Please notify the Dryad administrators at help@datadryad.org");
        }

        // set list of author names to the bean only
        // when the entire file is read
        pbean.setAuthors(authors);
        if(keywords.size() > 0) {
            pbean.setSubjectKeywords(keywords);
        }


        return pbean;
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
