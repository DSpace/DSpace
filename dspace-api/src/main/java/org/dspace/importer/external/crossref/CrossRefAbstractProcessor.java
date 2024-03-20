/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.importer.external.crossref;

import java.io.IOException;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.Collection;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.dspace.importer.external.metadatamapping.contributor.JsonPathMetadataProcessor;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

public class CrossRefAbstractProcessor implements JsonPathMetadataProcessor {

    private final static Logger log = LogManager.getLogger();

    private String path;

    @Override
    public Collection<String> processMetadata(String json) {
        JsonNode rootNode = convertStringJsonToJsonNode(json);
        JsonNode abstractNode = rootNode.at(path);
        Collection<String> values = new ArrayList<>();
        if (!abstractNode.isMissingNode()) {
            String abstractValue = abstractNode.textValue();
            if (StringUtils.isNotEmpty(abstractValue)) {
                abstractValue = prettifyAbstract(abstractValue);
                if (abstractValue != null) {
                    values.add(abstractValue);
                }
            }
        }
        return values;
    }

    /**
     * remove JATS markup from abstract
     *
     * @param abstractValue abstract with JATS markup
     * @return abstract without JATS markup
     */
    private String prettifyAbstract(String abstractValue) {
        if (!abstractValue.contains("<jats:")) {
            // no cleanup required
            return abstractValue;
        }

        String xmlString = "<root>" + abstractValue + "</root>";
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        Document xmlDoc;
        try {
            DocumentBuilder builder = factory.newDocumentBuilder();
            InputSource is = new InputSource(new StringReader(xmlString));
            xmlDoc = builder.parse(is);
        } catch (SAXException | IOException | ParserConfigurationException e) {
            log.warn("unable to parse XML markup in CrossRef abstract field: " + e.getMessage());
            return null;
        }

        StringBuilder sb = new StringBuilder();

        NodeList rootElements = xmlDoc.getElementsByTagName("root");
        Node rootElement = rootElements.item(0);
        NodeList childElements = rootElement.getChildNodes();
        for (int i = 0; i < childElements.getLength(); i++) {
            Node childElement = childElements.item(i);
            String nodeName = childElement.getNodeName();
            if (StringUtils.equals(nodeName, "jats:title")) {
                if (! StringUtils.equals(childElement.getTextContent(), "Abstract")) {
                    sb.append(childElement.getTextContent());
                    sb.append("\n");
                }
            } else if (StringUtils.equals(nodeName, "jats:sec")) {
                NodeList secElements = childElement.getChildNodes();
                for (int j = 0; j < secElements.getLength(); j++) {
                    Node secChildElement = secElements.item(j);
                    sb.append(secChildElement.getTextContent());
                    sb.append("\n");
                }
                sb.append("\n");
            }
        }

        return sb.toString().trim();
    }

    private JsonNode convertStringJsonToJsonNode(String json) {
        ObjectMapper mapper = new ObjectMapper();
        JsonNode body = null;
        try {
            body = mapper.readTree(json);
        } catch (JsonProcessingException e) {
            log.error("Unable to process json response.", e);
        }
        return body;
    }

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }
}
