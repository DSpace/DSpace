/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.core;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.List;
import java.util.Properties;
import javax.activation.DataHandler;
import javax.activation.DataSource;
import javax.activation.FileDataSource;
import javax.mail.Address;
import javax.mail.BodyPart;
import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.Multipart;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.ContentType;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeBodyPart;
import javax.mail.internet.MimeMessage;
import javax.mail.internet.MimeMultipart;
import javax.mail.internet.ParseException;

import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.velocity.Template;
import org.apache.velocity.VelocityContext;
import org.apache.velocity.app.Velocity;
import org.apache.velocity.app.VelocityEngine;
import org.apache.velocity.exception.MethodInvocationException;
import org.apache.velocity.exception.ParseErrorException;
import org.apache.velocity.exception.ResourceNotFoundException;
import org.apache.velocity.runtime.resource.loader.StringResourceLoader;
import org.apache.velocity.runtime.resource.util.StringResourceRepository;
import org.dspace.services.ConfigurationService;
import org.dspace.services.factory.DSpaceServicesFactory;

/**
 * Class representing an e-mail message, also used to send e-mails.
 * <p>
 * Typical use:
 * </p>
 * <p>
 * <code>Email email = new Email();</code><br>
 * <code>email.addRecipient("foo@bar.com");</code><br>
 * <code>email.addArgument("John");</code><br>
 * <code>email.addArgument("On the Testing of DSpace");</code><br>
 * <code>email.send();</code><br>
 * </p>
 * <p>
 * <code>name</code> is the name of an email template in
 * <code>dspace-dir/config/emails/</code> (which also includes the subject.)
 * <code>arg0</code> and <code>arg1</code> are arguments to fill out the
 * message with.
 * <P>
 * Emails are formatted using Apache Velocity.  Headers such as Subject may be
 * supplied by the template, by defining them using #set().  Example:
 * </p>
 *
 * <pre>
 *
 *     ## This is a comment line which is stripped
 *     ##
 *     ## Parameters:   {0}  is a person's name
 *     ##               {1}  is the name of a submission
 *     ##
 *     #set($subject = 'Example e-mail')
 *
 *     Dear ${params[0]},
 *
 *     Thank you for sending us your submission &quot;${params[1]}&quot;.
 *
 * </pre>
 *
 * <p>
 * If the example code above was used to send this mail, the resulting mail
 * would have the subject <code>Example e-mail</code> and the body would be:
 * </p>
 *
 * <pre>
 *
 *
 *     Dear John,
 *
 *     Thank you for sending us your submission &quot;On the Testing of DSpace&quot;.
 *
 * </pre>
 *
 * @author Robert Tansley
 * @author Jim Downing - added attachment handling code
 * @author Adan Roman Ruiz at arvo.es - added inputstream attachment handling code
 */
public class Email {
    /**
     * The content of the message
     */
    private String content;
    private String contentName;

    /**
     * The subject of the message
     */
    private String subject;

    /**
     * The arguments to fill out
     */
    private final List<Object> arguments;

    /**
     * The recipients
     */
    private final List<String> recipients;

    /**
     * Reply to field, if any
     */
    private String replyTo;

    private final List<FileAttachment> attachments;
    private final List<InputStreamAttachment> moreAttachments;

    /**
     * The character set this message will be sent in
     */
    private String charset;

    private static final Logger LOG = LogManager.getLogger();

    /** Velocity template settings. */
    private static final String RESOURCE_REPOSITORY_NAME = "Email";
    private static final Properties VELOCITY_PROPERTIES = new Properties();
    static {
        VELOCITY_PROPERTIES.put(Velocity.RESOURCE_LOADERS, "string");
        VELOCITY_PROPERTIES.put("resource.loader.string.description",
                "Velocity StringResource loader");
        VELOCITY_PROPERTIES.put("resource.loader.string.class",
                StringResourceLoader.class.getName());
        VELOCITY_PROPERTIES.put("resource.loader.string.repository.name",
                RESOURCE_REPOSITORY_NAME);
        VELOCITY_PROPERTIES.put("resource.loader.string.repository.static",
                "false");
    }

    /** Velocity template for a message body */
    private Template template;

    /**
     * Create a new email message.
     */
    public Email() {
        arguments = new ArrayList<>(50);
        recipients = new ArrayList<>(50);
        attachments = new ArrayList<>(10);
        moreAttachments = new ArrayList<>(10);
        subject = "";
        template = null;
        content = "";
        replyTo = null;
        charset = null;
    }

