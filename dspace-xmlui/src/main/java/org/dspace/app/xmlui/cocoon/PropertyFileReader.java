/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.xmlui.cocoon;

import java.io.FileInputStream;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import org.apache.avalon.framework.parameters.Parameters;
import org.apache.cocoon.environment.Redirector;
import org.apache.cocoon.environment.SourceResolver;

/**
 * Class will read the the specified configuration file via the property-file parameter.
 * 
 * It accepts the name of the property file to read and the properties to set to
 * the sitemap scope.  For example:
 * <pre>{@code
 * 		<map:act type="PropertyFileReader">
 *  		<map:parameter name="property-file" value="/absolute/path/to/property/file />
 *			<map:parameter name="some.property" value="some_property" />
 *			...
 *		</map:act>
 * }</pre>
 * Will place the value of the "some.property" property in the "some_property" variable to be
 * used in the sitemap using the {some_property} syntax.
 * 
 * @author Jay Paz
 * 
 */
public class PropertyFileReader {

	public Map act(Redirector redirector, SourceResolver resolver,
			Map objectModel, String source, Parameters parameters)
			throws Exception {
		Map<String, String> map = new HashMap<String, String>();

		String propertyFile = parameters.getParameter("property-file");
		
		Properties props = new Properties();
		InputStream in = new FileInputStream(propertyFile);
        try {
		    props.load(in);
        } finally {
		    in.close();
        }
		
		final String[] parameterNames = parameters.getNames();
		
		for (int i = 0; i < parameterNames.length; i++) {
			final String paramName = parameterNames[i];
			if ("property-file".equals(paramName)) {
				continue;
			}
			map.put(parameters.getParameter(paramName), props
					.getProperty(paramName));
		}

		return map;
	}
}
