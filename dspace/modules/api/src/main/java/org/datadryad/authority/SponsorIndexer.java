package org.datadryad.authority;

import org.datadryad.api.DryadOrganizationConcept;
import org.dspace.authority.AuthorityValue;
import org.dspace.authority.indexer.AuthorityIndexerInterface;
import org.dspace.content.authority.AuthorityObject;
import org.dspace.content.authority.Concept;
import org.dspace.core.Context;

import java.sql.SQLException;

/**
 * Created by IntelliJ IDEA.
 * User: fabio.bolognesi
 * Date: Mar 1, 2011
 * Time: 2:42:07 PM
 * To change this template use File | Settings | File Templates.
 */
public class SponsorIndexer extends JournalConceptIndexer implements AuthorityIndexerInterface {
    {
        SOURCE = "SPONSORS";
        FIELD_NAME = "dryad_sponsor";
    }

    @Override
    public void init() {
        Context context = null;
        try {
            context = new Context();
            Concept[] concepts = Concept.findAll(context, AuthorityObject.ID);
            for (Concept concept : concepts) {
                DryadOrganizationConcept organizationConcept = DryadOrganizationConcept.getOrganizationConceptMatchingConceptID(context, concept.getID());
                if (organizationConcept.getSubscriptionPaid()) {
                    for (AuthorityValue doc : createAuthorityValues(organizationConcept)) {
                        authorities.add(doc);
                    }
                }
            }
            context.complete();
        } catch (SQLException e) {
            if (context != null) {
                context.abort();
            }
        }
    }
}



