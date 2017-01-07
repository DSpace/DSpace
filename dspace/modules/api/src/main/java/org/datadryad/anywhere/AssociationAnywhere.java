package org.datadryad.anywhere;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.PosixParser;

import org.apache.commons.httpclient.HttpClient;


import org.apache.commons.httpclient.methods.GetMethod;

import org.apache.log4j.Logger;

import org.dspace.JournalUtils;
import org.dspace.content.Item;
import org.dspace.content.authority.AuthorityMetadataValue;
import org.dspace.core.ConfigurationManager;
import org.dspace.core.Context;
import org.dspace.identifier.DOIIdentifierProvider;
import org.w3c.dom.Document;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;
import javax.xml.transform.*;
import javax.xml.xpath.*;
import java.io.*;
import java.net.URLEncoder;
import java.text.SimpleDateFormat;
import java.util.Date;
import org.datadryad.api.DryadJournalConcept;

/**
 * Load CustomerCredit facilitates communication with the Association Anywhere service and
 * Provides updated to the Journal Concepts stored in the Editable Authority Control
 *
 * It will be called both interactively during Workflow processing of Submissions as well as
 * from a nightly cron service that will update available credits for the specified Journal from the
 * commandline.
 *
 * @author mdiggory at atmire.com, lantian at atmire.com
 */
public class AssociationAnywhere {

    protected static Logger log = Logger.getLogger(AssociationAnywhere.class);

    public static void main(String[] argv) throws Exception {
        CommandLineParser parser = new PosixParser();
        Options options = new Options();

        options.addOption("i", "customer id", true, "customer id");
        options.addOption("p", "data package DOI", true, "data package DOI, in the form doi:10.5016/dryad.abc123");
        options.addOption("u", "update customer settings", false, "update customer settings");
        options.addOption("t", "tally credit", false, "tally credit");
        options.addOption("l", "list customer", false, "list customer");
        CommandLine line = parser.parse(options, argv);

        Context context = new Context();
        try {
            if (line.hasOption("u")) {
                log.debug("aa updating credits");
                if (line.hasOption("i")) {
                    DryadJournalConcept journalConcept = JournalUtils.getJournalConceptByCustomerID(line.getOptionValue("i"));
                    if (journalConcept != null) {
                        updateConcept(context, journalConcept);
                    } else {
                        log.debug("no journal concept with the customerID" + line.getOptionValue("i"));
                    }
                } else {
                    DryadJournalConcept[] journalConcepts = JournalUtils.getAllJournalConcepts();
                    for (DryadJournalConcept journalConcept : journalConcepts) {
                        updateConcept(context, journalConcept);
                    }
                }
            } else if (line.hasOption("t")) {
                log.debug("aa tallying credits");
                tallyCredit(context, line.getOptionValue("i"), line.getOptionValue("p"));
            } else if (line.hasOption("l")) {
                log.debug("aa list customer");
                System.out.print(printDocument(loadCustomerInfo(context, line.getOptionValue("i"))));
            } else if (line.hasOption("i")) {
                log.debug("aa listing credits");
                //load credit
                String credit = getCredit(context, line.getOptionValue("i"));
                System.out.println("credit : "+ credit);
            }
        } finally {
            context.commit();
        }
    }

    /**
     * private constructor
     *
     * @throws Exception
     */
    private AssociationAnywhere()
            throws Exception
    {

    }

    
    /**
       Returns the number of credits currently available for the given customer.
     **/
    private static String getCredit(Context context, String customerId)
            throws Exception
    {
        log.debug("getting credits for customerId " + customerId);
        try {

            String requestUrl =  ConfigurationManager.getProperty("association.anywhere.url")
                    + "/CENCREDWEBSVCLIB.GET_CREDITS_XML?p_input_xml_doc="
		+ URLEncoder.encode(createRequest(context, customerId,"sync credits", "load-credit"));
            log.debug("AA URL was made from customerID " + customerId + ", sync credits");

            Process p = Runtime.getRuntime().exec("curl " + requestUrl);
            p.waitFor();
            BufferedReader reader =
                new BufferedReader(new InputStreamReader(p.getInputStream()));

            String line = "";
            StringBuffer sb = new StringBuffer();
            while ((line = reader.readLine())!= null) {
                sb.append(line + "\n");
            }

            Document doc = getResponseAsDocument(sb.toString());

            if(getStringValue(doc, "//status").equals("SUCCESS")) {
                    return getStringValue(doc, "//totCreditsAccepted");
            }
                    
            /*
              // This code is not working due to InvalidAlgorithmParameterException when generating the ssl keys in client.executeMethod()
              // Ryan is investigating ways to restore it to avoid using the above system call to curl
            HttpClient client = new HttpClient();
            GetMethod get = new GetMethod(requestUrl);
            client.executeMethod(get);

            if( get.getStatusCode() <= 299 )
            {
                Document doc = getResponseAsDocument(get.getResponseBodyAsString());
                return getStringValue(doc, "//totCreditsAccepted");
            }
            */

        }
        catch (Exception e) {
            log.error("errors when loading customer credit:", e);
            e.printStackTrace();

        }

        return null;
    }

