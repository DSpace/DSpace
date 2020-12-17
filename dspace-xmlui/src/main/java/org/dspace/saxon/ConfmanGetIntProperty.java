/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.saxon;

import java.math.BigInteger;

import net.sf.saxon.expr.XPathContext;
import net.sf.saxon.lib.ExtensionFunctionCall;
import net.sf.saxon.lib.ExtensionFunctionDefinition;
import net.sf.saxon.om.Sequence;
import net.sf.saxon.om.StructuredQName;
import net.sf.saxon.trans.XPathException;
import net.sf.saxon.value.IntegerValue;
import net.sf.saxon.value.SequenceType;
import net.sf.saxon.value.StringValue;
import org.dspace.services.ConfigurationService;
import org.dspace.utils.DSpace;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Define a Saxon integrated extension function for access to the DSpace
 * ConfigurationManager's getIntProperty method.
 *
 * @author Mark H. Wood <mwood@iupui.edu>
 */
public class ConfmanGetIntProperty
        extends ExtensionFunctionDefinition {
    private static final Logger LOG
            = LoggerFactory.getLogger(ConfmanGetIntProperty.class);

    private static final ConfigurationService cfg
            = new DSpace().getConfigurationService();

    @Override
    public StructuredQName getFunctionQName() {
        LOG.debug("getFunctionQName");
        return new StructuredQName("confman", "https://dspace.org/api/confman",
                "getIntProperty");
    }

    // TODO this class should handle both 1- and 2-argument calls.
    // @Override
    // int getMinimumNumberOfArguments();

    // TODO this class should handle both 1- and 2-argument calls.
    // @Override
    // int getMaximumNumberOfArguments();

    @Override
    public SequenceType[] getArgumentTypes() {
        return new SequenceType[]{
            SequenceType.SINGLE_STRING, // property name
            SequenceType.SINGLE_INTEGER // default value
        };
    }

    @Override
    public SequenceType getResultType(SequenceType[] sts) {
        return SequenceType.SINGLE_INTEGER;
    }

    @Override
    public ExtensionFunctionCall makeCallExpression() {
        return new ExtensionFunctionCall() {
            @Override
            public Sequence call(XPathContext context, Sequence[] arguments)
                    throws XPathException {
                String key = ((StringValue) arguments[0]).getStringValue();
                // TODO this method should handle both 1- and 2-argument calls.
                int defaultValue = (int) ((IntegerValue) arguments[1]).longValue();
                long value = cfg.getIntProperty(key, defaultValue);
                return IntegerValue.makeIntegerValue(BigInteger.valueOf(value));
            }
        };
    }
}
