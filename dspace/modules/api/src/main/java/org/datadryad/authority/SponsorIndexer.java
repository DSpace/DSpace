package org.datadryad.authority;

import org.datadryad.api.DryadOrganizationConcept;
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
    private static Boolean sponsors_cached = false;

    @Override
    public void init(Context context) {
        super.init(context);
        if (!sponsors_cached) {
            try {
                Concept[] concepts = Concept.findAll(context, AuthorityObject.ID);
                for (Concept concept : concepts) {
                    DryadOrganizationConcept organizationConcept = DryadOrganizationConcept.getOrganizationConceptMatchingConceptID(context, concept.getID());
                    if (organizationConcept.getSubscriptionPaid()) {
                        authorities.addAll(createAuthorityValues(organizationConcept));
                    }
                }
            } catch (SQLException e) {

            }
            sponsors_cached = true;
        }
    }

    @Override
    public String getSource() {
        return "SPONSORS";
    }

    @Override
    public String getField() {
        return "dryad_sponsor";
    }

}



