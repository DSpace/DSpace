package org.datadryad.submission.xml;

import org.datadryad.submission.EmailParser;

import nu.xom.Element;

public class CorrespondingAuthor extends Element {

	public CorrespondingAuthor(String aAuthorName) {
		super("Corresponding_Author");
		appendChild(EmailParser.flipName(aAuthorName));
	}

}
