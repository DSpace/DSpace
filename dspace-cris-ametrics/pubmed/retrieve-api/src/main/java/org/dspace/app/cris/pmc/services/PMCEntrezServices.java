/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * https://github.com/CILEA/dspace-cris/wiki/License
 */
package org.dspace.app.cris.pmc.services;

import it.cilea.osd.common.utils.ClasspathEntityResolver;
import it.cilea.osd.common.utils.XMLUtils;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HttpStatus;
import org.apache.commons.httpclient.NameValuePair;
import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.log4j.Logger;
import org.dspace.app.cris.pmc.model.PMCRecord;
import org.springframework.util.StringUtils;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

public class PMCEntrezServices
{
    /** log4j logger */
    private static Logger log = Logger.getLogger(PMCEntrezServices.class);

    private static String ENTREZ_CITE_ENDPOINT = "http://www.pubmedcentral.nih.gov/utils/entrez2pmcciting.cgi";

    private static String ENTREZ_PMC_TO_PUBMED_ENDPOINT = "http://www.pubmedcentral.nih.gov/utils/pmcentrez.cgi";

    private static String ENTREZ_PUBMED_TO_PMC_ENDPOINT = "http://www.pubmedcentral.nih.gov/utils/entrezpmc.cgi";

    private static String ENTREZ_FETCH_ENDPOINT = "http://eutils.ncbi.nlm.nih.gov/entrez/eutils/efetch.fcgi";

    private static String ENTREZ_SEARCH_ENDPOINT = "http://eutils.ncbi.nlm.nih.gov/entrez/eutils/esearch.fcgi";

    private static final Pattern fullFetchDocsumPattern = Pattern
            .compile(
                    "\\s*([0-9]+:\\s*.+?\\.\\n.+?\\.\\n.+?\\nPMCID: [0-9]+\\s*)+?(([0-9]+:\\s*.+?\\.\\n.+?\\.\\n.+?\\nPMCID: [0-9]+\\s*)*)",
                    Pattern.MULTILINE | Pattern.DOTALL);

    private static final Pattern fetchDocsumPattern = Pattern.compile(
            "[0-9]+:\\s*(.+?)\\.\\n(.+?)\\.\\n(.+?)\\nPMCID: ([0-9]+)\\s*?",
            Pattern.MULTILINE | Pattern.DOTALL);

    private static final Pattern singleNoNumDocPattern = Pattern.compile(
            "\\s*(.+[\\?!\\.]+)\\n(.+)\\.\\n(.+)\\nPMCID: ([0-9]+)\\s*",
            Pattern.MULTILINE | Pattern.DOTALL);

    public Set<Integer> getCitedByPMEDID(int pmedID) throws PMCEntrezException
    {
        return getCitedByPMEDIDs(pmedID).get(pmedID);
    }

    public Map<Integer, Set<Integer>> getCitedByPMEDIDs(Integer... pmedID)
            throws PMCEntrezException
    {
        return getMultiIDs(ENTREZ_CITE_ENDPOINT, "PMID", "PMCID", pmedID);
    }

    public PMCRecord getPMCRecord(Integer pmcID) throws PMCEntrezException
    {
        return getMultiPMCRecord(pmcID).get(0);
    }

    public List<PMCRecord> getMultiPMCRecord(Integer... pmcID)
            throws PMCEntrezException
    {
        GetMethod method = null;
        if (pmcID == null || pmcID.length == 0)
        {
            return new ArrayList<PMCRecord>();
        }
        try
        {
            HttpClient client = new HttpClient();
            method = new GetMethod(ENTREZ_FETCH_ENDPOINT);

            NameValuePair view = new NameValuePair("retmode", "text");
            NameValuePair ids = new NameValuePair("id",
                    StringUtils.arrayToCommaDelimitedString(pmcID));
            NameValuePair rettype = new NameValuePair("rettype", "docsum");
            NameValuePair db = new NameValuePair("db", "pmc");
            method.setQueryString(new NameValuePair[] { view, ids, rettype, db });

            // Execute the method.
            int statusCode = client.executeMethod(method);

            if (statusCode != HttpStatus.SC_OK)
            {
                throw new PMCEntrezException("Entrez webservice failure: "
                        + method.getStatusLine());
            }
            String response = it.cilea.osd.common.utils.StringUtils
                    .convertStreamToString(method.getResponseBodyAsStream(),
                            "UTF-8");

            log.debug("Entrez response for PMC IDs: "
                    + StringUtils.arrayToCommaDelimitedString(pmcID) + "\n"
                    + response);
            List<PMCRecord> result = new ArrayList<PMCRecord>();

            extractPMCResult(response, result, pmcID);
            if (result.size() != pmcID.length)
            {
                throw new PMCEntrezException(
                        "Failed to parse the Entrez response, found "
                                + result.size() + " PMC records were expected "
                                + pmcID.length);
            }
            return result;
        }
        catch (Exception e)
        {
            throw new PMCEntrezException("Entrez webservice failure: "
                    + e.getMessage() + " query string: "
                    + method.getQueryString(), e);
        }
        finally
        {
            method.releaseConnection();
        }
    }