    /**
     * Add a recipient
     *
     * @param email the recipient's email address
     */
    public void addRecipient(String email) {
        recipients.add(email);
    }

    /**
     * Set the content of the message. Setting this also "resets" the message
     * formatting - <code>addArgument</code> will start over. Comments and any
     * "Subject:" line must be stripped.
     *
     * @param name a name for this message body
     * @param cnt the content of the message
     */
    public void setContent(String name, String cnt) {
        content = cnt;
        contentName = name;
        arguments.clear();
    }

    /**
     * Set the subject of the message
     *
     * @param s the subject of the message
     */
    public void setSubject(String s) {
        subject = s;
    }

    /**
     * Set the reply-to email address
     *
     * @param email the reply-to email address
     */
    public void setReplyTo(String email) {
        replyTo = email;
    }

    /**
     * Fill out the next argument in the template
     *
     * @param arg the value for the next argument
     */
    public void addArgument(Object arg) {
        arguments.add(arg);
    }

    public void addAttachment(File f, String name) {
        attachments.add(new FileAttachment(f, name));
    }

    /** When given a bad MIME type for an attachment, use this instead. */
    private static final String DEFAULT_ATTACHMENT_TYPE = "application/octet-stream";

    public void addAttachment(InputStream is, String name, String mimetype) {
        if (null == mimetype) {
            LOG.error("Null MIME type replaced with '" + DEFAULT_ATTACHMENT_TYPE
                    + "' for attachment '" + name + "'");
            mimetype = DEFAULT_ATTACHMENT_TYPE;
        } else {
            try {
                new ContentType(mimetype); // Just try to parse it.
            } catch (ParseException ex) {
                LOG.error("Bad MIME type '" + mimetype
                        + "' replaced with '" + DEFAULT_ATTACHMENT_TYPE
                        + "' for attachment '" + name + "'", ex);
                mimetype = DEFAULT_ATTACHMENT_TYPE;
            }
        }

        moreAttachments.add(new InputStreamAttachment(is, name, mimetype));
    }

    public void setCharset(String cs) {
        charset = cs;
    }

    /**
     * "Reset" the message. Clears the arguments, attachments and recipients,
     * but leaves the subject and content intact.
     */
    public void reset() {
        arguments.clear();
        recipients.clear();
        attachments.clear();
        moreAttachments.clear();
        replyTo = null;
        charset = null;
    }

