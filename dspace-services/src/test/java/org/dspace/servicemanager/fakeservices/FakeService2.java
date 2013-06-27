/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
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
