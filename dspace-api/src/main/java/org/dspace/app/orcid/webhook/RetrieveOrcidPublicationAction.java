/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.orcid.webhook;

import java.io.IOException;

import org.apache.solr.client.solrj.SolrServerException;
import org.dspace.app.suggestion.orcid.OrcidPublicationLoader;
import org.dspace.content.Item;
import org.dspace.core.Context;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;

/**
 * Implementation of {@link OrcidWebhookAction} that retrieve all the
 * publication from ORCID that does not have DSpaceCris as source.
 *
 * @author Luca Giamminonni (luca.giamminonni at 4science.it)
 *
 */
@Order(Ordered.LOWEST_PRECEDENCE)
public class RetrieveOrcidPublicationAction implements OrcidWebhookAction {

    @Autowired
    private OrcidPublicationLoader orcidPublicationLoader;

    @Override
    public void perform(Context context, Item profile, String orcid) {
        try {
            orcidPublicationLoader.importWorks(context, profile, orcid);
        } catch (SolrServerException | IOException e) {
            throw new RuntimeException(e);
        }
    }

}
