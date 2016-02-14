/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.xmlui.aspect.general;

import java.util.HashMap;
import java.util.Map;

import org.apache.avalon.framework.logger.AbstractLogEnabled;
import org.apache.avalon.framework.parameters.Parameters;
import org.apache.cocoon.matching.Matcher;
import org.apache.cocoon.sitemap.PatternException;
import org.apache.commons.lang.StringUtils;
import org.dspace.services.factory.DSpaceServicesFactory;

/**
 * Use the configuration in Dspace.cfg to select paths in sitemap.xmap
 * 
 * Original Concept, JSPUI version:    Universidade do Minho   at www.uminho.pt
 * Sponsorship of XMLUI version:    Instituto Oceanográfico de España at www.ieo.es
 * 
 * @author Adán Román Ruiz at arvo.es
 */

public class ConfigurationMatcher extends AbstractLogEnabled implements Matcher
{
    /**
     * Format "keyInDspace.cfg,value,value"
     * "Only property" check if it is defined(not empty), 
     * "property,value" check if property has this value, 
     * "property, value,value..." check that property has one of the following values.
     * operator ! is alowed: "!property, value, value2" property has not value 1 nor value 2
     * @param pattern
     *            name of sitemap parameter to find
     * @param objectModel
     *            environment passed through via cocoon
     * @return null or map containing value of sitemap parameter 'pattern'
     */
    public Map match(String pattern, Map objectModel, Parameters parameters)  throws PatternException
    {
		boolean not = false;
		boolean itMatch = false;
		if (pattern.startsWith("!")) {
			not = true;
			pattern = pattern.substring(1);
		}
		String[] expressions = pattern.split(",");
		String propertyValue = DSpaceServicesFactory.getInstance().getConfigurationService().getProperty(expressions[0]);
		if (expressions.length == 1) {
			if (StringUtils.isNotEmpty(propertyValue)) {
				itMatch = true;
			} else {
				itMatch = false;
			}
		} else {
			for (int i = 1; i < expressions.length; i++) {
				if (StringUtils.equalsIgnoreCase(expressions[i], propertyValue)) {
					itMatch = true;
					break;
				}
			}
			itMatch = false;
		}
		if (itMatch && !not) {
			return new HashMap();
		} else if (itMatch && not) {
			return null;
		} else if (!itMatch && !not) {
			return null;
		} else if (!itMatch && not) {
			return new HashMap();
		}
		return null;
	}
}
