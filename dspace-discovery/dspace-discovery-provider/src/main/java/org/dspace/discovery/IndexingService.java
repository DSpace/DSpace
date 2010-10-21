/*
 * IndexingService.java
 *
 * Version: $Revision: 5497 $
 *
 * Date: $Date: 2010-10-20 23:06:10 +0200 (wo, 20 okt 2010) $
 *
 * Copyright (c) 2002, Hewlett-Packard Company and Massachusetts
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
package org.dspace.discovery;

import org.dspace.content.DSpaceObject;
import org.dspace.core.Context;

import java.io.IOException;
import java.sql.SQLException;

/**
 * Interface used for indexing dspaceobject into discovery
 *
 * @author Kevin Van de Velde (kevin at atmire dot com)
 * @author Mark Diggory (markd at atmire dot com)
 * @author Ben Bosman (ben at atmire dot com)
 */
public interface IndexingService {

    void indexContent(Context context, DSpaceObject dso)
            throws SQLException;

    void indexContent(Context context, DSpaceObject dso,
                      boolean force) throws SQLException;

    void unIndexContent(Context context, DSpaceObject dso)
            throws SQLException, IOException;

    void unIndexContent(Context context, String handle)
            throws SQLException, IOException;

    void unIndexContent(Context context, String handle, boolean commit)
            throws SQLException, IOException;

    void reIndexContent(Context context, DSpaceObject dso)
            throws SQLException, IOException;

    void createIndex(Context context) throws SQLException, IOException;

    void updateIndex(Context context);

    void updateIndex(Context context, boolean force);

    void cleanIndex(boolean force) throws IOException,
            SQLException, SearchServiceException;
}
