/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.submit.factory.impl;

import static org.dspace.app.rest.submit.factory.impl.COARNotifyServiceUtils.extractPattern;

import java.sql.SQLException;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import javax.servlet.http.HttpServletRequest;

import org.dspace.app.ldn.NotifyPatternToTrigger;
import org.dspace.app.ldn.NotifyServiceEntity;
import org.dspace.app.ldn.service.NotifyPatternToTriggerService;
import org.dspace.app.ldn.service.NotifyService;
import org.dspace.app.rest.exception.DSpaceBadRequestException;
import org.dspace.app.rest.model.patch.LateObjectEvaluator;
import org.dspace.content.InProgressSubmission;
import org.dspace.content.Item;
import org.dspace.core.Context;
import org.springframework.beans.factory.annotation.Autowired;


/**
 * Submission "add" PATCH operation
 *
 * To add the COAR Notify Service of workspace item.
 *
 * Example: <code>
 * curl -X PATCH http://${dspace.server.url}/api/submission/workspaceitems/31599 -H "Content-Type:
 * application/json" -d '[{ "op": "add", "path": "/sections/coarnotify/review/-"}, "value": ["1","2"]'
 * </code>
 */
public class COARNotifyServiceAddPatchOperation extends AddPatchOperation<String> {

    @Autowired
    private NotifyPatternToTriggerService notifyPatternToTriggerService;

    @Autowired
    private NotifyService notifyService;

    @Override
    protected Class<String[]> getArrayClassForEvaluation() {
        return String[].class;
    }

    @Override
    protected Class<String> getClassForEvaluation() {
        return String.class;
    }

    @Override
    void add(Context context, HttpServletRequest currentRequest, InProgressSubmission source, String path,
            Object value) throws Exception {

        Set<String> values = evaluateArrayObject((LateObjectEvaluator) value)
            .stream()
            .collect(Collectors.toSet());

        List<NotifyServiceEntity> services =
            values.stream()
                  .map(id ->
                      findService(context, Integer.parseInt(id)))
                  .collect(Collectors.toList());

        services.forEach(service ->
            createNotifyPattern(context, source.getItem(), service, extractPattern(path)));
    }

    private NotifyServiceEntity findService(Context context, int serviceId) {
        try {
            NotifyServiceEntity service = notifyService.find(context, serviceId);
            if (service == null) {
                throw new DSpaceBadRequestException("no service found for the provided value: " + serviceId + "");
            }
            return service;
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    private void createNotifyPattern(Context context, Item item, NotifyServiceEntity service, String pattern) {
        try {
            NotifyPatternToTrigger notifyPatternToTrigger = notifyPatternToTriggerService.create(context);
            notifyPatternToTrigger.setItem(item);
            notifyPatternToTrigger.setNotifyService(service);
            notifyPatternToTrigger.setPattern(pattern);
            notifyPatternToTriggerService.update(context, notifyPatternToTrigger);
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

}
