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
 * least a type attribute to facilitate deserialization.
 *
 * @author Andrea Bollini (andrea.bollini at 4science.it)
 */
public interface RestModel extends Serializable {

    String ROOT = "root";
    String CONTENT_REPORT = "contentreport";
    String CORE = "core";
    String EPERSON = "eperson";
    String DISCOVER = "discover";
    String CONFIGURATION = "config";
    String INTEGRATION = "integration";
    String STATISTICS = "statistics";
    String SUBMISSION = "submission";
    String SYSTEM = "system";
    String WORKFLOW = "workflow";
    String AUTHORIZATION = "authz";
    String VERSIONING = "versioning";
    String AUTHENTICATION = "authn";
    String TOOLS = "tools";

    String getType();

    @JsonIgnore
    default String getTypePlural() {
        return English.plural(getType());
    }
}
