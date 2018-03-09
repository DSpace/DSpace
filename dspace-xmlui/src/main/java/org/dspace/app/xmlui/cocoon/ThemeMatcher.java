/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.xmlui.cocoon;

import java.sql.SQLException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

import org.apache.avalon.framework.logger.AbstractLogEnabled;
import org.apache.avalon.framework.parameters.Parameters;
import org.apache.cocoon.environment.ObjectModelHelper;
import org.apache.cocoon.environment.Request;
import org.apache.cocoon.matching.Matcher;
import org.apache.cocoon.sitemap.PatternException;
import org.dspace.app.xmlui.configuration.XMLUIConfiguration;
import org.dspace.app.xmlui.configuration.Theme;
import org.dspace.app.xmlui.utils.HandleUtil;
import org.dspace.content.DSpaceObject;
import org.dspace.services.factory.DSpaceServicesFactory;

/**
 * This class determines the correct Theme to apply to the URL. This is
 * determined by the Theme rules defined in the xmlui.xml configuration file.
 * Each rule is evaluated in order and the first rule to match is the selected
 * Theme.
 * 
 * Once the Theme has been selected the following sitemap parameters are
 * provided: {themeName} is a unique name for the Theme, and {theme} is the
 * theme's path.
 * 
 * @author Scott Phillips
 */

public class ThemeMatcher extends AbstractLogEnabled implements Matcher {

	/**
	 * @param src
	 *            name of sitemap parameter to find (ignored)
	 * @param objectModel
	 *            environment passed through via cocoon
	 * @param parameters
	 *            parameters passed to this matcher in the sitemap
	 * @return null or map containing value of sitemap parameter 'src'
	 */
	public Map match(String src, Map objectModel, Parameters parameters)
			throws PatternException {
		try {
			Request request = ObjectModelHelper.getRequest(objectModel);
			String uri = request.getSitemapURI();
			DSpaceObject dso = HandleUtil.obtainHandle(objectModel);

			
			// Allow the user to override the theme configuration
			if (DSpaceServicesFactory.getInstance().getConfigurationService().getBooleanProperty("xmlui.theme.allowoverrides",false))
			{
				String themePathOverride  = request.getParameter("themepath");
				if (themePathOverride != null && themePathOverride.length() > 0)
				{
					// Allowing the user to specify the theme path is a security risk because it 
					// allows the user to direct which sitemap is executed next. An attacker could 
					// use this in combination with another attack execute code on the server. 
					// Ultimately this option should not be turned on in a production system and 
					// only used in development. However lets do some simple sanity checks to 
					// protect us a little even when under development.
					
					// Allow: allow all letters and numbers plus periods (but not consecutive), 
					// dashes, underscores, and forward slashes 
					if (!themePathOverride.matches("^[a-zA-Z0-9][a-zA-Z0-9/_\\-]*/?$")) {
						
						throw new IllegalArgumentException("The user specified theme path, \""+themePathOverride+"\", may be " +
								"an exploit attempt. To use this feature please limit your theme paths to only letters " +
								"(a-Z), numbers(0-9), dashes(-), underscores (_), and trailing forward slashes (/).");
					}
					
					// The user is selecting to override a theme, ignore any set
					// rules to apply and use the one specified.
					String themeNameOverride = request.getParameter("themename");
					String themeIdOverride = request.getParameter("themeid");
					
					if (themeNameOverride == null || themeNameOverride.length() == 0)
                    {
                        themeNameOverride = "User specified theme";
                    }
					
					getLogger().debug("User as specified to override theme selection with theme "+
							"(name=\""+themeNameOverride+"\", path=\""+themePathOverride+"\", id=\""+themeIdOverride+"\")");
					
					Map<String, String> result = new HashMap<String, String>();
					result.put("themeName", themeNameOverride);
					result.put("theme", themePathOverride);
					result.put("themeID", themeIdOverride);
					
					return result;
				}
			}
			
			
			List<Theme> rules = XMLUIConfiguration.getThemeRules();
			getLogger().debug("Checking if URL=" + uri + " matches any theme rules.");
			for (Theme rule : rules) {
				getLogger().debug("rule=" + rule.getName());
				if (!(rule.hasRegex() || rule.hasHandle()))
                {
                    // Skip any rule without a pattern or handle
                    continue;
                }

				getLogger().debug("checking for patterns");
				if (rule.hasRegex()) {
					// If the rule has a pattern ensure that the URL matches it.
					Pattern pattern = rule.getPattern();
					if (!pattern.matcher(uri).find())
                    {
                        continue;
                    }
				}

				getLogger().debug("checking for handles");
				if (rule.hasHandle() && !HandleUtil.inheritsFrom(dso, rule.getHandle()))
                {
                    continue;
                }

				getLogger().debug("rule selected!!");
				Map<String, String> result = new HashMap<String, String>();
				result.put("themeName", rule.getName());
				result.put("theme", rule.getPath());
				result.put("themeID", rule.getId());
				
				request.getSession().setAttribute("themeName", rule.getName());
				request.getSession().setAttribute("theme", rule.getPath());
				request.getSession().setAttribute("themeID", rule.getId());
				
				return result;
			}

		} catch (SQLException sqle) {
			throw new PatternException(sqle);
		}

		// No themes matched.
		return null;
	}

}
