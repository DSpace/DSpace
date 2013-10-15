package org.datadryad.submission.xml;

import nu.xom.Element;

public class JournalCode extends Element {

	public JournalCode() {
		super("Journal_Code");
	}

	public JournalCode(String aJournalCode) {
		super("Journal_Code");
		appendChild(aJournalCode);
	}
}
