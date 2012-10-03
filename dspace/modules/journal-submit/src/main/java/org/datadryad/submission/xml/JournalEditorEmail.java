package org.datadryad.submission.xml;

import nu.xom.Element;

public class JournalEditorEmail extends Element {
	
	public JournalEditorEmail() {
		super("Journal_Editor_Email");
	}

	public JournalEditorEmail(String aJournalEditorEmail) {
		super("Journal_Editor_Email");
		appendChild(aJournalEditorEmail);
	}
}
