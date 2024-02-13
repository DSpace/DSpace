/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.ldn.action;

import java.net.URI;

import com.google.gson.Gson;
import org.apache.http.Header;
import org.apache.http.HttpException;
import org.apache.http.HttpHeaders;
import org.apache.http.HttpStatus;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.dspace.app.ldn.model.Notification;
import org.dspace.content.Item;
import org.dspace.core.Context;

/**
 * Action to send LDN Message
 *
 * @author Mohamed Eskander (mohamed.eskander at 4science.com)
 */
public class SendLDNMessageAction implements LDNAction {

    private static final Logger log = LogManager.getLogger(SendLDNMessageAction.class);

    private CloseableHttpClient client = null;

    public SendLDNMessageAction() {

        HttpClientBuilder builder = HttpClientBuilder.create();
        client = builder
            .disableAutomaticRetries()
            .setMaxConnTotal(5)
            .build();
    }

    @Override
    public ActionStatus execute(Context context, Notification notification, Item item) throws Exception {
        //TODO authorization with Bearer token should be supported.

        String url = notification.getTarget().getInbox();

        HttpPost httpPost = new HttpPost(url);
        httpPost.addHeader("Content-Type", "application, ld+json");
        Gson gson = new Gson();
        httpPost.setEntity(new StringEntity(gson.toJson(notification), "UTF-8"));

        try {
            //Server-side request forgery Critical check gitHub failure is a false positive,
            //because the LDN Service URL is configured by the user from DSpace
            //frontend configuration at /admin/ldn/services
            CloseableHttpResponse response = client.execute(httpPost);
            if (isSuccessful(response.getStatusLine().getStatusCode())) {
                return ActionStatus.CONTINUE;
            } else if (isRedirect(response.getStatusLine().getStatusCode())) {
                return handleRedirect(response, httpPost);
            } else {
                return ActionStatus.ABORT;
            }
        } catch (Exception e) {
            log.error(e);
            return ActionStatus.ABORT;
        }
    }

    private boolean isSuccessful(int statusCode) {
        return statusCode == HttpStatus.SC_ACCEPTED ||
            statusCode == HttpStatus.SC_CREATED;
    }

    private boolean isRedirect(int statusCode) {
        return statusCode == HttpStatus.SC_MOVED_PERMANENTLY ||
            statusCode == HttpStatus.SC_MOVED_TEMPORARILY;
    }

    private ActionStatus handleRedirect(CloseableHttpResponse oldresponse,
                                        HttpPost request) throws HttpException {

        Header[] urls = oldresponse.getHeaders(HttpHeaders.LOCATION);
        String url = urls.length > 0 && urls[0] != null ? urls[0].getValue() : null;
        if (url == null) {
            throw new HttpException("Error following redirect, unable to reach"
                + " the correct url.");
        }
        try {
            request.setURI(new URI(url));
            CloseableHttpResponse response = client.execute(request);
            if (isSuccessful(response.getStatusLine().getStatusCode())) {
                return ActionStatus.CONTINUE;
            }
        } catch (Exception e) {
            log.error("Error following redirect:", e);
        }

        return ActionStatus.ABORT;
    }
}