/**
 * $Id$
 * $URL$
 * *************************************************************************
 * Copyright (c) 2002-2009, DuraSpace.  All rights reserved
 * Licensed under the DuraSpace License.
 *
 * A copy of the DuraSpace License has been included in this
 * distribution and is available at: http://scm.dspace.org/svn/repo/licenses/LICENSE.txt
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
