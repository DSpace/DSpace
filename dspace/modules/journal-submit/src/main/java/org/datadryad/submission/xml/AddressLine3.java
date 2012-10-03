package org.datadryad.submission.xml;

import nu.xom.Element;

public class AddressLine3 extends Element {

	public AddressLine3(String aAddressLine) {
		super("Address_Line_3");
		appendChild(aAddressLine);
	}
}
