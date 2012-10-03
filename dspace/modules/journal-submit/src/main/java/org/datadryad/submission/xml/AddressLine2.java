package org.datadryad.submission.xml;

import nu.xom.Element;

public class AddressLine2 extends Element {

	public AddressLine2(String aAddressLine) {
		super("Address_Line_2");
		appendChild(aAddressLine);
	}
}
