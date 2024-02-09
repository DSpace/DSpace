/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest;

import static org.apache.commons.lang.StringUtils.isNotBlank;

import org.apache.logging.log4j.Logger;
import org.dspace.services.ConfigurationService;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * The Cron Job for maintaining the DiscoFeeds. The DiscoFeeds are stored in the `feedsContent`.
 * DiscoFeeds are downloading in intervals because it's a big JSON file.
 *
 * @author Milan Majchrak (milan.majchrak at dataquest.sk)
 */
@Component
public class ClarinDiscoJuiceFeedsUpdateScheduler implements InitializingBean {

    protected static Logger log =
            org.apache.logging.log4j.LogManager.getLogger(ClarinDiscoJuiceFeedsUpdateScheduler.class);
    private static String feedsContent;

    @Autowired
    ClarinDiscoJuiceFeedsDownloadService clarinDiscoJuiceFeedsDownloadService;

    @Autowired
    private ConfigurationService configurationService;

    /**
     * Instead of static {} method.
     */
    @Override
    public void afterPropertiesSet() throws Exception {
        // Initial load of the feeds.
        cronJobSch();
    }

    /**
     * The DiscoFeeds are downloading every hour.
     */
    @Scheduled(cron = "${discojuice.refresh:-}")
    public void cronJobSch() {
        // 2024/02 - unless explicitly turned on, do not use discofeed
        boolean isAllowed = configurationService.getBooleanProperty("shibboleth.discofeed.allowed", false);
        if (!isAllowed) {
            return;
        }

        log.debug("CRON Job - going to download the discovery feeds.");
        String newFeedsContent = clarinDiscoJuiceFeedsDownloadService.createFeedsContent();
        if (isNotBlank(newFeedsContent)) {
            feedsContent = newFeedsContent;
        } else {
            log.error("Failed to obtain additional discovery feeds!");
        }
    }

    public String getFeedsContent() {
        return feedsContent;
    }
}
