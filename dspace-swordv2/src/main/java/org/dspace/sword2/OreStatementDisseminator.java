/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.sword2;

import org.dspace.content.Item;
import org.dspace.core.Context;
import org.swordapp.server.OREStatement;
import org.swordapp.server.Statement;
import org.swordapp.server.SwordError;
import org.swordapp.server.SwordServerException;

public class OreStatementDisseminator extends GenericStatementDisseminator
        implements SwordStatementDisseminator
{
    public Statement disseminate(Context context, Item item)
            throws DSpaceSwordException, SwordError, SwordServerException
    {
        SwordUrlManager urlManager = new SwordUrlManager(
                new SwordConfigurationDSpace(), context);
        String aggUrl = urlManager.getAggregationUrl(item);
        String remUrl = urlManager.getOreStatementUri(item);
        Statement s = new OREStatement(remUrl, aggUrl);
        this.populateStatement(context, item, s);
        return s;
    }
}
