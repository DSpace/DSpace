package org.datadryad.submission.xml;

import nu.xom.Element;

public class Zip extends Element {

	public Zip(String aZip) {
		super("Zip");
		appendChild(aZip);
	}
}
