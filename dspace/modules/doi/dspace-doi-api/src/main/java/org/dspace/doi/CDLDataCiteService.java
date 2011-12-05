package org.dspace.doi;

import java.io.IOException;
import java.lang.String;
import java.sql.SQLException;
import java.util.*;

import javax.mail.MessagingException;

import com.sun.corba.se.impl.orbutil.concurrent.Sync;
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.UsernamePasswordCredentials;
import org.apache.commons.httpclient.auth.AuthPolicy;
import org.apache.commons.httpclient.auth.AuthScope;
import org.apache.commons.httpclient.methods.*;
import org.apache.commons.httpclient.params.HttpMethodParams;
import org.apache.log4j.Logger;
import org.dspace.content.DCValue;
import org.dspace.content.DSpaceObject;
import org.dspace.content.Item;
import org.dspace.content.ItemIterator;
import org.dspace.core.ConfigurationManager;
import org.dspace.core.Email;
import org.dspace.core.I18nUtil;
import org.dspace.identifier.IdentifierNotFoundException;
import org.dspace.identifier.IdentifierNotResolvableException;
import org.dspace.identifier.IdentifierService;
import org.dspace.utils.DSpace;

public class CDLDataCiteService {

    private static final Logger log = Logger.getLogger(CDLDataCiteService.class);

    private static final String BASEURL = "https://n2t.net/ezid";

    private String myUsername;
    private String myPassword;

    public static final String DC_CREATOR = "dc.creator";
    public static final String DC_TITLE = "dc.title";
    public static final String DC_PUBLISHER = "dc.publisher";
    public static final String DC_DATE_AVAILABLE = "dc.date.available";
    public static final String DC_DATE= "dc.date";
    public static final String DC_SUBJECT= "dc.subject";
    public static final String DC_RELATION_ISREFERENCEBY= "dc.relation.isreferencedby";
    public static final String DC_RIGHTS= "dc.rights";
    public static final String DC_DESCRIPTION= "dc.description";


    public static final String DATACITE_CREATOR = "datacite.creator";
    public static final String DATACITE_TITLE = "datacite.title";
    public static final String DATACITE_PUBLISHER = "datacite.publisher";
    public static final String DATACITE_PUBBLICATIONYEAR = "datacite.publicationyear";

    public String publisher = null;


    int registeredItems=0;
    int synchItems=0;
    int notProcessItems=0;
    int itemsWithErrors=0;



    public CDLDataCiteService(final String aUsername, final String aPassword) {
        myUsername = aUsername;
        myPassword = aPassword;
    }

    /**
     * @param aDOI A DOI in the form <code>10.5061/dryad.1731</code>
     * @param aURL A URL in the form
     *             <code>http://datadryad.org/handle/10255/dryad.1731</code>
     * @return A response message from the remote service
     * @throws IOException If there was trouble connection and communicating to
     *                     the remote service
     */
    public String registerDOI(String aDOI, String aURL, Map<String, String> metadata) throws IOException {
        changePrefixDOIForTestEnv(aDOI);

        if(aDOI.startsWith("doi")){
            aDOI = aDOI.substring(4);
        }

        PutMethod put = new PutMethod(BASEURL + "/id/doi%3A" + aDOI);
        return executeHttpMethod(aURL, metadata, put);
    }


    /**
     * @param aDOI A DOI in the form <code>10.5061/dryad.1731</code>
     * @param aURL A URL in the form
     *             <code>http://datadryad.org/handle/10255/dryad.1731</code>
     * @return A response message from the remote service
     * @throws IOException If there was trouble connection and communicating to
     *                     the remote service
     */
    public String update(String aDOI, String aURL, Map<String, String> metadata) throws IOException {
        changePrefixDOIForTestEnv(aDOI);

        if(aDOI.startsWith("doi")){
            aDOI = aDOI.substring(4);
        }

        PostMethod post = new PostMethod(BASEURL + "/id/doi%3A" + aDOI);

        if(aURL!=null)
            return executeHttpMethod(aURL, metadata, post);

        return executeHttpMethod(null, metadata, post);

    }

    public String lookup(String aDOI) throws IOException {
        changePrefixDOIForTestEnv(aDOI);

        if(aDOI.startsWith("doi")){
            aDOI = aDOI.substring(4);
        }

        GetMethod get = new GetMethod(BASEURL + "/id/doi%3A" + aDOI);
        HttpMethodParams params = new HttpMethodParams();

        get.setRequestHeader("Content-Type", "text/plain");
        get.setRequestHeader("Accept", "text/plain");

        this.getClient(true).executeMethod(get);


        String response = get.getResponseBodyAsString();
        return response;
    }


