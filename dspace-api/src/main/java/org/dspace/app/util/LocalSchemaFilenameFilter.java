/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.util;

import java.io.File;
import java.io.FilenameFilter;
import java.util.regex.Pattern;

/*
 * This class is used to identify local schema files when running the itemupdate and itemimport tools.
 */

public class LocalSchemaFilenameFilter implements FilenameFilter {

    static Pattern patt = Pattern.compile("^metadata_.*.xml$");
    
    @Override
    public boolean accept(File arg0, String arg1) {
        return patt.matcher(arg1).matches();
    }

}
