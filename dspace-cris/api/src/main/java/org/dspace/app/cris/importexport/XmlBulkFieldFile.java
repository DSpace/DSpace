/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * https://github.com/CILEA/dspace-cris/wiki/License
 */
package org.dspace.app.cris.importexport;

import java.util.List;

import org.w3c.dom.Element;

public class XmlBulkFieldFile implements IBulkChangeFieldFile {
	private List<Element> elementList;
	
	public XmlBulkFieldFile(List<Element> elementList) {
		this.elementList = elementList;
	}

	@Override
	public int size() {
		return elementList.size();
	}
	
	@Override
	public IBulkChangeFieldFileValue get(int y) {
		return new XMLBulkFieldFileValue(elementList.get(y));
	}
}
