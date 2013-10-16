/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */

/*
 * @(#)BaseResourceTest.java
 */
package fi.helsinki.lib.simplerest;

import org.junit.After;
import static org.junit.Assert.*;
import org.junit.Before;
import org.junit.Test;

import org.restlet.data.MediaType;
import org.restlet.data.Status;
import org.restlet.representation.StringRepresentation;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;


import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

/**
 * JUnit test.
 * <ul>
 * <li>http://www.junit.org/</li>
 * <li>http://junit.sourceforge.net/doc/faq/faq.htm</li>
 * </ul>
 * Testing the methods of <code>BaseResource</code> class.
 * @author Markos Mevorah
 * @version %I%, %G%
 * @see fi.helsinki.lib.simplerest.BaseResource
 */
public class BaseResourceTest {

    /**
     * @see fi.helsinki.lib.simplerest.BaseResource
     */
    private BaseResource baseResource;
    /**
     * @see javax.xml.parsers.DocumentBuilderFactory
     */
    private DocumentBuilderFactory factory;
    /**
     * @see javax.xml.parsers.DocumentBuilder
     */
    private DocumentBuilder builder;
    /**
     * @see org.w3c.dom.Document
     */
    private Document document;
    /**
     * @see org.w3c.dom.Element
     */
    private Element testElement;
    /**
     * @see String
     */
    private String testString;

    public BaseResourceTest() {
    }

    /**
     * JUnit method annotated with {@link org.junit.Before}.
     * Initializing the test resources.
     * @throws Exception
     */
    @Before
    public void setUp() throws Exception {
        this.baseResource = new BaseResource() {
        };
        this.factory = DocumentBuilderFactory.newInstance();
        this.builder = this.factory.newDocumentBuilder();
        this.document = this.builder.newDocument();
        this.testElement = this.document.createElement("TestForm");
        this.testString = "test";
    }

    /**
     * JUnit method annotated with {@link org.junit.After}.
     * Releasing the test resources.
     */
    @After
    public void tearDown() {
        this.baseResource = null;
        this.factory = null;
        this.builder = null;
        this.document = null;
        this.testElement = null;
        this.testString = null;
    }

    /**
     * Test of setAttribute method, of class BaseResource.
     */
    @Test(expected = NullPointerException.class)
    public void testGetAuthenticatedContext() throws Exception {
        this.baseResource.getAuthenticatedContext();
    }

    /**
     * Test of makeInputRow method, of class BaseResource.
     * @throws Exception
     */
    @Test
    public void testMakeInputRow() throws Exception {
        this.baseResource.makeInputRow(
                this.document, this.testElement, this.testString,
                this.testString, this.testString);
        NodeList testFormChildNodes = this.testElement.getChildNodes();
        assertEquals(3, testFormChildNodes.getLength());
        Element label = (Element) this.testElement.getFirstChild();
        assertEquals(Node.ELEMENT_NODE, label.getNodeType());
        assertEquals(label.getTagName(), "label");
        NamedNodeMap labelAttributes = label.getAttributes();
        assertEquals(1, labelAttributes.getLength());
        assertEquals(this.testString, label.getAttribute("for"));
        Node labelChild = label.getFirstChild();
        assertEquals(Node.TEXT_NODE, labelChild.getNodeType());
        assertEquals(this.testString, labelChild.getTextContent());
        Element input = (Element) label.getNextSibling();
        assertEquals(Node.ELEMENT_NODE, input.getNodeType());
        assertEquals(input.getTagName(), "input");
        NamedNodeMap inputAttributes = input.getAttributes();
        assertEquals(3, inputAttributes.getLength());
        assertEquals(this.testString, input.getAttribute("type"));
        assertEquals(this.testString, input.getAttribute("name"));
        assertEquals(this.testString, input.getAttribute("id"));
        Element br = (Element) input.getNextSibling();
        assertEquals(Node.ELEMENT_NODE, br.getNodeType());
        assertEquals(br.getTagName(), "br");
    }

