package org.datadryad.authority;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.PosixParser;
import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.response.QueryResponse;
import org.apache.solr.common.SolrDocument;
import org.apache.solr.common.SolrDocumentList;
import org.dspace.authority.AuthoritySearchService;
import org.dspace.authority.AuthorityValue;
import org.dspace.content.authority.Concept;
import org.dspace.content.authority.Scheme;
import org.dspace.content.authority.Term;
import org.dspace.core.ConfigurationManager;
import org.dspace.core.Context;
import org.dspace.core.Utils;
import org.dspace.utils.DSpace;
import org.datadryad.api.DryadJournalConcept;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.*;

/**
 * User: lantian @ atmire . com
 * Date: 4/17/14
 * Time: 1:25 PM
 */
public final class ImportJournal2Authority {


    /** DSpace Context object */
    private Context context;
    // Reading DryadJournalSubmission.properties

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
                instituteScheme.setLastModified(context, date);
                instituteScheme.setCreated(context, date);
                instituteScheme.setLang(context, "en");
                instituteScheme.setStatus(context, "Published");
            }


            context.commit();
            //Get all journal configurations
            Map<String, Map<String, String>> journalProperties = getJournals();


            if(authDocs != null) {
                int maxDocs = authDocs.size();

                //import all the authors
                for (int i = 0; i < maxDocs; i++) {
                    SolrDocument solrDocument = authDocs.get(i);
                    if (solrDocument != null) {
                        AuthorityValue authorityValue = new AuthorityValue(solrDocument);
                        if (authorityValue.getId() != null) {
                            ArrayList<Concept> aConcepts = Concept.findByIdentifier(context,authorityValue.getId());
                            if (aConcepts==null||aConcepts.size()==0) {
                                Concept aConcept = instituteScheme.createConcept(context, authorityValue.getId());
                                aConcept.setLastModified(context, authorityValue.getLastModified());
                                aConcept.setCreated(context, authorityValue.getCreationDate());
                                aConcept.setLang(context, "en");
                                aConcept.setStatus(context, Concept.Status.ACCEPTED.name());
                                aConcept.setTopConcept(context, true);
                                String fullName = authorityValue.getValue();

                                if (solrDocument.getFieldValue("source")!=null) {
                                    String source = String.valueOf(solrDocument.getFieldValue("source"));
                                    aConcept.setSource(context, source);
                                    if (source.equals("LOCAL-DryadJournal")) {
                                        Map<String,String> val = journalProperties.get(authorityValue.getValue());
                                        if (val!=null) {
                                            journalProperties.remove(authorityValue.getValue());
                                            for (String prop : DryadJournalConcept.journalMetadata.stringPropertyNames()) {
                                                if (val.get(prop)!=null && val.get(prop).length()>0) {
                                                    aConcept.addMetadata(context, "journal",prop,null,"",val.get(prop),authorityValue.getId(),0);
                                                }
                                            }
                                        }
                                    }
                                }
                                Term aTerm = aConcept.createTerm(context, fullName,Term.prefer_term);
                                aTerm.setStatus(context, Concept.Status.ACCEPTED.name());
                                context.commit();
                            }
                        }
                    }
                }
            }

            Set<String> keys = journalProperties.keySet();
            for(String key : keys) {
                Map<String,String> val = journalProperties.get(key);
                Concept[] aConcepts = Concept.findByPreferredLabel(context,val.get("fullname"),instituteScheme.getID());
                if (aConcepts==null||aConcepts.length==0) {
                    Concept aConcept = instituteScheme.createConcept(context);
                    aConcept.setLastModified(context, date);
                    aConcept.setCreated(context, date);
                    aConcept.setLang(context, "en");
                    aConcept.setStatus(context, Concept.Status.ACCEPTED.name());
                    aConcept.setTopConcept(context, true);
                    for (String prop : DryadJournalConcept.journalMetadata.stringPropertyNames()) {
                        if (val.get(prop)!=null && val.get(prop).length()>0) {
                            aConcept.addMetadata(context, "journal",prop,null,"",val.get(prop),aConcept.getIdentifier(),0);
                        }
                    }
                    Term aTerm = aConcept.createTerm(context, val.get("fullname"),Term.prefer_term);
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


    private HashMap<String, Map<String, String>> getJournals() {

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
                for (String prop : DryadJournalConcept.journalMetadata.stringPropertyNames()) {
                    String newprop = properties.getProperty(str + prop);
                    if (newprop != null) {
                        map.put(prop, newprop);
                    }
                }
                if (map.get(str + DryadJournalConcept.PARSING_SCHEME) != null) {
                    map.put(str + DryadJournalConcept.PARSING_SCHEME, "false");
                }
                if (map.get(str + DryadJournalConcept.PUBLICATION_BLACKOUT) != null) {
                    map.put(str + DryadJournalConcept.PUBLICATION_BLACKOUT, "false");
                }
                map.put(DryadJournalConcept.JOURNAL_ID, journalType);

                String key = properties.getProperty(str + DryadJournalConcept.FULLNAME);
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


        String value = props.get(DryadJournalConcept.FULLNAME);

        String integratedJournal = props.get(DryadJournalConcept.INTEGRATED);

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
