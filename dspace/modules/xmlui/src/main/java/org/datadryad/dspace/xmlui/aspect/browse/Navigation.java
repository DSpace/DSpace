/*
 * Navigation.java
 */
package org.datadryad.dspace.xmlui.aspect.browse;

import java.io.IOException;
import java.sql.SQLException;

import org.dspace.app.xmlui.cocoon.AbstractDSpaceTransformer;
import org.dspace.app.xmlui.utils.UIException;
import org.dspace.app.xmlui.wing.WingException;
import org.dspace.app.xmlui.wing.element.List;
import org.dspace.app.xmlui.wing.element.Options;

import org.dspace.authorize.AuthorizeException;

import org.xml.sax.SAXException;

/**
 * Provides navigation options for the dryadbrowse aspect. This is more than
 * likely just temporary until the discovery module provides something like
 * this.
 * 
 * @author Kevin S. Clarke
 */
public class Navigation extends AbstractDSpaceTransformer {

	public void addOptions(Options options) throws SAXException, WingException,
			UIException, SQLException, IOException, AuthorizeException {
		List info = options.addList("DryadBrowse");

		info.setHead("Browse");
		info.addItemXref(contextPath + "/search-filter?field=dc.contributor.author_filter&fq=location:l2", "Authors");
		info.addItemXref(contextPath + "/search-filter?field=prism.publicationName_filter&fq=location:l2", "Journal Title");
	}
}
