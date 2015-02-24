package org.dspace.app.xmlui.aspect.authority.util;

import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.PosixParser;
import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.response.QueryResponse;
import org.apache.solr.common.SolrDocument;
import org.apache.solr.common.SolrDocumentList;
import org.dspace.authority.AuthoritySearchService;
import org.dspace.authority.AuthorityValue;
import org.dspace.content.authority.AuthorityMetadataValue;
import org.dspace.content.authority.Concept;
import org.dspace.content.authority.Scheme;
import org.dspace.content.authority.Term;
import org.dspace.core.ConfigurationManager;
import org.dspace.core.Context;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.*;

import org.apache.commons.cli.CommandLine;

import org.apache.commons.cli.Options;


import org.dspace.core.Utils;
import org.dspace.utils.DSpace;

/**
 * User: lantian @ atmire . com
 * Date: 4/17/14
 * Time: 1:25 PM
 */
public final class ImportJournal2Authority {


    /** DSpace Context object */
    private Context context;
    // Reading DryadJournalSubmission.properties
    public static final String JOURNAL_ID = "journalID";
    public static final String ALLOW_REVIEW_WORKFLOW = "allowReviewWorkflow";
    public static final String EMBARGO_ALLOWED = "embargoAllowed";
    public static final String FULLNAME = "fullname";
    public static final String INTEGRATED = "integrated";
    public static final String MANUSCRIPT_NUMBER_IGNORE_PATTERN = "manuscriptNumberIgnorePattern";
    public static final String METADATADIR = "metadataDir";
    public static final String NOTIFY_ON_ARCHIVE = "notifyOnArchive";
    public static final String NOTIFY_ON_REVIEW = "notifyOnReview";
    public static final String NOTIFY_WEEKLY = "notifyWeekly";
    public static final String PARSING_SCHEME = "parsingScheme";
    public static final String PUBLICATION_BLACKOUT = "publicationBlackout";
    public static final String SPONSOR_NAME = "sponsorName";
    public static final String SUBSCRIPTION_PAID = "subscriptionPaid";

    /**
     * For invoking via the command line.  If called with no command line arguments,
     * it will negotiate with the user for the administrator details
     *
     * @param argv
     *            command-line arguments
     */
    public static void main(String[] argv)
            throws Exception
    {
        CommandLineParser parser = new PosixParser();
        Options options = new Options();

        ImportJournal2Authority ca = new ImportJournal2Authority();
        options.addOption("t", "test", true, "test mode");

        CommandLine line = parser.parse(options, argv);
        ca.importAuthority(line.getOptionValue("t"));
    }



    /**
     * constructor, which just creates and object with a ready context
     *
     * @throws Exception
     */
    private ImportJournal2Authority()
            throws Exception
    {
        context = new Context();
    }

