/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.core;

import java.util.List;

import org.dspace.core.service.NewsService;

public class NewsServiceImpl implements NewsService {
	private List<String> acceptableFilenames;
	
	public void setAcceptableFilenames(List<String> acceptableFilenames) {
		this.acceptableFilenames = acceptableFilenames;
	}
	
	@Override
	public boolean validate(String newsName) {
		if (acceptableFilenames != null) {
			return acceptableFilenames.contains(newsName);
		}
		return false;
	}

}
