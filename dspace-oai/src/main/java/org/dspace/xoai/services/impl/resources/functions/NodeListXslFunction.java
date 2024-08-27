/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */

package org.dspace.xoai.services.impl.resources.functions;

import static org.dspace.xoai.services.impl.resources.functions.StringXSLFunction.BASE;

import java.util.LinkedList;
import java.util.List;
import java.util.Objects;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import net.sf.saxon.s9api.ExtensionFunction;
import net.sf.saxon.s9api.ItemType;
import net.sf.saxon.s9api.OccurrenceIndicator;
import net.sf.saxon.s9api.QName;
import net.sf.saxon.s9api.SaxonApiException;
import net.sf.saxon.s9api.SequenceType;
import net.sf.saxon.s9api.XdmAtomicValue;
import net.sf.saxon.s9api.XdmItem;
import net.sf.saxon.s9api.XdmValue;
import org.apache.logging.log4j.Logger;
import org.bouncycastle.util.Arrays;
import org.w3c.dom.Document;
import org.w3c.dom.Element;


/**
 * Serves as proxy for call from XSL engine.
 *
 * @author Marian Berger (marian.berger at dataquest.sk)
 * @author Milan Majchrak (milan.majchrak at dataquest.sk)
 */
public abstract class NodeListXslFunction implements ExtensionFunction {

    private static final Logger log = org.apache.logging.log4j.LogManager.getLogger(NodeListXslFunction.class);
    protected abstract String getFnName();

    protected abstract List<String> getList(String param);
    @Override
    final public QName getName() {
        return new QName(BASE, getFnName());
    }

    @Override
    final public SequenceType getResultType() {
        return SequenceType.makeSequenceType(ItemType.STRING, OccurrenceIndicator.ZERO_OR_MORE);
    }

    @Override
    final public SequenceType[] getArgumentTypes() {
        return new SequenceType[]{
                SequenceType.makeSequenceType(
                        ItemType.STRING, OccurrenceIndicator.ZERO_OR_MORE)};
    }

    @Override
    final public XdmValue call(XdmValue[] xdmValues) throws SaxonApiException {
        if (Objects.isNull(xdmValues) || Arrays.isNullOrContainsNull(xdmValues)) {
            return new XdmAtomicValue("");
        }

        String val;
        try {
            val = xdmValues[0].itemAt(0).getStringValue();
        } catch (Exception e) {
            // e.g. when no parameter is passed and xdmValues[0] ends with index error
            log.warn("Empty value in call of function of NodeListXslFunction type");
            val = "";
        }

        List<String> list = getList(val);
        DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
        javax.xml.parsers.DocumentBuilder db = null;
        try {
            db = dbf.newDocumentBuilder();
            Document newDoc = db.newDocument();
            Element rootElement = newDoc.createElement("root");
            newDoc.appendChild(rootElement);

            List<XdmItem> items = new LinkedList<>();
            for (String item : list) {
                items.add(new XdmAtomicValue(item));
            }
            return new XdmValue(items);

        } catch (ParserConfigurationException e) {
            throw new RuntimeException(e);
        }

    }
}