    /**
     * Create the administrator with the given details.  If the user
     * already exists then they are simply upped to administrator status
     *
     * @throws Exception
     */
    private void importAuthority(String test)
            throws Exception
    {
        // Of course we aren't an administrator yet so we need to
        // circumvent authorisation
        context.setIgnoreAuthorization(true);

        try {
            SolrQuery queryArgs = new SolrQuery();
            queryArgs.setQuery("field:prism.publicationName");
            queryArgs.setRows(-1);
            QueryResponse searchResponse = getSearchService().search(queryArgs);
            SolrDocumentList authDocs = searchResponse.getResults();
            int max = (int) searchResponse.getResults().getNumFound();

            queryArgs.setQuery("*:*");
            if(test!=null)
            {
                queryArgs.setRows(Integer.parseInt(test));
            }
            else
            {
                queryArgs.setRows(max);
            }

            searchResponse = getSearchService().search(queryArgs);
            authDocs = searchResponse.getResults();
            Date date = new Date();
            Scheme instituteScheme = Scheme.findByIdentifier(context,"Journal");
            if(instituteScheme==null){
                instituteScheme = Scheme.create(context, "Journal");
                instituteScheme.setLastModified(date);
                instituteScheme.setCreated(date);
                instituteScheme.setLang("en");
                instituteScheme.setStatus("Published");
                instituteScheme.update();
            }


            context.commit();
            //Get all journal configurations
            java.util.Map<String, Map<String, String>> journalProperties = getJournals();


            if(authDocs != null){
                int maxDocs = authDocs.size();

                //import all the authors
                for (int i = 0; i < maxDocs; i++) {
                    SolrDocument solrDocument = authDocs.get(i);
                    if(solrDocument != null){
                        AuthorityValue authorityValue = new AuthorityValue(solrDocument);
                        if(authorityValue.getId() != null){
                            ArrayList<Concept> aConcepts = Concept.findByIdentifier(context,authorityValue.getId());
                            if(aConcepts==null||aConcepts.size()==0)  {
                                Concept aConcept = instituteScheme.createConcept(authorityValue.getId());
                                aConcept.setLastModified(authorityValue.getLastModified());
                                aConcept.setCreated(authorityValue.getCreationDate());
                                aConcept.setLang("en");
                                aConcept.setStatus(Concept.Status.ACCEPTED);
                                aConcept.setTopConcept(true);
                                String fullName = authorityValue.getValue();

                                if(solrDocument.getFieldValue("source")!=null) {
                                    String source = String.valueOf(solrDocument.getFieldValue("source"));
                                    aConcept.setSource(source);
                                    if(source.equals("LOCAL-DryadJournal"))
                                    {
                                        Map<String,String> val = journalProperties.get(authorityValue.getValue());
                                        if(val!=null){
                                            journalProperties.remove(authorityValue.getValue());
                                            if(val.get("journalID")!=null&&val.get("journalID").length()>0)
                                            {
                                                aConcept.addMetadata("journal","journalID",null,"",val.get("journalID"),authorityValue.getId(),0);
                                            }
                                            if(val.get("fullname")!=null&&val.get("fullname").length()>0)
                                            {
                                                aConcept.addMetadata("journal","fullname",null,"",val.get("fullname"),authorityValue.getId(),0);
                                                fullName =  val.get("fullname");
                                            }

                                            if(val.get("metadataDir")!=null&&val.get("metadataDir").length()>0)
                                            {
                                                aConcept.addMetadata("journal","metadataDir",null,"",val.get("metadataDir"),authorityValue.getId(),0);
                                            }
                                            if(val.get("parsingScheme")!=null&&val.get("parsingScheme").length()>0)
                                            {
                                                aConcept.addMetadata("journal","parsingScheme",null,"",val.get("parsingScheme"),authorityValue.getId(),0);
                                            }
                                            if(val.get("integrated")!=null&&val.get("integrated").length()>0)
                                            {
                                                aConcept.addMetadata("journal","integrated",null,"",val.get("integrated"),authorityValue.getId(),0);
                                            }
                                            if(val.get("embargoAllowed")!=null&&val.get("embargoAllowed").length()>0)
                                            {
                                                aConcept.addMetadata("journal","embargoAllowed",null,"",val.get("embargoAllowed"),authorityValue.getId(),0);
                                            }
                                            if(val.get("allowReviewWorkflow")!=null&&val.get("allowReviewWorkflow").length()>0)
                                            {
                                                aConcept.addMetadata("journal","allowReviewWorkflow",null,"",val.get("allowReviewWorkflow"),authorityValue.getId(),0);
                                            }
                                            if(val.get("publicationBlackout")!=null&&val.get("publicationBlackout").length()>0)
                                            {
                                                aConcept.addMetadata("journal","publicationBlackout",null,"",val.get("publicationBlackout"),authorityValue.getId(),0);
                                            }
                                            if(val.get("subscriptionPaid")!=null&&val.get("subscriptionPaid").length()>0)
                                            {
                                                aConcept.addMetadata("journal","subscriptionPaid",null,"",val.get("subscriptionPaid"),authorityValue.getId(),0);
                                            }
                                            if(val.get("sponsorName")!=null&&val.get("sponsorName").length()>0)
                                            {
                                                aConcept.addMetadata("journal","sponsorName",null,"",val.get("sponsorName"),authorityValue.getId(),0);
                                            }
                                            if(val.get("notifyOnReview")!=null&&val.get("notifyOnReview").length()>0)
                                            {
                                                aConcept.addMetadata("journal","notifyOnReview",null,"",val.get("notifyOnReview"),authorityValue.getId(),0);
                                            }
                                            if(val.get("notifyOnArchive")!=null&&val.get("notifyOnArchive").length()>0)
                                            {
                                                aConcept.addMetadata("journal","notifyOnArchive",null,"",val.get("notifyOnArchive"),authorityValue.getId(),0);
                                            }
                                            if(val.get("notifyWeekly")!=null&&val.get("notifyWeekly").length()>0)
                                            {
                                                aConcept.addMetadata("journal","notifyWeekly",null,"",val.get("notifyWeekly"),authorityValue.getId(),0);
                                            }
                                            if(val.get("manuscriptNumberIgnorePattern")!=null&&val.get("manuscriptNumberIgnorePattern").length()>0)
                                            {
                                                aConcept.addMetadata("journal","manuscriptNumberIgnorePattern",null,"",val.get("manuscriptNumberIgnorePattern"),authorityValue.getId(),0);
                                            }
                                        }
                                    }
                                }
                                aConcept.update();
                                Term aTerm = aConcept.createTerm(fullName,Term.prefer_term);
                                aTerm.setStatus(Concept.Status.ACCEPTED.name());
                                aTerm.update();
                                context.commit();
                            }
                        }

                    }
                }

            }

            Set<String> keys = journalProperties.keySet();
            for(String key : keys)
            {
                Map<String,String> val = journalProperties.get(key);
                Concept[] aConcepts = Concept.findByPreferredLabel(context,val.get("fullname"),instituteScheme.getID());
                if(aConcepts==null||aConcepts.length==0)  {
                    Concept aConcept = instituteScheme.createConcept();
                    aConcept.setLastModified(date);
                    aConcept.setCreated(date);
                    aConcept.setLang("en");
                    aConcept.setTopConcept(true);
                    aConcept.setSource("LOCAL-DryadJournal");

                    if(val.get("journalID")!=null&&val.get("journalID").length()>0)
                    {
                        aConcept.addMetadata("journal","journalID",null,"",val.get("journalID"),aConcept.getIdentifier(),0);
                    }
                    if(val.get("fullname")!=null&&val.get("fullname").length()>0)
                    {
                        aConcept.addMetadata("journal","fullname",null,"",val.get("fullname"),aConcept.getIdentifier(),0);
                    }

                    if(val.get("metadataDir")!=null&&val.get("metadataDir").length()>0)
                    {
                        aConcept.addMetadata("journal","metadataDir",null,"",val.get("metadataDir"),aConcept.getIdentifier(),0);
                    }
                    if(val.get("parsingScheme")!=null&&val.get("parsingScheme").length()>0)
                    {
                        aConcept.addMetadata("journal","parsingScheme",null,"",val.get("parsingScheme"),aConcept.getIdentifier(),0);
                    }
                    if(val.get("integrated")!=null&&val.get("integrated").length()>0)
                    {
                        aConcept.addMetadata("journal","integrated",null,"",val.get("integrated"),aConcept.getIdentifier(),0);
                    }
                    if(val.get("embargoAllowed")!=null&&val.get("embargoAllowed").length()>0)
                    {
                        aConcept.addMetadata("journal","embargoAllowed",null,"",val.get("embargoAllowed"),aConcept.getIdentifier(),0);
                    }
                    if(val.get("allowReviewWorkflow")!=null&&val.get("allowReviewWorkflow").length()>0)
                    {
                        aConcept.addMetadata("journal","allowReviewWorkflow",null,"",val.get("allowReviewWorkflow"),aConcept.getIdentifier(),0);
                    }
                    if(val.get("publicationBlackout")!=null&&val.get("publicationBlackout").length()>0)
                    {
                        aConcept.addMetadata("journal","publicationBlackout",null,"",val.get("publicationBlackout"),aConcept.getIdentifier(),0);
                    }
                    if(val.get("subscriptionPaid")!=null&&val.get("subscriptionPaid").length()>0)
                    {
                        aConcept.addMetadata("journal","subscriptionPaid",null,"",val.get("subscriptionPaid"),aConcept.getIdentifier(),0);
                    }
                    if(val.get("sponsorName")!=null&&val.get("sponsorName").length()>0)
                    {
                        aConcept.addMetadata("journal","sponsorName",null,"",val.get("sponsorName"),aConcept.getIdentifier(),0);
                    }
                    if(val.get("notifyOnReview")!=null&&val.get("notifyOnReview").length()>0)
                    {
                        aConcept.addMetadata("journal","notifyOnReview",null,"",val.get("notifyOnReview"),aConcept.getIdentifier(),0);
                    }
                    if(val.get("notifyOnArchive")!=null&&val.get("notifyOnArchive").length()>0)
                    {
                        aConcept.addMetadata("journal","notifyOnArchive",null,"",val.get("notifyOnArchive"),aConcept.getIdentifier(),0);
                    }
                    if(val.get("notifyWeekly")!=null&&val.get("notifyWeekly").length()>0)
                    {
                        aConcept.addMetadata("journal","notifyWeekly",null,"",val.get("notifyWeekly"),aConcept.getIdentifier(),0);
                    }
                    if(val.get("manuscriptNumberIgnorePattern")!=null&&val.get("manuscriptNumberIgnorePattern").length()>0)
                    {
                        aConcept.addMetadata("journal","manuscriptNumberIgnorePattern",null,"",val.get("manuscriptNumberIgnorePattern"),aConcept.getIdentifier(),0);
                    }
                    aConcept.update();


                    Term aTerm = aConcept.createTerm(val.get("fullname"),Term.prefer_term);
                    aTerm.update();
                    context.commit();
                }
            }

        }catch (Exception e)
        {
            System.out.print(e);
            System.out.print(e.getStackTrace());
        }

        context.complete();

        System.out.println("Authority imported");
    }


