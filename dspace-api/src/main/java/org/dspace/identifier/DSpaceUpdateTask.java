package org.dspace.identifier;

import java.io.IOException;
import java.sql.SQLException;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.dspace.content.DSpaceObject;
import org.dspace.core.Context;
import org.dspace.curate.AbstractCurationTask;
import org.dspace.curate.Curator;
import org.dspace.storage.rdbms.DatabaseManager;
import org.dspace.storage.rdbms.TableRow;

/**
 * Ensure that a DSpaceObject has a DSpace internal identifier.
 *
 * @author mhwood
 */
public class DSpaceUpdateTask
        extends AbstractCurationTask
{
    @Override
    public int perform(DSpaceObject dso)
            throws IOException
    {
        Context ctx = null;
        IdentifierProvider provider = new DSpaceProvider();
        try {
            ctx = new Context();
            String id = provider.lookup(ctx, dso);
            setResult(dso.getTypeText() + " " + dso.getID() + " already registered as " + id);
            ctx.complete();
            return Curator.CURATE_SKIP;
        } catch (IdentifierNotFoundException ex) { // No identifier, so make one
            try {
                provider.register(ctx, dso);
                if (null != ctx)
                    ctx.complete();
                return Curator.CURATE_SUCCESS;
            } catch (SQLException | IdentifierException e1) {
                setResult(dso.getTypeText() + ' ' + dso.getID() + " not registered:  " + e1.getMessage());
                return Curator.CURATE_ERROR;
            }
        } catch (SQLException | IdentifierException ex) {
            setResult("Exception on " + dso.getTypeText() + " " + dso.getID() + ":  "
                    + ex.getMessage());
            if (null != ctx)
                ctx.abort();
            return Curator.CURATE_ERROR;
        }
    }
}
