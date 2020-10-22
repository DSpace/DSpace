/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.suggestion.oaire;

import java.util.List;

import org.dspace.content.Item;
import org.dspace.importer.external.datamodel.ImportRecord;

public class OAIREPublicationApproverServiceImpl {

    private List<AuthorNamesApprover> pipeline;

    public void setPipeline(List<AuthorNamesApprover> pipeline) {
        this.pipeline = pipeline;
    }

    public List<ImportRecord> approve(Item researcher, List<ImportRecord> importRecord) {
        List<ImportRecord> filtered = importRecord;
        for (AuthorNamesApprover authorNameApprover : pipeline) {
            filtered = authorNameApprover.filter(researcher, importRecord);
        }
        return filtered;
    }
}
