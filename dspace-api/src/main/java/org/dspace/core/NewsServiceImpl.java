/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.core;

import java.util.ArrayList;
import java.util.List;

import org.dspace.core.service.NewsService;


public class NewsServiceImpl implements NewsService {
	private List<String> acceptableFilenames;
	
	public void setAcceptableFilenames(List<String> acceptableFilenames) {
		this.acceptableFilenames = addLocalesToAcceptableFilenames(acceptableFilenames);
	}


	protected List<String> addLocalesToAcceptableFilenames(List<String> acceptableFilenames){
		if(ConfigurationManager.getProperty("webui.supported.locales") == null) {
			String[] locales = ConfigurationManager.getProperty("webui.supported.locales").split(",");
			List<String> newAcceptableFilenames = new ArrayList<>();
			newAcceptableFilenames.addAll(acceptableFilenames);

			for (String local : locales) {
				for (String acceptableFilename : acceptableFilenames) {
					String[] values = acceptableFilename.split("\\.");
					int lastPoint = acceptableFilename.lastIndexOf(".");
					newAcceptableFilenames.add(values[0] + "_" + local + "." + values[1]);
					newAcceptableFilenames.add(acceptableFilename.substring(0, lastPoint) + "_" + local + acceptableFilename.substring(lastPoint));
				}
			}
			return newAcceptableFilenames;
		}

		return  acceptableFilenames;
	}


	@Override
	public boolean validate(String newsName) {
		if (acceptableFilenames != null) {
			return acceptableFilenames.contains(newsName);
		}
		return false;
	}

}