    /**
     * Load the customer Information from Association Anywhere so it can be parsed and Journal
     * Concepts may be updated.
     *
     * @param customerId Journal Id in Association Anywhere.
     * @return Document of response.
     * @throws Exception
     */
    private static Document loadCustomerInfo(Context context, String customerId)
            throws Exception
    {

        try {

            String requestUrl =  ConfigurationManager.getProperty("association.anywhere.url")
		+ "/CENSSAWEBSVCLIB.GET_CUST_INFO_XML?p_input_xml_doc="
		+ URLEncoder.encode(createRequest(context, customerId,"load customer info", "customer-info"));
            log.debug("AA URL was made from customerID " + customerId + ", load customer info");

            Process p = Runtime.getRuntime().exec("curl " + requestUrl);
            p.waitFor();
            BufferedReader reader =
                new BufferedReader(new InputStreamReader(p.getInputStream()));

            String line = "";
            StringBuffer sb = new StringBuffer();
            while ((line = reader.readLine())!= null) {
                sb.append(line + "\n");
            }

            Document doc = getResponseAsDocument(sb.toString());

            if(getStringValue(doc, "//status").equals("SUCCESS")) {
                    return doc;
            }
                    
            /*
              // This code is not working due to InvalidAlgorithmParameterException when generating the ssl keys in client.executeMethod()
              // Ryan is investigating ways to restore it to avoid using the above system call to curl
            HttpClient client = new HttpClient();
            GetMethod get = new GetMethod(requestUrl);
            client.executeMethod(get);

            if( get.getStatusCode() <= 299 )
            {
                return getResponseAsDocument(get.getResponseBodyAsString());
            }
            */
        }
        catch (Exception e) {
            log.error("errors when loading customer information:" + e.getMessage(), e);
        }

        return null;
    }


    /**
     * Tally a Credit in Association Anywhere for the specified Journal.
     *
     * @param context A DSpace Context
     * @param customerId the Journal's customerId in Association Anywhere.
     * @param dataPackageDOI the DOI of a data package being tallied
     * @return status of response from Association Anywhere
     * @throws AssociationAnywhereException
     */
    public static String tallyCredit(Context context, String customerId, String dataPackageDOI) throws AssociationAnywhereException {
        // Don't tally if this is a versioned DOI, as the credit was tallied at the archiving of the original package.
        if (DOIIdentifierProvider.isVersionedDOI(dataPackageDOI)) {
            return "VERSIONED_DOI";
        }

        log.debug("tallying one credit for customerId " + customerId);
        
        if("test".equals(customerId))
        {
            return "SUCCESS";
        }

        String status = null;

        try {
            String requestUrl =  ConfigurationManager.getProperty("association.anywhere.url")
		+ "/CENCREDWEBSVCLIB.INS_CREDIT_XML?p_input_xml_doc="
		+ URLEncoder.encode(createRequest(context, customerId, dataPackageDOI, "update-credit"));
	    log.debug("AA URL was made from customerID " + customerId + ", packageDOI " + dataPackageDOI);

            Process p = Runtime.getRuntime().exec("curl " + requestUrl);
            p.waitFor();
            BufferedReader reader =
                new BufferedReader(new InputStreamReader(p.getInputStream()));

            String line = "";
            StringBuffer sb = new StringBuffer();
            while ((line = reader.readLine())!= null) {
                sb.append(line + "\n");
            }

            Document doc = getResponseAsDocument(sb.toString());
            log.debug("AA responded with " + sb.toString());
            status = getStringValue(doc, "//status");

            return status;
                
            /*
              // This code is not working due to InvalidAlgorithmParameterException when generating the ssl keys in client.executeMethod()
              // Ryan is investigating ways to restore it to avoid using the above system call to curl

            HttpClient client = new HttpClient();
            GetMethod get = new GetMethod(requestUrl);
            client.executeMethod(get);

            if (get.getStatusCode() <= 299) {
                log.debug("Response Code : " + get.getStatusLine().getStatusCode());
                Document result = getResponseAsDocument(get.getResponseBodyAsString());
                log.debug("Response body : " + get.getResponseBodyAsString());
                status = getStringValue(result, "//credit-update-status/status");

                if("FAILURE".equals(status))
                {
                    throw new AssociationAnywhereException(get.getResponseBodyAsString());
                }
            }
            
            return status;
            */
        }
        catch (Exception e)
        {
            log.debug("AA threw an exception for " + customerId + ": " + e.getMessage());
            throw new AssociationAnywhereException("unable to deduct credits for " + customerId, e);
        }

    }

    /**
     Retrieve the type of payment plan being used by the customer.
     **/
    private String getCustomerPlanType(String customerId) {
        return "Error getCustomerPlanType";
    }