    private String executeHttpMethod(String aURL, Map<String, String> metadata, EntityEnclosingMethod httpMethod) throws IOException {

        HashMap<String, String> map = new HashMap<String, String>();

        if(aURL!=null)
            map.put("_target", aURL);

        if (log.isDebugEnabled()) {
            log.debug("Adding _target to metadata for update: " + aURL);
        }

        if (metadata != null) {
	        log.debug("Adding other metadata");
            map.putAll(metadata);
	    }

        //logMetadata(metadata);
	
        httpMethod.setRequestEntity(new StringRequestEntity(encodeAnvl(map), "text/plain", "UTF-8"));

        httpMethod.setRequestHeader("Content-Type", "text/plain");
        httpMethod.setRequestHeader("Accept", "text/plain");

        this.getClient(false).executeMethod(httpMethod);
        return httpMethod.getStatusLine().toString();
    }

    private void logMetadata(Map<String, String> metadata) {
        log.debug("Adding the following Metadata:");
        if(metadata!=null){
            Set<String> keys = metadata.keySet();
            for(String key : keys){
                log.debug(key + ": " + metadata.get(key));
            }
        }
    }


    private String changePrefixDOIForTestEnv(String doi){

        // if test env
//        if(!ConfigurationManager.getBooleanProperty("doi.datacite.connected", false)){
//            doi = doi.substring(doi.indexOf('/')+1);
//            doi = "doi:10.5072/" + doi;
//        }
        return doi;
    }



    public void synchAll(){

        registeredItems=0;
        synchItems=0;
        notProcessItems=0;
        itemsWithErrors=0;

        int itemCounter=0;

        System.out.println("Starting....");
        Item item  = null;
        String doi = null;
        List<Item> itemsToProcess=new ArrayList<Item>();
        try {
            itemsToProcess = getItems();

            System.out.println("Item to process: " + itemsToProcess.size());

            for(Item item1 : itemsToProcess){

                itemCounter++;
                System.out.println("processing: " + itemCounter + " of " + itemsToProcess.size());

                item=item1;
                doi = getDoiValue(item);

                if(doi!=null){
                    String response = lookup(doi);

                    if(response.contains("invalid DOI identifier")){
                        registerItem(item, doi);
                        registeredItems++;
                    }
                    else{
                        updateItem(item, doi);
                        synchItems++;
                    }
                }
                else{

                    // Impossible to process
                    System.out.println("Item not processed because doi is absent: " + item.getID());
                    notProcessItems++;
                }
            }

        } catch (SQLException e) {
            System.out.println("problem with Item: " + (item !=null ? item.getID() : null) + " - " + doi);
            e.printStackTrace(System.out);
            itemsWithErrors++;

        } catch (IOException e) {
            System.out.println("problem with Item: " + (item !=null ? item.getID() : null) + " - " + doi);
            e.printStackTrace(System.out);
            itemsWithErrors++;

        }

        System.out.println("Synchronization executed. Prcocessed Items:" + itemsToProcess.size() + " registeredItems:" + registeredItems + " updateItems:" + synchItems + " notProcessedItems:" + notProcessItems + " itemsWithErrors:" + itemsWithErrors);
    }


    private List<Item> getItems() throws SQLException {
        org.dspace.core.Context context = new org.dspace.core.Context();
        context.turnOffAuthorisationSystem();
        ItemIterator items = Item.findAll(context);

        // clean list item, process only dataPackages or DataFiles
        List<Item> itemsToProcess = new ArrayList<Item>();
        while (items.hasNext()) {
            Item item = items.next();
            String doi = getDoiValue(item);
            if (doi != null && doi.startsWith("doi")) {
                itemsToProcess.add(item);
            }
        }
        return itemsToProcess;
    }


    private void updateItem(Item item, String doi) throws IOException {
        try{
            System.out.println("Update Item: " + doi + " result: " + this.update(doi, null, createMetadataList(item)));

        }catch (DOIFormatException de){
            System.out.println("Can't synch the following Item: " + item.getID() + " - " + doi);
            de.printStackTrace(System.out);
            itemsWithErrors++;
        }
    }



