/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.submit.factory.impl;

import java.util.List;
import java.util.Set;

import jakarta.servlet.http.HttpServletRequest;
import org.dspace.app.ldn.NotifyPatternToTrigger;
import org.dspace.app.ldn.NotifyServiceEntity;
import org.dspace.app.ldn.service.NotifyPatternToTriggerService;
import org.dspace.app.ldn.service.NotifyService;
import org.dspace.app.rest.exception.DSpaceBadRequestException;
import org.dspace.content.InProgressSubmission;
import org.dspace.core.Context;
import org.dspace.utils.DSpace;
import org.springframework.beans.factory.annotation.Autowired;


/**
 * Submission "replace" PATCH operation
 *
 * To replace the COAR Notify Service of workspace item.
 *
 * Example: <code>
 * curl -X PATCH http://${dspace.server.url}/api/submission/workspaceitems/31599 -H "Content-Type:
 * application/json" -d '[{ "op": "replace", "path": "/sections/coarnotify/review/0"}, "value": "10"]'
 * </code>
 */
public class NotifyServiceReplacePatchOperation extends ReplacePatchOperation<Integer> {

    @Autowired
    private NotifyPatternToTriggerService notifyPatternToTriggerService;

    @Autowired
    private NotifyService notifyService;

    private NotifySubmissionService coarNotifySubmissionService = new DSpace().getServiceManager()
        .getServiceByName("coarNotifySubmissionService", NotifySubmissionService.class);

    @Override
    protected Class<Integer[]> getArrayClassForEvaluation() {
        return Integer[].class;
    }

    @Override
    protected Class<Integer> getClassForEvaluation() {
        return Integer.class;
    }

    @Override
    void replace(Context context, HttpServletRequest currentRequest, InProgressSubmission source, String path,
            Object value) throws Exception {

        int index = coarNotifySubmissionService.extractIndex(path);
        String pattern = coarNotifySubmissionService.extractPattern(path);

        List<NotifyPatternToTrigger> notifyPatterns =
            notifyPatternToTriggerService.findByItemAndPattern(context, source.getItem(), pattern);

        if (index >= notifyPatterns.size()) {
            throw new DSpaceBadRequestException("the provided index[" + index + "] is out of the rang");
        }

        NotifyServiceEntity notifyServiceEntity = notifyService.find(context, Integer.parseInt(value.toString()));
        if (notifyServiceEntity == null) {
            throw new DSpaceBadRequestException("no service found for the provided value: " + value + "");
        }

        coarNotifySubmissionService.checkCompatibilityWithPattern(context,
            pattern, Set.of(notifyServiceEntity.getID()));

        NotifyPatternToTrigger notifyPatternToTriggerOld = notifyPatterns.get(index);
        notifyPatternToTriggerOld.setNotifyService(notifyServiceEntity);

        notifyPatternToTriggerService.update(context, notifyPatternToTriggerOld);
    }

}
