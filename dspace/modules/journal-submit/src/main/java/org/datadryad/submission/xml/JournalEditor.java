package org.datadryad.submission.xml;

import nu.xom.Element;

public class JournalEditor extends Element {
	
	public JournalEditor() {
		super("Journal_Editor");
	}

	public JournalEditor(String aJournalEditor) {
		super("Journal_Editor");
		appendChild(aJournalEditor);
	}
}
