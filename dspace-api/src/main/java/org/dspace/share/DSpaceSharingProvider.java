/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.share;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.Map;
import java.util.TreeMap;

import org.dspace.core.ConfigurationManager;
import org.dspace.services.share.ShareItem;
import org.dspace.services.share.ShareProvider;

/**
 * DSpace generic share provider (url based)
 * 
 * @author João Melo <jmelo@lyncode.com>
 */
public class DSpaceSharingProvider implements ShareProvider {
	private static final String DEFAULT_IMAGE_EXTENSION = ".png";
	private static Map<String, DSpaceSharingProvider> providers;
	
	
	public static DSpaceSharingProvider getProvider (String id) {
		if (providers == null) providers = new TreeMap<String, DSpaceSharingProvider>();
		if (!providers.containsKey(id)) {
			Integer maxD = ConfigurationManager.getIntProperty("sharingbar", id+".max.description", -1);
			if (maxD == -1) maxD = null;
			Integer maxT = ConfigurationManager.getIntProperty("sharingbar", id+".max.title", -1);
			if (maxT == -1) maxT = null;
			String url = ConfigurationManager.getProperty("sharingbar", id+".url");
			String image = ConfigurationManager.getProperty("sharingbar", id+".image");
			if (image == null) image = id+DEFAULT_IMAGE_EXTENSION;
			if (url != null)
				providers.put(id, new DSpaceSharingProvider(id, url, maxD, maxT, image));
		}
		return providers.get(id);
	}
	
	private String id;
	private String url;
	private Integer maxDescription;
	private Integer maxTitle;
	private String image;
	
	public DSpaceSharingProvider (String id, String url, Integer maxD, Integer maxT, String image) {
		this.url = url;
		this.maxDescription = maxD;
		this.maxTitle = maxT;
		this.image = image;
		this.id = id;
	}
	
	@Override
	public boolean isAvailable(ShareItem item) {
		return (item != null && item.getUrl() != null);
	}

	@Override
	public String generateUrl(ShareItem item) {
		String title = item.getTitle();
		if (title != null && this.maxTitle != null) {
			if (title.length() > this.maxTitle)
				title = title.substring(0, this.maxTitle-3)+"...";
		}
		if (title == null) title = "";
		String description = item.getDescription();
		if (description != null && this.maxDescription != null) {
			if (description.length() > this.maxDescription)
				description = description.substring(0, this.maxDescription-3)+"...";
		}
		if (description == null) description = "";
		
		String url = this.url.
				replace("[$title]", this.urlEncode(title)).
				replace("[$url]", this.urlEncode(item.getUrl())).
				replace("[$description]", this.urlEncode(description));
		
		return url;
	}
	
	private String urlEncode (String s) {
		try {
			return URLEncoder.encode(s,"UTF-8");
		} catch (UnsupportedEncodingException e) {
			return s;
		}
	}

	@Override
	public String getImage() {
		return this.image;
	}

	@Override
	public String getId() {
		return id;
	}

}
