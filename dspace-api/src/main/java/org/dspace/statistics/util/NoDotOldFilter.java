/**
 * 
 */
package org.dspace.statistics.util;

import java.io.File;
import java.io.FileFilter;

/**
 * @author uhahn
 *
 */
public class NoDotOldFilter implements FileFilter {

	/**
	 * all files with ending .old are fitered out
	 * motivation: each "ant update" run backs up changed config files with .old extensions
	 * these files were read unexpectedly in addition to the current config files 
	 */

	/* (non-Javadoc)
	 * @see java.io.FileFilter#accept(java.io.File)
	 */
	@Override
	public boolean accept(File inFile) {
		if(inFile.getName().matches(".*\\.old$"))
			return false;
		return true;
	}

}