    private void registerItem(Item item, String doi) throws IOException {
        try{
            DOI doiObj = new DOI(doi, item);

            System.out.println("Register Item: " + doi + " result: " + this.registerDOI(doi, doiObj.getTargetURL().toString(),  createMetadataList(item)));

        }catch (DOIFormatException de){
            System.out.println("Can't register the following Item: " + item.getID() + " - " + doi);
            de.printStackTrace(System.out);
            itemsWithErrors++;
        }
    }


    private String escape(String s) {
        return s.replace("%", "%25").replace("\n", "%0A").
                replace("\r", "%0D").replace(":", "%3A");
    }


    HttpClient client = new HttpClient();

    private HttpClient getClient(boolean lookup) throws IOException {
        List authPrefs = new ArrayList(2);
        authPrefs.add(AuthPolicy.DIGEST);
        authPrefs.add(AuthPolicy.BASIC);
        client.getParams().setParameter(AuthPolicy.AUTH_SCHEME_PRIORITY, authPrefs);
        if(!lookup) client.getState().setCredentials(AuthScope.ANY, new UsernamePasswordCredentials(myUsername, myPassword));
        return client;
    }


    /**
     * Have to test this on dev since it's also IP restricted.
     *
     * @param args
     */
    public static void main(String[] args) throws IOException {
        String usage = "Usage\nto register or update a specific Item --> class username password doi target register|update  \nto lookup a specific item --> class doi \nto synchronize all the items against dataCite --> class username password synchall";
        CDLDataCiteService service;


        // LOOKUP: args[0]=DOI
        if(args.length == 1){
            service = new CDLDataCiteService(null, null);
            String doiID = args[0];
            System.out.println(service.lookup(doiID));
        }
        // SYNCHALL: args= USERNAME PASSWORD synchall
        else if(args.length==3 && args[2].equals("synchall")){
            String username = args[0];
            String password = args[1];
            service = new CDLDataCiteService(username, password);
            service.synchAll();
        }
        // REGISTER || UPDATE: args= USERNAME PASSWORD DOI URL ACTION
        else if (args.length == 5){
            String username = args[0];
            String password = args[1];
            String doiID = args[2];
            String target = args[3];
            String action = args[4];

            org.dspace.core.Context context = null;
            try {
                context = new org.dspace.core.Context();
            } catch (SQLException e) {
                System.exit(1);
            }
            context.turnOffAuthorisationSystem();
            IdentifierService identifierService = new DSpace().getSingletonService(IdentifierService.class);
            DSpaceObject dso = null;
            try {
                // FOR local TEST!
                //dso = identifierService.resolve(context, "doi:10.5061/dryad.7mm0p");

    	    	log.debug("obtaining dspace object");
                dso = identifierService.resolve(context, doiID);

	    	    log.debug("dspace object is " + dso);

            } catch (IdentifierNotFoundException e) {
                e.printStackTrace(System.out);
                System.exit(1);
            } catch (IdentifierNotResolvableException e) {
                e.printStackTrace(System.out);
                System.exit(1);
            }

	        log.debug("checking for existance of item metadata");
            Map<String, String> metadata = null;
            if (dso != null && dso instanceof Item){
                metadata = createMetadataList((Item) dso);
	        }
	    
            service = new CDLDataCiteService(username, password);

            if (action.equals("register")) {
                System.out.println(service.registerDOI(doiID, target, metadata));
            } else if (action.equals("update")) {
                System.out.println(service.update(doiID, target, metadata));
            } else{
                 System.out.println(usage);
            }
        }else{
            System.out.println(usage);
        }
    }


