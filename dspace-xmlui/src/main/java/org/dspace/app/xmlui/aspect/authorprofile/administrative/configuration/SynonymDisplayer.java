/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.xmlui.aspect.authorprofile.administrative.configuration;

import org.apache.cocoon.environment.Request;
import org.dspace.core.ConfigurationManager;

/**
 *
 * @author Roeland Dillen (roeland at atmire dot com)
 * @author Kevin Van de Velde (kevin at atmire dot com)
 * @author Ben Bosman (ben at atmire dot com)
 * @author Mark Diggory (markd at atmire dot com)
 */
public class SynonymDisplayer implements FieldDisplayer {
    @Override
    public String displayValue(Request request, String value) {
        return ConfigurationManager.getProperty("dspace.url")+"/author/"+value;
    }
}
