/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.webui.util;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.List;

import javax.servlet.http.HttpServletRequest;

import org.dspace.app.webui.util.UIUtil;
import org.dspace.core.ConfigurationManager;

public abstract class ASolrQueryConfigurator {
	
	protected abstract String getComponentIdentifier();
	
	public int getEtAl(HttpServletRequest request, String type) {
		int etAl = UIUtil.getIntParameter(request, "etAl");
		if (etAl == -1) {
			etAl = ConfigurationManager.getIntProperty(
					getComponentIdentifier() +"." + type + ".etal", -1);
		}
		return etAl;
	}

	public int getRPP(HttpServletRequest request, String type) {
		int rpp = UIUtil.getIntParameter(request, "rpp");
		if (rpp == -1) {
			rpp = ConfigurationManager.getIntProperty(
					getComponentIdentifier() +"." + type + ".rpp",
					Integer.MAX_VALUE);
		}
		return rpp;
	}

	public String getOrder(HttpServletRequest request, String type) {
		String order = request.getParameter("order");
		if (order == null) {
			order = ConfigurationManager
					.getProperty(getComponentIdentifier() +"." + type
							+ ".order");
		}
		return order;
	}

	public int getSortBy(HttpServletRequest request, String type) {
		int sortBy = UIUtil.getIntParameter(request, "sort_by");
		if (sortBy == -1) {
			sortBy = ConfigurationManager.getIntProperty(
					getComponentIdentifier() +"." + type + ".sortby", -1);
		}
		return sortBy;
	}

	public List<String> getFilters(String type) {
		List<String> filters = new ArrayList<String>();
		int idx = 1;
		while (ConfigurationManager.getProperty(getComponentIdentifier() +"."
				+ type + ".filters." + idx) != null) {
			filters.add(ConfigurationManager
					.getProperty(getComponentIdentifier() + "." + type
							+ ".filters." + idx));
			idx++;
		}
		return filters;
	}

	public String getQuery(String type, String... parameters) {
		return MessageFormat.format(
				ConfigurationManager.getProperty(getComponentIdentifier() +"."
						+ type + ".query"), parameters);
	}

}
