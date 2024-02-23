/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.qaevent.service.impl;

import java.util.Map;
import java.util.Optional;

import org.dspace.content.QAEvent;
import org.dspace.core.Context;
import org.dspace.eperson.EPerson;
import org.dspace.qaevent.security.QASecurity;
import org.dspace.qaevent.service.QAEventSecurityService;

/**
 * Implementation of the security service for QAEvents.
 * This implementation manages a configuration of {@link QASecurity} instances,
 * each responsible for security checks for a specific QA source.
 *
 * @author Mykhaylo Boychuk (mykhaylo.boychuk@4science.com)
 */
public class QAEventSecurityServiceImpl implements QAEventSecurityService {

    /**
     * The default security settings to be used when specific configurations are not available for a QA source.
     */
    private QASecurity defaultSecurity;

    /**
     * A mapping of QA source names to their corresponding QASecurity configurations.
     */
    private Map<String, QASecurity> qaSecurityConfiguration;

    public void setQaSecurityConfiguration(Map<String, QASecurity> qaSecurityConfiguration) {
        this.qaSecurityConfiguration = qaSecurityConfiguration;
    }

    public void setDefaultSecurity(QASecurity defaultSecurity) {
        this.defaultSecurity = defaultSecurity;
    }

    /**
     * Generate a query to restrict the qa events returned by other search/find method to the only ones visible to the
     * specified user
     * 
     * @param context     the context
     * @param user        the eperson to consider
     * @param sourceName  the source name
     * @return            the solr filter query
     */
    @Override
    public Optional<String> generateQAEventFilterQuery(Context context, EPerson user, String sourceName) {
        QASecurity qaSecurity = getQASecurity(sourceName);
        return qaSecurity.generateFilterQuery(context, user);
    }

    /**
     * Retrieves the QASecurity configuration for the specified QA source, or uses the default
     * configuration if not available.
     *
     * @param qaSource The name of the QA source.
     * @return The QASecurity configuration for the specified QA source, or the default configuration if not available.
     */
    private QASecurity getQASecurity(String qaSource) {
        return qaSecurityConfiguration.getOrDefault(qaSource, defaultSecurity);
    }

    /**
     * Determines whether the user is authorized to see the specified QA event.
     * 
     * @param context   the context
     * @param user      the eperson to consider
     * @param qaEvent   the qaevent to check
     * @return <code>true</code> if the specified user can see the specified event
     */
    @Override
    public boolean canSeeEvent(Context context, EPerson user, QAEvent qaEvent) {
        String source = qaEvent.getSource();
        QASecurity qaSecurity = getQASecurity(source);
        return qaSecurity.canSeeQASource(context, user) && qaSecurity.canSeeQAEvent(context, user, qaEvent);
    }

    /**
     * Determines whether the user is authorized to see events from the specified QA source.
     * @param context    The context.
     * @param user       The EPerson to consider
     * @param sourceName The source name
     * 
     * @return True if the user is authorized to see events from the source, false otherwise.
     */
    @Override
    public boolean canSeeSource(Context context, EPerson user, String sourceName) {
        QASecurity qaSecurity = getQASecurity(sourceName);
        return qaSecurity.canSeeQASource(context, user);
    }

}