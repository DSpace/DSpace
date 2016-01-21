/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.xoai.tests.integration.xoai;

import com.lyncode.builder.MapBuilder;
import com.lyncode.xoai.dataprovider.services.api.ResourceResolver;
import com.lyncode.xoai.dataprovider.services.impl.BaseDateProvider;
import com.lyncode.xoai.dataprovider.xml.xoaiconfig.Configuration;
import com.lyncode.xoai.dataprovider.xml.xoaiconfig.FormatConfiguration;
import org.apache.solr.client.solrj.SolrServer;
import org.dspace.xoai.controller.DSpaceOAIDataProvider;
import org.dspace.xoai.services.api.config.ConfigurationService;
import org.dspace.xoai.services.api.config.XOAIManagerResolver;
import org.dspace.xoai.services.api.EarliestDateResolver;
import org.dspace.xoai.services.api.FieldResolver;
import org.dspace.xoai.tests.DSpaceTestConfiguration;
import org.dspace.xoai.tests.helpers.stubs.*;
import org.junit.After;
import org.junit.Before;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultMatcher;
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;
import org.springframework.test.web.servlet.result.XpathResultMatchers;
import org.springframework.web.context.WebApplicationContext;

import javax.xml.xpath.XPathExpressionException;
import java.util.Date;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.springframework.test.web.servlet.setup.MockMvcBuilders.webAppContextSetup;


@RunWith(SpringJUnit4ClassRunner.class)
@WebAppConfiguration
@ContextConfiguration(classes = { DSpaceTestConfiguration.class, DSpaceOAIDataProvider.class })
public abstract class AbstractDSpaceTest {
    private static BaseDateProvider baseDateProvider = new BaseDateProvider();
    @Autowired WebApplicationContext wac;
    private MockMvc mockMvc;

    private StubbedXOAIManagerResolver xoaiManagerResolver;
    private StubbedConfigurationService configurationService;
    private StubbedFieldResolver databaseService;
    private StubbedEarliestDateResolver earliestDateResolver;
    private StubbedSetRepository setRepository;
    private StubbedResourceResolver resourceResolver;
    private SolrServer solrServer;

    @Before
    public void setup() {
        xoaiManagerResolver = (StubbedXOAIManagerResolver) this.wac.getBean(XOAIManagerResolver.class);
        configurationService = (StubbedConfigurationService) this.wac.getBean(ConfigurationService.class);
        databaseService = (StubbedFieldResolver) this.wac.getBean(FieldResolver.class);
        earliestDateResolver = (StubbedEarliestDateResolver) this.wac.getBean(EarliestDateResolver.class);
        setRepository = this.wac.getBean(StubbedSetRepository.class);
        setRepository.clear();
//        solrServer = this.wac.getBean(SolrServer.class);
        resourceResolver = (StubbedResourceResolver) this.wac.getBean(ResourceResolver.class);
        xoaiManagerResolver.configuration();
    }
    
    @After
    public void teardown() {
        // Nullify all resources so that JUnit will clean them up
        xoaiManagerResolver = null;
        configurationService = null;
        databaseService = null;
        earliestDateResolver = null;
        setRepository = null;
        resourceResolver = null;
        mockMvc = null;
        solrServer = null;
    }

    protected MockMvc againstTheDataProvider() {
        if (this.mockMvc == null) this.mockMvc = webAppContextSetup(this.wac).build();
        return this.mockMvc;
    }

    protected Configuration theConfiguration () {
        return xoaiManagerResolver.configuration();
    }

    protected StubbedConfigurationService theDSpaceConfiguration () {
        return configurationService;
    }

    protected StubbedFieldResolver theDatabase () {
        return databaseService;
    }

    protected StubbedEarliestDateResolver theEarlistEarliestDate () {
        return earliestDateResolver;
    }

    protected XpathResultMatchers oaiXPath(String xpath) throws XPathExpressionException {
        return MockMvcResultMatchers.xpath(this.replaceXpath(xpath), new MapBuilder<String, String>()
                .withPair("o", "http://www.openarchives.org/OAI/2.0/")
                .build());
    }

    private String replaceXpath(String xpath) {
        int offset = 0;
        String newXpath = "";
        Pattern pattern = Pattern.compile("/[^/]+");
        Matcher matcher = pattern.matcher(xpath);
        while (matcher.find()) {
            if (matcher.start() > offset) newXpath += xpath.substring(offset, matcher.start());
            if (!matcher.group().contains(":") && !matcher.group().startsWith("/@"))
                newXpath += "/o:" + matcher.group().substring(1);
            else
                newXpath += matcher.group();
            offset = matcher.end() + 1;
        }

        return newXpath;
    }

    protected String representationOfDate (Date date) {
        return baseDateProvider.format(date);
    }

    protected StubbedSetRepository theSetRepository () {
        return setRepository;
    }

    protected com.lyncode.xoai.dataprovider.xml.xoaiconfig.ContextConfiguration aContext(String baseUrl) {
        return new com.lyncode.xoai.dataprovider.xml.xoaiconfig.ContextConfiguration(baseUrl);
    }

    protected XpathResultMatchers responseDate() throws XPathExpressionException {
        return oaiXPath("/OAI-PMH/responseDate");
    }

    protected ResultMatcher verb(org.hamcrest.Matcher<? super String> matcher) throws XPathExpressionException {
        return oaiXPath("/OAI-PMH/request/@verb").string(matcher);
    }

    protected XpathResultMatchers resumptionToken() throws XPathExpressionException {
        return oaiXPath("//resumptionToken");
    }

    protected StubbedResourceResolver theResourseResolver() {
        return resourceResolver;
    }

    protected FormatConfiguration aFormat(String id) {
        return new FormatConfiguration(id);
    }

    protected ItemRepositoryBuilder.DSpaceItemBuilder anItem() {
        return new ItemRepositoryBuilder.DSpaceItemBuilder();
    }

    private ItemRepositoryBuilder itemRepositoryBuilder;

    public ItemRepositoryBuilder theItemRepository() {
        if (itemRepositoryBuilder == null)
            itemRepositoryBuilder = new ItemRepositoryBuilder(solrServer);
        return itemRepositoryBuilder;
    }
}