/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */

package org.dspace.xoai.services.impl.resources.functions;

import static org.dspace.xoai.services.impl.resources.functions.StringXSLFunction.BASE;

import java.util.Objects;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.dom.DOMSource;

import net.sf.saxon.s9api.DocumentBuilder;
import net.sf.saxon.s9api.ExtensionFunction;
import net.sf.saxon.s9api.ItemType;
import net.sf.saxon.s9api.OccurrenceIndicator;
import net.sf.saxon.s9api.Processor;
import net.sf.saxon.s9api.QName;
import net.sf.saxon.s9api.SaxonApiException;
import net.sf.saxon.s9api.SequenceType;
import net.sf.saxon.s9api.XdmAtomicValue;
import net.sf.saxon.s9api.XdmValue;
import org.bouncycastle.util.Arrays;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;


/**
 * Serves as proxy for call from XSL engine.
 * @author Marian Berger (marian.berger at dataquest.sk)
 */
public abstract class NodeListXslFunction implements ExtensionFunction {

    protected abstract String getFnName();

    protected abstract NodeList getNodeList(String param);

    @Override
    final public QName getName() {
        return new QName(BASE, getFnName());
    }

    @Override
    final public SequenceType getResultType() {
        return SequenceType.makeSequenceType(ItemType.ANY_NODE, OccurrenceIndicator.ZERO_OR_ONE);
    }

    @Override
    final public SequenceType[] getArgumentTypes() {
        return new SequenceType[]{
                SequenceType.makeSequenceType(
                        ItemType.STRING, OccurrenceIndicator.ONE)};
    }

    @Override
    final public XdmValue call(XdmValue[] xdmValues) throws SaxonApiException {
        if (Objects.isNull(xdmValues) || Arrays.isNullOrContainsNull(xdmValues)) {
            return new XdmAtomicValue("");
        }
        NodeList nodeList = getNodeList(xdmValues[0].itemAt(0).getStringValue());
        DocumentBuilder db = new Processor(false).newDocumentBuilder();
        if (Objects.isNull(nodeList)) {
            try {
                nodeList = DocumentBuilderFactory.newInstance().newDocumentBuilder().newDocument().getChildNodes();
            } catch (ParserConfigurationException e) {
                e.printStackTrace();
                return null;
            }
        }
        Node parent = null;
        try {
            parent = DocumentBuilderFactory.newInstance().newDocumentBuilder().newDocument();
            for (int i = 0; i < nodeList.getLength(); i++) {
                parent.appendChild(nodeList.item(i));
            }
            return db.build(new DOMSource(parent));
        } catch (ParserConfigurationException e) {
            e.printStackTrace();
            return null;
        }
    }
}
