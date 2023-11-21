/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.qaevent.action;

import org.dspace.qaevent.QualityAssuranceAction;
import org.dspace.qaevent.service.dto.NotifyMessageDTO;
import org.dspace.qaevent.service.dto.QAMessageDTO;

/**
 * Implementation of {@link QualityAssuranceAction} that add a simple metadata to the given
 * item.
 *
 * @author Francesco Bacchelli (francesco.bacchelli at 4science.it)
 *
 */
public class QANotifySimpleMetadataAction extends ASimpleMetadataAction {

    public String extractMetadataValue(QAMessageDTO message) {
        return ((NotifyMessageDTO) message).getHref();
    }
}
