package org.datadryad.submission;

import javax.mail.Address;
import javax.mail.MessagingException;
import javax.mail.Multipart;
import javax.mail.Part;
import javax.mail.Session;
import javax.mail.internet.MimeMessage;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.lang.StringUtils;
import org.dspace.core.ConfigurationManager;
import org.dspace.core.Email;
import org.dspace.core.I18nUtil;
import org.dspace.submit.utils.DryadJournalSubmissionUtils;

import org.jdom.Document;
import org.jdom.JDOMException;
import org.jdom.input.SAXBuilder;
import org.jdom.output.Format;
import org.jdom.output.XMLOutputter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.io.StringReader;

import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Properties;
import java.util.Scanner;
import java.util.StringTokenizer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Email processing servlet.
 * 
 * @author Kevin S. Clarke <ksclarke@gmail.com>
 */
@SuppressWarnings("serial")
public class DryadEmailSubmission extends HttpServlet {

    private static Logger LOGGER = LoggerFactory
    .getLogger(DryadEmailSubmission.class);

    private static String PROPERTIES_PROPERTY = "dryad.properties.filename";

    private static String EMAIL_TEMPLATE = "journal_submit_error";

    // Maps not concurrent but we only access, don't write to except at init()
    // map of Journal Codes to Journals
    private static Map<String, PartnerJournal> myJournals; 
    
    // map of Journal Names to Journal Codes (in case code is not preent in submission)
    private static Map<String, String> myJournalNames;

    /**
     * Handles the HTTP <code>GET</code> method by informing the caller that
     * <code>GET</code> is not supported, only <code>POST</code>.
     * 
     * @param aRequest A servlet request
     * @param aResponse A servlet response
     * @throws ServletException If a servlet-specific error occurs
     * @throws IOException If an I/O error occurs
     */
    @Override
    protected void doGet(HttpServletRequest aRequest,
            HttpServletResponse aResponse) throws ServletException, IOException {
        aResponse.sendError(HttpServletResponse.SC_METHOD_NOT_ALLOWED,
        "GET is not supported, you must POST to this service");
    }

