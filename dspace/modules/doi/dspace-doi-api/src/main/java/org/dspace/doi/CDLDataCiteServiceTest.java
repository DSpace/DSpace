package org.dspace.doi;

import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.UsernamePasswordCredentials;
import org.apache.commons.httpclient.auth.AuthPolicy;
import org.apache.commons.httpclient.auth.AuthScope;
import org.apache.commons.httpclient.methods.*;
import org.apache.log4j.Logger;
import java.io.IOException;
import java.util.*;

public class CDLDataCiteServiceTest {

    private static Logger log = Logger.getLogger(CDLDataCiteServiceTest.class);
    private static final String BASEURL = "https://n2t.net/ezid";

    private String myUsername;
    private String myPassword;

    public String publisher = null;


    public static void main(String[] args) throws IOException {
        String username = args[0];
        String password = args[1];

        Map<String, String> metadata = createMetadataListXML();
        CDLDataCiteServiceTest service = new CDLDataCiteServiceTest(username, password);
        String updateOutput = service.update("10.5061/DRYAD.487", metadata);
	log.info("Output of the update command: " + updateOutput);
    }


    public CDLDataCiteServiceTest(final String aUsername, final String aPassword) {
        myUsername = aUsername;
        myPassword = aPassword;
    }


    public String update(String aDOI,Map<String, String> metadata) throws IOException {
        PostMethod post = new PostMethod(BASEURL + "/id/doi%3A" + aDOI);
        return executeHttpMethod(metadata, post);
    }

    private String executeHttpMethod(Map<String, String> metadata, EntityEnclosingMethod httpMethod) throws IOException {

        logMetadata(metadata);

        httpMethod.setRequestEntity(new StringRequestEntity(encodeAnvl(metadata), "text/plain", "UTF-8"));
        httpMethod.setRequestHeader("Content-Type", "text/plain");
        httpMethod.setRequestHeader("Accept", "text/plain");

        this.getClient(false).executeMethod(httpMethod);
	log.info("HTTP status: " + httpMethod.getStatusLine());
	log.debug("HTTP response text: " + httpMethod.getResponseBodyAsString(1000));
        return httpMethod.getStatusLine().toString();
    }

    private void logMetadata(Map<String, String> metadata) {
        log.info("Adding the following Metadata:");
	log.info(encodeAnvl(metadata));
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


    private static Map<String, String> createMetadataListXML() {
        Map<String, String> metadata = new HashMap<String, String>();

        String xmlout =  "<?xml version=\"1.0\" encoding=\"UTF-8\"?>" +
	    "<resource xmlns=\"http://datacite.org/schema/kernel-2.2\" " +
	    "xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" " +
	    "xsi:schemaLocation=\"http://datacite.org/schema/kernel-2.2 http://schema.datacite.org/meta/kernel-2.2/metadata.xsd\" " +
	    "metadataVersionNumber=\"1\" lastMetadataUpdate=\"2006-05-04\">" +
                "<identifier identifierType=\"DOI\">10.5061/DRYAD.2222</identifier>" +
                "<creators>" +
                "<creator>" +
                "<creatorName>Toru, Nozawa</creatorName>" +
                "</creator>" +
                "</creators>" +
                "<titles>" +
                "<title>National Institute for Environmental Studies and Center for Climate System Research Japan</title>" +
                "</titles>" +
                "<publisher>World Data Center for Climate (WDCC)</publisher>" +
                "<publicationYear>2004</publicationYear>" +
                "</resource>";

        log.debug("test metadata is " + xmlout);
        metadata.put("datacite", xmlout);

        return metadata;
    }

    private String encodeAnvl(Map<String, String> metadata) {
        Iterator<Map.Entry<String, String>> i = metadata.entrySet().iterator();
        StringBuffer b = new StringBuffer();
        while (i.hasNext()) {
            Map.Entry<String, String> e = i.next();
            b.append(escape(e.getKey()) + ": " + escape(e.getValue()) + "");
        }
        return b.toString();
    }

    private String escape(String s) {
        return s.replace("%", "%25").replace("\n", "%0A").
	    replace("\r", "%0D").replace(":", "%3A");
    }

}
