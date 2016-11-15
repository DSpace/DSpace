/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.webui.viewer;

import java.sql.SQLException;

import javax.servlet.http.HttpServletRequest;
import org.dspace.content.Bitstream;
import org.dspace.content.Item;
import org.dspace.core.Context;

public interface JSPViewer extends org.dspace.app.util.IViewer {
	/**
	 * 
	 * @return true to embed or false to full page
	 */
	boolean isEmbedded();
	
	/**
	 * 
	 * @return jsp to embed
	 */
	String getViewJSP();
	
	/**
	 * Add all information needed to the view as request attributes 
	 * ,
	 * @throws SQLException 
	 */
	void prepareViewAttribute(Context context,HttpServletRequest request ,Bitstream bitstream) throws SQLException;

}
