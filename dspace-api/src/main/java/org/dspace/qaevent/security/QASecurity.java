/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.qaevent.security;

import java.util.Optional;

import org.dspace.content.QAEvent;
import org.dspace.core.Context;
import org.dspace.eperson.EPerson;

/**
 * The QASecurity interface defines methods for implementing security strategies
 * related to Quality Assurance (QA) events. Classes implementing this interface should
 * provide logic to filter and determine visibility of QA events based on the user's permissions.
 * 
 * @author Andrea Bollini (andrea.bollini at 4science.com)
 *
 */
public interface QASecurity {

    /**
     * Return a SOLR queries that can be applied querying the qaevent SOLR core to retrieve only the qaevents visible to
     * the provided user
     * 
     * @param context the DSpace context
     * @param user    the user to consider to restrict the visible qaevents
     * @return        the SOLR filter query to apply
     */
    public Optional<String> generateFilterQuery(Context context, EPerson user);

    /**
     * Return <code>true</code> it the user is potentially allowed to see events in the qasource that adopt this
     * security strategy
     *
     * @param context the DSpace context
     * @param user the user to consider to restrict the visible qaevents
     * @return <code>true</code> if the user can eventually see some qaevents
     */
    public boolean canSeeQASource(Context context, EPerson user);

    /**
     * Return <code>true</code> it the user is potentially allowed to see events in the qasource that adopt this
     * security strategy
     *
     * @param context the DSpace context
     * @param user    the user to consider to restrict the visible qaevents
     * @return        <code>true</code> if the user can see the provided qaEvent
     */
    public boolean canSeeQAEvent(Context context, EPerson user, QAEvent qaEvent);
}
