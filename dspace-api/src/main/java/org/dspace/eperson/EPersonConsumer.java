/*
 * EPersonConsumer.java
 *
 * Version: $Revision$
 *
 * Date: $Date$
 *
 * Copyright (c) 2002-2007, Hewlett-Packard Company and Massachusetts
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

package org.dspace.eperson;

import org.apache.log4j.Logger;
import org.dspace.core.*;
import org.dspace.event.Consumer;
import org.dspace.event.Event;

import javax.mail.MessagingException;
import java.util.Date;

/**
 * Class for handling updates to EPersons
 *
 * Recommended filter:  EPerson+Create
 *
 * @version $Revision$
 *
 * @author Stuart Lewis
 */
public class EPersonConsumer implements Consumer
{
    /** log4j logger */
    private static Logger log = Logger.getLogger(EPersonConsumer.class);

    /**
     * Initalise the consumer
     *
     * @throws Exception
     */
    public void initialize()
        throws Exception
    {

    }

    /**
     * Consume the event
     *
     * @param context
     * @param event
     * @throws Exception
     */
    public void consume(Context context, Event event)
        throws Exception
    {
        int st = event.getSubjectType();
        int et = event.getEventType();
        int id = event.getSubjectID();

        switch (st)
        {
            // If an EPerson is changed
            case Constants.EPERSON:
                if (et == Event.CREATE)
                {
                    // Notify of new user registration
                    String notifyRecipient = ConfigurationManager.getProperty("registration.notify");
                    if (notifyRecipient == null) {
                        notifyRecipient = "";
                    }
                    notifyRecipient = notifyRecipient.trim();

                    if(!notifyRecipient.equals(""))
                    {
                        try
                        {
                            EPerson eperson = EPerson.find(context, id);
                            Email adminEmail = ConfigurationManager.getEmail(I18nUtil.getEmailFilename(context.getCurrentLocale(), "registration_notify"));
                            adminEmail.addRecipient(notifyRecipient);

                            adminEmail.addArgument(eperson.getFirstName() + " " + eperson.getLastName()); // Name
                            adminEmail.addArgument(eperson.getEmail());
                            adminEmail.addArgument(new Date());
                                                
                            adminEmail.setReplyTo(eperson.getEmail());

                            adminEmail.send();

                            log.info(LogManager.getHeader(context, "registerion_alert", "user="
                                    + eperson.getEmail()));
                        }
                        catch (MessagingException me)
                        {
                            log.warn(LogManager.getHeader(context,
                                "error_emailing_administrator", ""), me);
                        }
                    }
                } else if (et == Event.DELETE)
                {
                    // TODO: Implement this if required
                }
                break;
            default:
                log.warn("consume() got unrecognized event: " + event.toString());
        }

    }

    /**
     * Handle the end of the event
     *
     * @param ctx
     * @throws Exception
     */
    public void end(Context ctx)
        throws Exception
    {

    }

    /**
     * Finish the event
     *
     * @param ctx
     */
    public void finish(Context ctx)
    {

    }
}