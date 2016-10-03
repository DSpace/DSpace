/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * https://github.com/CILEA/dspace-cris/wiki/License
 */
package org.dspace.app.webui.cris.util;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.Locale;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.jsp.JspException;

import org.apache.commons.lang.StringUtils;
import org.dspace.app.cris.model.ACrisObject;
import org.dspace.app.cris.util.ResearcherPageUtils;
import org.dspace.app.webui.util.IDisplayMetadataValueStrategy;
import org.dspace.app.webui.util.UIUtil;
import org.dspace.authority.AuthorityValueGenerator;
import org.dspace.browse.BrowseDSpaceObject;
import org.dspace.browse.BrowseItem;
import org.dspace.content.Item;
import org.dspace.content.Metadatum;
import org.dspace.content.authority.Choices;
import org.dspace.core.ConfigurationManager;
import org.dspace.core.I18nUtil;
import org.dspace.core.Utils;
import org.dspace.discovery.IGlobalSearchResult;

public class CrisDisplayStrategy implements IDisplayMetadataValueStrategy {

    @Override
	public String getMetadataDisplay(HttpServletRequest hrq, int limit, boolean viewFull, String browseType,
			int colIdx, String field, Metadatum[] metadataArray, BrowseItem item, boolean disableCrossLinks,
			boolean emph) {
		ACrisObject crisObject = (ACrisObject) ((BrowseDSpaceObject) item).getBrowsableDSpaceObject();
		String metadata = "-";
		if (metadataArray.length > 0) {
			metadata = "<a href=\"" + hrq.getContextPath() + "/cris/" + crisObject.getPublicPath() + "/"
					+ ResearcherPageUtils.getPersistentIdentifier(crisObject) + "\">"
					+ Utils.addEntities(metadataArray[0].value) + "</a>";
		}
		metadata = (emph ? "<strong>" : "") + metadata + (emph ? "</strong>" : "");
		return metadata;
	}

	@Override
	public String getMetadataDisplay(HttpServletRequest hrq, int limit, boolean viewFull, String browseType,
			int colIdx, String field, Metadatum[] metadataArray, Item item, boolean disableCrossLinks, boolean emph) {
		String metadata;
		// limit the number of records if this is the author field (if
		// -1, then the limit is the full list)
		boolean truncated = false;
		int loopLimit = metadataArray.length;
		if (limit != -1) {
			loopLimit = (limit > metadataArray.length ? metadataArray.length : limit);
			truncated = (limit < metadataArray.length);
		}

		StringBuffer sb = new StringBuffer();
		for (int j = 0; j < loopLimit; j++) {
			if (metadataArray[j].confidence != Choices.CF_ACCEPTED) {
				continue;
			}
			buildBrowseLink(hrq, viewFull, browseType, metadataArray, disableCrossLinks, sb, j);
			buildAuthority(hrq, metadataArray, sb, j);
			if (j < (loopLimit - 1)) {
				if (colIdx != -1) // we are showing metadata in a table row
									// (browse or item list)
				{
					sb.append("; ");
				} else {
					// we are in the item tag
					sb.append("<br />");
				}
			}
		}
		if (truncated) {
            Locale locale = UIUtil.getSessionLocale(hrq); 
            String etal = I18nUtil.getMessage("itemlist.et-al", locale);

			sb.append(", " + etal);
		}

		if (colIdx != -1) // we are showing metadata in a table row (browse or
							// item list)
		{
			metadata = (emph ? "<strong><em>" : "<em>") + sb.toString() + (emph ? "</em></strong>" : "</em>");
		} else {
			// we are in the item tag
			metadata = (emph ? "<strong>" : "") + sb.toString() + (emph ? "</strong>" : "");
		}

		return metadata;
	}

