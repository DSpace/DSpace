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

    private static final String BASEURL = "https://n2t.net/ezid";

    private String myUsername;
    private String myPassword;

    public String publisher = null;


    public static void main(String[] args) throws IOException {
        String username = args[0];
        String password = args[1];

        Map<String, String> metadata = createMetadataListXML();
        CDLDataCiteServiceTest service = new CDLDataCiteServiceTest(username, password);
        System.out.println(service.update("10.5061/DRYAD.2222", metadata));
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
	System.out.println("HTTP output: " + httpMethod.getStatusText());
        return httpMethod.getStatusLine().toString();
    }

    private void logMetadata(Map<String, String> metadata) {
        System.out.println("Adding the following Metadata:");
        if(metadata!=null){
            Set<String> keys = metadata.keySet();
            for(String key : keys){
                System.out.println(key + ": " + metadata.get(key));
            }
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


    private static Map<String, String> createMetadataListXML() {
        Map<String, String> metadata = new HashMap<String, String>();

        String xmlout = "<resource  xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" metadataVersionNumber=\"1\"" +
                " lastMetadataUpdate=\"2006-05-04\" xsi:noNamespaceSchemaLocation=\"datacite-metadata-v2.0.xsd\">" +
                "<identifier identifierType=\"DOI\">10.5061/DRYAD.2222</identifier>" +
                "<creators>" +
                "<creator>" +
                "<creatorName>Toru, Nozawa</creatorName>" +
                "</creator>" +
                "<creator>" +
                "<creatorName>Utor, Awazon</creatorName>" +
                "<nameIdentifier nameIdentifierScheme=\"ISNI\">1422 4586 3573 0476</nameIdentifier>" +
                "</creator>" +
                "</creators>" +
                "<titles>" +
                "<title>National Institute for Environmental Studies and Center for Climate System Research Japan</title>" +
                "<title titleType=\"Subtitle\">A survey</title>" +
                "</titles>" +
                "<publisher>World Data Center for Climate (WDCC)</publisher>" +
                "<publicationYear>2004</publicationYear>" +
                "<subjects>" +
                "<subject>Earth sciences and geology</subject>" +
                "</subjects>" +
                "<contributors>" +
                "<contributor contributorType=\"DataManager\">" +
                "<contributorName>PANGAEA</contributorName>" +
                "</contributor>" +
                "<contributor contributorType=\"ContactPerson\">" +
                "<contributorName>Doe, John</contributorName>" +
                "<nameIdentifier nameIdentifierScheme=\"ORCID\">xyz789</nameIdentifier>" +
                "</contributor>" +
                "</contributors>" +
                "<dates>" +
                "<date dateType=\"Valid\">2005-04-05</date>" +
                "<date dateType=\"Accepted\">2005-01-01</date>" +
                "</dates>" +
                "<language>en</language>" +
                "<resourceType resourceTypeGeneral=\"Image\">Animation</resourceType>" +
                "<alternateIdentifiers>" +
                "<alternateIdentifier alternateIdentifierType=\"ISBN\">937-0-1234-56789-X</alternateIdentifier>" +
                "</alternateIdentifiers>" +
                "<relatedIdentifiers>" +
                "<relatedIdentifier relationType=\"IsCitedBy\" relatedIdentifierType=\"DOI\">10.1234/testpub</relatedIdentifier>" +
                "<relatedIdentifier relationType=\"Cites\" relatedIdentifierType=\"URN\">http://testing.ts/testpub" +
                "</relatedIdentifier>" +
                "</relatedIdentifiers>" +
                "<sizes>" +
                "<size>285 kb</size>" +
                "<size>100 pages</size>" +
                "</sizes>" +
                "<formats>" +
                "<format>text/plain</format>" +
                "</formats>" +
                "<version>1.0</version>" +
                "<rights>Open Database License [ODbL]</rights>" +
                "<descriptions>" +
                "<description descriptionType=\"Other\">" +
                "The current xml-example for a DataCite record is the official example from the documentation." +
                "<br/>" +
                "Please look on datacite.org to find the newest versions of sample data and schemas." +
                "</description>" +
                "</descriptions>" +
                "</resource>";

        System.out.println(xmlout);
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

}
