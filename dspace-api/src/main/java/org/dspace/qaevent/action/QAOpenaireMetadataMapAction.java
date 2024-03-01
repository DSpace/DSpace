/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.qaevent.action;

import org.dspace.qaevent.service.dto.OpenaireMessageDTO;
import org.dspace.qaevent.service.dto.QAMessageDTO;

/**
 * Openaire Implementation {@link AMetadataMapAction}
 * 
 * @author Francesco Bacchelli (francesco.bacchelli at 4science.it)
 *
 */
public class QAOpenaireMetadataMapAction extends AMetadataMapAction {

    @Override
    public String extractMetadataType(QAMessageDTO message) {
        return ((OpenaireMessageDTO)message).getType();
    }

    @Override
    public String extractMetadataValue(QAMessageDTO message) {
        return ((OpenaireMessageDTO)message).getValue();
    }

}
