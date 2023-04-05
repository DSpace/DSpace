/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.test;

import org.apache.commons.lang3.StringUtils;
import org.dspace.AbstractIntegrationTestWithDatabase;
import org.dspace.app.rest.Application;
import org.dspace.app.rest.utils.DSpaceConfigurationInitializer;
import org.dspace.app.rest.utils.DSpaceKernelInitializer;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.web.server.LocalServerPort;
import org.springframework.context.ApplicationContext;
import org.springframework.http.HttpEntity;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestExecutionListeners;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit4.SpringRunner;

/**
 * Abstract web client integration test class that will initialize the Spring Boot test environment by starting up
 * a full test webserver (on a random port).
 * <P>
 * As running a test webserver is an expensive operation, this Abstract class is only necessary to perform
 * Integration Tests on *Servlets*. If you are performing integration tests on a Spring Controller
 * (@Controller annotation), you should use AbstractControllerIntegrationTest
 * <P>
 * NOTE: The annotations on this class should be kept in sync with those on AbstractControllerIntegrationTest.
 * The ONLY differences should be in the "webEnvironment" param passed to @SpringBootTest, and the removal
 * of @WebAppConfiguration (which is only allowed in a mock environment)
 *
 * @author Tim Donohue
 * @see org.dspace.app.rest.test.AbstractControllerIntegrationTest
 */
// Run tests with JUnit 4 and Spring TestContext Framework
@RunWith(SpringRunner.class)
// Specify main class to use to load Spring ApplicationContext
// ALSO tell Spring to start a web server on a random port
// NOTE: By default, Spring caches and reuses ApplicationContext for each integration test (to speed up tests)
// See: https://docs.spring.io/spring/docs/current/spring-framework-reference/testing.html#integration-testing
@SpringBootTest(classes = Application.class, webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
// Load DSpace initializers in Spring ApplicationContext (to initialize DSpace Kernel & Configuration)
@ContextConfiguration(initializers = { DSpaceKernelInitializer.class, DSpaceConfigurationInitializer.class })
// Load our src/test/resources/application-test.properties to override some settings in default application.properties
@TestPropertySource(locations = "classpath:application-test.properties")
// Enable our custom Logging listener to log when each test method starts/stops
@TestExecutionListeners(listeners = {LoggingTestExecutionListener.class},
                        mergeMode = TestExecutionListeners.MergeMode.MERGE_WITH_DEFAULTS)
public class AbstractWebClientIntegrationTest extends AbstractIntegrationTestWithDatabase {
    // (Random) port chosen for test web server
    @LocalServerPort
    private int port;

    // RestTemplate class with access to test web server
    @Autowired
    private TestRestTemplate restTemplate;

    // Spring Application context
    @Autowired
    protected ApplicationContext applicationContext;

    /**
     * Get client TestRestTemplate for making HTTP requests to test webserver
     * @return TestRestTemplate
     */
    public TestRestTemplate getClient() {
        return restTemplate;
    }

    /**
     * Return the full URL of a request at a specific path.
     * (http://localhost:[port][path])
     * @param path Path (should start with a slash)
     * @return full URL
     */
    public String getURL(String path) {
        return "http://localhost:" + port + path;
    }

    /**
     * Perform a GET request and return response as a String
     * @param path path to perform GET against
     * @return ResponseEntity with a String body
     */
    public ResponseEntity<String> getResponseAsString(String path) {
        return getClient().getForEntity(getURL(path), String.class);
    }

    /**
     * Perform an authenticated (via Basic Auth) GET request and return response as a String
     * @param path path to perform GET against
     * @param username Username
     * @param password Password
     * @return ResponseEntity with a String body
     */
    public ResponseEntity<String> getResponseAsString(String path, String username, String password) {
        return getClient().withBasicAuth(username, password).getForEntity(getURL(path), String.class);
    }

    /**
     * Perform an authenticated (via Basic Auth) POST request and return response as a String.
     * @param path path to perform GET against
     * @param username Username (may be null to perform an unauthenticated POST)
     * @param password Password
     * @param requestEntity unknown -- not used.
     * @return ResponseEntity with a String body
     */
    public ResponseEntity<String> postResponseAsString(String path, String username, String password,
                                                       HttpEntity requestEntity) {
        // If username is not empty, perform an authenticated POST. Else attempt without AuthN
        if (StringUtils.isNotBlank(username)) {
            return getClient().withBasicAuth(username, password).postForEntity(getURL(path), requestEntity,
                                                                               String.class);
        } else {
            return getClient().postForEntity(getURL(path), requestEntity, String.class);
        }
    }
}

