/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.test;

import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.setup.MockMvcBuilders.webAppContextSetup;

import java.nio.charset.StandardCharsets;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.List;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.Filter;
import jakarta.servlet.http.Cookie;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.Logger;
import org.dspace.AbstractIntegrationTestWithDatabase;
import org.dspace.app.TestApplication;
import org.dspace.app.rest.model.patch.Operation;
import org.dspace.app.rest.security.DSpaceCsrfTokenRepository;
import org.dspace.app.rest.utils.DSpaceConfigurationInitializer;
import org.dspace.app.rest.utils.DSpaceKernelInitializer;
import org.junit.Assert;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.web.servlet.support.ErrorPageFilter;
import org.springframework.data.rest.webmvc.RestMediaTypes;
import org.springframework.hateoas.MediaTypes;
import org.springframework.http.MediaType;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestExecutionListeners;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.springframework.test.web.servlet.result.MockMvcResultHandlers;
import org.springframework.test.web.servlet.setup.DefaultMockMvcBuilder;
import org.springframework.web.context.WebApplicationContext;

/**
 * Abstract integration test class that will take care of setting up the Spring Boot environment to run
 * integration tests against @Controller classes (Spring Controllers).
 * <P>
 * This Abstract class uses Spring Boot's default mock environment testing scheme, which relies on MockMvc to "mock"
 * a webserver and call Spring Controllers directly. This avoids the cost of starting a webserver.
 * <P>
 * If you need to test a Servlet (or something not a Spring Controller), you will NOT be able to use this class.
 * Instead, please use the AbstractWebClientIntegrationTest in this same package.
 *
 * @author Tom Desair
 * @author Tim Donohue
 * @see org.dspace.app.rest.test.AbstractWebClientIntegrationTest
 */
// Run tests with JUnit and Spring TestContext Framework
@RunWith(SpringRunner.class)
// Specify main class to use to load Spring ApplicationContext
// NOTE: By default, Spring caches and reuses ApplicationContext for each integration test (to speed up tests)
// See: https://docs.spring.io/spring/docs/current/spring-framework-reference/testing.html#integration-testing
@SpringBootTest(classes = TestApplication.class)
// Load DSpace initializers in Spring ApplicationContext (to initialize DSpace Kernel & Configuration)
@ContextConfiguration(initializers = { DSpaceKernelInitializer.class, DSpaceConfigurationInitializer.class })
// Tell Spring to make ApplicationContext an instance of WebApplicationContext (for web-based tests)
@WebAppConfiguration
// Load our src/test/resources/application-test.properties to override some settings in default application.properties
@TestPropertySource(locations = "classpath:application-test.properties")
// Enable our custom Logging listener to log when each test method starts/stops
@TestExecutionListeners(listeners = {LoggingTestExecutionListener.class},
    mergeMode = TestExecutionListeners.MergeMode.MERGE_WITH_DEFAULTS)
public class AbstractControllerIntegrationTest extends AbstractIntegrationTestWithDatabase {

    private static final Logger log = org.apache.logging.log4j.LogManager.getLogger();

    protected static final String AUTHORIZATION_HEADER = "Authorization";
    protected static final String AUTHORIZATION_COOKIE = "Authorization-cookie";

    //The Authorization header contains a value like "Bearer TOKENVALUE". This constant string represents the part that
    //sits before the actual authentication token and can be used to easily compose or parse the Authorization header.
    protected static final String AUTHORIZATION_TYPE = "Bearer ";

    public static final String REST_SERVER_URL = "http://localhost/api/";
    public static final String BASE_REST_SERVER_URL = "http://localhost";

    // Our standard/expected content type
    protected MediaType contentType = new MediaType(MediaTypes.HAL_JSON.getType(),
                                                    MediaTypes.HAL_JSON.getSubtype(), StandardCharsets.UTF_8);

    protected MediaType textUriContentType = RestMediaTypes.TEXT_URI_LIST;

    protected HttpMessageConverter mappingJackson2HttpMessageConverter;

    @Autowired
    private WebApplicationContext webApplicationContext;

    @Autowired
    private List<Filter> requestFilters;

    @Autowired
    void setConverters(HttpMessageConverter<?>[] converters) {

        this.mappingJackson2HttpMessageConverter = Arrays.asList(converters).stream().filter(
            hmc -> hmc instanceof MappingJackson2HttpMessageConverter).findAny().get();

        Assert.assertNotNull("the JSON message converter must not be null",
                             this.mappingJackson2HttpMessageConverter);
    }

    /**
     * Create a test web client without an authorization token (an anonymous
     * session).
     *
     * @return the test client.
     * @throws SQLException passed through.
     */
    public MockMvc getClient() throws SQLException {
        return getClient(null);
    }

