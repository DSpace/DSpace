package org.dspace.sword2;

import org.dspace.content.Item;
import org.dspace.core.Context;
import org.swordapp.server.Statement;
import org.swordapp.server.SwordError;
import org.swordapp.server.SwordServerException;

public interface SwordStatementDisseminator
{
	public Statement disseminate(Context context, Item item)
        throws DSpaceSwordException, SwordError, SwordServerException;
}
