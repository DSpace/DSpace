/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.test;

import static org.springframework.test.web.servlet.setup.MockMvcBuilders.webAppContextSetup;

import java.sql.SQLException;
import java.util.Arrays;
import java.util.List;

import javax.servlet.Filter;

import org.apache.commons.io.Charsets;
import org.dspace.app.rest.Application;
import org.dspace.app.rest.utils.ApplicationConfig;
import org.junit.Assert;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.hateoas.MediaTypes;
import org.springframework.http.MediaType;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.TestExecutionListeners;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.context.support.DependencyInjectionTestExecutionListener;
import org.springframework.test.context.support.DirtiesContextTestExecutionListener;
import org.springframework.test.context.transaction.TransactionalTestExecutionListener;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.result.MockMvcResultHandlers;
import org.springframework.web.context.WebApplicationContext;

/**
 * Abstract controller integration test class that will take care of setting up the
 * environment to run the integration test
 */
@RunWith(SpringJUnit4ClassRunner.class)
@SpringBootTest(classes = {Application.class, ApplicationConfig.class})
@TestExecutionListeners({DependencyInjectionTestExecutionListener.class, DirtiesContextTestExecutionListener.class,
        TransactionalTestExecutionListener.class})
@DirtiesContext
@WebAppConfiguration
public class AbstractControllerIntegrationTest extends AbstractIntegrationTestWithDatabase {

    public static final String REST_SERVER_URL = "http://localhost/api/";

    protected MediaType contentType = new MediaType(MediaTypes.HAL_JSON.getType(),
            MediaTypes.HAL_JSON.getSubtype(), Charsets.UTF_8);


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
        if(context != null && context.isValid()) {
            context.commit();
        }

        return webAppContextSetup(webApplicationContext)
                //Always log the repsonse to debug
                .alwaysDo(MockMvcResultHandlers.log())
                //Add all filter implementations
                .addFilters(requestFilters.toArray(new Filter[requestFilters.size()]))
                .build();
    }

}