    private HashMap<String, Map<String, String>> getJournals(){

        HashMap<String, Map<String, String>> journalProperties = new HashMap<String, Map<String, String>>();

        String journalPropFile = ConfigurationManager.getProperty("submit.journal.config");
        Properties properties = new Properties();
        try {
            properties.load(new InputStreamReader(new FileInputStream(journalPropFile), "UTF-8"));
            String journalTypes = properties.getProperty("journal.order");

            for (int i = 0; i < journalTypes.split(",").length; i++) {
                String journalType = journalTypes.split(",")[i].trim();

                String str = "journal." + journalType + ".";

                Map<String, String> map = new HashMap<String, String>();
                map.put(JOURNAL_ID, journalType);
                map.put(ALLOW_REVIEW_WORKFLOW, properties.getProperty(str + ALLOW_REVIEW_WORKFLOW));
                map.put(EMBARGO_ALLOWED, properties.getProperty(str + EMBARGO_ALLOWED));
                map.put(FULLNAME, properties.getProperty(str + FULLNAME));
                map.put(INTEGRATED, properties.getProperty(str + INTEGRATED));
                map.put(MANUSCRIPT_NUMBER_IGNORE_PATTERN, properties.getProperty(str + MANUSCRIPT_NUMBER_IGNORE_PATTERN));
                map.put(METADATADIR, properties.getProperty(str + METADATADIR));
                map.put(NOTIFY_ON_ARCHIVE, properties.getProperty(str + NOTIFY_ON_ARCHIVE));
                map.put(NOTIFY_ON_REVIEW, properties.getProperty(str + NOTIFY_ON_REVIEW));
                map.put(NOTIFY_WEEKLY, properties.getProperty(str + NOTIFY_WEEKLY));
                map.put(PARSING_SCHEME, properties.getProperty(str + PARSING_SCHEME, "false"));
                map.put(PUBLICATION_BLACKOUT, properties.getProperty(str + PUBLICATION_BLACKOUT, "false"));
                map.put(SPONSOR_NAME, properties.getProperty(str + SPONSOR_NAME));
                map.put(SUBSCRIPTION_PAID, properties.getProperty(str + SUBSCRIPTION_PAID));


                String key = properties.getProperty(str + FULLNAME);
                if(key!=null&&key.length()>0){
                    journalProperties.put(key, map);
                }
            }

        }catch (IOException e) {
            //log.error("Error while loading journal properties", e);
        }
        return journalProperties;

    }