    /**
     * Handles the HTTP <code>POST</code> method.
     * 
     * @param aRequest A servlet request
     * @param aResponse A servlet response
     * @throws ServletException If a servlet-specific error occurs
     * @throws IOException If an I/O error occurs
     */
    @Override
    protected void doPost(HttpServletRequest aRequest,
            HttpServletResponse aResponse) throws ServletException, IOException {
        PrintWriter toBrowser = getWriter(aResponse);
        InputStream postBody = aRequest.getInputStream();
        Session session = Session.getInstance(new Properties());

        if (LOGGER.isInfoEnabled()) {
            LOGGER.info("Request encoding: " + aRequest.getCharacterEncoding());
        }

        try {
            MimeMessage mime = new MimeMessage(session, postBody);
            String contentType = mime.getContentType();
            String encoding = mime.getEncoding();
            String contentID = mime.getContentID();

            if (LOGGER.isInfoEnabled()) {
                LOGGER.info("MIME contentType/ID/encoding: " + contentType
                        + " " + contentID + " " + encoding);
            }

            Part part = getTextPart(mime);
            if (part == null){
                throw new SubmissionException("Unexpected email type: " 
                        + mime.getContent().getClass().getName() + " reported content-type was " + contentType);
            }

            String message;
            if (encoding != null) {
                message = (String)part.getContent();
            }
            else {
                InputStream in = part.getInputStream();
                InputStreamReader isr = new InputStreamReader(in, "UTF-8");
                BufferedReader br = new BufferedReader(isr);
                StringBuilder builder = new StringBuilder();
                String line;

                while ((line = br.readLine()) != null) {
                    builder.append(line);
                    builder.append(System.getProperty("line.separator"));
                }

                message = builder.toString();
            }

            Address[] addresses = mime.getFrom();

            // Then we can hand off to implementer of EmailParser
            ParsingResult result = processMessage(message);

            // Do this because this is what the parsers are expecting to
            // build the corresponding author field from
            for (Address address : addresses) {
                message = "From: " + address.toString()
                + System.getProperty("line.separator") + message;
                result.setSenderEmailAddress(address.toString());
            }

            if (result.getStatus() != null) {
                throw new SubmissionException(result.getStatus());
            }

            // isHas?
            if (result.isHasFlawedId()) {
                throw new SubmissionException("Result ID is flawed: "
                        + result.getSubmissionId());
            }

            // We'll use JDOM b/c the libs are already included in DSpace
            SAXBuilder saxBuilder = new SAXBuilder();
            String xml = result.getSubmissionData().toString();

            // FIXME: Individual Email parsers don't supply a root element
            // Our JDOM classes below will add version, encoding, etc.
            xml = "<DryadEmailSubmission>"
                + System.getProperty("line.separator") + xml
                + "</DryadEmailSubmission>";

            StringReader xmlReader = new StringReader(xml);

            try {
                Format format = Format.getPrettyFormat();
                XMLOutputter toFile = new XMLOutputter(format);
                Document doc = saxBuilder.build(xmlReader);
                String journalCode = result.getJournalCode();

                if (LOGGER.isDebugEnabled()) {
                    LOGGER.debug("Getting metadata dir for " + journalCode);
                }

                PartnerJournal journal = myJournals.get(journalCode);

                if (journal == null ) {
                    throw new SubmissionRuntimeException("Journal (" + journalCode + ") not properly registered");
                }

                File dir = journal.getMetadataDir();
                String submissionId = result.getSubmissionId();
                String filename = DryadJournalSubmissionUtils.escapeFilename(submissionId + ".xml");
                File file = new File(dir, filename);
                FileOutputStream out = new FileOutputStream(file);
                OutputStreamWriter writer = new OutputStreamWriter(out, "UTF-8");

                // And we write the output to our submissions directory
                toFile.output(doc, new BufferedWriter(writer));
            }
            catch (JDOMException details) {
                if (LOGGER.isErrorEnabled()) {
                    LOGGER.debug(xml);
                }

                throw new SubmissionRuntimeException(details);
            }

            // Nice to return our result in case we are debugging output
            toBrowser.println(xml);
            toBrowser.close();
        }
        catch (Exception details) {
            sendEmailIfConfigured(details);

            if (details instanceof SubmissionException) {
                throw (SubmissionException) details;
            }
            else {
                throw new SubmissionException(details);
            }
        }
    }

    /**
     * This method was added because multipart content may contain other multipart content; this
     * needs to dig down until some text is found
     * @param part Either the full message or a part
     * @return a part with text/plain content
     * @throws MessagingException
     * @throws IOException
     */
    private Part getTextPart(Part part) throws MessagingException, IOException{
        String contentType = part.getContentType();

        if (contentType != null && contentType.startsWith("text/plain")){
            return part;   //
        }
        else if (contentType != null && 
                contentType.startsWith("multipart/alternative") ||
                contentType.startsWith("multipart/mixed")){    //could just use multipart as prefix, but what does this cover?
            Multipart mp = (Multipart)part.getContent();
            for (int i=0, count = mp.getCount();i<count;i++){
                Part p = mp.getBodyPart(i);
                Part pt = getTextPart(p);
                if (pt != null)
                    return pt;
            }
        }
        return null;
    }

