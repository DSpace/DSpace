/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.submit.factory.impl;

import static org.dspace.app.rest.submit.factory.impl.COARNotifyServiceUtils.extractIndex;
import static org.dspace.app.rest.submit.factory.impl.COARNotifyServiceUtils.extractPattern;

import java.util.List;
import javax.servlet.http.HttpServletRequest;

import org.dspace.app.ldn.NotifyPatternToTrigger;
import org.dspace.app.ldn.service.NotifyPatternToTriggerService;
import org.dspace.app.ldn.service.NotifyService;
import org.dspace.app.rest.exception.DSpaceBadRequestException;
import org.dspace.content.InProgressSubmission;
import org.dspace.content.Item;
import org.dspace.core.Context;
import org.springframework.beans.factory.annotation.Autowired;


/**
 * Submission "remove" PATCH operation
 *
 * To remove the COAR Notify Service of workspace item.
 *
 * Example: <code>
 * curl -X PATCH http://${dspace.server.url}/api/submission/workspaceitems/31599 -H "Content-Type:
 * application/json" -d '[{ "op": "remove", "path": "/sections/coarnotify/review/0"}]'
 * </code>
 */
public class COARNotifyServiceRemovePatchOperation extends RemovePatchOperation<String> {

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
    void remove(Context context, HttpServletRequest currentRequest, InProgressSubmission source, String path,
            Object value) throws Exception {

        Item item = source.getItem();

        String pattern = extractPattern(path);
        int index = extractIndex(path);

        List<NotifyPatternToTrigger> notifyPatterns =
            notifyPatternToTriggerService.findByItemAndPattern(context, item, pattern);

        if (index >= notifyPatterns.size()) {
            throw new DSpaceBadRequestException("the provided index[" + index + "] is out of the rang");
        }

        notifyPatternToTriggerService.delete(context, notifyPatterns.get(index));
    }

}
