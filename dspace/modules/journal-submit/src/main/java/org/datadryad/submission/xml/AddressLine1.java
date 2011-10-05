package org.datadryad.submission.xml;

import nu.xom.Element;

public class AddressLine1 extends Element {

	public AddressLine1(String aAddressLine) {
		super("Address_Line_1");
		appendChild(aAddressLine);
	}
}
