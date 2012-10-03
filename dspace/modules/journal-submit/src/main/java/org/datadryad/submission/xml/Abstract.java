package org.datadryad.submission.xml;

import nu.xom.Element;

public class Abstract extends Element {

	public Abstract(String aAbstract) {
		super("Abstract");
		appendChild(aAbstract.replaceAll("\\r\\n\\t", " ")
				.replaceAll("\\s+", " ").trim());
	}
}
