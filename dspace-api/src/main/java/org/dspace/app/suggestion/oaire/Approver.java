/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.suggestion.oaire;

import org.dspace.app.suggestion.SuggestionEvidence;
import org.dspace.content.Item;
import org.dspace.importer.external.datamodel.ImportRecord;

/**
 * Interface used in {@see org.dspace.app.suggestion.oaire.OAIREPublicationApproverServiceImpl}
 * to construct filtering pipeline.
 * 
 * For each Approver, the service call filter method.
 * 
 * @author Pasquale Cavallo (pasquale.cavallo at 4science dot it)
 *
 */
public interface Approver {

    /**
     * Filter method which return a subset of ImportRecord starting from the existing list.
     * 
     * @param researcher DSpace item
     * @param importRecord the record to evaluate
     * @return the generated suggestion evidence or null if the record should be discarded
     */
    public SuggestionEvidence filter(Item researcher, ImportRecord importRecords);

}
