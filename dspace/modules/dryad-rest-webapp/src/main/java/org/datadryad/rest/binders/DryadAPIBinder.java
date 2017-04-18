package org.datadryad.rest.binders;

import org.apache.log4j.Logger;
import org.datadryad.rest.handler.AbstractHandlerGroup;
import org.datadryad.rest.handler.ManuscriptHandlerGroup;
import org.datadryad.rest.storage.*;
import org.datadryad.rest.storage.rdbms.*;
import org.glassfish.hk2.utilities.binding.AbstractBinder;

/**
 * Created by daisie on 4/14/17.
 */
public class DryadAPIBinder extends AbstractBinder {
    private final static Logger log = Logger.getLogger(DryadAPIBinder.class);

    @Override
    protected void configure() {
        bind(JournalConceptDatabaseStorageImpl.class).to(AbstractOrganizationConceptStorage.class);
        bind(ManuscriptDatabaseStorageImpl.class).to(AbstractManuscriptStorage.class);
        bind(OAuthTokenDatabaseStorageImpl.class).to(OAuthTokenStorageInterface.class);
        bind(AuthorizationDatabaseStorageImpl.class).to(AuthorizationStorageInterface.class);
        bind(PackageDatabaseStorageImpl.class).to(AbstractPackageStorage.class);
        bind(ManuscriptHandlerGroup.class).to(AbstractHandlerGroup.class);
    }
}
