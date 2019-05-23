package org.dspace.xmlworkflow.cristin;


import org.apache.commons.lang3.StringUtils;
import org.apache.log4j.Logger;
import org.dspace.core.ConfigurationManager;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import javax.annotation.Nonnull;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.*;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;

public class UpdateWorkflow {

    private static Logger log = Logger.getLogger(UpdateWorkflow.class);
    private static String path = ConfigurationManager.getProperty("dspace.dir")+File.separator+"config"+File.separator+"workflow.xml";

    private UpdateWorkflow() {
    }

    public static boolean addXmlWorkFlowNameMap(@Nonnull String path, @Nonnull String handle, @Nonnull String workflowProcess) throws IOException {
        Document dom = loadDom(path);
        boolean isXmlWorkflowProcessConfigured = isXmlWorkflowProcessConfigured(dom, workflowProcess);
        Node workflowMap = dom.getElementsByTagName("workflow-map").item(0);
        Element nameMapElement = findNameMapElementForCollection(dom, handle);

        if (!isXmlWorkflowProcessConfigured || nameMapElement != null && workflowProcess.equals(nameMapElement.getAttribute("workflow"))) {
            return false;
        }

        if (nameMapElement == null) {
            nameMapElement = dom.createElement("name-map");
            nameMapElement.setAttribute("collection", handle);
            nameMapElement.setAttribute("workflow", workflowProcess);
            workflowMap.appendChild(nameMapElement);
            log.info(String.format("Adding name-map element \n %s in %s", nodeToString(nameMapElement), path));
        } else {
            String originalNameMapElement = nodeToString(nameMapElement);
            nameMapElement.setAttribute("workflow", workflowProcess);
            log.info(
                    String.format("Changing name-map element \n %s to %s in %s",
                                    originalNameMapElement, nodeToString(nameMapElement), path)
            );
        }

        writeDomToFile(dom, new File(path));
        return true;
    }

    public static boolean addXmlWorkFlowNameMap(@Nonnull String handle, @Nonnull String workFlowProcess) throws IOException {
        return addXmlWorkFlowNameMap(path, handle, workFlowProcess);
    }

    public static boolean removeXmlWorkFlowNameMap(@Nonnull String path, @Nonnull String handle) throws IOException {
        Document dom = loadDom(path);
        Node workflowMap = dom.getElementsByTagName("workflow-map").item(0);
        Element nameMapElement = findNameMapElementForCollection(dom, handle);

        if (nameMapElement == null) {
            return false;
        }

        log.info(String.format("Removing name-map element \n %s from %s", nodeToString(nameMapElement), path));
        workflowMap.removeChild(nameMapElement);
        writeDomToFile(dom, new File(path));
        return true;
    }

    public static boolean removeXmlWorkFlowNameMap(@Nonnull String handle) throws IOException {
        return removeXmlWorkFlowNameMap(path, handle);
    }

    private static Document loadDom(String path) throws IOException {
        try (FileInputStream fis = new FileInputStream(path);
             InputStreamReader r = new InputStreamReader(fis, StandardCharsets.UTF_8)
        ){
            InputSource is = new InputSource(r);
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            DocumentBuilder builder = factory.newDocumentBuilder();
            Document dom = builder.parse(is);
            dom.getDocumentElement().normalize();
            return dom;
        } catch (ParserConfigurationException | SAXException e) {
            throw new IOException(e);
        }
    }

    private static Element findNameMapElementForCollection(Document dom, String handle) throws IOException {
        String expression = String.format("/wf-config/workflow-map/name-map[@collection='%s']", handle);
        Element nameMap = findElement(dom, expression);
        if (nameMap != null) {
            log.debug(String.format("Found name-map element matching handle: %s \n %s", handle, nodeToString(nameMap)));
        }
        return nameMap;
    }

    private static boolean isXmlWorkflowProcessConfigured(Document dom, String workflowProcess) throws IOException {
        String expression = String.format("/wf-config/workflow[@id='%s']", workflowProcess);
        Element workflow = findElement(dom, expression);
        if (workflow != null) {
            log.debug(String.format("Found workflow element matching workflowProcess: %s \n %s", workflowProcess, nodeToString(workflow)));
        }
        return workflow != null;
    }

    private static Element findElement(Document dom, String expression) throws IOException {
        XPathFactory xFactory = XPathFactory.newInstance();
        XPath xpath = xFactory.newXPath();
        Element queryElement = null;
        try {
            queryElement = (Element) xpath.evaluate(expression, dom, XPathConstants.NODE);
        } catch (XPathExpressionException e) {
            //suppress exceptions only related to input parameters in evaluate
        }
        if (queryElement != null) {
            log.debug(String.format("Found element matching expression: %s \n %s", expression, nodeToString(queryElement)));
        }
        return queryElement;
    }

    private static void writeDomToFile(Document dom, File file) throws IOException {
        if (file.exists()) {
            String backupPath = StringUtils.substringBeforeLast(file.getPath(), ".") + ".bak";
            log.info(String.format("Backing up %s to %s", file.getPath(), backupPath));
            Files.move(file.toPath(), new File(backupPath).toPath(), StandardCopyOption.REPLACE_EXISTING);
        }

        try {
            Transformer transformer = getXmlTransformer();
            DOMSource source = new DOMSource(dom);
            StringWriter sw = new StringWriter();

            transformer.transform(source, new StreamResult(sw));
            log.debug(String.format("Writing dom to file: %s \n %s", file.getPath(), sw.toString()));
            transformer.transform(source, new StreamResult(file));
        } catch (TransformerException e) {
            throw new IOException(e);
         }
    }

    private static Transformer getXmlTransformer() throws TransformerConfigurationException {
        Transformer transformer = TransformerFactory.newInstance().newTransformer();
        transformer.setOutputProperty(OutputKeys.METHOD, "xml");
        transformer.setOutputProperty(OutputKeys.ENCODING, StandardCharsets.UTF_8.name());
        transformer.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "yes");
        transformer.setOutputProperty(OutputKeys.INDENT, "yes");
        transformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "4");
        return transformer;
    }

    private static String nodeToString(Node node) throws IOException {
        try {
            StringWriter sw = new StringWriter();
            Transformer transformer = getXmlTransformer();
            transformer.transform(new DOMSource(node), new StreamResult(sw));
            return sw.toString();
        } catch (TransformerException e) {
            throw new IOException(e);
        }
    }
}