    /**
     * Sends the email.  If the template defines a Velocity context property
     * named among the values of DSpace configuration property
     * {@code mail.message.headers} then that name and its value will be added
     * to the message's headers.
     *
     * <p>"subject" is treated specially:  if {@link setSubject()} has not been called,
     * the value of any "subject" property will be used as if setSubject had
     * been called with that value.  Thus a template may define its subject, but
     * the caller may override it.
     *
     * @throws MessagingException if there was a problem sending the mail.
     * @throws IOException        if IO error
     */
    public void send() throws MessagingException, IOException {
        ConfigurationService config
                = DSpaceServicesFactory.getInstance().getConfigurationService();

        // Get the mail configuration properties
        String from = config.getProperty("mail.from.address");
        boolean disabled = config.getBooleanProperty("mail.server.disabled", false);

        // If no character set specified, attempt to retrieve a default
        if (charset == null) {
            charset = config.getProperty("mail.charset");
        }

        // Get session
        Session session = DSpaceServicesFactory.getInstance().getEmailService().getSession();

        // Create message
        MimeMessage message = new MimeMessage(session);

        // Set the recipients of the message
        Iterator<String> i = recipients.iterator();

        while (i.hasNext()) {
            message.addRecipient(Message.RecipientType.TO, new InternetAddress(
                i.next()));
        }

        // Format the mail message body
        VelocityEngine templateEngine = new VelocityEngine();
        templateEngine.init(VELOCITY_PROPERTIES);

        VelocityContext vctx = new VelocityContext();
        vctx.put("config", new UnmodifiableConfigurationService(config));
        vctx.put("params", Collections.unmodifiableList(arguments));

        if (null == template) {
            if (StringUtils.isBlank(content)) {
                // No template and no content -- PANIC!!!
                throw new MessagingException("Email has no body");
            }
            // No template, so use a String of content.
            StringResourceRepository repo = (StringResourceRepository)
                    templateEngine.getApplicationAttribute(RESOURCE_REPOSITORY_NAME);
            repo.putStringResource(contentName, content);
            // Turn content into a template.
            template = templateEngine.getTemplate(contentName);
        }

        StringWriter writer = new StringWriter();
        try {
            template.merge(vctx, writer);
        } catch (MethodInvocationException | ParseErrorException
                | ResourceNotFoundException ex) {
            LOG.error("Template not merged:  {}", ex.getMessage());
            throw new MessagingException("Template not merged", ex);
        }
        String fullMessage = writer.toString();

        // Set some message header fields
        Date date = new Date();
        message.setSentDate(date);
        message.setFrom(new InternetAddress(from));

        // Get headers defined by the template.
        for (String headerName : config.getArrayProperty("mail.message.headers")) {
            String headerValue = (String) vctx.get(headerName);
            if ("subject".equalsIgnoreCase(headerName)) {
                if (null != subject) {
                    subject = headerValue;
                }
            } else if ("charset".equalsIgnoreCase(headerName)) {
                charset = headerValue;
            } else {
                message.setHeader(headerName, headerValue);
            }
        }

        // Set the subject of the email.
        if (charset != null) {
            message.setSubject(subject, charset);
        } else {
            message.setSubject(subject);
        }

        // Add attachments
        if (attachments.isEmpty() && moreAttachments.isEmpty()) {
            // If a character set has been specified, or a default exists
            if (charset != null) {
                message.setText(fullMessage, charset);
            } else {
                message.setText(fullMessage);
            }
        } else {
            Multipart multipart = new MimeMultipart();

            // create the first part of the email
            BodyPart messageBodyPart = new MimeBodyPart();
            messageBodyPart.setText(fullMessage);
            multipart.addBodyPart(messageBodyPart);

            // Add file attachments
            for (FileAttachment attachment : attachments) {
                // add the file
                messageBodyPart = new MimeBodyPart();
                messageBodyPart.setDataHandler(new DataHandler(
                        new FileDataSource(attachment.file)));
                messageBodyPart.setFileName(attachment.name);
                multipart.addBodyPart(messageBodyPart);
            }

            // Add stream attachments
            for (InputStreamAttachment attachment : moreAttachments) {
                // add the stream
                messageBodyPart = new MimeBodyPart();
                messageBodyPart.setDataHandler(new DataHandler(
                        new InputStreamDataSource(attachment.name,attachment.mimetype,attachment.is)));
                messageBodyPart.setFileName(attachment.name);
                multipart.addBodyPart(messageBodyPart);
            }

            message.setContent(multipart);
        }

        if (replyTo != null) {
            Address[] replyToAddr = new Address[1];
            replyToAddr[0] = new InternetAddress(replyTo);
            message.setReplyTo(replyToAddr);
        }

        if (disabled) {
            StringBuilder text = new StringBuilder(
                "Message not sent due to mail.server.disabled:\n");

            Enumeration<String> headers = message.getAllHeaderLines();
            while (headers.hasMoreElements()) {
                text.append(headers.nextElement()).append('\n');
            }

            if (!attachments.isEmpty()) {
                text.append("\nAttachments:\n");
                for (FileAttachment f : attachments) {
                    text.append(f.name).append('\n');
                }
                text.append('\n');
            }

            text.append('\n').append(fullMessage);

            LOG.info(text.toString());
        } else {
            Transport.send(message);
        }
    }

    /**
     * Get the VTL template for an email message. The message is suitable
     * for inserting values using Apache Velocity.
     *
     * @param emailFile
     *            full name for the email template, for example "/dspace/config/emails/register".
     *
     * @return the email object, configured with subject and body.
     *
     * @throws IOException if IO error
     *                     if the template couldn't be found, or there was some other
     *                     error reading the template
     */
    public static Email getEmail(String emailFile)
        throws IOException {
        String charset = null;
        StringBuilder contentBuffer = new StringBuilder();
        try (
            InputStream is = new FileInputStream(emailFile);
            InputStreamReader ir = new InputStreamReader(is, "UTF-8");
            BufferedReader reader = new BufferedReader(ir);
            ) {
            boolean more = true;
            while (more) {
                String line = reader.readLine();
                if (line == null) {
                    more = false;
                } else {
                    contentBuffer.append(line);
                    contentBuffer.append("\n");
                }
            }
        }
        Email email = new Email();
        email.setContent(emailFile, contentBuffer.toString());
        if (charset != null) {
            email.setCharset(charset);
        }
        return email;
    }
    /*
     * Implementation note: It might be necessary to add a quick utility method
     * like "send(to, subject, message)". We'll see how far we get without it -
     * having all emails as templates in the config allows customisation and
     * internationalisation.
     *
     * Note that everything is stored and the run in send() so that only send()
     * throws a MessagingException.
     */

