/*
 * DSpaceRecordFactory.java
 *
 * Version: $Revision$
 *
 * Date: $Date$
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

package org.dspace.app.oai;

import java.sql.SQLException;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Properties;

import org.apache.log4j.Logger;

import ORG.oclc.oai.server.catalog.RecordFactory;
import ORG.oclc.oai.server.verb.CannotDisseminateFormatException;

import org.dspace.content.Collection;
import org.dspace.content.Community;
import org.dspace.content.DCValue;
import org.dspace.content.Item;
import org.dspace.core.LogManager;
import org.dspace.search.HarvestedItemInfo;

/**
 * Implementation of the OAICat RecordFactory base class for DSpace items.
 *
 * @author  Robert Tansley
 * @version $Revision$
 */
public class DSpaceRecordFactory extends RecordFactory
{
    /** log4j category */
    private static Logger log = Logger.getLogger(DSpaceRecordFactory.class);

    public DSpaceRecordFactory(Properties properties)
    {
        // We don't use the OAICat properties; pass on up
        super(properties);
    }


    protected String fromOAIIdentifier(String identifier)
    {
        // Our local identifier is actually the same as the OAI one (the Handle)
        return identifier;
    }
    

    public String quickCreate(Object nativeItem, String schemaURL,
        String metadataPrefix)
	throws IllegalArgumentException, CannotDisseminateFormatException
    {
        // Not supported
        return null;
    }


    protected String getOAIIdentifier(Object nativeItem)
    {
        String h = "hdl:" + ((HarvestedItemInfo) nativeItem).handle;
        return h;
    }
    
    
    protected String getDatestamp(Object nativeItem)
    {
        return ((HarvestedItemInfo) nativeItem).datestamp;
    }


    protected Iterator getSetSpecs(Object nativeItem)
    {
        HarvestedItemInfo hii = (HarvestedItemInfo) nativeItem;
        List setSpecs = new LinkedList();
        
        // Convert container IDs to set specs (comm-id:coll-id)
        for (int i = 0; i < hii.containers.length; i++)
        {
            setSpecs.add(String.valueOf(hii.containers[i][0]) + ":" +
                         String.valueOf(hii.containers[i][1]));
        }
        
        return setSpecs.iterator();
    }
    
    
    protected boolean isDeleted(Object nativeItem)
    {
        HarvestedItemInfo hii = (HarvestedItemInfo) nativeItem;
        return hii.withdrawn;
    }
    
    
    protected Iterator getAbouts(Object nativeItem)
    {
        // Nothing in the about section for now
        return new LinkedList().iterator();
    }
}