    /**
     * Create a test web client which uses a given authorization token.
     *
     * @param authToken a suitable Bearer token.
     * @return the test client.
     * @throws SQLException passed through.
     */
    public MockMvc getClient(String authToken) throws SQLException {
        if (context != null && context.isValid()) {
            context.commit();
        }

        DefaultMockMvcBuilder mockMvcBuilder = setupDefaultMockMvcBuilder(true);

        // Default to performing a GET request to the root path
        MockHttpServletRequestBuilder defaultRequest = get("/");

        // If an authToken was passed in, also make sure request sends the authToken in the "Authorization" header
        if (StringUtils.isNotBlank(authToken)) {
            defaultRequest.header(AUTHORIZATION_HEADER, AUTHORIZATION_TYPE + authToken);
        }

        // Make sure all MockMvc requests (in all tests) include a valid CSRF token by default.
        defaultRequest.with(validCsrfToken());

        return mockMvcBuilder.defaultRequest(defaultRequest).build();
    }

    public MockHttpServletResponse getAuthResponse(String user, String password) throws Exception {
        return getClient().perform(post("/api/authn/login")
                                       .param("user", user)
                                       .param("password", password))
                          .andReturn().getResponse();
    }

    public MockHttpServletResponse getAuthResponseWithXForwardedForHeader(String user, String password,
                                                                          String xForwardedFor) throws Exception {
        return getClient().perform(post("/api/authn/login")
                                       .param("user", user)
                                       .param("password", password)
                                       .header("X-Forwarded-For", xForwardedFor))
                          .andReturn().getResponse();
    }


    public String getAuthToken(String user, String password) throws Exception {
        return StringUtils.substringAfter(
            getAuthResponse(user, password).getHeader(AUTHORIZATION_HEADER),
            AUTHORIZATION_TYPE);
    }

    public String getAuthTokenWithXForwardedForHeader(String user, String password, String xForwardedFor)
        throws Exception {
        return StringUtils.substringAfter(
            getAuthResponseWithXForwardedForHeader(user, password, xForwardedFor).getHeader(AUTHORIZATION_HEADER),
            AUTHORIZATION_TYPE);
    }

    public String getPatchContent(List<Operation> ops) {
        ObjectMapper objectMapper = new ObjectMapper();
        try {
            return objectMapper.writeValueAsString(ops);
        } catch (JsonProcessingException e) {
            e.printStackTrace();
        }
        return null;
    }

    public static RequestPostProcessor ip(final String ipAddress) {
        return request -> {
            request.setRemoteAddr(ipAddress);
            return request;
        };
    }

    /**
     * Adds a *valid* CSRF token to the current mock request. This is useful for tests
     * that need to verify behavior when an valid CSRF token is sent.
     * Usage: .with(validCsrfToken())
     * <P>
     * TODO: Ideally this would be replaced by Spring Security's "csrf().asHeader()".  But, at this time
     * that post processor doesn't fully support CookieCsrfTokenRepository's (which is what DSpace uses)
     * See https://stackoverflow.com/a/77368421 and https://github.com/spring-projects/spring-security/issues/12774
     * @return RequestPostProcessor with invalid token added
     */
    public RequestPostProcessor validCsrfToken() {
        return request -> {
            // Obtain the current CSRF token cookie from the (mock) backend via GET request
            // TODO: This method may be expensive for ITs as it GETs a new token for every request. We may want to
            // investigate if caching the CSRF token would work without causing random test failures.
            Cookie csrfCookie = getCsrfTokenCookie();

            if (csrfCookie != null) {
                // Get any currently set cookies & append CSRF cookie to list
                Cookie[] cookies = request.getCookies();

                // To the current request, add the obtained CSRF cookie and matching CSRF header
                request.setCookies(ArrayUtils.add(cookies, csrfCookie));
                request.addHeader(DSpaceCsrfTokenRepository.DEFAULT_CSRF_HEADER_NAME, csrfCookie.getValue());
                return request;
            } else {
                log.warn("Could not obtain CSRFToken to add it to the current mock request");
                return request;
            }
        };
    }

