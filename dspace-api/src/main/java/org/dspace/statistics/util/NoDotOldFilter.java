/**
 * 
 */
package org.dspace.statistics.util;

import java.io.File;
import java.io.FileFilter;

/**
 * @author Ulrich Hahn
 *
 */
public class NoDotOldFilter implements FileFilter {

	/**
	 * All files with ending .old are fitered out.
	 * Motivation: each "ant update" run backs up changed config files with .old extensions.
	 * These files were read unexpectedly in addition to the current config files.
	 */

	/* (non-Javadoc)
	 * @see java.io.FileFilter#accept(java.io.File)
	 */
	@Override
	public boolean accept(File inFile) {
		if(inFile.getName().matches(".+\\.old$"))
			return false;
		return true;
	}

}
