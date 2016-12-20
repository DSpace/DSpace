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
import java.text.MessageFormat;
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

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.velocity.Template;
import org.apache.velocity.VelocityContext;
import org.apache.velocity.app.Velocity;
import org.apache.velocity.app.VelocityEngine;
import org.apache.velocity.runtime.log.Log4JLogChute;
import org.apache.velocity.runtime.resource.loader.ClasspathResourceLoader;
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
 * </p>
 * <p>
 * Emails are formatted using <code>java.text.MessageFormat.</code>
 * Additionally, comment lines (starting with '#') are stripped, and if a line
 * starts with "Subject:" the text on the right of the colon is used for the
 * subject line. For example:
 * </p>
 *
 * <pre>
 *
 *     # This is a comment line which is stripped
 *     #
 *     # Parameters:   {0}  is a person's name
 *     #               {1}  is the name of a submission
 *     #
 *     Subject: Example e-mail
 *
 *     Dear {0},
 *
 *     Thank you for sending us your submission &quot;{1}&quot;.
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
 * <p>
 * Note that parameters like <code>{0}</code> cannot be placed in the subject
 * of the e-mail; they won't get filled out.
 * </p>
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
    private static final Properties VELOCITY_PROPERTIES = new Properties();
    static {
        VELOCITY_PROPERTIES.put("runtime.log.logsystem.class",
                Log4JLogChute.class.getCanonicalName());
        VELOCITY_PROPERTIES.put("resource.loader", "classpath");
        VELOCITY_PROPERTIES.put("classpath.resource.loader.description",
                "Velocity Classpath Resource Loader");
        VELOCITY_PROPERTIES.put("classpath.resource.loader.class",
                ClasspathResourceLoader.class.getCanonicalName());
        VELOCITY_PROPERTIES.put("classpath.resource.loader.cache", "true");
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
     * @param cnt the content of the message
     */
    public void setContent(String cnt) {
        content = cnt;
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
            log.error("Null MIME type replaced with '" + DEFAULT_ATTACHMENT_TYPE
                    + "' for attachment '" + name + "'");
            mimetype = DEFAULT_ATTACHMENT_TYPE;
        } else {
            try {
                new ContentType(mimetype); // Just try to parse it.
            } catch (ParseException ex) {
                log.error("Bad MIME type '" + mimetype
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
     * Sends the email.
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

        // Format the mail message
        VelocityEngine templateEngine = new VelocityEngine();
        templateEngine.init(VELOCITY_PROPERTIES);

        org.apache.velocity.context.Context ctx = new VelocityContext();
        ctx.put("config", new UnmodifiableConfigurationService(config));
        ctx.put("params", Collections.unmodifiableList(arguments));

        if (null == template) { // No template, so look for a String of content.
            if (null == content) { // SNH -- see constructor.
                // No template and no content -- PANIC!!!
                LOG.error("Email has no body");
                template = new Template();
                template.setData("No content.");
            } else {   // Turn content into a template.
                template = new Template();
                template.setData(content);
            }
        }

        StringWriter writer = new StringWriter();
        template.merge(ctx, writer);
        String fullMessage = writer.toString();

        Date date = new Date();
        message.setSentDate(date);
        message.setFrom(new InternetAddress(from));

        // Set the subject of the email (may contain parameters)
        String fullSubject = MessageFormat.format(subject, arguments.toArray());
        if (charset != null) {
            message.setSubject(fullSubject, charset);
        } else {
            message.setSubject(fullSubject);
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
            for (FileAttachment attachment : attachments) {
                // add the file
                messageBodyPart = new MimeBodyPart();
                messageBodyPart.setDataHandler(new DataHandler(
                        new FileDataSource(attachment.file)));
                messageBodyPart.setFileName(attachment.name);
                multipart.addBodyPart(messageBodyPart);
            }
            message.setContent(multipart);
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
            StringBuffer text = new StringBuffer(
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

            LOG.info(text);
        } else {
            Transport.send(message);
        }
    }

    /**
     * Get the plain-text template for an email message. The message is suitable for
     * inserting values using {@link java.text.MessageFormat}.
     *
     * @param emailFile full name for the email template, for example "/dspace/config/emails/register".
     * @return the email object, with the content and subject filled out from
     * the template
     * @throws IOException if IO error
     *                     if the template couldn't be found, or there was some other
     *                     error reading the template
     */
    public static Email getEmail(String emailFile)
        throws IOException {
        String charset = null;
        String subject = "";
        StringBuilder contentBuffer = new StringBuilder();
        InputStream is = null;
        InputStreamReader ir = null;
        BufferedReader reader = null;
        try {
            is = new FileInputStream(emailFile);
            ir = new InputStreamReader(is, "UTF-8");
            reader = new BufferedReader(ir);
            boolean more = true;
            while (more) {
                String line = reader.readLine();
                if (line == null) {
                    more = false;
                } else if (line.toLowerCase().startsWith("subject:")) {
                    subject = line.substring(8).trim();
                } else if (line.toLowerCase().startsWith("charset:")) {
                    charset = line.substring(8).trim();
                } else if (!line.startsWith("#")) {
                    contentBuffer.append(line);
                    contentBuffer.append("\n");
                }
            }
        } finally {
            if (reader != null) {
                try {
                    reader.close();
                } catch (IOException ioe) {
                    // ignore
                }
            }
            if (ir != null) {
                try {
                    ir.close();
                } catch (IOException ioe) {
                    // ignore
                }
            }
            if (is != null) {
                try {
                    is.close();
                } catch (IOException ioe) {
                    // ignore
                }
            }
        }
        Email email = new Email();
        email.setSubject(subject);
        email.setContent(contentBuffer.toString());
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
     * Associate the message with an external Velocity
     * {@link org.apache.velocity.Template}.  You should also supply a subject
     * using {@link #setSubject(java.lang.String)}.
     *
     * @param templateName simple name to be looked up in the Velocity template
     *                      resources.
     */
    public void setTemplate(String templateName) {
        template = Velocity.getTemplate(templateName);
    }

    /**
     * Test method to send an email to check email server settings
     *
     * @param args the command line arguments given
     */
    public static void main(String[] args) {
        ConfigurationService config = DSpaceServicesFactory.getInstance().getConfigurationService();
        String to = config.getProperty("mail.admin");
        String subject = "DSpace test email";
        String server = config.getProperty("mail.server");
        String url = config.getProperty("dspace.url");
        Email e = new Email();
        e.setSubject(subject);
        e.addRecipient(to);
        e.content = "This is a test email sent from DSpace: " + url;
        System.out.println("\nAbout to send test email:");
        System.out.println(" - To: " + to);
        System.out.println(" - Subject: " + subject);
        System.out.println(" - Server: " + server);
        boolean disabled = config.getBooleanProperty("mail.server.disabled", false);
        try {
            if (disabled) {
                System.err.println("\nError sending email:");
                System.err.println(" - Error: cannot test email because mail.server.disabled is set to true");
                System.err.println("\nPlease see the DSpace documentation for assistance.\n");
                System.err.println("\n");
                System.exit(1);
                return;
            }
            e.send();
        } catch (MessagingException | IOException me) {
            System.err.println("\nError sending email:");
            System.err.println(" - Error: " + me);
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
    public class InputStreamDataSource implements DataSource {
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
     * Wrap ConfigurationService to prevent templates from modifying the configuration.
     */
    private class UnmodifiableConfigurationService {
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
