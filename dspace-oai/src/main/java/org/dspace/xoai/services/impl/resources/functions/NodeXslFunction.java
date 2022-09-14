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


/**
 * Serves as proxy for call from XSL engine.
 */
public abstract class NodeXslFunction implements ExtensionFunction {

    protected abstract String getFnName();

    protected abstract Node getNode(String param);

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
        Node node = getNode(xdmValues[0].itemAt(0).getStringValue());
        if (Objects.isNull(node)) {
            try {
                node = DocumentBuilderFactory.newInstance().newDocumentBuilder().newDocument();
            } catch (ParserConfigurationException e) {
                e.printStackTrace();
            }
        }
        DocumentBuilder db = new Processor(false).newDocumentBuilder();
        var res = db.build(new DOMSource(node));
        return res;
    }
}
