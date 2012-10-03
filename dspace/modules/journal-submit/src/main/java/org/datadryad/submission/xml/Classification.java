package org.datadryad.submission.xml;

import nu.xom.Element;

public class Classification extends Element {

	public Classification(String... aKeywordList) {
		super("Classification");
		
		for (String keywordValue : aKeywordList) {
			Element keyword = new Element("keyword");
			keyword.appendChild(keywordValue);
			appendChild(keyword);
		}
	}

}
