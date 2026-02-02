/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.qaevent.service;

import java.util.Optional;

import org.dspace.content.QAEvent;
import org.dspace.core.Context;
import org.dspace.eperson.EPerson;

/**
 * Interface to limit the visibility of {@link QAEvent} to specific users.
 *
 * @author Andrea Bollini (andrea.bollini at 4science.com)
 *
 */
public interface QAEventSecurityService {

    /**
     * Check if the specified user can see a specific QASource
     * @param context     the context
     * @param user        the eperson to consider
     * @param sourceName  the source name
     * @return          <code>true</code> if the specified user can eventually see events in the QASource
     */
    boolean canSeeSource(Context context, EPerson user, String sourceName);

    /**
     * Check if the specified user can see a specific QAEvent. It is expected that a QAEvent in a not visible QASource
     * cannot be accessed. So implementation of this method should enforce this rule.
     * 
     * @param context   the context
     * @param user      the eperson to consider
     * @param qaEvent   the qaevent to check
     * @return <code>true</code> if the specified user can see the specified event
     */
    boolean canSeeEvent(Context context, EPerson user, QAEvent qaEvent);

    /**
     * Generate a query to restrict the qa events returned by other search/find method to the only ones visible to the
     * specified user
     * 
     * @param context     the context
     * @param user        the eperson to consider
     * @param sourceName  the source name
     * @return the solr filter query
     */
    public Optional<String> generateQAEventFilterQuery(Context context, EPerson user, String sourceName);

}
