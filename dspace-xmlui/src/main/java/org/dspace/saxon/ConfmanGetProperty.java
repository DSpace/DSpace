/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.saxon;

import net.sf.saxon.expr.XPathContext;
import net.sf.saxon.lib.ExtensionFunctionCall;
import net.sf.saxon.lib.ExtensionFunctionDefinition;
import net.sf.saxon.om.Sequence;
import net.sf.saxon.om.StructuredQName;
import net.sf.saxon.trans.XPathException;
import net.sf.saxon.value.SequenceType;
import net.sf.saxon.value.StringValue;
import org.dspace.services.ConfigurationService;
import org.dspace.utils.DSpace;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Define a Saxon integrated extension function for access to the DSpace
 * ConfigurationManager's getProperty method.
 *
 * @author Mark H. Wood <mwood@iupui.edu>
 */
public class ConfmanGetProperty
        extends ExtensionFunctionDefinition {
    private static final Logger LOG
            = LoggerFactory.getLogger(ConfmanGetProperty.class);

    private static final ConfigurationService cfg
            = new DSpace().getConfigurationService();

    @Override
    public StructuredQName getFunctionQName() {
        LOG.debug("getFunctionQName");
        return new StructuredQName("confman", "https://dspace.org/api/confman",
                "getProperty");
    }

    @Override
    public SequenceType[] getArgumentTypes() {
        return new SequenceType[]{SequenceType.SINGLE_STRING};
    }

    @Override
    public SequenceType getResultType(SequenceType[] sts) {
        return SequenceType.SINGLE_STRING;
    }

    @Override
    public ExtensionFunctionCall makeCallExpression() {
        LOG.debug("makeCallExpression");
        return new ExtensionFunctionCall() {
            @Override
            public Sequence call(XPathContext context, Sequence[] arguments)
                    throws XPathException {
                String key = ((StringValue) arguments[0]).getStringValue();
                LOG.debug("called with key '{}'", key);
                String value = cfg.getProperty(key);
                return StringValue.makeStringValue(value);
            }
        };
    }
}
