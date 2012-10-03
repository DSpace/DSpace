package org.datadryad.submission.xml;

import nu.xom.Element;

public class Journal extends Element {
	
	public Journal() {
		super("Journal");
	}
	
	public Journal(String aJournalName) {
		super("Journal");
		appendChild(aJournalName);
	}
}
