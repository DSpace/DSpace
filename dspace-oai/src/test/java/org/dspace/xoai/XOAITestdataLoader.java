package org.dspace.xoai;


import com.google.common.base.Predicate;
import com.google.common.collect.Iterables;
import com.lyncode.xoai.dataprovider.core.XOAIContext;
import com.lyncode.xoai.dataprovider.data.internal.ItemHelper;
import com.lyncode.xoai.dataprovider.data.internal.MetadataFormat;
import com.lyncode.xoai.dataprovider.data.internal.MetadataTransformer;
import com.lyncode.xoai.dataprovider.filter.FilterManager;
import com.lyncode.xoai.dataprovider.sets.StaticSet;
import com.lyncode.xoai.dataprovider.transform.XSLTransformer;
import com.lyncode.xoai.dataprovider.xml.xoaiconfig.Configuration;
import com.lyncode.xoai.dataprovider.xml.xoaiconfig.ContextConfiguration;
import com.lyncode.xoai.dataprovider.xml.xoaiconfig.SetConfiguration;
import org.apache.solr.common.SolrDocument;
import org.dspace.xoai.data.DSpaceItem;
import org.dspace.xoai.data.DSpaceSolrItem;
import org.dspace.xoai.services.api.context.ContextService;
import org.dspace.xoai.services.api.xoai.DSpaceFilterResolver;
import org.dspace.xoai.services.impl.xoai.BaseDSpaceFilterResolver;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import javax.annotation.Nullable;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.stream.StreamSource;
import java.io.*;
import java.lang.reflect.Field;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class XOAITestdataLoader {

    private static final String oaiDcXSLT = "../dspace/config/crosswalks/oai/metadataFormats/oai_dc.xsl";
    private static final String xoaiConfiguration = "../dspace/config/crosswalks/oai/xoai.xml";
    private static final Map<TransformerType, String> metadataTransformer;

    static {
        metadataTransformer = new HashMap<>();
        metadataTransformer.put(TransformerType.DRIVER, "../dspace/config/crosswalks/oai/transformers/driver.xsl");
        metadataTransformer.put(TransformerType.OPENAIRE, "../dspace/config/crosswalks/oai/transformers/openaire30.xsl");
    }

    enum TransformerType {
        REQUEST("request", "defaultFilter", null), DRIVER("driver", "driverFilter", "driverSet"), OPENAIRE("openaire", "openAireFilter", "openaireSet");

        private final String transformer;
        private final String filter;
        private final String set;

        TransformerType(String transformer, String filter, String set) {
            this.transformer = transformer;
            this.filter = filter;
            this.set = set;
        }

        public String getTransformer() {
            return transformer;
        }

        public String getFilter() {
            return filter;
        }

        public String getSet() {
            return set;
        }
    }

    static Map<String, List<String>> loadMetadata(String resource, TransformerType transformerType) throws Exception {
        Path resourcePath = Paths.get(XOAITestdataLoader.class.getResource(resource).getPath());
        byte[] bytes = Files.readAllBytes(resourcePath);
        String metadata = new String(bytes, StandardCharsets.UTF_8);

        SolrDocument mockSolrDocument = mock(SolrDocument.class);
        when(mockSolrDocument.getFieldValue("item.compile")).thenReturn(metadata);
        when(mockSolrDocument.getFieldValues(anyString())).thenReturn(Collections.emptyList());
        when(mockSolrDocument.getFieldValue("item.deleted")).thenReturn(false);

        DSpaceItem item = new DSpaceSolrItem(mockSolrDocument);
        ItemHelper itemHelper = new ItemHelper(item);

        Map<String, List<String>> parsed;

        try (InputStream is = loadXslTransformer(itemHelper, transformerType)) {
            Document dom = createDom(is);
            parsed = parseResult(dom);
        }
        return parsed;
    }

    static void assertMetadataFieldsSize(int actual, int expected) {
        String reason = String.format("We expect %d metadata fields. " +
                "Have you been a naughty developer and modified the xslt transforms without proper testing first?:)", expected);
        assertThat(reason, actual, is(expected));
    }

    static XOAIContext oaiContext(TransformerType transformerType) throws Exception {
        Configuration xoaiConfiguration = loadXoaiConfiguration();
        ContextConfiguration contextConfiguration = configurationContexts(xoaiConfiguration).get(transformerType.getTransformer());
        StaticSet staticSet = staticSet(xoaiConfiguration, transformerType);
        FilterManager filterManager = filterManager(xoaiConfiguration);

        XOAIContext xoaiContext = new XOAIContext(contextConfiguration.getBaseUrl(),
                contextConfiguration.getName(),
                contextConfiguration.getDescription(),
                loadMetadataTransformer(metadataTransformer.get(transformerType)),
                Collections.singletonList(loadOaiDcMetadataFormat()),
                Collections.singletonList(staticSet));
        xoaiContext.setCondition(filterManager.getFilter(transformerType.getFilter()));

        return xoaiContext;
    }

    static DSpaceFilterResolver filterResolver() throws Exception {
        BaseDSpaceFilterResolver baseDSpaceFilterResolver = new BaseDSpaceFilterResolver();

        ContextService contextServiceMock = mock(ContextService.class);
        when(contextServiceMock.getContext()).thenReturn(null);

        Field contextService = baseDSpaceFilterResolver.getClass().getDeclaredField("contextService");
        contextService.setAccessible(true);
        contextService.set(baseDSpaceFilterResolver, contextServiceMock);

        return baseDSpaceFilterResolver;
    }

    private static InputStream loadXslTransformer(ItemHelper itemHelper, TransformerType transformerType) throws Exception {
        InputStream is;
        if (metadataTransformer.containsKey(transformerType)) {
            is = itemHelper.toPipeline(true)
                    .apply(loadMetadataTransformer(metadataTransformer.get(transformerType)).getXslTransformer().getValue())
                    .apply(loadOaiDcMetadataFormat().getTransformer())
                    .getTransformed();
        } else {
            is = itemHelper.toPipeline(true)
                    .apply(loadOaiDcMetadataFormat().getTransformer())
                    .getTransformed();
        }
        return is;
    }

    private static MetadataTransformer loadMetadataTransformer(String xsl) throws Exception {
        return new MetadataTransformer(new XSLTransformer(loadTransformer(xsl)));
    }

    private static MetadataFormat loadOaiDcMetadataFormat() throws Exception {
        return new MetadataFormat("oai_dc", loadTransformer(oaiDcXSLT), "http://www.openarchives.org/OAI/2.0/oai_dc/", "http://www.openarchives.org/OAI/2.0/oai_dc.xsd");
    }

    private static Transformer loadTransformer(String xsl) throws Exception {
        Transformer resource;
        try (InputStreamReader isr = new InputStreamReader(new FileInputStream(xsl), StandardCharsets.UTF_8);
             BufferedReader br = new BufferedReader(isr)) {
            TransformerFactory factory = TransformerFactory.newInstance();
            resource = factory.newTransformer(new StreamSource(br));
        }
        return resource;
    }

    private static Configuration loadXoaiConfiguration() throws Exception {
        Configuration configuration;
        try (InputStream is = new FileInputStream(xoaiConfiguration)){
            configuration = Configuration.readConfiguration(is);
        }
        return configuration;
    }

    private static Map<String, ContextConfiguration> configurationContexts(Configuration xoaiConfiguration) {
        Map<String, ContextConfiguration> configurationContexts = new HashMap<>();
        for (ContextConfiguration contextConfiguration : xoaiConfiguration.getContexts()) {
            configurationContexts.put(contextConfiguration.getBaseUrl(), contextConfiguration);
        }
        return configurationContexts;
    }

    private static StaticSet staticSet(Configuration xoaiConfiguration, final TransformerType transformerType) {
        SetConfiguration setConfiguration = Iterables.find(xoaiConfiguration.getSets(), new Predicate<SetConfiguration>() {
            @Override
            public boolean apply(@Nullable SetConfiguration setConfiguration) {
                return setConfiguration != null && setConfiguration.getId().equalsIgnoreCase(transformerType.getSet());
            }
        });
        return new StaticSet(setConfiguration.getSpec(), setConfiguration.getName());
    }

    private static FilterManager filterManager(Configuration xoaiConfiguration) throws Exception {
        return new FilterManager(filterResolver(), xoaiConfiguration.getFilters(), xoaiConfiguration.getConditions());
    }

    private static Map<String, List<String>> parseResult(Document dom) {
        Map<String, List<String>> data = new HashMap<>();
        NodeList nList = dom.getDocumentElement().getChildNodes();
        for (int i = 0; i < nList.getLength(); i++) {
            Node node = nList.item(i);
            if (node.getNodeType() == Node.ELEMENT_NODE) {
                if (!data.containsKey(node.getNodeName())) {
                    data.put(node.getNodeName(), new ArrayList<String>());
                }
                data.get(node.getNodeName()).add(node.getTextContent());
            }
        }
        return data;
    }

    private static Document createDom(InputStream is) throws ParserConfigurationException, IOException, SAXException {
        DocumentBuilderFactory builderFactory = DocumentBuilderFactory.newInstance();
        DocumentBuilder builder = builderFactory.newDocumentBuilder();
        return builder.parse(is);
    }
}
