/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.ldn.action;

import static org.dspace.app.ldn.RdfMediaType.APPLICATION_JSON_LD;

import java.util.ArrayList;
import java.util.List;

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
import org.springframework.http.converter.HttpMessageConverter;
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
        List<HttpMessageConverter<?>> messageConverters = new ArrayList<HttpMessageConverter<?>>();
        messageConverters.add(new JsonLdHttpMessageConverter());
        messageConverters.addAll(restTemplate.getMessageConverters());
        restTemplate.setMessageConverters(messageConverters);
    }

    @Override
    public ActionStatus execute(Context context, Notification notification, Item item) throws Exception {
        //TODO authorization with Bearer token should be supported.

        HttpHeaders headers = new HttpHeaders();
        headers.add("Content-Type", APPLICATION_JSON_LD.toString());

        HttpEntity<Notification> request = new HttpEntity<>(notification, headers);

        log.info("Announcing notification {}", request);

        // https://notify-inbox.info/inbox/ for test

        ResponseEntity<String> response = restTemplate.postForEntity(
            notification.getTarget().getInbox(),
            request,
            String.class
        );

        if (response.getStatusCode() == HttpStatus.ACCEPTED ||
            response.getStatusCode() == HttpStatus.CREATED) {
            return ActionStatus.CONTINUE;
        }

        return ActionStatus.ABORT;
    }

}
