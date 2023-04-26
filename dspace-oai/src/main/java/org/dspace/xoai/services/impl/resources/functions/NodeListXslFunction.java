/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */

package org.dspace.xoai.services.impl.resources.functions;

import static org.apache.logging.log4j.LogManager.getLogger;
import static org.dspace.xoai.services.impl.resources.functions.StringXSLFunction.BASE;

import java.util.Objects;
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
import org.apache.logging.log4j.Logger;
import org.bouncycastle.util.Arrays;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;


/**
 * Serves as proxy for call from XSL engine.
 *
 * @author Marian Berger (marian.berger at dataquest.sk)
 * @author Milan Majchrak (milan.majchrak at dataquest.sk)
 */
public abstract class NodeListXslFunction implements ExtensionFunction {

    protected abstract String getFnName();

    protected abstract NodeList getNodeList(String param);

    private static final Logger log = getLogger(NodeListXslFunction.class);

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
        Node oneNode = nodeList.item(0);

        DocumentBuilder db = new Processor(false).newDocumentBuilder();
        DOMSource sourceObj = new DOMSource(oneNode);
        var res = db.wrap(sourceObj);
        return res;
    }
}
