package org.datadryad.submission.xml;

import nu.xom.Element;

public class City extends Element {

	public City(String aCity) {
		super("City");
		appendChild(aCity);
	}
}
