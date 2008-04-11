/*
 * Email.java
 *
 * Version: $Revision: 1.5 $
 *
 * Date: $Date: 2004/12/22 17:48:45 $
 *
 * Copyright (c) 2002, Hewlett-Packard Company and Massachusetts
 * Institute of Technology.  All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are
 * met:
 *
 * - Redistributions of source code must retain the above copyright
 * notice, this list of conditions and the following disclaimer.
 *
 * - Redistributions in binary form must reproduce the above copyright
 * notice, this list of conditions and the following disclaimer in the
 * documentation and/or other materials provided with the distribution.
 *
 * - Neither the name of the Hewlett-Packard Company nor the name of the
 * Massachusetts Institute of Technology nor the names of their
 * contributors may be used to endorse or promote products derived from
 * this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 * ``AS IS'' AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 * LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR
 * A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT
 * HOLDERS OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT,
 * INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING,
 * BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS
 * OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 * ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR
 * TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE
 * USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH
 * DAMAGE.
 */
package org.dspace.core;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Properties;

import javax.mail.Address;
import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeBodyPart;
import javax.mail.internet.MimeMessage;
import javax.mail.internet.MimeMultipart;

/**
 * Class representing an e-mail message, also used to send e-mails.
 * <P>
 * Typical use:
 * <P>
 * <code>Email email = ConfigurationManager.getEmail(name);</code><br>
 * <code>email.addRecipient("foo@bar.com");</code><br>
 * <code>email.addArgument("John");</code><br>
 * <code>email.addArgument("On the Testing of DSpace");</code><br>
 * <code>email.send();</code><br>
 * <P>
 * <code>name</code> is the name of an email template in
 * <code>dspace-dir/config/emails/</code> (which also includes the subject.)
 * <code>arg0</code> and <code>arg1</code> are arguments to fill out the
 * message with.
 * <P>
 * Emails are formatted using <code>java.text.MessageFormat.</code>
 * Additionally, comment lines (starting with '#') are stripped, and if a line
 * starts with "Subject:" the text on the right of the colon is used for the
 * subject line. For example:
 * <P>
 * 
 * <pre>
 * 
 *  # This is a comment line which is stripped
 *  #
 *  # Parameters:   {0}  is a person's name
 *  #               {1}  is the name of a submission
 *  #
 *  Subject: Example e-mail
 * 
 *  Dear {0},
 * 
 *  Thank you for sending us your submission &quot;{1}&quot;.
 *  
 * </pre>
 * 
 * <P>
 * If the example code above was used to send this mail, the resulting mail
 * would have the subject <code>Example e-mail</code> and the body would be:
 * <P>
 * 
 * <pre>
 * 
 * 
 *  Dear John,
 * 
 *  Thank you for sending us your submission &quot;On the Testing of DSpace&quot;.
 *  
 * </pre>
 * 
 * <P>
 * Note that parameters like <code>{0}</code> cannot be placed in the subject
 * of the e-mail; they won't get filled out.
 * 
 * 
 * @author Robert Tansley
 * @version $Revision: 1.5 $
 */
public class Email
{
    /*
     * Implementation note: It might be necessary to add a quick utility method
     * like "send(to, subject, message)". We'll see how far we get without it -
     * having all emails as templates in the config allows customisation and
     * internationalisation.
     * 
     * Note that everything is stored and the run in send() so that only send()
     * throws a MessagingException.
     */

    /** The content of the message */
    private String content;

    /** The subject of the message */
    private String subject;

    /** The arguments to fill out */
    private List arguments;

    /** The recipients */
    private List recipients;

    /** Reply to field, if any */
    private String replyTo;

    /** Attachments */
    private List attachments;

    /**
     * Create a new email message.
     */
    Email()
    {
        arguments = new ArrayList();
        recipients = new ArrayList();
        subject = "";
        content = "";
        replyTo = null;
        attachments = new ArrayList();
    }

    /**
     * Add a recipient
     * 
     * @param email
     *            the recipient's email address
     */
    public void addRecipient(String email)
    {
        recipients.add(email);
    }

    /**
     * Add attachment.
     * 
     * @param attachment single attachment
     */
    public void addAttachment(MimeBodyPart attachment)
    {
        attachments.add(attachment);
    }

    /**
     * Set the content of the message. Setting this "resets" the message
     * formatting -<code>addArgument</code> will start. Comments and any
     * "Subject:" line must be stripped.
     * 
     * @param cnt
     *            the content of the message
     */
    void setContent(String cnt)
    {
        content = cnt;
        arguments = new ArrayList();
    }

    /**
     * Set the subject of the message
     * 
     * @param s
     *            the subject of the message
     */
    void setSubject(String s)
    {
        subject = s;
    }

    /**
     * Set the reply-to email address
     * 
     * @param email
     *            the reply-to email address
     */
    public void setReplyTo(String email)
    {
        replyTo = email;
    }

    /**
     * Fill out the next argument in the template
     * 
     * @param arg
     *            the value for the next argument
     */
    public void addArgument(Object arg)
    {
        arguments.add(arg);
    }

    /**
     * "Reset" the message. Clears the arguments and recipients, but leaves the
     * subject and content intact.
     */
    public void reset()
    {
        arguments = new ArrayList();
        recipients = new ArrayList();
        replyTo = null;
    }

    /**
     * Sends the email.
     * 
     * @throws MessagingException
     *             if there was a problem sending the mail.
     */
    public void send() throws MessagingException
    {
        // Get the mail configuration properties
        String server = ConfigurationManager.getProperty("mail.server");
        String from = ConfigurationManager.getProperty("mail.from.address");
        String localhost = ConfigurationManager.getProperty("mail.localhost");
        
        // Set up properties for mail session
        Properties props = System.getProperties();
        props.put("mail.smtp.host", server);
        if (localhost != null) {
          props.put("mail.smtp.localhost", localhost);
        }

        // Get session
        Session session = Session.getDefaultInstance(props, null);

        // Create message
        MimeMessage message = new MimeMessage(session);

        // Set the recipients of the message
        Iterator i = recipients.iterator();

        while (i.hasNext())
        {
            message.addRecipient(Message.RecipientType.TO, new InternetAddress(
                    (String) i.next()));
        }

        // Format the mail message
        Object[] args = arguments.toArray();
        String fullMessage = MessageFormat.format(content, args);

        message.setFrom(new InternetAddress(from));
        message.setSubject(subject);

        MimeMultipart mp = new MimeMultipart();

        // Add the first part, which is text
        MimeBodyPart text = new MimeBodyPart();
        text.setContent(fullMessage,"text/plain");
	mp.addBodyPart(text);

	// Add the rest of the parts
	for (i=attachments.iterator(); i.hasNext(); ) {
	  mp.addBodyPart((MimeBodyPart)i.next());
	}

        message.setContent(mp);
        message.saveChanges();

        if (replyTo != null)
        {
            Address[] replyToAddr = new Address[1];
            replyToAddr[0] = new InternetAddress(replyTo);
            message.setReplyTo(replyToAddr);
        }

        Transport.send(message);
    }
}