    /**
     * Test method to send an email to check email server settings
     *
     * @param args command line arguments.  The first is the path to an email
     *              template file; the rest are the positional arguments for the
     *              template.  If there are no arguments, a short, plain test
     *              message is sent.
     */
    public static void main(String[] args) {
        ConfigurationService config
                = DSpaceServicesFactory.getInstance().getConfigurationService();
        String to = config.getProperty("mail.admin");
        String subject = "DSpace test email";
        String server = config.getProperty("mail.server");
        String url = config.getProperty("dspace.ui.url");
        Email message;
        try {
            if (args.length <= 0) {
                message = new Email();
                message.setContent("testing", "This is a test email sent from DSpace: " + url);
            } else {
                message = Email.getEmail(args[0]);
                for (int i = 1; i < args.length; i++) {
                    message.addArgument(args[i]);
                }
            }
            message.setSubject(subject);
            message.addRecipient(to);
            System.out.println("\nAbout to send test email:");
            System.out.println(" - To: " + to);
            System.out.println(" - Subject: " + subject);
            System.out.println(" - Server: " + server);
            boolean disabled = config.getBooleanProperty("mail.server.disabled", false);
            if (disabled) {
                System.err.println("\nError sending email:");
                System.err.println(" - Error: cannot test email because mail.server.disabled is set to true");
                System.err.println("\nPlease see the DSpace documentation for assistance.\n");
                System.err.println("\n");
                System.exit(1);
                return;
            }
            message.send();
        } catch (MessagingException | IOException ex) {
            System.err.println("\nError sending email:");
            System.err.format(" - Error: %s%n", ex);
            System.err.println("\nPlease see the DSpace documentation for assistance.\n");
            System.err.println("\n");
            System.exit(1);
        }
        System.out.println("\nEmail sent successfully!\n");
    }

    /**
     * Utility struct class for handling file attachments.
     *
     * @author ojd20
     */
    private static class FileAttachment {
        public FileAttachment(File f, String n) {
            this.file = f;
            this.name = n;
        }

        File file;

        String name;
    }

    /**
     * Utility struct class for handling file attachments.
     *
     * @author Adán Román Ruiz at arvo.es
     */
    private static class InputStreamAttachment {
        public InputStreamAttachment(InputStream is, String name, String mimetype) {
            this.is = is;
            this.name = name;
            this.mimetype = mimetype;
        }

        InputStream is;
        String mimetype;
        String name;
    }

    /**
     * @author arnaldo
     */
    public static class InputStreamDataSource implements DataSource {
        private final String name;
        private final String contentType;
        private final ByteArrayOutputStream baos;

        InputStreamDataSource(String name, String contentType, InputStream inputStream) throws IOException {
            this.name = name;
            this.contentType = contentType;
            baos = new ByteArrayOutputStream();
            int read;
            byte[] buff = new byte[256];
            while ((read = inputStream.read(buff)) != -1) {
                baos.write(buff, 0, read);
            }
        }

        @Override
        public String getContentType() {
            return contentType;
        }

        @Override
        public InputStream getInputStream() throws IOException {
            return new ByteArrayInputStream(baos.toByteArray());
        }

        @Override
        public String getName() {
            return name;
        }

        @Override
        public OutputStream getOutputStream() throws IOException {
            throw new IOException("Cannot write to this read-only resource");
        }
    }

    /**
     * Wrap ConfigurationService to prevent templates from modifying
     * the configuration.
     */
    public static class UnmodifiableConfigurationService {
        private final ConfigurationService configurationService;

        /**
         * Swallow an instance of ConfigurationService.
         *
         * @param cs the real instance, to be wrapped.
         */
        public UnmodifiableConfigurationService(ConfigurationService cs) {
            configurationService = cs;
        }

        /**
         * Look up a key in the actual ConfigurationService.
         *
         * @param key to be looked up in the DSpace configuration.
         * @return whatever value ConfigurationService associates with {@code key}.
         */
        public String get(String key) {
            return configurationService.getProperty(key);
        }
    }
}
