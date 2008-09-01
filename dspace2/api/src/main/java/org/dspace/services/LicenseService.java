/**
 * $Id$
 * $URL$
 * LicenseService.java - Dspace - Sep 1, 2008 5:29:10 PM - azeckoski
 **************************************************************************
 * Copyright (c) 2008 Aaron Zeckoski
 * Licensed under the Apache License, Version 2.0
 * 
 * A copy of the Apache License has been included in this 
 * distribution and is available at: http://www.apache.org/licenses/LICENSE-2.0.txt
 *
 * Aaron Zeckoski (azeckoski @ gmail.com) (aaronz @ vt.edu) (aaron @ caret.cam.ac.uk)
 */

package org.dspace.services;

import java.util.Locale;


/**
 * A service for setting and getting the licenses used in this instance of dSpace<br/>
 * FIXME I think these should all be i18n compliant and should require {@link Locale}
 * 
 * @author Aaron Zeckoski (azeckoski @ gmail.com)
 */
public interface LicenseService {

    /**
     * Gets the default submission license for this dspace instance
     * @return the current default submission license
     */
    public String getDefaultSubmissionLicense();

    /**
     * Sets the default  submission license for this dspace instance
     * @param licenseText the full license text
     */
    public void setDefaultSubmissionLicense(String licenseText);

    /**
     * Get the license text of any license file in the system by the file name
     * @param licenseFileName the full name of the license file
     * @return the text of the license file OR null if not found
     */
    public String getLicenseText(String licenseFileName);

}
