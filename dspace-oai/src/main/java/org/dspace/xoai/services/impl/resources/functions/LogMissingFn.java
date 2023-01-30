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

import net.sf.saxon.s9api.ExtensionFunction;
import net.sf.saxon.s9api.ItemType;
import net.sf.saxon.s9api.OccurrenceIndicator;
import net.sf.saxon.s9api.QName;
import net.sf.saxon.s9api.SaxonApiException;
import net.sf.saxon.s9api.SequenceType;
import net.sf.saxon.s9api.XdmAtomicValue;
import net.sf.saxon.s9api.XdmValue;
import org.bouncycastle.util.Arrays;
import org.dspace.utils.XslLogUtil;


/**
 * Serves as proxy for call from XSL engine.
 * @author Marian Berger (marian.berger at dataquest.sk)
 */
public class LogMissingFn implements ExtensionFunction {

    @Override
    public QName getName() {
        return new QName(BASE, "logMissing");
    }

    @Override
    public SequenceType getResultType() {
        return SequenceType.makeSequenceType(ItemType.STRING, OccurrenceIndicator.ZERO_OR_ONE);
    }

    @Override
    public SequenceType[] getArgumentTypes() {
        return new SequenceType[]{
                SequenceType.makeSequenceType(ItemType.STRING, OccurrenceIndicator.ONE),
                SequenceType.makeSequenceType(ItemType.STRING, OccurrenceIndicator.ONE)
        };
    }

    @Override
    public XdmValue call(XdmValue[] xdmValues) throws SaxonApiException {
        if (Objects.isNull(xdmValues) || Arrays.isNullOrContainsNull(xdmValues)) {
            return new XdmAtomicValue("");
        }
        return new XdmAtomicValue(checks(XslLogUtil.logMissing(xdmValues[0].itemAt(0).getStringValue(),
                xdmValues[0].itemAt(1).getStringValue())));
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