    /**
     * Test of setAttribute method, of class BaseResource.
     * @throws Exception
     */
    @Test
    public void testSetAttribute() throws Exception {
        this.baseResource.setAttribute(this.testElement,
                                       this.testString, this.testString);
        NamedNodeMap testElementAttributes = this.testElement.getAttributes();
        assertEquals(1, testElementAttributes.getLength());
        assertEquals(this.testString,
                     this.testElement.getAttribute(this.testString));
    }

    /**
     * Test of setClass method, of class BaseResource.
     * @throws Exception
     */
    @Test
    public void testSetClass() throws Exception {
        this.baseResource.setClass(this.testElement, this.testString);
        NamedNodeMap testElementAttributes = this.testElement.getAttributes();
        assertEquals(1, testElementAttributes.getLength());
        assertEquals(this.testString, this.testElement.getAttribute("class"));
    }

    /**
     * Test of setId method, of class BaseResource.
     * @throws Exception
     */
    @Test
    public void testSetId() throws Exception {
        this.baseResource.setId(this.testElement, this.testString);
        NamedNodeMap testElementAttributes = this.testElement.getAttributes();
        assertEquals(1, testElementAttributes.getLength());
        assertEquals(this.testString, this.testElement.getAttribute("id"));
    }

    /**
     * Test of addDtDd method, of class BaseResource.
     * @throws Exception
     */
    @Test
    public void testAddDtDd() throws Exception {
        this.baseResource.addDtDd(this.document, this.testElement,
                                  this.testString, this.testString);
        NodeList testFormChildNodes = this.testElement.getChildNodes();
        assertEquals(2, testFormChildNodes.getLength());
        Element dt = (Element) this.testElement.getFirstChild();
        assertEquals(Node.ELEMENT_NODE, dt.getNodeType());
        assertEquals(dt.getTagName(), "dt");
        Node dtChild = dt.getFirstChild();
        assertEquals(Node.TEXT_NODE, dtChild.getNodeType());
        assertEquals(this.testString, dtChild.getTextContent());
        Element dd = (Element) dt.getNextSibling();
        assertEquals(Node.ELEMENT_NODE, dd.getNodeType());
        assertEquals(dd.getTagName(), "dd");
        Node ddChild = dd.getFirstChild();
        assertEquals(Node.TEXT_NODE, ddChild.getNodeType());
        assertEquals(this.testString, ddChild.getTextContent());
    }

    /**
     * Test of successOk method, of class BaseResource.
     */
    @Test
    public void testSuccessOk() {
        StringRepresentation representation =
                             this.baseResource.successOk(this.testString);
        assertEquals(MediaType.TEXT_PLAIN, representation.getMediaType());
        assertEquals("test", representation.getText());
    }

    /**
     * Test of successCreated method, of class BaseResource.
     */
    @Test
    public void testSuccessCreated() {
        StringRepresentation representation =
                             this.baseResource.successCreated(this.testString,
                                                              this.testString);
        assertEquals(MediaType.TEXT_PLAIN, representation.getMediaType());
        assertEquals(this.testString, representation.getText());
    }

    /**
     * Test of errorInternal method, of class BaseResource.
     */
    @Test
    public void testErrorInternal() {
        StringRepresentation representation =
                             this.baseResource.errorInternal(null,
                                                             this.testString);
        assertEquals(MediaType.TEXT_PLAIN, representation.getMediaType());
        assertEquals(this.testString, representation.getText());
    }

    /**
     * Test of errorNotFound method, of class BaseResource.
     */
    @Test
    public void testErrorNotFound() {
        StringRepresentation representation =
                             this.baseResource.errorNotFound(null,
                                                             this.testString);
        assertEquals(MediaType.TEXT_PLAIN, representation.getMediaType());
        assertEquals(this.testString, representation.getText());
    }

    /**
     * Test of error method, of class BaseResource.
     */
    @Test
    public void testError() {
        StringRepresentation representation =
                             this.baseResource.error(null,
                                                     this.testString,
                                                     Status.SUCCESS_OK);
        assertEquals(MediaType.TEXT_PLAIN, representation.getMediaType());
        assertEquals(this.testString, representation.getText());
    }

    /**
     * Test of baseUrl method, of class BaseResource.
     */
    @Test(expected = NullPointerException.class)
    public void testBaseUrl() {
        String actualUrl = this.baseResource.baseUrl();
    }
}