    @Override
    public void init(ServletConfig aConfig) throws ServletException {
        super.init(aConfig);

        // First, get our properties from the configuration file
        Properties props = new Properties();
        String propFileName;

        // If we're running in the Jetty/Maven plugin we set properties here
        if ((propFileName = System.getProperty(PROPERTIES_PROPERTY)) != null) {
            try {
                props.load(new FileReader(new File(propFileName)));
            }
            catch (IOException details) {
                throw new SubmissionException(details);
            }

            if (LOGGER.isDebugEnabled()) {
                LOGGER.debug("Using {} properties from {}", new Object[] {
                        props.size(), propFileName });
            }
        }
        // Otherwise, we're running in the standard DSpace Tomcat
        else {
            if(!ConfigurationManager.isConfigured()) {
                // not configured
                // Get config parameter
                String config = getServletContext().getInitParameter("dspace.config");

                // Load in DSpace config
                ConfigurationManager.loadConfig(config);                
            }

            String journalPropFile = ConfigurationManager.getProperty("submit.journal.config");
            File propFile = new File(journalPropFile);

            if (!propFile.exists()) {
                throw new SubmissionException("Can't find properties file: "
                        + propFile.getAbsolutePath());
            }

            try {
                props.load(new FileReader(propFile));
            }
            catch (IOException details) {
                throw new SubmissionException(details);
            }

            if (LOGGER.isDebugEnabled()) {
                LOGGER.debug("Using properties from {}",propFile);
            }
        }

        // Next, turn those properties into something we can use
        Map<String, PartnerJournal> journals = new HashMap<String, PartnerJournal>();
        Enumeration<?> names = props.propertyNames();

        while (names.hasMoreElements()) {
            String propName = names.nextElement().toString();
            StringTokenizer tokenizer = new StringTokenizer(propName, ".");

            if (tokenizer.countTokens() < 3) continue;

            while (tokenizer.hasMoreTokens()) {
                if (tokenizer.nextToken().equals("journal")) {
                    String code = tokenizer.nextToken();
                    String property = tokenizer.nextToken();
                    PartnerJournal journal;

                    if (journals.containsKey(code)) {
                        journal = journals.get(code);
                    }
                    else {
                        journal = new PartnerJournal(code);
                        journals.put(code, journal);
                    }

                    if (property.equals("parsingScheme")) {
                        journal.setParsingScheme(props.getProperty(propName));
                    }
                    else if (property.equals("metadataDir")) {
                        journal.setMetadataDir(props.getProperty(propName));
                    }
                    else if (property.equals("fullname")) {
                        journal.setFullName(props.getProperty(propName));
                    }
                    // else ignore
                }
                // else ignore
            }
        }

        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("Checking that all journals are correctly registered");
        }

