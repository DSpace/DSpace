/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.test;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.setup.MockMvcBuilders.webAppContextSetup;

import java.nio.charset.StandardCharsets;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.List;
import javax.servlet.Filter;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.lang3.StringUtils;
import org.dspace.app.rest.Application;
import org.dspace.app.rest.model.patch.Operation;
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
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.test.web.servlet.MockMvc;
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
// Run tests with JUnit 4 and Spring TestContext Framework
@RunWith(SpringRunner.class)
// Specify main class to use to load Spring ApplicationContext
// NOTE: By default, Spring caches and reuses ApplicationContext for each integration test (to speed up tests)
// See: https://docs.spring.io/spring/docs/current/spring-framework-reference/testing.html#integration-testing
@SpringBootTest(classes = Application.class)
// Load DSpace initializers in Spring ApplicationContext (to initialize DSpace Kernel & Configuration)
@ContextConfiguration(initializers = { DSpaceKernelInitializer.class, DSpaceConfigurationInitializer.class })
// Tell Spring to make ApplicationContext an instance of WebApplicationContext (for web-based tests)
@WebAppConfiguration
public class AbstractControllerIntegrationTest extends AbstractIntegrationTestWithDatabase {

    protected static final String AUTHORIZATION_HEADER = "Authorization";
    protected static final String AUTHORIZATION_COOKIE = "Authorization-cookie";

    //The Authorization header contains a value like "Bearer TOKENVALUE". This constant string represents the part that
    //sits before the actual authentication token and can be used to easily compose or parse the Authorization header.
    protected static final String AUTHORIZATION_TYPE = "Bearer ";

    public static final String REST_SERVER_URL = "http://localhost/api/";
    public static final String BASE_REST_SERVER_URL = "http://localhost";

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

    public MockMvc getClient() throws SQLException {
        return getClient(null);
    }

    public MockMvc getClient(String authToken) throws SQLException {
        if (context != null && context.isValid()) {
            context.commit();
        }

        DefaultMockMvcBuilder mockMvcBuilder = webAppContextSetup(webApplicationContext)
            //Always log the response to debug
            .alwaysDo(MockMvcResultHandlers.print())
            //Add all filter implementations
            .addFilters(new ErrorPageFilter())
            .addFilters(requestFilters.toArray(new Filter[requestFilters.size()]));

        if (StringUtils.isNotBlank(authToken)) {
            mockMvcBuilder.defaultRequest(
                get("").header(AUTHORIZATION_HEADER, AUTHORIZATION_TYPE + authToken));
        }

        return mockMvcBuilder
            .build();
    }

    public MockHttpServletResponse getAuthResponse(String user, String password) throws Exception {
        return getClient().perform(post("/api/authn/login")
                                       .param("user", user)
                                       .param("password", password))
                          .andReturn().getResponse();
    }

    public String getAuthToken(String user, String password) throws Exception {
        return StringUtils.substringAfter(
            getAuthResponse(user, password).getHeader(AUTHORIZATION_HEADER),
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
}
