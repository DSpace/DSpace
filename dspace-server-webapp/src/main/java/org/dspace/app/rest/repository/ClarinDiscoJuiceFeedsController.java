/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.repository;

import static org.springframework.web.bind.annotation.RequestMethod.GET;

import java.io.IOException;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.Logger;
import org.dspace.app.rest.ClarinDiscoJuiceFeedsDownloadService;
import org.dspace.app.rest.ClarinDiscoJuiceFeedsUpdateScheduler;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * The REST Controller for retrieving DiscoJuiceFeeds.
 *
 * @author Milan Majchrak (milan.majchrak at dataquest.sk)
 */
@RestController
@RequestMapping("/api/discojuice/feeds")
public class ClarinDiscoJuiceFeedsController {

    public static final String APPLICATION_JAVASCRIPT_UTF8 = "application/javascript;charset=utf-8";


    protected static Logger log = org.apache.logging.log4j.LogManager.getLogger(ClarinDiscoJuiceFeedsController.class);

    @Autowired
    ClarinDiscoJuiceFeedsDownloadService clarinDiscoJuiceFeedsDownloadService;

    @Autowired
    ClarinDiscoJuiceFeedsUpdateScheduler clarinDiscoJuiceFeedsUpdateScheduler;

    @RequestMapping(method = GET, produces = APPLICATION_JAVASCRIPT_UTF8)
    @PreAuthorize("permitAll()")
    public ResponseEntity getDiscojuiceFeeds(@RequestParam(value = "callback", required = false) String callback,
                                             HttpServletResponse response) throws IOException {
        // Download feeds
        String feedsContent = clarinDiscoJuiceFeedsUpdateScheduler.getFeedsContent();
        if (StringUtils.isBlank(feedsContent)) {
            response.sendError(HttpServletResponse.SC_NO_CONTENT);
            return null;
        }

        // If callback is not null wrap the feedsContent to the callback string.
        String responseString = StringUtils.isBlank(callback) ? feedsContent : callback + "(" + feedsContent + ")";
        return ResponseEntity.ok()
                .body(responseString);
    }
}
