package org.datadryad.submission;

import org.apache.commons.lang.StringEscapeUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.datadryad.rest.models.Manuscript;
import org.dspace.JournalUtils;
import org.dspace.content.authority.Concept;
import org.dspace.core.ConfigurationManager;
import org.dspace.core.Context;
import org.dspace.core.Email;
import org.dspace.core.I18nUtil;

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
import java.io.*;
import java.sql.SQLException;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Email processing servlet.
 *
 * @author Kevin S. Clarke <ksclarke@gmail.com>
 */
@SuppressWarnings("serial")
public class DryadEmailSubmission extends HttpServlet {

    private static final Logger LOGGER = Logger.getLogger(DryadEmailSubmission.class);

    private static String PROPERTIES_PROPERTY = "dryad.properties.filename";

    private static String EMAIL_TEMPLATE = "journal_submit_error";

    // Timer for scheduled harvesting of emails
    private Timer myEmailHarvester;

    private static DryadGmailService dryadGmailService;

    /**
     * UPDATE: GET only works for authorization and testing Gmail API.
     * Handles the HTTP <code>GET</code> method by informing the caller that
     * <code>GET</code> is not supported, only <code>POST</code>.
     *
     * @param aRequest  A servlet request
     * @param aResponse A servlet response
     * @throws ServletException If a servlet-specific error occurs
     * @throws IOException      If an I/O error occurs
     */
    @Override
    protected void doGet(HttpServletRequest aRequest,
                         HttpServletResponse aResponse) throws ServletException, IOException {
        String requestURI = aRequest.getRequestURI();
        if (requestURI.contains("authorize")) {
            String queryString = aRequest.getQueryString();
            if (aRequest.getQueryString() == null) {
                // If we've never gotten a credential from here before, do this.
                String urlString = dryadGmailService.getAuthorizationURLString();
                LOGGER.info("userID "+dryadGmailService.getMyUserID());
                aResponse.sendRedirect(urlString);
            }
            else if (queryString.contains("code=")) {
                String code = queryString.substring(queryString.indexOf("=")+1);
                LOGGER.info("authorizing with code "+ code);
                // Generate Credential using retrieved code.
                dryadGmailService.authorize(code);
            }
            return;
        } else if (requestURI.contains("test")) {
            try {
                LOGGER.info(DryadGmailService.testMethod());
            } catch (IOException e) {
                LOGGER.info(e.getMessage());
                throw new RuntimeException(e.getMessage());
            }
        } else if (requestURI.contains("retrieve")) {
            LOGGER.info("manually running DryadGmailService");
            retrieveMail();
        } else if (requestURI.contains("clear")) {
            LOGGER.info("clearing retrieved messages");
            DryadGmailService.completeJournalProcessing();
        } else {
            aResponse.sendError(HttpServletResponse.SC_METHOD_NOT_ALLOWED,
                    "GET is not supported, you must POST to this service");
        }
    }

    /**
     * Handles the HTTP <code>POST</code> method.
     *
     * @param aRequest A servlet request
     * @param aResponse A servlet response
     * @throws ServletException If a servlet-specific error occurs
     * @throws IOException      If an I/O error occurs
     */
    @Override
    protected void doPost(HttpServletRequest aRequest,
                          HttpServletResponse aResponse) throws ServletException, IOException {

        LOGGER.info("Request encoding: " + aRequest.getCharacterEncoding());

        try {
            PrintWriter toBrowser = getWriter(aResponse);
            InputStream postBody = aRequest.getInputStream();
            Session session = Session.getInstance(new Properties());
            MimeMessage mime = new MimeMessage(session, postBody);
            processMimeMessage(mime);
            toBrowser.close();
        } catch (Exception details) {
            sendEmailIfConfigured(details);

            if (details instanceof SubmissionException) {
                throw (SubmissionException) details;
            } else {
                throw new SubmissionException(details);
            }
        }
    }

