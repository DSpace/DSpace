/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.builder;

import java.io.IOException;
import java.io.InputStream;
import java.sql.SQLException;

import org.dspace.authorize.AuthorizeException;
import org.dspace.content.Bitstream;
import org.dspace.content.service.DSpaceObjectService;
import org.dspace.core.Context;

/**
 * Builder class to build bitstreams in test cases.
 * This builder was created  for Clarin-Dspace import.
 *
 * @author Michaela Paurikova(michaela.paurikova at dataquest.sk)
 */
public class ClarinBitstreamBuilder extends AbstractDSpaceObjectBuilder<Bitstream> {

    private Bitstream bitstream;

    protected ClarinBitstreamBuilder(Context context) {
        super(context);
    }

    public static ClarinBitstreamBuilder createBitstream(Context context, InputStream is)
            throws SQLException, IOException {
        ClarinBitstreamBuilder builder = new ClarinBitstreamBuilder(context);
        return builder.create(context, is);
    }

    private ClarinBitstreamBuilder create(Context context, InputStream is)
            throws SQLException, IOException {
        this.context = context;
        bitstream = bitstreamService.create(context, is);

        return this;
    }

    @Override
    public void cleanup() throws Exception {
        try (Context c = new Context()) {
            c.turnOffAuthorisationSystem();
            // Ensure object and any related objects are reloaded before checking to see what needs cleanup
            bitstream = c.reloadEntity(bitstream);
            if (bitstream != null) {
                delete(c, bitstream);
                c.complete();
            }
        }
    }

    @Override
    protected DSpaceObjectService<Bitstream> getService() {
        return bitstreamService;
    }

    @Override
    public Bitstream build() throws SQLException, AuthorizeException {
        try {
            bitstreamService.update(context, bitstream);
            context.dispatchEvents();
            indexingService.commit();
        } catch (Exception e) {
            return null;
        }

        return bitstream;
    }
}
