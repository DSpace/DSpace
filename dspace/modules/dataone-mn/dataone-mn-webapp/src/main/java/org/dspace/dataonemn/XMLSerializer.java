package org.dspace.dataonemn;

import java.io.IOException;
import java.io.OutputStream;

import nu.xom.Element;
import nu.xom.Serializer;

public class XMLSerializer extends Serializer {

	public static final int INDENTATION = 2;

	public XMLSerializer(OutputStream aOutputStream) {
		super(aOutputStream);
		setIndent(INDENTATION);
	}

	@Override
	public void write(Element aElement) throws IOException {
		super.write(aElement);
	}

	@Override
	public void writeStartTag(Element aElement) throws IOException {
		super.writeStartTag(aElement);
	}

	@Override
	public void writeEndTag(Element aElement) throws IOException {
		super.writeEndTag(aElement);
	}
}
