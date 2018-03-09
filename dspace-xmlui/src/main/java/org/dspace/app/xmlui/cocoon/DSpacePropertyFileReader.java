/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.xmlui.cocoon;

import java.util.HashMap;
import java.util.Map;

import org.apache.avalon.framework.parameters.Parameters;
import org.apache.cocoon.acting.AbstractAction;
import org.apache.cocoon.environment.Redirector;
import org.apache.cocoon.environment.SourceResolver;
import org.dspace.services.factory.DSpaceServicesFactory;

/**
 * Class will read the DSpace configuration file via
 * {@link DSpaceServicesFactory#getConfigurationService()}.
 * It accepts the name of the property to read and the name of the variable to
 * use in the sitemap scope.
 *
 * <p>
 * For example:
 * <pre>{@code
 * 		<map:act type="DSpacePropertyFileReader">
 *			<map:parameter name="dspace.dir" value="dspace_dir" />
 *			<map:transform type="Include" src="{dspace_dir}/config/news.xml" /> 
 *		</map:act>
 * }</pre>
 * Will place the value of the {@code dspace.dir} property in the
 * {@code dspace_dir} variable to be used in the sitemap.
 * 
 * @author Jay Paz
 * 
 */
public class DSpacePropertyFileReader extends AbstractAction {

    /**
     * Reading action.
     *
     * @param redirector unused.
     * @param resolver unused.
     * @param objectModel unused.
     * @param source unused.
     * @param parameters Reader parameters.
     * @return a map of parameter names to DSpace configuration values.
     * @throws Exception passed through.
     */
    @Override
	public Map act(Redirector redirector, SourceResolver resolver,
			Map objectModel, String source, Parameters parameters)
			throws Exception {

		Map<String, String> map = new HashMap<String, String>();

		final String[] parameterNames = parameters.getNames();
		
		for (int i = 0; i < parameterNames.length; i++) {
			final String paramName = parameterNames[i];
			map.put(parameters.getParameter(paramName),
					DSpaceServicesFactory.getInstance().getConfigurationService().getProperty(paramName));
		}
		
		return map;
	}
}
