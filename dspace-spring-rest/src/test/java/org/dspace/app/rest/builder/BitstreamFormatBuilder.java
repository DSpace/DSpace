package org.dspace.app.rest.builder;

import org.apache.log4j.Logger;
import org.dspace.authorize.AuthorizeException;
import org.dspace.content.BitstreamFormat;
import org.dspace.core.Context;
import org.dspace.discovery.SearchServiceException;
import org.dspace.service.DSpaceCRUDService;

import java.sql.SQLException;

/**
 * Created by jonas - jonas@atmire.com on 04/12/17.
 */
public class BitstreamFormatBuilder extends AbstractCRUDBuilder<BitstreamFormat> {

    /* Log4j logger*/
    private static final Logger log =  Logger.getLogger(BitstreamFormatBuilder.class);

    private BitstreamFormat bitstreamFormat;

    protected BitstreamFormatBuilder(Context context) {
        super(context);
    }

    @Override
    protected void cleanup() throws Exception {
        delete(bitstreamFormat);
    }

    @Override
    protected DSpaceCRUDService<BitstreamFormat> getCRUDService() {
        return bitstreamFormatService;
    }

    @Override
    public BitstreamFormat build() {
        try{

            bitstreamFormatService.update(context, bitstreamFormat);
            context.dispatchEvents();

            indexingService.commit();
        } catch (SearchServiceException e) {
            log.error(e);
        } catch (SQLException e) {
            log.error(e);
        } catch (AuthorizeException e) {
            log.error(e);;
        }
        return bitstreamFormat;
    }


    public static BitstreamFormatBuilder createBitstreamFormat(Context context) throws SQLException, AuthorizeException {
        BitstreamFormatBuilder bitstreamFormatBuilder = new BitstreamFormatBuilder(context);
        return bitstreamFormatBuilder.create(context);
    }
    private BitstreamFormatBuilder create(Context context) throws SQLException, AuthorizeException {
        this.context = context;

        bitstreamFormat = bitstreamFormatService.create(context);

        return this;
    }

    public BitstreamFormatBuilder withMimeType(String mimeType){
        bitstreamFormat.setMIMEType(mimeType);
        return this;
    }

    public BitstreamFormatBuilder withDescription(String description){
        bitstreamFormat.setDescription(description);
        return this;
    }

}
