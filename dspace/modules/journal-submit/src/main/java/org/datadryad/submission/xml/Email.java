package org.datadryad.submission.xml;

import nu.xom.Element;

public class Email extends Element {

	public Email(String aEmail) {
		super("Email");
		appendChild(aEmail);
	}
}
