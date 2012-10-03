package org.datadryad.submission.xml;

import nu.xom.Element;

public class SubmissionMetadata extends Element {

	public SubmissionMetadata(String aManuscriptID, String aTitle) {
		super("Submission_Metadata");
		
		Element manuscript = new Element("Manuscript");
		Element articleTitle = new Element("Article_Title");
		
		manuscript.appendChild(aManuscriptID);
		articleTitle.appendChild(aTitle);
		
		appendChild(manuscript);
		appendChild(articleTitle);
	}

}
