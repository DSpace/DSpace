/**
 * Copyright (c) 2002-2010, DuraSpace.  All rights reserved
 * Licensed under the DuraSpace License.
 *
 * A copy of the DuraSpace License has been included in this
 * distribution and is available at: http://www.dspace.org/license
 */
package org.dspace.activators;

import org.dspace.kernel.Activator;
import org.dspace.kernel.ServiceManager;

/**
 * Created by IntelliJ IDEA.
 * User: mdiggory
 * Date: May 13, 2010
 * Time: 7:59:39 AM
 * To change this template use File | Settings | File Templates.
 */
public class FakeActivator implements Activator {

    public String status = "Not Started";

    public void start(ServiceManager serviceManager) {
        status = "Started";
    }

    public void stop(ServiceManager serviceManager) {
        status = "Stopped";
    }
}
