/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.vocabulary;

import org.apache.xpath.XPathAPI;
import org.dspace.core.ConfigurationManager;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.TransformerException;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * This class represents a single controlled vocabulary node
 * It also contains references to its child nodes
 *
 * @author Kevin Van de Velde (kevin at atmire dot com)
 */
public class ControlledVocabulary {
    private String id;
    private String label;
    private String value;
    private List<ControlledVocabulary> childNodes;

    public ControlledVocabulary(String id, String label, String value, List<ControlledVocabulary> childNodes) {
        this.id = id;
        this.label = label;
        this.value = value;
        this.childNodes = childNodes;
    }

    /**
     * Load the vocabulary with the given filename, if no vocabulary is found null is returned
     * The vocabulary file will need to located in the [dspace.dir]/config/controlled-vocabulary directory.
     *
     * @param fileName the name of the vocabulary file.
     * @return a controlled vocabulary object
     * @throws IOException Should something go wrong with reading the file
     * @throws SAXException Error during xml parsing
     * @throws ParserConfigurationException Error during xml parsing
     * @throws TransformerException Error during xml parsing
     * TODO: add some caching !
     */
    public static ControlledVocabulary loadVocabulary(String fileName) throws IOException, SAXException, ParserConfigurationException, TransformerException {
        StringBuilder filePath = new StringBuilder();
        filePath.append(ConfigurationManager.getProperty("dspace.dir")).append(File.separatorChar).append("config").append(File.separatorChar).append("controlled-vocabularies").append(File.separator).append(fileName).append(".xml");

        File controlledVocFile = new File(filePath.toString());
        if(controlledVocFile.exists()){
            DocumentBuilder builder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
            Document document = builder.parse(controlledVocFile);
            return loadVocabularyNode(XPathAPI.selectSingleNode(document, "node"), "");
        }else{
            return null;
        }

    }

    /**
     * Loads a single node & all its child nodes recursively
     * @param node The current node that we need to parse
     * @param initialValue the value of parent node
     * @return a vocabulary node with all its children
     * @throws TransformerException should something go wrong with loading the xml
     */
    private static ControlledVocabulary loadVocabularyNode(Node node, String initialValue) throws TransformerException {
        Node idNode = node.getAttributes().getNamedItem("id");
        String id = null;
        if(idNode != null){
            id = idNode.getNodeValue();
        }
        Node labelNode = node.getAttributes().getNamedItem("label");
        String label = null;
        if(labelNode != null){
            label = labelNode.getNodeValue();
        }
        String value;
        if(0 < initialValue.length()){
            value = initialValue + "::" + label;
        }else{
            value = label;
        }
        NodeList subNodes = XPathAPI.selectNodeList(node, "isComposedBy/node");

        List<ControlledVocabulary> subVocabularies = new ArrayList<ControlledVocabulary>(subNodes.getLength());
        for(int i = 0; i < subNodes.getLength(); i++){
            subVocabularies.add(loadVocabularyNode(subNodes.item(i), value));
        }
        
        return new ControlledVocabulary(id, label, value, subVocabularies);
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getLabel() {
        return label;
    }

    public void setLabel(String label) {
        this.label = label;
    }

    public List<ControlledVocabulary> getChildNodes() {
        return childNodes;
    }

    public void setChildNodes(List<ControlledVocabulary> childNodes) {
        this.childNodes = childNodes;
    }

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }
}
