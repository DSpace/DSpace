package org.dspace.app.suggestion.oaire;

import java.util.List;

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
     * @param importRecords list of importRecords
     * @return filtered list of importRecords
     */
    public List<ImportRecord> filter(Item researcher, List<ImportRecord> importRecords);

}
