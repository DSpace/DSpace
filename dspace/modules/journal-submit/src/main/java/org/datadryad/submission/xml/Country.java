package org.datadryad.submission.xml;

import nu.xom.Element;

public class Country extends Element {

	public Country(String aCountry) {
		super("Country");
		appendChild(aCountry);
	}
}