    /**
     * Adds a *valid* CSRF token to the current mock request via a request parameter. This is useful for tests
     * that need to verify behavior when an valid CSRF token is sent via a request param (e.g. ?_csrf=[token]).
     * Usage: .with(validCsrfTokenViaParam())
     * <P>
     * This method is identical to validCsrfToken() except it sends the CSRF token as a request parameter instead of
     * an HTTP Header.
     * <P>
     * TODO: Ideally this would be replaced by Spring Security's "csrf()".  But, at this time
     * that post processor doesn't fully support CookieCsrfTokenRepository's (which is what DSpace uses)
     * See https://stackoverflow.com/a/77368421 and https://github.com/spring-projects/spring-security/issues/12774
     * @return RequestPostProcessor with invalid token added
     */
    public RequestPostProcessor validCsrfTokenViaParam() {
        return request -> {
            // Obtain the current CSRF token cookie from the (mock) backend via GET request
            // TODO: This method may be expensive for ITs as it GETs a new token for every request. We may want to
            // investigate if caching the CSRF token would work without causing random test failures.
            Cookie csrfCookie = getCsrfTokenCookie();

            if (csrfCookie != null) {
                // Get any currently set cookies & append CSRF cookie to list
                Cookie[] cookies = request.getCookies();

                // If an CSRF cookie already exists in this list of cookies, remove it. We'll replace it below.
                // (This is necessary because our tests all default to calling validCsrfToken())
                cookies = Arrays.stream(cookies)
                                .filter(c -> !c.getName()
                                               .equalsIgnoreCase(
                                                   DSpaceCsrfTokenRepository.DEFAULT_CSRF_COOKIE_NAME))
                                .toArray(Cookie[]::new);

                // Also remove CSRF header if it is already set. We'll send the CSRF token via a request parameter
                request.removeHeader(DSpaceCsrfTokenRepository.DEFAULT_CSRF_HEADER_NAME);

                // To the current request, add the obtained CSRF cookie and matching CSRF parameter
                request.setCookies(ArrayUtils.add(cookies, csrfCookie));
                request.addParameter(DSpaceCsrfTokenRepository.DEFAULT_CSRF_PARAMETER_NAME, csrfCookie.getValue());
                return request;
            } else {
                log.warn("Could not obtain CSRFToken to add it to the current mock request");
                return request;
            }
        };
    }

    /**
     * Adds an invalid CSRF token to the current mock request. This is useful for tests which need to verify behavior
     * when an invalid CSRF token is sent.
     * <P>
     * Usage: .with(invalidCsrfToken())
     * @return RequestPostProcessor with invalid token added
     */
    public RequestPostProcessor invalidCsrfToken() {
        return request -> {
            // Get any currently set request Cookies
            Cookie[] cookies = request.getCookies();

            // If an CSRF cookie already exists in this list of cookies, remove it. We'll replace it below.
            // (This is necessary because our tests all default to sending a *valid* CSRF token)
            cookies = Arrays.stream(cookies)
                                       .filter(c -> !c.getName()
                                                      .equalsIgnoreCase(
                                                          DSpaceCsrfTokenRepository.DEFAULT_CSRF_COOKIE_NAME))
                                       .toArray(Cookie[]::new);

            // Also remove CSRF header if it is already set. We'll replace it below.
            request.removeHeader(DSpaceCsrfTokenRepository.DEFAULT_CSRF_HEADER_NAME);

            // To the current mock request, add a fake CSRF cookie and header that do NOT match.
            request.setCookies(ArrayUtils.add(cookies,
                                              new Cookie(DSpaceCsrfTokenRepository.DEFAULT_CSRF_COOKIE_NAME,
                                                         "fake-csrf-token")));
            request.addHeader(DSpaceCsrfTokenRepository.DEFAULT_CSRF_HEADER_NAME, "not-a-valid-csrf-token");
            return request;
        };
    }


    /**
     * Setup our DefaultMockMvcBuilder for DSpace.  This is centralized in a private method as it's used in
     * multiple places in this class.
     * @param enableLogging whether to default to logging the MockMvc request or not.
     * @return DefaultMockMvcBuilder
     */
    private DefaultMockMvcBuilder setupDefaultMockMvcBuilder(boolean enableLogging) {
        DefaultMockMvcBuilder defaultMockMvcBuilder = webAppContextSetup(webApplicationContext)
            // Add all filter implementations
            .addFilters(new ErrorPageFilter())
            .addFilters(requestFilters.toArray(new Filter[requestFilters.size()]))
            // Enable/Integrate Spring Security with MockMVC
            .apply(springSecurity());

        if (enableLogging) {
            // Always log the MockMvc request/response, to allow for easier debugging.
            return defaultMockMvcBuilder.alwaysDo(MockMvcResultHandlers.print());
        } else {
            return defaultMockMvcBuilder;
        }
    }


    /**
     * Return the Cookie set by Spring Security which contains the CSRF token.
     * The value() of the cookie is the CSRF Token.
     * @return Cookie with CSRF token
     * @throws Exception
     */
    public Cookie getCsrfTokenCookie() {
        try {
            // Set up a non-logging MockMvc builder to obtain the CSRF cookie
            // This call is not logged by default to avoid cluttering logs of ITs, but you can switch this to "true"
            // if you wish it to be logged.
            MockMvc mockMvc = setupDefaultMockMvcBuilder(false).build();

            // Perform a GET request to our CSRF endpoint to obtain the current CSRF token
            MvcResult mvcResult = mockMvc.perform(get("/api/security/csrf")).andReturn();

            // Read and return the Cookie which contains the CSRF token for DSpace
            return mvcResult.getResponse().getCookie(DSpaceCsrfTokenRepository.DEFAULT_CSRF_COOKIE_NAME);
        } catch (Exception e) {
            log.error("Could not obtain the CSRF token cookie for Integration Tests", e);
        }
        return null;
    }
}