    // Costants for SOLR DOC
    public static final String DOC_ID="id";
    public static final String DOC_DISPLAY_VALUE="display-value";
    public static final String DOC_VALUE="value";
    public static final String DOC_FULL_TEXT="full-text";
    public static final String DOC_FIELD="field";
    public static final String DOC_SOURCE="source";
    private String SOURCE="LOCAL";
    private Map<String, String> createHashMap(Map<String, String> props) throws Exception {
        Map<String, String> values = new HashMap <String, String>();


        String value = props.get(FULLNAME);

        String integratedJournal = props.get(INTEGRATED);
//        if(integratedJournal!=null && integratedJournal.equals("true"))
//            value+="*";

        values.put(DOC_ID, Utils.getMD5(value));
        values.put(DOC_SOURCE, SOURCE);
        values.put(DOC_FIELD, "prism.publicationName");
        values.put(DOC_DISPLAY_VALUE, value);
        values.put(DOC_VALUE, value);
        values.put(DOC_FULL_TEXT, value);
        return values;
    }

    private AuthoritySearchService getSearchService(){
        DSpace dspace = new DSpace();

        org.dspace.kernel.ServiceManager manager = dspace.getServiceManager() ;

        return manager.getServiceByName(AuthoritySearchService.class.getName(),AuthoritySearchService.class);
    }
}
