/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */

package org.dspace.subscriptions;

import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import javax.annotation.Resource;

import org.apache.logging.log4j.Logger;
import org.dspace.content.Item;
import org.dspace.content.crosswalk.StreamDisseminationCrosswalk;
import org.dspace.content.service.ItemService;
import org.dspace.core.Context;
import org.dspace.core.Email;
import org.dspace.core.I18nUtil;
import org.dspace.discovery.IndexableObject;
import org.dspace.eperson.EPerson;
import org.dspace.subscriptions.service.SubscriptionGenerator;
import org.springframework.beans.factory.annotation.Autowired;


/**
 * Implementation class of SubscriptionGenerator
 * which will handle the logic of sending the emails
 * in case of content subscriptions
 *
 * @author Alba Aliu
 */

public class ContentGenerator implements SubscriptionGenerator<IndexableObject> {
    private final Logger log = org.apache.logging.log4j.LogManager.getLogger(ContentGenerator.class);
    @Resource(name = "entityDissemination")
    private Map<String, StreamDisseminationCrosswalk> mapEntityDisseminatorProperty = new HashMap();
    @Autowired
    private ItemService itemService;

    @Override
    public void notifyForSubscriptions(Context c, EPerson ePerson, List<IndexableObject> indexableComm,
                                       List<IndexableObject> indexableColl,
                                       List<IndexableObject> indexableItems) {
        try {
            // send the notification to the user
            if (ePerson != null) {
                Locale supportedLocale = I18nUtil.getEPersonLocale(ePerson);
                Email email = Email.getEmail(I18nUtil.getEmailFilename(supportedLocale, "subscriptions_content"));
                email.addRecipient(ePerson.getEmail());
                email.addArgument(generateHtmlBodyMail(c, indexableComm));
                email.addArgument(generateHtmlBodyMail(c, indexableColl));
                email.addArgument(generateHtmlBodyMail(c, indexableItems));
                email.send();
            }
        } catch (Exception ex) {
            // log this email error
            log.warn("cannot email user" + " eperson_id" + ePerson.getID()
                    + " eperson_email" + ePerson.getEmail());
        }
    }

    private String generateHtmlBodyMail(Context context, List<IndexableObject> indexableObjects) {
        try {
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            out.write("\n".getBytes(StandardCharsets.UTF_8));
            if (indexableObjects.size() > 0) {
                for (IndexableObject indexableObject : indexableObjects) {
                    out.write("\n".getBytes(StandardCharsets.UTF_8));
                    Item item = (Item) indexableObject.getIndexedObject();
                    mapEntityDisseminatorProperty.get(itemService.getEntityType(item)).disseminate(context, item, out);
                }
                return out.toString();
            } else {
                out.write("No items".getBytes(StandardCharsets.UTF_8));
            }
            return out.toString();
        } catch (Exception e) {
            log.error(e.getMessage());
            return null;
        }
    }
}
