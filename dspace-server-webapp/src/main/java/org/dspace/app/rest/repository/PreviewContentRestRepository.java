/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.repository;

import java.sql.SQLException;
import java.util.List;
import java.util.Objects;

import org.apache.logging.log4j.Logger;
import org.dspace.app.rest.model.PreviewContentRest;
import org.dspace.content.PreviewContent;
import org.dspace.content.service.PreviewContentService;
import org.dspace.core.Context;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;

@Component(PreviewContentRest.CATEGORY + "." + PreviewContentRest.NAME)
public class PreviewContentRestRepository extends DSpaceRestRepository<PreviewContentRest, Integer> {

    private static Logger log = org.apache.logging.log4j.LogManager.getLogger(PreviewContentRestRepository.class);

    @Autowired
    PreviewContentService previewContentService;

    @Override
    public PreviewContentRest findOne(Context context, Integer integer) {
        PreviewContent previewContent;
        try {
            previewContent = previewContentService.find(context, integer);
        } catch (SQLException e) {
            String msg = "Database error occurred while finding preview content " +
                    "for ID = " + integer + " Error msg: " + e.getMessage();
            log.error(msg, e);
            throw new RuntimeException(msg, e);
        }
        if (Objects.isNull(previewContent)) {
            return null;
        }
        return converter.toRest(previewContent, utils.obtainProjection());
    }

    @Override
    public Page<PreviewContentRest> findAll(Context context, Pageable pageable) {
        try {
            List<PreviewContent> previewContentList = previewContentService.findAll(context);
            return converter.toRestPage(previewContentList, pageable, utils.obtainProjection());
        } catch (SQLException e) {
            String msg = "Database error occurred while finding all preview contents. Error msg: " + e.getMessage();
            log.error(msg, e);
            throw new RuntimeException(msg, e);
        }
    }

    @Override
    public Class<PreviewContentRest> getDomainClass() {
        return PreviewContentRest.class;
    }
}
