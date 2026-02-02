/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.qaevent.service;

import org.dspace.content.QAEvent;
import org.dspace.core.Context;

/**
 * Service that handle the actions that can be done related to an
 * {@link QAEvent}.
 * 
 * @author Andrea Bollini (andrea.bollini at 4science.it)
 *
 */
public interface QAEventActionService {

    /**
     * Accept the given event.
     *
     * @param context the DSpace context
     * @param qaevent the event to be accepted
     */
    public void accept(Context context, QAEvent qaevent);

    /**
     * Discard the given event.
     *
     * @param context the DSpace context
     * @param qaevent the event to be discarded
     */
    public void discard(Context context, QAEvent qaevent);

    /**
     * Reject the given event.
     *
     * @param context the DSpace context
     * @param qaevent the event to be rejected
     */
    public void reject(Context context, QAEvent qaevent);
}
