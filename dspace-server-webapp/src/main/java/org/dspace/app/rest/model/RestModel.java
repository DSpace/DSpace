/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.model;

import java.io.Serializable;

import com.fasterxml.jackson.annotation.JsonIgnore;
import org.atteo.evo.inflector.English;

/**
 * A REST resource directly or indirectly (in a collection) exposed must have at
 * least a type attribute to facilitate deserialization
 *
 * @author Andrea Bollini (andrea.bollini at 4science.it)
 */
public interface RestModel extends Serializable {

    public static final String ROOT = "root";
    public static final String CORE = "core";
    public static final String EPERSON = "eperson";
    public static final String DISCOVER = "discover";
    public static final String CONFIGURATION = "config";
    public static final String INTEGRATION = "integration";
    public static final String STATISTICS = "statistics";
    public static final String SUBMISSION = "submission";
    public static final String SYSTEM = "system";
    public static final String WORKFLOW = "workflow";
    public static final String AUTHORIZATION = "authz";

    public String getType();

    @JsonIgnore
    default public String getTypePlural() {
        return English.plural(getType());
    }
}