        // Returns validated map or throws an exception if there are problems
        myJournals = validate(journals);
        // Returns a mapping of Journal Names to Journal Codes
        myJournalNames = mapJournalNamesToCodes(journals);
    }

    /**
     * If we're running within DSpace (and not the Maven/Jetty test instance),
     * we can send email through there using their template system.
     * 
     * @param aException An exception that was thrown in the process of
     *        receiving a journal submission
     * @throws SubmissionException
     * @throws IOException
     */
    private void sendEmailIfConfigured(Exception aException)
    throws SubmissionException {
        try {
            if (ConfigurationManager.isConfigured()) {
                String exceptionMessage = aException.getMessage();
                StringBuilder message = new StringBuilder(exceptionMessage);
                String admin = ConfigurationManager.getProperty("mail.admin");
                String logDir = ConfigurationManager.getProperty("log.dir");
                Email email = ConfigurationManager.getEmail(I18nUtil.getEmailFilename(Locale.getDefault(), EMAIL_TEMPLATE));

                if (logDir == null || admin == null) {
                    throw new SubmissionException(
                    "DSpace mail is not properly configured");
                }

                for (StackTraceElement trace : aException.getStackTrace()) {
                    message.append(System.getProperty("line.separator"));
                    message.append("at ").append(trace.getClass()).append("(");
                    message.append(trace.getFileName()).append(":");
                    message.append(trace.getLineNumber()).append(")");
                }

                email.addRecipient(admin);
                email.addArgument(message);
                email.addArgument(logDir + "/journal-submit.log");
                email.send();
            }
        }
        catch (Exception details) {
            if (details instanceof SubmissionException) {
                throw (SubmissionException) details;
            }
            else {
                throw new SubmissionException(details);
            }
        }
    }

    private ParsingResult processMessage(String aMessage)
    throws SubmissionException {
        List<String> lines = new ArrayList<String>();
        Scanner emailScanner = new Scanner(aMessage);
        String journalName = null;
        String journalCode = null;

        while (emailScanner.hasNextLine()) {
            String line = emailScanner.nextLine();

            if (LOGGER.isTraceEnabled()) {
                LOGGER.trace("line=" + line);
            }

            if (StringUtils.stripToEmpty(line).equals("")) {
                continue;
            }
            else {
                Pattern journalCodePattern = Pattern.compile("^(Journal Code):(.+)");
                Matcher journalCodeMatcher = journalCodePattern.matcher(line);

                if(journalCodeMatcher.find()) {
                    journalCode = StringUtils.stripToEmpty(journalCodeMatcher.group(2));
                    // strip out leading NBSP if present
                    if (journalCode.codePointAt(0) == 160){
                        journalCode = journalCode.substring(1);
                    }
                }
                
                Pattern journalNamePattern = Pattern.compile("^(JOURNAL|Journal Name):(.+)");
                Matcher journalNameMatcher = journalNamePattern.matcher(line);

                if (journalNameMatcher.find()) {
                    journalName = StringUtils.stripToEmpty(journalNameMatcher.group(2));
                    if (journalName.codePointAt(0) == 160){          //Journal of Heredity has started inserting NBSP in several fields, including journal title
                        journalName = journalName.substring(1);
                    }
                }

                // Stop reading lines at EndDryadContent
                if(line.contains("EndDryadContent")) {
                    break;
                }
                lines.add(line);
            }
        }

        // After reading the entire message, attempt to find the PartnerJournal object by
        // Journal Code.  If Journal Code is not present, fall back to Journal Name
        if(journalCode == null) {
            LOGGER.debug("Journal Code not found in message, trying by journal name: " + journalName);
            if(journalName != null) {
                journalCode = myJournalNames.get(journalName);
            } else {
                throw new SubmissionException("Journal Code not present and Journal Name not found in message");
            }
            if(journalCode == null) {
                throw new SubmissionException("Journal Name " + journalName + " did not match a known Journal Code");
            }
        }
        
        if (journalCode != null) {
            PartnerJournal journal = myJournals.get(journalCode);
            if (journal != null){
                EmailParser parser = journal.getParser();
                ParsingResult result = parser.parseMessage(lines);

                result.setJournalCode(journalCode);
                result.setJournalName(journalName);

                return result;
            }
            else {
                throw new SubmissionException("Journal " + journalCode + " not found in configuration");
            }
        }
        else {
            throw new SubmissionException("Journal code not found in message");
        }
    }

    private Map<String, PartnerJournal> validate(
            Map<String, PartnerJournal> aJournalMap) {
        Map<String, PartnerJournal> results = new HashMap<String, PartnerJournal>();

        for (String journalCode : aJournalMap.keySet()) {
            PartnerJournal journal = aJournalMap.get(journalCode);
            if (!journal.isComplete()) {
                throw new SubmissionRuntimeException(journal.getName()
                        + "'s configuration isn't complete");
            }
            else {
                // now store our metadata by the journal name instead of code
                results.put(journalCode, journal);
            }

            if (LOGGER.isDebugEnabled()) {
                LOGGER.debug("Registered journal: " + journal.toString());
            }
        }
        
        return results;
    }

    private Map<String, String> mapJournalNamesToCodes(
            Map<String, PartnerJournal> aJournalMap) {
        Map<String, String> results = new HashMap<String, String>();

        for (String journalCode : aJournalMap.keySet()) {
            PartnerJournal journal = aJournalMap.get(journalCode);
            results.put(journal.getName(), journalCode);
        }
        return results;
    }

    /**
     * Returns a short description of the servlet.
     * 
     * @return a String containing servlet description
     */
    @Override
    public String getServletInfo() {
        return "Pre-processor for email from Dryad partner journals.";
    }

    /**
     * Returns a PrintWriter with the correct character encoding set.
     * 
     * @param aResponse In which to set the character encoding
     * @return A <code>PrintWriter</code> to send text through
     * @throws IOException If there is trouble getting a writer
     */
    private PrintWriter getWriter(HttpServletResponse aResponse)
    throws IOException {
        aResponse.setContentType("xml/application; charset=UTF-8");
        return aResponse.getWriter();
    }
}
