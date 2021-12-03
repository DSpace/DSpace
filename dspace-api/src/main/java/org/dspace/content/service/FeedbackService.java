/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.content.service;
import java.io.IOException;
import javax.mail.MessagingException;
import javax.servlet.http.HttpServletRequest;

import org.dspace.core.Context;

/**
 * Service interface class for the Feedback object.
 * 
 * @author Mykhaylo Boychuk (mykhaylo.boychuk@4science.com)
 */
public interface FeedbackService {

    public void sendEmail(Context context, HttpServletRequest request, String recipientEmail, String senderEmail,
            String message, String page) throws IOException, MessagingException;

}