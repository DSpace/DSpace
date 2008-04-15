/*
 * MigrateHandle.java
 *
 * Version: $Revision: 1727 $
 *
 * Date: $Date: 2007-01-19 10:52:10 +0000 (Fri, 19 Jan 2007) $
 *
 * Copyright (c) 2002-2005, Hewlett-Packard Company and Massachusetts
 * Institute of Technology.  All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are
 * met:
 *
 * - Redistributions of source code must retain the above copyright
 * notice, this list of conditions and the following disclaimer.
 *
 * - Redistributions in binary form must reproduce the above copyright
 * notice, this list of conditions and the following disclaimer in the
 * documentation and/or other materials provided with the distribution.
 *
 * - Neither the name of the Hewlett-Packard Company nor the name of the
 * Massachusetts Institute of Technology nor the names of their
 * contributors may be used to endorse or promote products derived from
 * this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 * ``AS IS'' AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 * LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR
 * A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT
 * HOLDERS OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT,
 * INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING,
 * BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS
 * OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 * ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR
 * TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE
 * USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH
 * DAMAGE.
 */
package org.dspace.administer.update1516;

import org.dspace.core.Context;
import org.dspace.storage.rdbms.DatabaseManager;
import org.dspace.storage.rdbms.TableRow;
import org.dspace.storage.rdbms.TableRowIterator;
import org.dspace.uri.ExternalIdentifier;
import org.dspace.uri.ObjectIdentifier;
import org.dspace.uri.ObjectIdentifierService;
import org.dspace.uri.IdentifierException;
import org.dspace.uri.dao.ExternalIdentifierDAO;
import org.dspace.uri.dao.ExternalIdentifierDAOFactory;
import org.dspace.uri.dao.ExternalIdentifierStorageException;
import org.dspace.uri.handle.Handle;

import java.sql.SQLException;

/**
 * @author Richard Jones
 */
public class MigrateHandle
{
    public void migrate()
            throws SQLException, ExternalIdentifierStorageException, IdentifierException
    {
        Context context = new Context();
        context.setIgnoreAuthorization(true);

        ExternalIdentifierDAO dao = ExternalIdentifierDAOFactory.getInstance(context);

        String query = "SELECT * FROM handle";
        TableRowIterator tri = DatabaseManager.query(context, query);
        while (tri.hasNext())
        {
            TableRow row = tri.next();
            ObjectIdentifier oid = ObjectIdentifierService.get(context, row.getIntColumn("resource_type_id"), row.getIntColumn("resource_id"));
            ExternalIdentifier eid = new Handle(row.getStringColumn("handle"), oid);
            dao.update(eid);
        }
        tri.close();

        context.complete();
    }
}