    private void extractPMCResult(String regionToMatch, List<PMCRecord> result,
            Integer... pmcID) throws PMCEntrezException
    {
        int start = regionToMatch.indexOf("1: ");
        String toProcess = regionToMatch.substring(start + 3);

        for (int i = 1; i < pmcID.length; i++)
        {
            int end = toProcess.indexOf((i + 1) + ": ",
                    toProcess.indexOf("PMCID: "));
            result.add(parsePMCRecord(toProcess.substring(0, end), pmcID[i - 1]));
            toProcess = toProcess.substring(end + 3);
        }
        result.add(parsePMCRecord(toProcess, pmcID[pmcID.length - 1]));
    }

    private PMCRecord parsePMCRecord(String toProcess, Integer pmcID)
            throws PMCEntrezException
    {
        String title = null;
        String authors = null;
        String publicationNote = null;
        String PMCID = null;

        Matcher singlePMC = singleNoNumDocPattern.matcher(toProcess);
        if (singlePMC.matches() && singlePMC.groupCount() == 4)
        {
            title = singlePMC.group(1);
            authors = singlePMC.group(2);
            publicationNote = singlePMC.group(3);
            PMCID = singlePMC.group(4);

            if (!PMCID.equalsIgnoreCase(pmcID.toString()))
            {
                title = toProcess;
                authors = null;
                publicationNote = null;
            }
        }
        else
        {
            title = toProcess;
            log.warn("Unparserizable text for PMCID: " + pmcID + " text was ["
                    + toProcess + "]");
        }

        PMCRecord rec = new PMCRecord();
        rec.setId(pmcID);
        rec.setTitle(title);
        rec.setAuthors(authors);
        rec.setPublicationNote(publicationNote);
        rec.setPubmedIDs(getPubmedIDs(pmcID));

        return rec;
    }

    private void extractPMCResultBACKUP(String regionToMatch,
            List<PMCRecord> result, Integer... pmcID) throws PMCEntrezException
    {
        Matcher matcher = fullFetchDocsumPattern.matcher(regionToMatch);

        String title = null;
        String authors = null;
        String publicationNote = null;
        String PMCID = null;

        if (matcher.matches())
        {
            String pmcmatch;
            if (matcher.groupCount() == 2 && matcher.group(2) != null)
            {
                pmcmatch = matcher.group(2);
            }
            else
            {
                pmcmatch = matcher.group(1);
            }

            Matcher singlePMC = fetchDocsumPattern.matcher(pmcmatch);
            if (!singlePMC.matches())
            {
                throw new PMCEntrezException(
                        "Entrez webservice failure: unpaserizable response for PMCID "
                                + StringUtils
                                        .arrayToCommaDelimitedString(pmcID));
            }

            title = singlePMC.group(1);
            authors = singlePMC.group(2);
            publicationNote = singlePMC.group(3);
            PMCID = singlePMC.group(4);

            PMCRecord rec = new PMCRecord();
            rec.setId(Integer.valueOf(PMCID));
            rec.setTitle(title);
            rec.setAuthors(authors);
            rec.setPublicationNote(publicationNote);
            rec.setPubmedIDs(getPubmedIDs(Integer.valueOf(PMCID)));

            result.add(rec);
            if (matcher.groupCount() == 2 && matcher.group(2) != null)
            {
                // extractPMCResult(matcher.group(2), result, pmcID);
                extractPMCResult(matcher.group(1), result, pmcID);
            }
        }
        else
        {
            throw new PMCEntrezException(
                    "Entrez webservice failure: unpaserizable response for PMCID "
                            + StringUtils.arrayToCommaDelimitedString(pmcID));
        }
    }

    public List<Integer> getPubmedIDs(Integer pmcID) throws PMCEntrezException
    {
        return new ArrayList<Integer>(getMultiPubmedIDs(pmcID).get(pmcID));
    }

    public Map<Integer, Set<Integer>> getMultiPubmedIDs(Integer... pmcID)
            throws PMCEntrezException
    {
        return getMultiIDs(ENTREZ_PMC_TO_PUBMED_ENDPOINT, "PMCID", "PMID",
                pmcID);
    }

