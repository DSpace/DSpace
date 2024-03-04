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

import org.apache.logging.log4j.Logger;
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
    private static final Logger log = org.apache.logging.log4j.LogManager.getLogger(QAEventSecurityServiceImpl.class);

    private Map<String, QASecurity> qaSecurityConfiguration;

    public void setQaSecurityConfiguration(Map<String, QASecurity> qaSecurityConfiguration) {
        this.qaSecurityConfiguration = qaSecurityConfiguration;
    }

    public void setDefaultSecurity(QASecurity defaultSecurity) {
        this.defaultSecurity = defaultSecurity;
    }

    @Override
    public Optional<String> generateQAEventFilterQuery(Context context, EPerson user, String qaSource) {
        QASecurity qaSecurity = getQASecurity(qaSource);
        return qaSecurity.generateFilterQuery(context, user);
    }

    private QASecurity getQASecurity(String qaSource) {
        return qaSecurityConfiguration.getOrDefault(qaSource, defaultSecurity);
    }

    @Override
    public boolean canSeeEvent(Context context, EPerson user, QAEvent qaEvent) {
        String source = qaEvent.getSource();
        QASecurity qaSecurity = getQASecurity(source);
        return qaSecurity.canSeeQASource(context, user) && qaSecurity.canSeeQAEvent(context, user, qaEvent);
    }

    @Override
    public boolean canSeeSource(Context context, EPerson user, String qaSource) {
        QASecurity qaSecurity = getQASecurity(qaSource);
        return qaSecurity.canSeeQASource(context, user);
    }

}