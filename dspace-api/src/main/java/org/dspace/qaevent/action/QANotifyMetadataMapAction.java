/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.qaevent.action;

import org.dspace.qaevent.service.dto.NotifyMessageDTO;
import org.dspace.qaevent.service.dto.QAMessageDTO;

/**
 * Notify Implementation {@link AMetadataMapAction}
 * 
 * @author Francesco Bacchelli (francesco.bacchelli at 4science.it)
 *
 */
public class QANotifyMetadataMapAction extends AMetadataMapAction {

    @Override
    public String extractMetadataType(QAMessageDTO message) {
        return ((NotifyMessageDTO)message).getRelationship();
    }

    @Override
    public String extractMetadataValue(QAMessageDTO message) {
        return ((NotifyMessageDTO)message).getHref();
    }

}
