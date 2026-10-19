/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.qaevent.action;

import org.dspace.qaevent.QualityAssuranceAction;
import org.dspace.qaevent.service.dto.OpenaireMessageDTO;
import org.dspace.qaevent.service.dto.QAMessageDTO;

/**
 * Implementation of {@link QualityAssuranceAction} that add a simple metadata to the given
 * item.
 *
 * @author Andrea Bollini (andrea.bollini at 4science.it)
 *
 */
public class QAOpenaireSimpleMetadataAction extends ASimpleMetadataAction {

    public String extractMetadataValue(QAMessageDTO message) {
        return ((OpenaireMessageDTO) message).getAbstracts();
    }
}