	private void buildBrowseLink(HttpServletRequest hrq, boolean viewFull, String browseType, Metadatum[] metadataArray,
			boolean disableCrossLinks, StringBuffer sb, int j) {
		String startLink = "";
		String endLink = "";
		if (StringUtils.isEmpty(browseType)) {
			browseType = "author";
		}
		String argument;
		String value;
		argument = "authority";
		String authority = metadataArray[j].authority;
		value = metadataArray[j].value;
		if (viewFull) {
			argument = "vfocus";
		}
		try {
			if(authority.startsWith(AuthorityValueGenerator.GENERATE)) {
				startLink = "<a target=\"_blank\" href=\"" + hrq.getContextPath() + "/browse?type=" + browseType + "&amp;";
			}
			else {
				startLink = "<a target=\"_blank\" href=\"" + hrq.getContextPath() + "/browse?type=" + browseType + "&amp;"
					+ argument + "=" + URLEncoder.encode(authority, "UTF-8");
			}
		} catch (UnsupportedEncodingException e) {
			throw new RuntimeException(e.getMessage(), e);
		}

		if (metadataArray[j].language != null) {
			try {
				startLink = startLink + "&amp;" + argument + "_lang="
						+ URLEncoder.encode(metadataArray[j].language, "UTF-8");
			} catch (UnsupportedEncodingException e) {
				throw new RuntimeException(e.getMessage(), e);
			}
		}

		if ("authority".equals(argument)) {
			startLink += "\" class=\"authority " + browseType + "\">";
		} else {
			startLink = startLink + "\">";
		}
		endLink = "</a>";
		sb.append(startLink);
		sb.append(Utils.addEntities(value));
		sb.append(endLink);

	}

	private void buildAuthority(HttpServletRequest hrq,
            Metadatum[] metadataArray, StringBuffer sb, int j)
    {
        String startLink = "";
        String endLink = "";
		String authority = metadataArray[j].authority;
		if (StringUtils.isNotBlank(authority)) {
			if (authority.startsWith(AuthorityValueGenerator.GENERATE)) {
				String[] split = StringUtils.split(authority, AuthorityValueGenerator.SPLIT);
				String type = null, info = null;
				if (split.length > 0) {
					type = split[1];
					if (split.length > 1) {
						info = split[2];
					}
				}
				String externalContextPath = ConfigurationManager.getProperty("cris","external.domainname.authority.service."+type);
				startLink = "<a target=\"_blank\" href=\"" + externalContextPath + info;
				startLink += "\" class=\"authority\">";
				startLink += "\" class=\"authority\">&nbsp;<img style=\"width: 16px; height: 16px;\" src=\"" + hrq.getContextPath() + "/images/mini-icon-orcid.png\" alt=\"\">";
				endLink = "</a>";
				sb.append(startLink);				
				sb.append(endLink);
			}
			else {
				startLink = "<a target=\"_blank\" href=\"" + hrq.getContextPath() + "/cris/rp/" + authority;
				startLink += "\" class=\"authority\">";
				endLink = "</a>";
				sb.append(startLink);
				sb.append(" <i class=\"fa fa-user\"></i>");
				sb.append(endLink);
			}
		}
		
    }


	@Override
	public String getExtraCssDisplay(HttpServletRequest hrq, int limit, boolean b, String browseType, int colIdx,
			String field, Metadatum[] metadataArray, BrowseItem browseItem, boolean disableCrossLinks, boolean emph) throws JspException {
		return null;
	}

	@Override
	public String getMetadataDisplay(HttpServletRequest hrq, int limit, boolean viewFull, String browseType,
			int colIdx, String field, Metadatum[] metadataArray, IGlobalSearchResult item, boolean disableCrossLinks,
			boolean emph) throws JspException {
		ACrisObject crisObject = (ACrisObject) item;
		String metadata = "-";
		if (metadataArray.length > 0) {
			metadata = "<a href=\"" + hrq.getContextPath() + "/cris/" + crisObject.getPublicPath() + "/"
					+ ResearcherPageUtils.getPersistentIdentifier(crisObject) + "\">"
					+ Utils.addEntities(metadataArray[0].value) + "</a>";
		}
		metadata = (emph ? "<strong>" : "") + metadata + (emph ? "</strong>" : "");
		return metadata;
	}

    @Override
    public String getExtraCssDisplay(HttpServletRequest hrq, int limit,
            boolean b, String browseType, int colIdx, String field,
            Metadatum[] metadataArray, Item item, boolean disableCrossLinks,
            boolean emph) throws JspException
    {
        return null;
    }

}