    private void retrieveMail () {
        LOGGER.info("retrieving mail with label '" + ConfigurationManager.getProperty("submit.journal.email.label") + "'");
        try {
            List<String> messageIDs = DryadGmailService.getJournalMessageIds();
            if (messageIDs != null) {
                LOGGER.info("retrieved " + messageIDs.size() + " messages");
                for (String mID : messageIDs) {
                    MimeMessage message = DryadGmailService.getMessageForId(mID);
                    try {
                        processMimeMessage(message);
                    } catch (Exception details) {
                        DryadGmailService.addErrorLabelForMessageWithId(mID);
                        LOGGER.info("Exception thrown while processing message " + mID + ": " + details.getMessage() + ", " + details.getClass().getName());
                    }
                    DryadGmailService.removeJournalLabelForMessageWithId(mID);
                }
            }
            DryadGmailService.completeJournalProcessing();
        } catch (IOException e) {
            LOGGER.info("Exception thrown: " + e.getMessage() + ", " + e.getClass().getName());
        }
    }
    /**
     * This method was added because multipart content may contain other multipart content; this
     * needs to dig down until some text is found
     *
     * @param part Either the full message or a part
     * @return a part with text/plain content
     * @throws MessagingException
     * @throws IOException
     */
    private Part getTextPart(Part part) throws MessagingException, IOException {
        String contentType = part.getContentType();

        if (contentType != null && contentType.startsWith("text/plain")) {
            return part;   //
        } else if (contentType != null &&
                contentType.startsWith("multipart/alternative") ||
                contentType.startsWith("multipart/mixed")) {    //could just use multipart as prefix, but what does this cover?
            Multipart mp = (Multipart) part.getContent();
            for (int i = 0, count = mp.getCount(); i < count; i++) {
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
                props.load(new InputStreamReader(new FileInputStream(propFileName), "UTF-8"));
            } catch (IOException details) {
                throw new SubmissionException(details);
            }

        }
        // Otherwise, we're running in the standard DSpace Tomcat
        else {
            if (!ConfigurationManager.isConfigured()) {
                // not configured
                // Get config parameter
                String config = getServletContext().getInitParameter("dspace.config");

                // Load in DSpace config
                ConfigurationManager.loadConfig(config);
            }

            dryadGmailService = new DryadGmailService();
        }

        LOGGER.debug("scheduling email harvesting");
        myEmailHarvester = new Timer();
        // schedule email harvesting to happen once an hour
        int timerInterval = Integer.parseInt(ConfigurationManager.getProperty("submit.journal.email.timer"));
        myEmailHarvester.schedule(new DryadEmailSubmissionHarvester(), 0, 1000 * timerInterval);
    }

