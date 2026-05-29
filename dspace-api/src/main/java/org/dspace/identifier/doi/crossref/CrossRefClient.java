/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */

package org.dspace.identifier.doi.crossref;

import java.io.IOException;
import java.io.StringReader;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.util.List;

import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.mime.MultipartEntityBuilder;
import org.dspace.identifier.doi.DOIIdentifierException;
import org.jdom2.Document;
import org.jdom2.Element;
import org.jdom2.JDOMException;
import org.jdom2.filter.Filters;
import org.jdom2.input.SAXBuilder;
import org.jdom2.output.Format;
import org.jdom2.output.XMLOutputter;
import org.jdom2.xpath.XPathExpression;
import org.jdom2.xpath.XPathFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class CrossRefClient {

    /**
     * Timeout for API responses. Defaults to 2 minutes (120,000
     * milliseconds) as processing can be very, very slow. We don’t
     * get any answer at all from this CrossRef API until its own
     * internal queue has reached the task we give it, so the amount
     * of time it takes to respond depends strongly on the number of
     * other people who are trying to register DOIs at the same time.
     * In the future it might be nice to re-engineer the CrossRef DOI
     * connector to enqueue asynchronously and wait for a response or
     * poll for success separately.
     */
    protected int TIMEOUT = 120000;

    private static final Logger LOG = LoggerFactory.getLogger(CrossRefClient.class);

    private final BaseHttpClient client;
    private final String scheme;
    private final String host;
    private final Integer port;
    private final String path;

    private final String userName;
    private final String password;

    private static final String FORM_DATA_FILE = "mdFile";

    public CrossRefClient(
            @Value("${identifier.doi.crossref.schema:https}")
            String scheme,

            @Value("${identifier.doi.crossref.host:test.crossref.org}")
            String host,

            @Value("${identifier.doi.crossref.port:#{null}}")
            Integer port,

            @Value("${identifier.doi.crossref.path:/v2/deposits}")
            String path,

            @Value("${identifier.doi.user}")
            String userName,

            @Value("${identifier.doi.password}")
            String password,

            BaseHttpClient client) {
        this.scheme = scheme;
        this.host = host;
        this.port = port;
        this.path = path;
        this.userName = userName;
        this.password = password;
        this.client = client;
    }

    protected HttpResponse sendDepositRequest(Element metadataRoot) throws DOIIdentifierException {
        Format format = Format.getCompactFormat();
        format.setEncoding("UTF-8");
        XMLOutputter xout = new XMLOutputter(format);
        String xoutString = xout.outputString(new Document(metadataRoot));
        return sendDepositRequest(xoutString);
    }

    protected HttpResponse sendDepositRequest(String metadata) throws DOIIdentifierException {

        var httppost = buildRequest();

        LOG.info("Sending the following xml: \n{}", metadata);

        var payload = MultipartEntityBuilder.create()
                .addTextBody("operation", "doMDUpload")
                .addTextBody("usr", userName)
                .addTextBody("pwd", password)
                .addBinaryBody(FORM_DATA_FILE,
                        metadata.getBytes(),
                        ContentType.create("text/xml", StandardCharsets.UTF_8),
                        "requestData.xml")
                .build();
        httppost.setEntity(payload);

        return client.sendHttpRequest(httppost, false, this::handleErrorCodes);
    }

    private HttpPost buildRequest() {
        // check https://crossref.gitlab.io/knowledge_base/docs/services/xml-deposit-synchronous-2/  for API documentation

        var uri = new URIBuilder();
        uri.setScheme(scheme).setHost(host).setPath(path);

        if (port != null) {
            uri.setPort(port);
        }

        try {
            var post = new HttpPost(uri.build());
            var requestConfig = RequestConfig.custom()
                    .setConnectionRequestTimeout(TIMEOUT).setSocketTimeout(TIMEOUT).build();
            post.setConfig(requestConfig);

            return post;
        } catch (URISyntaxException e) {
            LOG.error("The URL we constructed to deposit a new DOI"
                      + "produced a URISyntaxException. Please check the configuration parameters!");
            LOG.error("The URL was {}.", scheme + "://" + host + path);
            throw new RuntimeException("The URL we constructed to deposit a new DOI "
                                       + "produced a URISyntaxException. " +
                                       "Please check the configuration parameters!", e);
        }
    }

    private void handleErrorCodes(int statusCode, String content) throws DOIIdentifierException {
        switch (statusCode) {
            case (200): {
                try {
                    SAXBuilder builder = new SAXBuilder();
                    Document doc = builder.build(new StringReader(content));
                    XPathExpression<Object> xpath = XPathFactory.instance().compile(
                            "/doi_batch_diagnostic/record_diagnostic[@status = 'Failure']", Filters.fpassthrough(),
                            null);
                    List<Object> res = xpath.evaluate(doc);
                    if (res.size() > 0) {
                        LOG.info("Crossref failed to process the input paylod.");
                        LOG.info("The response was: {}", content);
                        throw new DOIIdentifierException("Crossref returned 200 but with a failure status",
                                                                 DOIIdentifierException.BAD_REQUEST);
                    }

                    return;
                } catch (JDOMException e) {
                    LOG.warn("Crossref returned 200 but with invalid XML: the DOI might have been registered or not");
                    LOG.warn("The response was: {}", content);
                    throw new DOIIdentifierException("The response from Crossref was not valid parseable XML",
                                                             DOIIdentifierException.BAD_ANSWER);
                } catch (IOException e) {
                    LOG.warn("Crossref returned 200 but we failed to parse the response");
                    LOG.warn("The response was: {}", content);
                    throw new DOIIdentifierException("An internal error occurred parsing the response from Crossref",
                                                             DOIIdentifierException.INTERNAL_ERROR);
                }
            }

            case (400): {
                LOG.info("Crossref did not accept the sent request.");
                LOG.info("The response was: {}", content);
                throw new DOIIdentifierException("Crossref did not accept the sent request.",
                        DOIIdentifierException.BAD_REQUEST);
            }

            // we get a 401 if we forgot to send credentials or if the username
            // and password did not match.
            case (401): {
                LOG.info("We were unable to authenticate against the Crossref server!");
                LOG.info("The response was: {}", content);
                throw new DOIIdentifierException("Cannot authenticate at the "
                                                 + "Crossref server. Please check if username "
                                                 + "and password are set correctly.",
                        DOIIdentifierException.AUTHENTICATION_ERROR);
            }

            case (403): {
                var msg = "There was some error during the processing of the submission. " +
                          "The message body will contain the doi_batch_diagnostic message.";
                LOG.info(msg);
                LOG.info("The response was: {}", content);

                throw new DOIIdentifierException(msg,
                        DOIIdentifierException.BAD_REQUEST);
            }

            // 500 is documented and signals an internal server error
            case (500): {
                LOG.warn("Caught an http status code 500 while managing DOI " + "{}. Message was: ", content);
                throw new DOIIdentifierException("Crossref API has an internal error. "
                                                 + "It is temporarily impossible to manage DOIs. "
                                                 + "Further information can be found in DSpace log file.",
                        DOIIdentifierException.INTERNAL_ERROR);
            }

            // 504 is gateway timeout and means we should increase our client-side timeout
            case (504): {
                LOG.warn("Caught an http status code 504 (gateway timeout) while managing DOI. Message was: {}",
                        content);
                throw new DOIIdentifierException("Crossref API took too long responding to our request. "
                                                 + "Increase the CROSSREF_TIMEOUT in the " +
                                                 "identifier services spring configuration. "
                                                 + "Further information can be found in DSpace log file.",
                        DOIIdentifierException.INTERNAL_ERROR);
            }

            // Catch all other http status code in case we forgot one.
            default: {
                LOG.warn("While registering we got a http status code {} and the message \"{}\".",
                        statusCode, content);
                throw new DOIIdentifierException("Unable to parse an answer from Crossref API. " +
                                                 "Please have a look into the DSpace logs.",
                        DOIIdentifierException.BAD_ANSWER);
            }
        }
    }
}
