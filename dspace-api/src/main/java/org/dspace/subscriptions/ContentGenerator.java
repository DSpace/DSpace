/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.subscriptions;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.apache.commons.lang.StringUtils.EMPTY;

import java.io.ByteArrayOutputStream;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

import org.apache.logging.log4j.LogManager;
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
 * in case of 'content' subscriptionType
 */
@SuppressWarnings("rawtypes")
public class ContentGenerator implements SubscriptionGenerator<IndexableObject> {

    private final Logger log = LogManager.getLogger(ContentGenerator.class);

    @SuppressWarnings("unchecked")
    private Map<String, StreamDisseminationCrosswalk> entityType2Disseminator = new HashMap();

    @Autowired
    private ItemService itemService;

    @Override
    public void notifyForSubscriptions(Context context, EPerson ePerson,
                                       List<IndexableObject> indexableComm,
                                       List<IndexableObject> indexableColl) {
        try {
            if (Objects.nonNull(ePerson)) {
                Locale supportedLocale = I18nUtil.getEPersonLocale(ePerson);
                Email email = Email.getEmail(I18nUtil.getEmailFilename(supportedLocale, "subscriptions_content"));
                email.addRecipient(ePerson.getEmail());

                String bodyCommunitites = generateBodyMail(context, indexableComm);
                String bodyCollections = generateBodyMail(context, indexableColl);
                if (bodyCommunitites.equals(EMPTY) && bodyCollections.equals(EMPTY)) {
                    log.debug("subscription(s) of eperson {} do(es) not match any new items: nothing to send - exit silently", ePerson::getID);
                    return;
                }
                email.addArgument(bodyCommunitites);
                email.addArgument(bodyCollections);
                
                email.send();
            }
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            log.warn("Cannot email user eperson_id: {} eperson_email: {}", ePerson::getID, ePerson::getEmail);
        }
    }

    private String generateBodyMail(Context context, List<IndexableObject> indexableObjects) {
        try {
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            out.write("\n".getBytes(UTF_8));
            if (indexableObjects != null && !indexableObjects.isEmpty()) {
                for (IndexableObject indexableObject : indexableObjects) {
                    out.write("\n".getBytes(UTF_8));
                    Item item = (Item) indexableObject.getIndexedObject();
                    String entityType = itemService.getEntityTypeLabel(item);
                    Optional.ofNullable(entityType2Disseminator.get(entityType))
                            .orElseGet(() -> entityType2Disseminator.get("Item"))
                            .disseminate(context, item, out);
                }
                return out.toString();
            }
        } catch (Exception e) {
            log.error(e.getMessage(), e);
        }
        return EMPTY;
    }

    public void setEntityType2Disseminator(Map<String, StreamDisseminationCrosswalk> entityType2Disseminator) {
        this.entityType2Disseminator = entityType2Disseminator;
    }

}
