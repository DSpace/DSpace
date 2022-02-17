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

/**
 * Service that handle the actions that can be done related to an
 * {@link NBEvent}.
 * 
 * @author Andrea Bollini (andrea.bollini at 4science.it)
 *
 */
public interface NBEventActionService {

    /**
     * Accept the given event.
     *
     * @param context the DSpace context
     * @param nbevent the event to be accepted
     */
    public void accept(Context context, NBEvent nbevent);

    /**
     * Discard the given event.
     *
     * @param context the DSpace context
     * @param nbevent the event to be discarded
     */
    public void discard(Context context, NBEvent nbevent);

    /**
     * Reject the given event.
     *
     * @param context the DSpace context
     * @param nbevent the event to be rejected
     */
    public void reject(Context context, NBEvent nbevent);
}
