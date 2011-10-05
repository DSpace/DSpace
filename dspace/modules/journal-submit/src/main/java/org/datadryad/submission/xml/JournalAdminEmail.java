package org.datadryad.submission.xml;

import nu.xom.Element;

public class JournalAdminEmail extends Element {
	
	public JournalAdminEmail() {
		super("Journal_Admin_Email");
	}

	public JournalAdminEmail(String aJournalAdminEmail) {
		super("Journal_Admin_Email");
		appendChild(aJournalAdminEmail);
	}
}