    public static Map<String, String> createMetadataList(Item item) {
        Map<String, String> metadata = new HashMap<String, String>();

	    log.debug("generating DataCite metadata for " + item.getMetadata("dc.title")[0]);
	
        // dc: creator, title, publisher
        addMetadata(metadata, item, "dc.contributor.author", DC_CREATOR);
        addMetadata(metadata, item, "dc.title", DC_TITLE);
        addMetadata(metadata, item, "dc.publisher", DC_PUBLISHER);

        // datacite: creator, title, publisher
        addMetadata(metadata, item, "dc.contributor.author", DATACITE_CREATOR);
        addMetadata(metadata, item, "dc.title", DATACITE_TITLE);


        //addMetadata(metadata, item, "dc.publisher", DATACITE_PUBLISHER);
        metadata.put(DATACITE_PUBLISHER, "Dryad Digital Repository");


        // dc.date && datacite.publicationyear
        // date.available =  dc.date.available || dc.date.embargoUntil
        String publicationDate = null;
        DCValue[] values = item.getMetadata("dc.date.available");
        if (values != null && values.length > 0) publicationDate = values[0].value;
        else {
            values = item.getMetadata("dc.date.embargoUntil");
            if (values != null && values.length > 0) publicationDate = values[0].value;
        }
        if (publicationDate != null){
            metadata.put(DC_DATE_AVAILABLE, publicationDate.substring(0, 4));
            metadata.put(DC_DATE, publicationDate);
            metadata.put(DATACITE_PUBBLICATIONYEAR, publicationDate.substring(0, 4));
        }


        // others only dc.
        // dc.subject = dc:subject + dwc.ScientificName + dc:coverage.spatial + dc:coverage.temporal
        String subject = createSubject(item);
        if (subject != null && !subject.equals("")) metadata.put(DC_SUBJECT, subject);


        addMetadata(metadata, item, "dc.relation.isreferencedby",DC_RELATION_ISREFERENCEBY);
        addMetadata(metadata, item, "dc.rights.uri", DC_RIGHTS);
        addMetadata(metadata, item, "dc.description", DC_DESCRIPTION);

	   log.debug("DataCite metadata contains " + metadata.size() + " fields");

        return metadata;

    }

    private static String createSubject(Item item) {
        DCValue[] values;
        String subject = "";
        values = item.getMetadata("dc.subject");
        if (values != null && values.length > 0){
            for(DCValue temp: values){
                subject += temp.value + " ";
            }

        }

        values = item.getMetadata("dwc.ScientificName");
        if (values != null && values.length > 0){
            for(DCValue temp: values){
                subject += temp.value + " ";
            }
        }

        values = item.getMetadata("dc:coverage.spatial");
        if (values != null && values.length > 0){
            for(DCValue temp: values){
                subject += temp.value + " ";
            }
        }

        values = item.getMetadata("dc:coverage.temporal");
        if (values != null && values.length > 0){
            for(DCValue temp: values){
                subject += temp.value + " ";
            }
        }
        return subject;
    }

    private static void addMetadata(Map<String, String> metadataList, Item item, String itemMetadataInput, String dataCiteMetadataKey) {
        DCValue[] values = item.getMetadata(itemMetadataInput);
        if (values != null && values.length > 0) {
            metadataList.put(dataCiteMetadataKey, values[0].value);
        }
    }


    private String encodeAnvl(Map<String, String> metadata) {
        Iterator<Map.Entry<String, String>> i = metadata.entrySet().iterator();
        StringBuffer b = new StringBuffer();
        while (i.hasNext()) {
            Map.Entry<String, String> e = i.next();
            b.append(escape(e.getKey()) + ": " + escape(e.getValue()) + "\n");
        }
        return b.toString();
    }


    private String unescape(String s) {
        StringBuffer b = new StringBuffer();
        int i;
        while ((i = s.indexOf("%")) >= 0) {
            b.append(s.substring(0, i));
            b.append((char) Integer.parseInt(s.substring(i + 1, i + 3), 16));
            s = s.substring(i + 3);
        }
        b.append(s);
        return b.toString();
    }

    private Map<String, String> decodeAnvl(String anvl) {
        HashMap<String, String> metadata = new HashMap<String, String>();
        for (String l : anvl.split("[\\r\\n]+")) {
            String[] kv = l.split(":", 2);
            metadata.put(unescape(kv[0]).trim(), unescape(kv[1]).trim());
        }
        return metadata;
    }

    private static String getDoiValue(Item item) {
        DCValue[] doiVals = item.getMetadata("dc", "identifier", null, Item.ANY);
        if (doiVals != null && 0 < doiVals.length) {
            return doiVals[0].value;
        }
        return null;

    }

    public void emailException(String error, String item, String operation) throws IOException {
		String admin = ConfigurationManager.getProperty("mail.admin");
		Locale locale = I18nUtil.getDefaultLocale();
		String emailFile = I18nUtil.getEmailFilename(locale, "datacite_error");
		Email email = ConfigurationManager.getEmail(emailFile);

		// Write our stack trace to a string for output
		email.addRecipient(admin);

		// Add details to display in the email message
        email.addArgument(operation);
        //email.addArgument(aThrowable);
		email.addArgument(error);
        email.addArgument(item);

		try {
			email.send();
		}
		catch (MessagingException emailExceptionDetails) {
			throw new IOException(emailExceptionDetails);
		}
	}
}