    /**
     * If we're running within DSpace (and not the Maven/Jetty test instance),
     * we can send email through there using their template system.
     *
     * @param aException An exception that was thrown in the process of
     *                   receiving a journal submission
     * @throws SubmissionException
     * @throws IOException
     */
    private void sendEmailIfConfigured(Exception aException)
            throws SubmissionException {
        try {
            if (ConfigurationManager.isConfigured()) {
                String exceptionMessage = aException.toString();
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
        } catch (Exception details) {
            if (details instanceof SubmissionException) {
                throw (SubmissionException) details;
            } else {
                throw new SubmissionException(details);
            }
        }
    }

    private void processMimeMessage (MimeMessage mime) throws Exception {
        LOGGER.info("MIME contentType/ID/encoding: " + mime.getContentType()
                + " " + mime.getContentID() + " " + mime.getEncoding());

        Part part = getTextPart(mime);
        if (part == null) {
            throw new SubmissionException("Unexpected email type: "
                    + mime.getContent().getClass().getName() + " reported content-type was " + mime.getContentType());
        }

        String message;
        if (mime.getEncoding() != null) {
            message = (String) part.getContent();
        } else {
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

        List<String> dryadContent = new ArrayList<String>();
        Scanner emailScanner = new Scanner(message);
        String journalName = null;
        String journalCode = null;
        EmailParser parser = null;
        Manuscript manuscript = null;
        boolean dryadContentStarted = false;
        Context context = null;
        Concept concept = null;
        while (emailScanner.hasNextLine()) {
            String line = emailScanner.nextLine().replace("\u00A0",""); // \u00A0 is Unicode nbsp; these should be removed
            line = StringEscapeUtils.unescapeHtml(line);
            // Stop reading lines at EndDryadContent
            if (line.contains("EndDryadContent")) {
                break;
            }

            // Stop reading lines if we've run into the sig line
            if (StringUtils.stripToEmpty(line).equals("--")) {
                break;
            }

            if (StringUtils.stripToEmpty(line).equals("")) {
                continue;
            }

            // Skip lines that are just visual separators, like "xxxxxxxxxx" or "=========="
            if (StringUtils.stripToEmpty(line).matches("(.)\\1{5,}")) {
                continue;
            }

            Matcher journalCodeMatcher = Pattern.compile("^\\s*>*\\s*(Journal Code):\\s*([a-zA-Z]+)").matcher(line);
            if (journalCodeMatcher.find()) {
                journalCode = journalCodeMatcher.group(2);
                dryadContentStarted = true;
            }

            Matcher journalNameMatcher = Pattern.compile("^\\s*>*\\s*(JOURNAL|Journal Name):\\s*(.+)").matcher(line);
            if (journalNameMatcher.find()) {
                journalName = journalNameMatcher.group(2);
                journalName = StringUtils.stripToEmpty(journalName);
                dryadContentStarted = true;
            }

            if (dryadContentStarted) {
                dryadContent.add(line);
            }
        }
        // After reading the entire message, attempt to find the journal by
        // Journal Code.  If Journal Code is not present, fall back to Journal Name
        try {
            context = new Context();
            if (journalCode == null) {
                LOGGER.debug("Journal Code not found in message, trying by journal name: " + journalName);
                if (journalName != null) {
                    try {
                        concept = JournalUtils.getJournalConceptByName(context, journalName);
                    } catch (SQLException e) {
                        throw new SubmissionException(e);
                    }
                    journalCode = JournalUtils.getJournalShortID(concept);

                } else {
                    throw new SubmissionException("Journal Code not present and Journal Name not found in message");
                }
            }

            // if journalCode is still null, throw an exception.
            if (journalCode == null) {
                throw new SubmissionException("Journal Name " + journalName + " did not match a known Journal Name");
            }

            // find the associated concept and initialize the parser variable.
            journalCode = JournalUtils.cleanJournalCode(journalCode);
            try {
                concept = JournalUtils.getJournalConceptByShortID(context, journalCode);
            } catch (SQLException e) {
                throw new SubmissionException(e);
	        }

            if (concept == null) {
                throw new SubmissionException("Concept not found for journal " + journalCode);
            }

            // at this point, concept is not null.
            try {
                parser = getEmailParser(JournalUtils.getParsingScheme(concept));
                parser.parseMessage(dryadContent);
                manuscript = parser.getManuscript();
            } catch (SubmissionException e) {
                throw new SubmissionException("Journal " + journalCode + " parsing scheme not found");
            }

            if ((manuscript != null) && (JournalUtils.manuscriptIsValid(context, manuscript))) {
                // edit the manuscript ID to the canonical one:
                manuscript.manuscriptId = JournalUtils.getCanonicalManuscriptID(context, manuscript);

                JournalUtils.writeManuscriptToXMLFile(context, manuscript);
                JournalUtils.writeManuscriptToDB(context, manuscript);
            } else {
                throw new SubmissionException("Parser could not validly parse the message");
            }
        } catch (SQLException e) {
            throw new SubmissionException("Couldn't get context", e);
        }
        finally {
            try {
                if (context != null) {
                    context.complete();
                }
            } catch (SQLException e) {
                context.abort();
                throw new RuntimeException("Context.complete threw an exception, aborting instead");
            }
        }
    }

    private EmailParser getEmailParser(String myParsingScheme) throws SubmissionException {
        String className = EmailParser.class.getPackage().getName()
                + ".EmailParserFor" + StringUtils.capitalize(myParsingScheme);

        LOGGER.debug("Getting parser: " + className);

        try {
            return (EmailParser) Class.forName(className).newInstance();
        }
        catch (ClassNotFoundException details) {
            // return a base EmailParser
            return new EmailParser();
        }
        catch (IllegalAccessException details) {
            throw new SubmissionException(details);
        }
        catch (InstantiationException details) {
            throw new SubmissionException(details);
        }
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

    private class DryadEmailSubmissionHarvester extends TimerTask {
        @Override
        public void run() {
            retrieveMail();
        }
    }
}
