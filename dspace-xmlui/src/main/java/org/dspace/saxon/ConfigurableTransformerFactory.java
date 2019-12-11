/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.saxon;

import java.util.HashSet;
import java.util.Set;
import javax.annotation.PostConstruct;
import javax.inject.Inject;

import net.sf.saxon.Configuration;
import net.sf.saxon.TransformerFactoryImpl;
import net.sf.saxon.lib.ExtensionFunctionDefinition;
import org.dspace.services.ConfigurationService;
import org.dspace.utils.DSpace;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Wrapper which provides a configured SaxonTransformerFactory.  Configuration
 * from the DSpace {@link ConfigurationService}.
 *
 * <p>
 * Required properties:
 * <dl>
 *  <dt>functions</dt>
 *   <dd>zero or more {@link ExtensionFunctionDefinition} implementations</dd>
 * </dl>
 *
 * <p>
 * However, Avalon gets in our way, so properties are not yet required or
 * effective.  Instead, configure a list of class names as the value of the
 * DSpace property "saxon.functions" (in config/dspace.cfg or config/local.cfg).
 *
 * @author Mark H. Wood <mwood@iupui.edu>
 */
public class ConfigurableTransformerFactory
        extends TransformerFactoryImpl {
    private static final Logger LOG
            = LoggerFactory.getLogger(ConfigurableTransformerFactory.class);

    private static final ConfigurationService cfg
            = new DSpace().getConfigurationService();

    /** Our custom XPath functions. */
    private Set<ExtensionFunctionDefinition> functions;

    /**
     * Get a list of function definition classes from DSpace configuration,
     * instantiate one of each, and configure this factory.
     *
     * @throws ClassNotFoundException if a configured function cannot be found.
     * @throws InstantiationException if a configured function cannot be instantiated.
     * @throws IllegalAccessException if a configured function is inaccessible.
     */
    public ConfigurableTransformerFactory()
            throws ClassNotFoundException, InstantiationException, IllegalAccessException {
        super();
        LOG.debug("Constructor");

        functions = new HashSet<>();
        for (String functionName : cfg.getArrayProperty("saxon.functions")) {
            LOG.debug("adding function {}", functionName);
            functions.add((ExtensionFunctionDefinition) Class.forName(functionName).newInstance());
        }

        configure();
    }

    @Inject // This should work with Spring, but Avalon gets in our way.
    public void setFunctions(Set<ExtensionFunctionDefinition> functions) {
        LOG.debug("@Inject functions:  {}", functions);
        this.functions = functions;
    }

    /**
     * Apply injected configuration to the wrapped factory.
     */
    @PostConstruct // This should work with Spring, but Avalon gets in our way.
    public void configure() {
        LOG.debug("configure");
        Configuration configuration = getConfiguration();

        // Define our custom XPath functions.
        for (ExtensionFunctionDefinition function : functions) {
            LOG.debug("Defining function {}", function);
            configuration.registerExtensionFunction(function);
        }
    }
}
