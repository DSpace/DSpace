/*
 * ThemeMatcher.java
 *
 * Version: $Revision: 1.1 $
 *
 * Date: $Date: 2006/01/10 05:18:41 $
 *
 * Copyright (c) 2002-2005, Hewlett-Packard Company and Massachusetts
 * Institute of Technology.  All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are
 * met:
 *
 * - Redistributions of source code must retain the above copyright
 * notice, this list of conditions and the following disclaimer.
 *
 * - Redistributions in binary form must reproduce the above copyright
 * notice, this list of conditions and the following disclaimer in the
 * documentation and/or other materials provided with the distribution.
 *
 * - Neither the name of the Hewlett-Packard Company nor the name of the
 * Massachusetts Institute of Technology nor the names of their
 * contributors may be used to endorse or promote products derived from
 * this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 * ``AS IS'' AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 * LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR
 * A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT
 * HOLDERS OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT,
 * INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING,
 * BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS
 * OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 * ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR
 * TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE
 * USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH
 * DAMAGE.
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

/**
 * This class determines the correct Aspect to use. This is determined by the
 * url string, if it is prepended with a number followed by a slash (such as 1/
 * or 3/) then the Aspect identified by the number is used. When the URL does
 * not start with an integer then the first Aspect (aspect zero) is loaded.
 * 
 * Once the Aspect has been identified the following sitemap parameters are
 * provided: {ID} is the Aspect ID, {aspect} is the path to the aspect,
 * {aspectName} is a unique name for the aspect, and {prefix} is the aspect
 * identifier prepending the URL (if one exists!).
 * 
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
	 * @param pattern
	 *            name of sitemap parameter to find
	 * @param objectModel
	 *            environment passed through via cocoon
	 * @return null or map containing value of sitemap parameter 'pattern'
	 */
	public Map match(String src, Map objectModel, Parameters parameters)
			throws PatternException {
		try {
			Request request = ObjectModelHelper.getRequest(objectModel);
			String uri = request.getSitemapURI();
			DSpaceObject dso = HandleUtil.obtainHandle(objectModel);

			List<Theme> rules = XMLUIConfiguration.getThemeRules();
			getLogger().debug("Checking if URL=" + uri + " matches any theme rules.");
			for (Theme rule : rules) {
				getLogger().debug("rule=" + rule.getName());
				if (!(rule.hasRegex() || rule.hasHandle()))
					// Skip any rule with out a pattern or handle
					continue;

				getLogger().debug("checking for patterns");
				if (rule.hasRegex()) {
					// If the rule has a pattern insure that the URL matches it.
					Pattern pattern = rule.getPattern();
					if (!pattern.matcher(uri).find())
						continue;
				}

				getLogger().debug("checking for handles");
				if (rule.hasHandle()) {
					// If the rules has a handle insure that the DSO matches it.
					if (!HandleUtil.inheritsFrom(dso, rule.getHandle()))
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
