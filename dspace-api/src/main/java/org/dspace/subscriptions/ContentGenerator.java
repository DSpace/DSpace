/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */

package org.dspace.subscriptions;


import org.apache.logging.log4j.Logger;
import org.dspace.core.Context;
import org.dspace.core.Email;
import org.dspace.core.I18nUtil;
import org.dspace.core.LogManager;
import org.dspace.discovery.IndexableObject;
import org.dspace.eperson.EPerson;
import org.dspace.subscriptions.service.SubscriptionGenerator;

import java.util.List;
import java.util.Locale;


/**
 * Implementation class of SubscriptionGenerator
 * which will handle the logic of sending the emails
 * in case of content subscriptions
 *
 * @author Alba Aliu
 */

public class ContentGenerator implements SubscriptionGenerator<IndexableObject> {
    private final Logger log = org.apache.logging.log4j.LogManager.getLogger(ContentGenerator.class);

    @Override
    public void notifyForSubscriptions(Context c, EPerson ePerson, List<IndexableObject> dSpaceObjectListForContentType) {
        try {
            // send the notification to the user
            if (ePerson != null) {
                // Get rejector's name
                String rejector = getEPersonName(ePerson);
                Locale supportedLocale = I18nUtil.getEPersonLocale(ePerson);
                Email email = Email.getEmail(I18nUtil.getEmailFilename(supportedLocale, "submit_reject"));
                email.addRecipient(ePerson.getEmail());
                email.setContent("", "");
                email.send();
            } else {
                // DO nothing
            }
        } catch (Exception ex) {
            // log this email error
            log.warn(LogManager.getHeader(c, "notify_of_reject",
                    "cannot email user" + " eperson_id" + ePerson.getID()
                            + " eperson_email" + ePerson.getEmail()));
        }
    }

    public String getEPersonName(EPerson ePerson) {
        String submitter = ePerson.getFullName();

        submitter = submitter + "(" + ePerson.getEmail() + ")";

        return submitter;
    }
//    public ContentGenerator(LinkedHashMap<String, DSpaceObjectUpdates> updates) {
//        System.out.println(updates);
//    }
}
