/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.external;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Optional;
import java.util.regex.Pattern;

import org.apache.commons.lang.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.dspace.external.provider.orcid.xml.ExpandedSearchConverter;
import org.dspace.services.ConfigurationService;
import org.dspace.utils.DSpace;
import org.json.JSONObject;
import org.springframework.cache.annotation.Cacheable;

/**
 * A different implementation of the communication with the ORCID API.
 * The API returns no-cache headers, we use @Cacheable to cache the labels (id->name) for some time.
 * Originally the idea was to reuse the OrcidRestConnector, but in the end that just wraps apache http client.
 */
public class CachingOrcidRestConnector {
    private static final Logger log = LogManager.getLogger(CachingOrcidRestConnector.class);

    private String apiURL;
    // Access tokens are long-lived ~ 20years, don't bother with refreshing
    private volatile String _accessToken;
    private final ExpandedSearchConverter converter = new ExpandedSearchConverter();

    private static final Pattern p = Pattern.compile("^\\p{Alpha}+", Pattern.UNICODE_CHARACTER_CLASS);
    private static final String edismaxParams =  "&defType=edismax&qf=" +
            URLEncoder.encode( "family-name^4.0 credit-name^3.0 other-names^2.0 text", StandardCharsets.UTF_8);

    private final HttpClient httpClient = HttpClient
            .newBuilder()
            .connectTimeout( Duration.ofSeconds(5))
            .build();

    /*
     * We basically need to obtain the access token only once, but there is no guarantee this will succeed. The
     * failure shouldn't be fatal, so we'll try again next time.
     */
    private Optional<String> init() {
        if (_accessToken == null) {
            synchronized (CachingOrcidRestConnector.class) {
                if (_accessToken == null) {
                    log.info("Initializing Orcid connector");
                    ConfigurationService configurationService = new DSpace().getConfigurationService();
                    String clientSecret = configurationService.getProperty("orcid.application-client-secret");
                    String clientId = configurationService.getProperty("orcid.application-client-id");
                    String OAUTHUrl = configurationService.getProperty("orcid.token-url");

                    try {
                        _accessToken = getAccessToken(clientSecret, clientId, OAUTHUrl);
                    } catch (Exception e) {
                        log.error("Error during initialization of the Orcid connector", e);
                    }
                }
            }
        }
        return Optional.ofNullable(_accessToken);
    }

    /**
     * Set the URL of the ORCID API
     * @param apiURL
     */
    public void setApiURL(String apiURL) {
        this.apiURL = apiURL;
    }

    /**
     * Search the ORCID API
     *
     * The query is passed to the ORCID API as is, except when it contains just 'unicode letters'.
     * In that case, we try to be smart and turn it into edismax query with wildcard.
     *
     * @param query - the search query
     * @param start - initial offset when paging results
     * @param limit - maximum number of results to return
     * @return the results
     */
    public ExpandedSearchConverter.Results search(String query, int start, int limit) {
        String extra;
        // if query contains just 'unicode letters'; try to be smart and turn it into edismax query with wildcard
        if (p.matcher(query).matches()) {
            query += " || " + query + "*";
            extra = edismaxParams;
        } else {
            extra = "";
        }
        final String searchPath = String.format("expanded-search?q=%s&start=%s&rows=%s%s", URLEncoder.encode(query,
                StandardCharsets.UTF_8), start, limit, extra);

        return init().map(token -> {
            try (InputStream inputStream = httpGet(searchPath, token)) {
                return converter.convert(inputStream);
            } catch (IOException e) {
                log.error("Error during search", e);
                return ExpandedSearchConverter.ERROR;
            }
        }).orElse(ExpandedSearchConverter.ERROR);
    }

    /**
     * Get the label for an ORCID, ideally the name of the person.
     *
     * Null is:
     *  - either an error -> won't be cached,
     *  - or it means no result, which'd be odd provided we get here with a valid orcid -> not caching should be ok
     *
     * @param orcid the id you are looking for
     * @return the label or null in case nothing found/error
     */
    @Cacheable(cacheNames = "orcid-labels", unless = "#result == null")
    public String getLabel(String orcid) {
        log.debug("getLabel: " + orcid);
        // in theory, we could use orcid.org/v3.0/<ORCID>/personal-details, but didn't want to write another converter
        ExpandedSearchConverter.Results search = search("orcid:" + orcid, 0, 1);
        if (search.isOk() && search.numFound() > 0) {
            return search.results().get(0).label();
        }
        return null;
    }

    protected String getAccessToken(String clientSecret, String clientId, String OAUTHUrl) {
        if (StringUtils.isNotBlank(clientSecret)
                && StringUtils.isNotBlank(clientId)
                && StringUtils.isNotBlank(OAUTHUrl)) {
            String authenticationParameters =
                    String.format("client_id=%s&client_secret=%s&scope=/read-public&grant_type=client_credentials",
                            clientId, clientSecret);

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(java.net.URI.create(OAUTHUrl))
                    .POST(HttpRequest.BodyPublishers.ofString(authenticationParameters))
                    .timeout(Duration.ofSeconds(5))
                    .header("Accept", "application/json")
                    .header("Content-Type", "application/x-www-form-urlencoded")
                    .build();

            try {
                HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
                if (isSuccess(response)) {
                    JSONObject responseObject = new JSONObject(response.body());
                    return responseObject.getString("access_token");
                } else {
                    log.error("Error during initialization of the Orcid connector, status code: "
                            + response.statusCode());
                    throw new RuntimeException("Error during initialization of the Orcid connector, status code: "
                            + response.statusCode());
                }
            } catch (IOException | InterruptedException e) {
                log.error("Error during initialization of the Orcid connector", e);
                throw new RuntimeException(e);
            }
        } else {
            log.error("Missing configuration for Orcid connector");
            throw new RuntimeException("Missing configuration for Orcid connector");
        }
    }

    private InputStream httpGet(String path, String accessToken) throws IOException {
        String trimmedPath = path.replaceFirst("^/+", "").replaceFirst("/+$", "");

        String fullPath = apiURL + '/' + trimmedPath;

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(fullPath))
                .timeout(Duration.ofSeconds(5))
                .header("Content-Type", "application/vnd.orcid+xml")
                .header("Authorization", "Bearer " + accessToken)
                .build();

        try {
            HttpResponse<InputStream> response = httpClient.send(request, HttpResponse.BodyHandlers.ofInputStream());
            if (isSuccess(response)) {
                return response.body();
            } else {
                log.error("Error in rest connector for path: " + fullPath + ", status code: " + response.statusCode());
                throw new UnexpectedStatusException("Error in rest connector for path: "
                        + fullPath + ", status code: " + response.statusCode());
            }
        } catch (UnexpectedStatusException e) {
            throw e;
        } catch (IOException | InterruptedException e) {
            log.error("Error in rest connector for path: " + fullPath, e);
            throw new RuntimeException(e);
        }
    }

    private boolean isSuccess(HttpResponse<?> response) {
        return response.statusCode() >= 200 && response.statusCode() < 300;
    }

    private static class UnexpectedStatusException extends IOException {
        public UnexpectedStatusException(String message) {
            super(message);
        }
    }

    //Just for testing
    protected void forceAccessToken(String accessToken) {
        synchronized (CachingOrcidRestConnector.class) {
            this._accessToken = accessToken;
        }
    }
}
