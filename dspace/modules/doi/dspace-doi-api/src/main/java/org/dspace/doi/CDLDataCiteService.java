package org.dspace.doi;

import java.io.IOException;
import java.lang.String;
import java.net.URL;
import java.sql.SQLException;
import java.util.*;

import javax.net.ssl.HttpsURLConnection;

import org.apache.commons.codec.binary.Base64;
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
import org.dspace.identifier.IdentifierNotFoundException;
import org.dspace.identifier.IdentifierNotResolvableException;
import org.dspace.identifier.IdentifierService;
import org.dspace.utils.DSpace;

public class CDLDataCiteService {

    private static final Logger log = Logger.getLogger(CDLDataCiteService.class);

    private static final String BASEURL = "https://n2t.net/ezid";

    private String myUsername;
    private String myPassword;

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
    public String updateURL(String aDOI, String aURL, Map<String, String> metadata) throws IOException {
        PostMethod post = new PostMethod(BASEURL + "/id/doi%3A" + aDOI);
        return executeHttpMethod(aURL, metadata, post);
    }


    private String executeHttpMethod(String aURL, Map<String, String> metadata, EntityEnclosingMethod httpMethod) throws IOException {
        HttpMethodParams params = new HttpMethodParams();

        HashMap<String, String> map = new HashMap<String, String>();
        map.put("_target", aURL);
        if (log.isDebugEnabled()) {
            log.debug("Adding _target to metadata for update: " + aURL);
        }

        if (metadata != null) {
	    log.debug("Adding other metadata");
            map.putAll(metadata);
	}
	
        httpMethod.setRequestEntity(new StringRequestEntity(encodeAnvl(map), "text/plain", "UTF-8"));

        httpMethod.setRequestHeader("Content-Type", "text/plain");
        httpMethod.setRequestHeader("Accept", "text/plain");

        this.getClient().executeMethod(httpMethod);

        return httpMethod.getStatusLine().toString();
    }


    private String escape(String s) {
        return s.replace("%", "%25").replace("\n", "%0A").
                replace("\r", "%0D").replace(":", "%3A");
    }


    HttpClient client = new HttpClient();

    private HttpClient getClient() throws IOException {
        List authPrefs = new ArrayList(2);
        authPrefs.add(AuthPolicy.DIGEST);
        authPrefs.add(AuthPolicy.BASIC);
        client.getParams().setParameter(AuthPolicy.AUTH_SCHEME_PRIORITY, authPrefs);
        client.getState().setCredentials(AuthScope.ANY, new UsernamePasswordCredentials(myUsername, myPassword));
        return client;
    }


    /**
     * Have to test this on dev since it's also IP restricted.
     *
     * @param args
     */
    public static void main(String[] args) throws IOException {
        String usage = "Usage: class username password doi target register|update";
        CDLDataCiteService service;

        if (args.length == 5) {
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
                dso = identifierService.resolve(context, doiID);
            } catch (IdentifierNotFoundException e) {
                e.printStackTrace(System.out);
                System.exit(1);
            } catch (IdentifierNotResolvableException e) {
                e.printStackTrace(System.out);
                System.exit(1);
            }

            Map<String, String> metadata = null;
            if (dso != null && dso instanceof Item){
                metadata = createMetadataList((Item) dso);
	    }
	    
            service = new CDLDataCiteService(username, password);

            if (action.equals("register")) {
                System.out.println(service.registerDOI(doiID, target, metadata));
            } else if (action.equals("update")) {
                System.out.println(service.updateURL(doiID, target, metadata));
            } else {
                System.out.println(usage);
            }
        } else {
            System.out.println(usage);
        }
    }


    public static Map<String, String> createMetadataList(Item item) {
        Map<String, String> metadata = new HashMap<String, String>();

	log.debug("generating DataCite metadata for " + item.getMetadata("dc.title")[0]);
	
        // dc: creator, title, publisher
        addMetadata(metadata, item, "dc.contributor.author", "dc.creator");
        addMetadata(metadata, item, "dc.title", "dc.title");
        addMetadata(metadata, item, "dc.publisher", "dc.publisher");

        // datacite: creator, title, publisher
        addMetadata(metadata, item, "dc.contributor.author", "datacite.creator");
        addMetadata(metadata, item, "dc.title", "datacite.title");
        addMetadata(metadata, item, "dc.publisher", "datacite.publisher");


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
            metadata.put("dc.date.available", publicationDate.substring(0, 4));
            metadata.put("dc.date", publicationDate);
            metadata.put("datacite.publicationyear", publicationDate.substring(0, 4));
        }


        // others only dc.
        // dc.subject = dc:subject + dwc.ScientificName + dc:coverage.spatial + dc:coverage.temporal
        String suject = null;
        values = item.getMetadata("dc.subject");
        if (values != null && values.length > 0) suject += values[0].value + " ";

        values = item.getMetadata("dwc.ScientificName");
        if (values != null && values.length > 0) suject += values[0].value + " ";

        values = item.getMetadata("dc:coverage.spatial");
        if (values != null && values.length > 0) suject += values[0].value + " ";

        values = item.getMetadata("dc:coverage.temporal");
        if (values != null && values.length > 0) suject += values[0].value;

        if (suject != null) metadata.put("dc.subject", suject);


        addMetadata(metadata, item, "dc.relation.isreferencedby", "dc.relation.isreferencedby");
        addMetadata(metadata, item, "dc.rights.uri", "dc.rights");
        addMetadata(metadata, item, "dc.description", "dc.description");

	log.debug("DataCite metadata contains " + metadata.size() + " fields");

        return metadata;

    }

    private static void addMetadata(Map<String, String> metadataList, Item item, String itemMetadata, String hmMetadata) {
        DCValue[] values = item.getMetadata(itemMetadata);
        if (values != null && values.length > 0) {
            metadataList.put(hmMetadata, values[0].value);
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
}
