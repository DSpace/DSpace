/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */

package org.dspace.xoai.services.impl.resources.functions;

import java.util.Objects;

import net.sf.saxon.s9api.ExtensionFunction;
import net.sf.saxon.s9api.ItemType;
import net.sf.saxon.s9api.OccurrenceIndicator;
import net.sf.saxon.s9api.QName;
import net.sf.saxon.s9api.SaxonApiException;
import net.sf.saxon.s9api.SequenceType;
import net.sf.saxon.s9api.XdmAtomicValue;
import net.sf.saxon.s9api.XdmValue;
import org.apache.logging.log4j.Logger;
import org.bouncycastle.util.Arrays;


/**
 * Serves as proxy for call from XSL engine. Base for all functions having one string param
 * and returning one string.
 * @author Marian Berger (marian.berger at dataquest.sk)
 */
public abstract class StringXSLFunction implements ExtensionFunction {

    private static final Logger log = org.apache.logging.log4j.LogManager.getLogger(StringXSLFunction.class);
    public static final String BASE = "http://custom.crosswalk.functions";

    protected String uncertainString(Object val) {
        return val == null ? "" : val.toString();
    }

    protected abstract String getFnName();

//    protected abstract String getStringResult(String param);
    protected String getStringResult(String param) {
        return "";
    }

    @Override
    final public QName getName() {
        return new QName(BASE, getFnName());
    }

    @Override
    final public SequenceType getResultType() {
        return SequenceType.makeSequenceType(ItemType.STRING, OccurrenceIndicator.ZERO_OR_ONE);
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
            log.warn("Empty value in call of function of StringXslFunction type");
            val = "";
        }

        return new XdmAtomicValue(checks(getStringResult(val)));
    }

    private String checks(String got) {
        if (Objects.isNull(got) || got.isEmpty()) {
            return "";
        }

        if (got.equalsIgnoreCase("[#document: null]")) {
            return "";
        }

        return got;
    }
}
