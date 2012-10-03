package org.datadryad.submission.xml;

import nu.xom.Element;

public class State extends Element {

	public State(String aState) {
		super("State");
		appendChild(aState);
	}
}
