package org.datadryad.submission.xml;

import nu.xom.Element;

public class JournalEmbargoPeriod extends Element {
	
	public static final String UNTIL = "untilArticleAppears";
	
	public JournalEmbargoPeriod() {
		super("Journal_Embargo_Period");
	}

	public JournalEmbargoPeriod(String aJournalEmbargoPeriod) {
		super("Journal_Embargo_Period");
		appendChild(aJournalEmbargoPeriod);
	}
}
