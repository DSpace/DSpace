/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.nbevent;

import org.dspace.content.NBEvent;
import org.dspace.core.Context;

public interface NBEventActionService {
    public void accept(Context context, NBEvent nbevent);

    public void discard(Context context, NBEvent nbevent);

    public void reject(Context context, NBEvent nbevent);
}