    private Map<Integer, Set<Integer>> getMultiIDs(String endpoint, String key,
            String otherIdentifer, Integer... lookupIDs)
            throws PMCEntrezException
    {

        GetMethod method = null;

        try
        {
            HttpClient client = new HttpClient();
            method = new GetMethod(endpoint);

            NameValuePair view = new NameValuePair("view", "xml");
            NameValuePair ids = new NameValuePair("id",
                    StringUtils.arrayToCommaDelimitedString(lookupIDs));
            method.setQueryString(new NameValuePair[] { view, ids });
            // Execute the method.
            int statusCode = client.executeMethod(method);

            if (statusCode != HttpStatus.SC_OK)
            {
                throw new PMCEntrezException("Entrez webservice failure: "
                        + method.getStatusLine());
            }

            InputStream xmlResponse = method.getResponseBodyAsStream();
            DocumentBuilderFactory factory = DocumentBuilderFactory
                    .newInstance();
            factory.setValidating(false);
            factory.setIgnoringComments(true);
            factory.setIgnoringElementContentWhitespace(true);

            DocumentBuilder db = factory.newDocumentBuilder();
            db.setEntityResolver(new ClasspathEntityResolver());
            Document inDoc = db.parse(xmlResponse,
                    "classpath://it/cilea/pmc/services/");

            Element dataRoot = inDoc.getDocumentElement();

            Map<Integer, Set<Integer>> result = new HashMap<Integer, Set<Integer>>();
            List<Element> reforms = XMLUtils.getElementList(dataRoot, "REFORM");
            for (Element reform : reforms)
            {
                String myid = XMLUtils.getElementValue(reform, key);
                List<String> equivIds = XMLUtils.getElementValueList(reform,
                        otherIdentifer);
                Set<Integer> values = new LinkedHashSet<Integer>();
                for (String pmcid : equivIds)
                {
                    values.add(Integer.parseInt(pmcid));
                }
                result.put(Integer.parseInt(myid), values);
            }
            return result;
        }
        catch (Exception e)
        {
            throw new PMCEntrezException("Entrez webservice failure: "
                    + e.getMessage(), e);
        }
        finally
        {
            method.releaseConnection();
        }
    }

    public List<Integer> getPMCIDs(Integer pubmedID) throws PMCEntrezException
    {
        return new ArrayList<Integer>(getMultiPMCIDs(pubmedID).get(pubmedID));
    }

    public Map<Integer, Set<Integer>> getMultiPMCIDs(Integer... pubmedID)
            throws PMCEntrezException
    {
        return getMultiIDs(ENTREZ_PUBMED_TO_PMC_ENDPOINT, "PMID", "PMCID",
                pubmedID);
    }

    public List<Integer> getPubmedIDs(String doi) throws PMCEntrezException
    {
        GetMethod method = null;

        try
        {
            String lookupdoi = "\"" + doi.replaceFirst("doi:", "") + "\"";
            HttpClient client = new HttpClient();
            method = new GetMethod(ENTREZ_SEARCH_ENDPOINT);

            NameValuePair term = new NameValuePair("term", doi);
            NameValuePair db = new NameValuePair("db", "pubmed");
            NameValuePair field = new NameValuePair("field", "doi");
            method.setQueryString(new NameValuePair[] { term, db, field });

            // Execute the method.
            int statusCode = client.executeMethod(method);

            if (statusCode != HttpStatus.SC_OK)
            {
                throw new PMCEntrezException("Entrez webservice failure: "
                        + method.getStatusLine());
            }

            InputStream xmlResponse = method.getResponseBodyAsStream();

            DocumentBuilderFactory factory = DocumentBuilderFactory
                    .newInstance();
            factory.setValidating(false);
            factory.setIgnoringComments(true);
            factory.setIgnoringElementContentWhitespace(true);

            DocumentBuilder dbuilder = factory.newDocumentBuilder();
            dbuilder.setEntityResolver(new ClasspathEntityResolver());
            Document inDoc = dbuilder.parse(xmlResponse,
                    "classpath://it/cilea/pmc/services/");

            Element dataRoot = inDoc.getDocumentElement();
            Element idList = XMLUtils.getSingleElement(dataRoot, "IdList");
            String queryTranslation = XMLUtils.getElementValue(dataRoot,
                    "QueryTranslation");
            List<Integer> result = new ArrayList<Integer>();

            if (queryTranslation.startsWith(lookupdoi))
            {
                List<String> pubmedIds = XMLUtils.getElementValueList(idList,
                        "Id");
                for (String pid : pubmedIds)
                {
                    result.add(Integer.valueOf(pid));
                }
            }
            else
            {
                System.out
                        .println("Discard response, doi was not found as phrase and has been tokenized");
            }
            return result;
        }
        catch (Exception e)
        {
            throw new PMCEntrezException("Entrez webservice failure: "
                    + e.getMessage(), e);
        }
        finally
        {
            method.releaseConnection();
        }
    }

    public static void main(String[] args) throws PMCEntrezException
    {
        PMCEntrezServices serv = new PMCEntrezServices();
        List<PMCRecord> rec = serv.getMultiPMCRecord(2585848, 38832, 42999,
                43947, 209472, 280744);
        System.out.print(rec);
        System.out.println(serv.getPubmedIDs(50272));// 1279667
        System.out.println(serv.getPMCIDs(1279667));// 50272
        System.out.println(serv.getCitedByPMEDID(1279667));// 2585848, 38832,
                                                           // 42999, 43947,
                                                           // 209472, 280744,
                                                           // 289158, 1876894,
                                                           // 1890316, 2191760,
                                                           // 2192477, 2192884,
                                                           // 2193009, 2193975,
                                                           // 2199100, 2212356,
                                                           // 2213416
        System.out.println(serv.getPubmedIDs("10.1038/onc.2010.235"));// 20562908
    }
}