    /**
     * Update Credit for a specific customer id.
     * @param context
     * @param journalConcept
     * @throws Exception
     */
    private static void updateConcept(Context context, DryadJournalConcept journalConcept) throws Exception {
        String customerID = journalConcept.getCustomerID();
        if ("".equals(customerID)) {
            log.debug("concept has no customerID");
            return;
        }
        String credit = getCredit(context, customerID);
        Document customerInfo = loadCustomerInfo(context, customerID);
        String result = printDocument(customerInfo);

        String classSubclassDescr = getStringValue(customerInfo, "//classSubclassDescr");
        String statusCode = getStringValue(customerInfo, "//statusCode");

        if (classSubclassDescr != null && statusCode!=null && statusCode.contains("ACTIVE")) {
            if (classSubclassDescr.contains("Subscription")) {
                journalConcept.setPaymentPlan(DryadJournalConcept.SUBSCRIPTION_PLAN);
            } else if (classSubclassDescr.contains("Deferred")) {
                journalConcept.setPaymentPlan(DryadJournalConcept.DEFERRED_PLAN);
            } else {
                journalConcept.setPaymentPlan(DryadJournalConcept.NO_PLAN);
            }
        } else {
            if (credit!=null && Integer.parseInt(credit)>0) {
                //set as a prepaid journal
                journalConcept.setPaymentPlan(DryadJournalConcept.PREPAID_PLAN);
            } else {
                journalConcept.setPaymentPlan(DryadJournalConcept.NO_PLAN);
            }
        }
    }

    // ####################### PRIVATE UTILITY METHODS FOR FORMATTING REQUESTS AND PARSING RESPONSES

    private static Document getResponseAsDocument(String response) throws ParserConfigurationException, IOException, SAXException {
        DocumentBuilderFactory docFactory = DocumentBuilderFactory.newInstance();
        DocumentBuilder docBuilder = docFactory.newDocumentBuilder();
        InputSource is = new InputSource(new StringReader(response));
        return docBuilder.parse(is);
    }

    private static String getStringValue(Document doc, String path) throws ParserConfigurationException, IOException, SAXException, XPathExpressionException {
        XPathFactory factory = XPathFactory.newInstance();
        XPath xpath = factory.newXPath();
        XPathExpression expression = xpath.compile( path);
        return (String) expression.evaluate(doc, XPathConstants.STRING);
    }

    private static String printDocument(Document doc) throws TransformerException {
        TransformerFactory tf = TransformerFactory.newInstance();
        Transformer transformer = tf.newTransformer();
        transformer.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "no");
        transformer.setOutputProperty(OutputKeys.METHOD, "xml");
        transformer.setOutputProperty(OutputKeys.INDENT, "yes");
        transformer.setOutputProperty(OutputKeys.ENCODING, "UTF-8");
        transformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "4");
        StringWriter writer = new StringWriter();
        transformer.transform(new DOMSource(doc),new StreamResult(writer));
        return writer.toString();
    }

    static Templates template = null;

    static SimpleDateFormat dateFormat = new SimpleDateFormat("MM/dd/yyyy");

    private static String createRequest(Context context, String customerID, String transactionDescription, String form)
    {
        try {
            if(template == null) {
                template = TransformerFactory.newInstance().newTemplates(new StreamSource(AssociationAnywhere.class.getResourceAsStream("/anywhere/request-templates.xsl")));
	        }
            Transformer transformer = template.newTransformer();
            transformer.setParameter("username",ConfigurationManager.getProperty("association.anywhere.username"));
            transformer.setParameter("password", ConfigurationManager.getProperty("association.anywhere.password"));
            transformer.setParameter("customerID", customerID);
            transformer.setParameter("date", dateFormat.format(new Date()));

            if (transactionDescription == null) {
                transactionDescription = "";
            }

            transformer.setParameter("transactionDescription", transactionDescription);

            DryadJournalConcept journalConcept = JournalUtils.getJournalConceptByCustomerID(customerID);
            if (journalConcept != null) {
                String transactionType = journalConcept.getPaymentPlan();

                if (transactionType != null) {
                    transformer.setParameter("transactionType", transactionType);

                    String creditsAccepted = "1";
                    if(transactionType.equals(DryadJournalConcept.PREPAID_PLAN)) {
                        creditsAccepted = "-1";
                    }

                    transformer.setParameter("creditsAccepted", creditsAccepted);
                } else {
                    log.error("Journal w/ customerID " + customerID + " does not have a transactionType.");
                }
            }
            StringWriter writer = new StringWriter();
            transformer.transform(new StreamSource(new StringReader("<" + form + "/>")),new StreamResult(writer));
            return writer.toString();
        } catch (TransformerConfigurationException e) {
            log.error("Unable to generate request URL", e);
        } catch (TransformerException e) {
            log.error("Unable to generate request URL", e);
        }
        return null;
    }
}

