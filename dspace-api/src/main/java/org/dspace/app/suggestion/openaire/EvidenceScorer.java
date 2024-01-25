/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.suggestion.openaire;

import org.dspace.app.suggestion.SuggestionEvidence;
import org.dspace.content.Item;
import org.dspace.external.model.ExternalDataObject;

/**
 * Interface used in {@see org.dspace.app.suggestion.oaire.PublicationApproverServiceImpl}
 * to construct filtering pipeline.
 * 
 * For each EvidenceScorer, the service call computeEvidence method.
 * 
 * @author Andrea Bollini (andrea.bollini at 4science dot it)
 * @author Pasquale Cavallo (pasquale.cavallo at 4science dot it)
 *
 */
public interface EvidenceScorer {

    /**
     * Method to compute the suggestion evidence of an ImportRecord, a null evidence
     * would lead the record to be discarded.
     * 
     * @param researcher   DSpace item
     * @param importRecord the record to evaluate
     * @return the generated suggestion evidence or null if the record should be
     *         discarded
     */
    public SuggestionEvidence computeEvidence(Item researcher, ExternalDataObject importRecord);

}
