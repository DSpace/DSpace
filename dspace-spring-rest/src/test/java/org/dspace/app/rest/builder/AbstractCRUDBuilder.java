package org.dspace.app.rest.builder;

import org.apache.log4j.Logger;
import org.dspace.authorize.factory.AuthorizeServiceFactory;
import org.dspace.authorize.service.AuthorizeService;
import org.dspace.content.factory.ContentServiceFactory;
import org.dspace.content.service.BitstreamFormatService;
import org.dspace.core.Context;
import org.dspace.core.ReloadableEntity;
import org.dspace.discovery.IndexingService;
import org.dspace.service.DSpaceCRUDService;
import org.dspace.services.factory.DSpaceServicesFactory;

import java.util.LinkedList;
import java.util.List;

/**
 * Created by jonas - jonas@atmire.com on 04/12/17.
 */
public abstract class AbstractCRUDBuilder<T extends ReloadableEntity> {

    static AuthorizeService authorizeService;
    static IndexingService indexingService;
    static BitstreamFormatService bitstreamFormatService;

    private static List<AbstractCRUDBuilder> builders = new LinkedList<>();
    /** log4j category */
    private static final Logger log = Logger.getLogger(AbstractBuilder.class);

    protected Context context;


    protected AbstractCRUDBuilder(Context context){
        this.context = context;
        builders.add(this);
    }

    public static void init() {
        authorizeService = AuthorizeServiceFactory.getInstance().getAuthorizeService();
        indexingService = DSpaceServicesFactory.getInstance().getServiceManager().getServiceByName(IndexingService.class.getName(),IndexingService.class);
        bitstreamFormatService = ContentServiceFactory.getInstance().getBitstreamFormatService();

    }

    public static void destroy(){
        authorizeService = null;
        indexingService= null;
        bitstreamFormatService=null;
    }


    public static void cleanupObjects() throws Exception {
        for (AbstractCRUDBuilder builder : builders) {

            builder.cleanup();
        }
    }
    protected abstract void cleanup() throws Exception;

    protected abstract DSpaceCRUDService<T> getCRUDService();

    public abstract T build();

    public void delete(T dso) throws Exception {

        try(Context c = new Context()) {
            c.turnOffAuthorisationSystem();
            T attachedDso = c.reloadEntity(dso);
            if (attachedDso != null) {
                getCRUDService().delete(c, attachedDso);
            }
            c.complete();
        }

        indexingService.commit();
    }
}
