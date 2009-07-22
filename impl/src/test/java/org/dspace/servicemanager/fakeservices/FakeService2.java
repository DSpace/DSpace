/**
 * $Id: FakeService2.java 3299 2008-11-18 14:22:36Z azeckoski $
 * $URL: https://scm.dspace.org/svn/repo/dspace2/core/trunk/impl/src/test/java/org/dspace/servicemanager/fakeservices/FakeService2.java $
 * FakeService2.java - DSpace2 - Oct 27, 2008 12:56:26 PM - azeckoski
 **************************************************************************
 * Copyright (c) 2008 Aaron Zeckoski
 * Licensed under the Apache License, Version 2.0
 * 
 * A copy of the Apache License has been included in this 
 * distribution and is available at: http://www.apache.org/licenses/LICENSE-2.0.txt
 *
 * Aaron Zeckoski (azeckoski @ gmail.com) (aaronz @ vt.edu) (aaron @ caret.cam.ac.uk)
 */

package org.dspace.servicemanager.fakeservices;

import java.io.Serializable;

import org.dspace.kernel.mixins.InitializedService;


/**
 * Simple fake service 2
 * 
 * @author Aaron Zeckoski (azeckoski @ gmail.com)
 */
public class FakeService2 implements InitializedService, Comparable<FakeService2>, Serializable {
	private static final long serialVersionUID = 1L;

    public String data = "data";
    public String getData() {
        return data;
    }
    public void setData(String data) {
        this.data = data;
    }

    /* (non-Javadoc)
     * @see org.dspace.kernel.mixins.InitializedService#init()
     */
    public void init() {
        data = "initData";
    }

    public int compareTo(FakeService2 o) {
        return data.compareTo(o.data);
    }

}
