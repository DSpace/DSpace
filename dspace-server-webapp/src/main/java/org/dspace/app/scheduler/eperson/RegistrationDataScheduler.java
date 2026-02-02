/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.scheduler.eperson;

import java.sql.SQLException;

import org.dspace.core.Context;
import org.dspace.eperson.RegistrationData;
import org.dspace.eperson.service.RegistrationDataService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

/**
 * Contains all the schedulable task related to {@link RegistrationData} entities.
 * Can be enabled via the configuration property {@code eperson.registration-data.scheduler.enabled}
 *
 * @author Vincenzo Mecca (vins01-4science - vincenzo.mecca at 4science.com)
 **/
@Service
@ConditionalOnProperty(prefix = "eperson.registration-data.scheduler", name = "enabled", havingValue = "true")
public class RegistrationDataScheduler {

    private static final Logger log = LoggerFactory.getLogger(RegistrationDataScheduler.class);

    @Autowired
    private RegistrationDataService registrationDataService;

    /**
     * Deletes expired {@link RegistrationData}.
     * This task is scheduled to be run by the cron expression defined in the configuration file.
     *
     */
    @Scheduled(cron = "${eperson.registration-data.scheduler.expired-registration-data.cron:-}")
    protected void deleteExpiredRegistrationData() throws SQLException {
        Context context = new Context();
        context.turnOffAuthorisationSystem();
        try {

            registrationDataService.deleteExpiredRegistrations(context);

            context.restoreAuthSystemState();
            context.complete();
        } catch (Exception e) {
            context.abort();
            log.error("Failed to delete expired registrations", e);
            throw e;
        }
    }


}
