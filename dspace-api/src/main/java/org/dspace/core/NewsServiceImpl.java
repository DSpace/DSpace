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
