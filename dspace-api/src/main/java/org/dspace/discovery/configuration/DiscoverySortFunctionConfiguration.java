/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.discovery.configuration;

import java.io.Serializable;
import java.text.MessageFormat;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

/**
 *
 * Extension of {@link DiscoverySortFieldConfiguration} used to configure sorting
 * taking advantage of solr function feature.
 *
 * Order is evaluated by mean of function parameter value and passed in arguments as input.
 *
 * @author Corrado Lombardi (corrado.lombardi at 4science.it)
 *
 */
public class DiscoverySortFunctionConfiguration extends DiscoverySortFieldConfiguration {

    public static final String SORT_FUNCTION = "sort_function";
    private String function;
    private List<String> arguments;
    private String id;

    public void setFunction(final String function) {
        this.function = function;
    }

    public void setArguments(final List<String> arguments) {
        this.arguments = arguments;
    }

    @Override
    public String getType() {
        return SORT_FUNCTION;
    }

    @Override
    public String getMetadataField() {
        return id;
    }

    public void setId(final String id) {
        this.id = id;
    }

    /**
     * Returns the function to be used by solr to sort result
     * @param functionArgs variable arguments to be inserted in function
     * @return
     */
    public String getFunction(final Serializable... functionArgs) {
        final String args = String.join(",", Optional.ofNullable(arguments).orElse(Collections.emptyList()));
        final String result = function + "(" + args + ")";
        return MessageFormat.format(result, functionArgs);
    }

}
