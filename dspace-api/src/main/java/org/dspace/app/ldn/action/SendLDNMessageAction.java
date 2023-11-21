/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.ldn.action;

import static org.dspace.app.ldn.RdfMediaType.APPLICATION_JSON_LD;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.dspace.app.ldn.converter.JsonLdHttpMessageConverter;
import org.dspace.app.ldn.model.Notification;
import org.dspace.content.Item;
import org.dspace.core.Context;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;

/**
 * Action to send LDN Message
 *
 * @author Mohamed Eskander (mohamed.eskander at 4science.com)
 */
public class SendLDNMessageAction implements LDNAction {

    private static final Logger log = LogManager.getLogger(SendLDNMessageAction.class);

    private final RestTemplate restTemplate;

    public SendLDNMessageAction() {
        restTemplate = new RestTemplate();
        restTemplate.getMessageConverters().add(new JsonLdHttpMessageConverter());
    }

    @Override
    public ActionStatus execute(Context context, Notification notification, Item item) throws Exception {
        //TODO authorization with Bearer token should be supported.
        HttpHeaders headers = new HttpHeaders();
        headers.add("Content-Type", APPLICATION_JSON_LD.toString());

        HttpEntity<Notification> request = new HttpEntity<>(notification, headers);

        log.info("Announcing notification {}", request);

        ResponseEntity<String> response;

        try {
            response = restTemplate.postForEntity(
                notification.getTarget().getInbox(),
                request,
                String.class);
        } catch (Exception e) {
            log.error(e);
            return ActionStatus.ABORT;
        }

        if (response.getStatusCode() == HttpStatus.ACCEPTED ||
            response.getStatusCode() == HttpStatus.CREATED) {
            return ActionStatus.CONTINUE;
        }

        return ActionStatus.ABORT;
    }

}
