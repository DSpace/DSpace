/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.export.api;

import java.io.File;
import java.io.OutputStream;

import org.dspace.content.Item;

/**
 * 
 * @author João Melo <jmelo@lyncode.com>
 * @version $Revision$
 */
public interface ExportItemProvider {
	File getXSLT ();
	String getId ();
	String getImage ();
	void export (Item item, OutputStream output) throws ExportItemException;
	String getContentType();
	String getFileExtension();
}
