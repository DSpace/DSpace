package org.datadryad.anywhere;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.PosixParser;

import org.apache.commons.httpclient.HttpClient;


import org.apache.commons.httpclient.methods.GetMethod;

import org.apache.log4j.Logger;

import org.dspace.content.Item;
import org.dspace.content.authority.AuthorityMetadataValue;
import org.dspace.content.authority.Concept;
import org.dspace.content.authority.Scheme;
import org.dspace.core.ConfigurationManager;
import org.dspace.core.Context;
import org.w3c.dom.Document;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.*;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;
import javax.xml.xpath.*;
import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;
import java.net.URLEncoder;
import java.text.SimpleDateFormat;
import java.util.Date;

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

    public static void main(String[] argv)
            throws Exception
    {
        CommandLineParser parser = new PosixParser();
        Options options = new Options();

        options.addOption("i", "customer id", true, "customer id");
        options.addOption("u", "update credit", false, "update credit");
        options.addOption("d", "deduct credit", false, "deduct credit");
        options.addOption("l", "list customer", false, "list customer");
        CommandLine line = parser.parse(options, argv);

        if(line.hasOption("u"))
        {
            Context context = new Context();
            if(line.hasOption("i"))
                updateConcept(context, line.getOptionValue("i"));
            else
                updateConcept(context);

        }
        else if(line.hasOption("d")){
            deductCredit(line.getOptionValue("i"));
        }
        else if(line.hasOption("l")){
            System.out.print(printDocument(loadCustomerInfo(line.getOptionValue("i"))));
        }
        else if(line.hasOption("i")){
            //load credit
            String credit = getCredit(line.getOptionValue("i"));
            System.out.println("credit : "+ credit);
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

    private static String getCredit(String customerId)
            throws Exception
    {
        log.debug("getting credits for customerId " + customerId);
        try {

            String requestUrl =  ConfigurationManager.getProperty("association.anywhere.url")
                    + "/CENCREDWEBSVCLIB.GET_CREDITS_XML?p_input_xml_doc="
                    + URLEncoder.encode(createRequest(customerId,"load-credit"));

            HttpClient client = new HttpClient();
            GetMethod get = new GetMethod(requestUrl);
            client.executeMethod(get);

            if( get.getStatusCode() <= 299 )
            {
                Document doc = getResponseAsDocument(get.getResponseBodyAsString());
                return getStringValue(doc, "//totCreditsAccepted");
            }

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
    private static Document loadCustomerInfo(String customerId)
            throws Exception
    {

        try {

            String requestUrl =  ConfigurationManager.getProperty("association.anywhere.url")
                    + "/CENSSAWEBSVCLIB.GET_CUST_INFO_XML?p_input_xml_doc="
                    + URLEncoder.encode(createRequest(customerId,"customer-info"));
            HttpClient client = new HttpClient();
            GetMethod get = new GetMethod(requestUrl);
            client.executeMethod(get);

            if( get.getStatusCode() <= 299 )
            {
                return getResponseAsDocument(get.getResponseBodyAsString());
            }
        }
        catch (Exception e) {
            log.error("errors when loading customer information:" + e.getMessage(), e);
            e.printStackTrace();
        }

        return null;
    }


    /**
     * Deduct Credit from Association Anywhere for the specified Journal.
     *
     * @param customerId Journal Id in Association Anywhere.
     * @return status of response.
     * @throws AssociationAnywhereException
     */
    public static String deductCredit(String customerId) throws AssociationAnywhereException {
        log.debug("deducting one credit for customerId " + customerId);
        
        if("test".equals(customerId))
        {
            return "SUCCESS";
        }

        String status = null;

        try {
            String requestUrl =  ConfigurationManager.getProperty("association.anywhere.url")
                    + "/CENCREDWEBSVCLIB.INS_CREDIT_XML?p_input_xml_doc="
                    + URLEncoder.encode(createRequest(customerId,"update-credit"));
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
        }
        catch (Exception e)
        {
            throw new AssociationAnywhereException(e.getMessage());
        }

    }

    /**
     * Iterate over all Concepts and update credits available.
     *
     * @param context
     * @throws Exception
     */
    private static void updateConcept(Context context) throws Exception {

        Scheme scheme = Scheme.findByIdentifier(context,ConfigurationManager.getProperty("solrauthority.searchscheme.prism_publicationName"));
        for(Concept concept : scheme.getConcepts())
        {
            for(AuthorityMetadataValue value : concept.getMetadata("internal", "journal", "customerId",AuthorityMetadataValue.ANY))
            {
                updateConcept(concept, value.getValue());
            }
        }

        context.commit();
    }

    /**
     * Update Credit for a specific customer id.
     * @param context
     * @param customerId
     * @throws Exception
     */
    private static void updateConcept(Context context, String customerId)
            throws Exception {

        Scheme scheme = Scheme.findByIdentifier(context,ConfigurationManager.getProperty("solrauthority.searchscheme.prism_publicationName"));
        for(Concept concept : scheme.getConcepts())
        {
            for(AuthorityMetadataValue value : concept.getMetadata("internal", "journal", "customerId",AuthorityMetadataValue.ANY))
            {
                if(customerId != null && customerId.equals(value.getValue()))
                    updateConcept(concept, value.getValue());
            }
        }

        context.commit();

    }

    /**
     * Update Credit for a specific customer id.
     * @param customerId
     * @throws Exception
     */
    private static void updateConcept(Concept concept, String customerId)
            throws Exception {

        if(customerId==null)
        {
            throw new AssociationAnywhereException("customerId cannot be null");
        }

        String credit = getCredit(customerId);
        Document customerInfo = loadCustomerInfo(customerId);
        String result = printDocument(customerInfo);

        String classSubclassDescr = getStringValue(customerInfo, "//classSubclassDescr");
        String statusCode = getStringValue(customerInfo, "//statusCode");

        changeConceptMetadataValue(concept,"customerId",customerId);

        if(classSubclassDescr!=null&&classSubclassDescr.contains("Subscription"))
        {
            if(statusCode!=null&&statusCode.contains("ACTIVE"))
            {
                //set journal.submissionPaid=true and journal paymentPlanType=subscription
                changeConceptMetadataValue(concept,"subscriptionPaid","true");
                changeConceptMetadataValue(concept,"paymentPlanType","subscription");
            }
            else
            {
                //journal.submissionPaid=false and journal paymentPlanType=null
                changeConceptMetadataValue(concept,"subscriptionPaid","false");
                changeConceptMetadataValue(concept,"paymentPlanType","");
            }

        }
        else if(classSubclassDescr!=null&&classSubclassDescr.contains("Deferred"))
        {
            if(statusCode!=null&&statusCode.contains("ACTIVE"))
            {
                //set submissionPaid=true and paymentPlanType=deferred
                changeConceptMetadataValue(concept,"subscriptionPaid","true");
                changeConceptMetadataValue(concept,"paymentPlanType","deferred");
            }
            else
            {
                //journal.submissionPaid=false and journal paymentPlanType=null
                changeConceptMetadataValue(concept,"subscriptionPaid","false");
                changeConceptMetadataValue(concept,"paymentPlanType","");
            }
        }
        else
        {
            if(credit!=null&&Integer.parseInt(credit)>0)
            {
                //set submissionPaid=true and paymentPlanType=null
                changeConceptMetadataValue(concept,"subscriptionPaid","true");
                changeConceptMetadataValue(concept,"paymentPlanType","");
            }
            else
            {
                changeConceptMetadataValue(concept,"subscriptionPaid","false");
                changeConceptMetadataValue(concept,"paymentPlanType","");
            }
        }

        concept.update();

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

    static SimpleDateFormat dateFormat = new SimpleDateFormat("dd/mm/yyyyy");

    private static String createRequest(String customerId, String form)
    {
        try {
            if(template == null)
                template = TransformerFactory.newInstance().newTemplates(new StreamSource(AssociationAnywhere.class.getResourceAsStream("/anywhere/request-templates.xsl")));

            Transformer transformer = template.newTransformer();
            transformer.setParameter("username",ConfigurationManager.getProperty("association.anywhere.username"));
            transformer.setParameter("password", ConfigurationManager.getProperty("association.anywhere.password"));
            transformer.setParameter("customerId", customerId);
            transformer.setParameter("date", dateFormat.format(new Date()));

            StringWriter writer = new StringWriter();
            transformer.transform(new StreamSource(new StringReader("<" + form + "/>")),new StreamResult(writer));
            return writer.toString();
        } catch (TransformerConfigurationException e) {
            log.error(e.getMessage(), e);
        } catch (TransformerException e) {
            log.error(e.getMessage(), e);
        }
        return null;
    }

    private static void changeConceptMetadataValue(Concept concept,String field,String value)
    {
        AuthorityMetadataValue[] metadataValues = concept.getMetadata("internal","journal",field, Item.ANY);
        if(metadataValues==null||metadataValues.length==0)
        {
            concept.addMetadata("internal","journal",field,"en",value,null,-1);
        }
        else{

            for(AuthorityMetadataValue authorityMetadataValue:metadataValues)
            {
                if(!authorityMetadataValue.value.equals(value))
                {
                    concept.clearMetadata("internal","journal",field, Item.ANY);
                    concept.addMetadata("internal","journal",field,"en",value,null,-1);
                    break;
                }
            }
        }
    }
}

