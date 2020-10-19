/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.builder;

import java.io.IOException;
import java.sql.SQLException;

import org.apache.log4j.Logger;
import org.dspace.app.orcid.OrcidHistory;
import org.dspace.app.orcid.service.OrcidHistoryService;
import org.dspace.authorize.AuthorizeException;
import org.dspace.content.Item;
import org.dspace.core.Context;

/**
 * Builder to construct OrcidHistory objects
 *
 * @author Mykhaylo Boychuk (4science)
 */
public class OrcidHistoryBuilder extends  AbstractBuilder<OrcidHistory, OrcidHistoryService> {

    private static final Logger log = Logger.getLogger(OrcidHistoryBuilder.class);

    private OrcidHistory orcidHistory;

    protected OrcidHistoryBuilder(Context context) {
        super(context);
    }

    @Override
    protected OrcidHistoryService getService() {
        return orcidHistoryService;
    }

    @Override
    public void cleanup() throws Exception {
        delete(orcidHistory);
    }

    public static OrcidHistoryBuilder createOrcidHistory(Context context, Item owner, Item entity) {
        OrcidHistoryBuilder builder = new OrcidHistoryBuilder(context);
        return builder.create(context, owner, entity);
    }

    private OrcidHistoryBuilder create(Context context, Item owner, Item entity) {
        try {
            this.context = context;
            this.orcidHistory = getService().create(context, owner, entity);
        } catch (Exception e) {
            log.error("Error in OrcidHistoryBuilder.create(..), error: ", e);
        }
        return this;
    }

    @Override
    public OrcidHistory build() throws SQLException, AuthorizeException {
        try {
            getService().update(context, orcidHistory);
            context.dispatchEvents();

            indexingService.commit();
        } catch (Exception e) {
            log.error("Error in OrcidHistoryBuilder.build(), error: ", e);
        }
        return orcidHistory;
    }

    @Override
    public void delete(Context c, OrcidHistory orcidHistory) throws Exception {
        if (orcidHistory != null) {
            getService().delete(c, orcidHistory);
        }
    }

    /**
     * Delete the Test OrcidHistory referred to by the given ID
     * 
     * @param id                Integer of Test OrcidHistory to delete
     * @throws SQLException
     * @throws IOException
     */
    public static void deleteOrcidHistory(Integer id) throws SQLException, IOException {
        if (id == null) {
            return;
        }

        try (Context c = new Context()) {
            c.turnOffAuthorisationSystem();
            OrcidHistory orcidHistory = orcidHistoryService.find(c, id);
            if (orcidHistory != null) {
                try {
                    orcidHistoryService.delete(c, orcidHistory);
                } catch (AuthorizeException e) {
                    throw new RuntimeException(e);
                }
            }
            c.complete();
        }
    }

    public void delete(OrcidHistory orcidHistory) throws Exception {
        try (Context c = new Context()) {
            c.turnOffAuthorisationSystem();
            OrcidHistory attachedTab = c.reloadEntity(orcidHistory);
            if (attachedTab != null) {
                getService().delete(c, attachedTab);
            }
            c.complete();
        }
        indexingService.commit();
    }

    public OrcidHistoryBuilder withResponseMessage(String responseMessage) throws SQLException {
        orcidHistory.setResponseMessage(responseMessage);
        return this;
    }

    public OrcidHistoryBuilder withPutCode(String putCode) throws SQLException {
        orcidHistory.setPutCode(putCode);
        return this;
    }

    public OrcidHistoryBuilder withStatus(Integer status) throws SQLException {
        orcidHistory.setStatus(status);
        return this;
    }
}
