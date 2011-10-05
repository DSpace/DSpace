package org.datadryad.submission.xml;

import org.datadryad.submission.EmailParser;

import nu.xom.Element;

public class Authors extends Element {

	public Authors(String... aAuthorList) {
		super("Authors");
		
		for (String authorName : aAuthorList) {
			Element author = new Element("Author");
			author.appendChild(EmailParser.flipName(authorName));
			appendChild(author);
		}
	}

}
