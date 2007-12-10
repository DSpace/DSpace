/*
 * ItemProxy.java
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
package org.dspace.content.proxy;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.apache.log4j.Logger;

import org.dspace.authorize.AuthorizeException;
import org.dspace.authorize.AuthorizeManager;
import org.dspace.content.Bundle;
import org.dspace.content.Collection;
import org.dspace.content.DCValue;
import org.dspace.content.Item;
import org.dspace.content.MetadataSchema;
import org.dspace.core.Constants;
import org.dspace.core.Context;
import org.dspace.core.LogManager;
import org.dspace.eperson.EPerson;

/**
 * FIXME: This class could be optimized a great deal by being clever about
 * exactly what metadata (and maybe bundles) are pulled into memory.
 *
 * @author James Rutherford
 */
public class ItemProxy extends Item
{
    private static Logger log = Logger.getLogger(ItemProxy.class);

    private boolean bundlesLoaded = false;
    private boolean metadataLoaded = false;

    public ItemProxy(Context context, int id)
    {
        super(context, id);
    }

    @Override
    public Collection getOwningCollection()
    {
        if (owningCollection != null)
        {
            return owningCollection;
        }
        else if (owningCollectionId != -1)
        {
            owningCollection = collectionDAO.retrieve(owningCollectionId);
        }

        return super.getOwningCollection();
    }

    @Override
    public Bundle[] getBundles()
    {
        if (!bundlesLoaded)
        {
            setBundles(bundleDAO.getBundles(this));
        }

        return super.getBundles();
    }

    @Override
    public void setBundles(List<Bundle> bundles)
    {
        this.bundlesLoaded = true;

        super.setBundles(bundles);
    }

    @Override
    public Bundle[] getBundles(String name)
    {
        if (!bundlesLoaded)
        {
            setBundles(bundleDAO.getBundles(this));
        }

        return super.getBundles(name);
    }

    @Override
    public void addBundle(Bundle b) throws AuthorizeException
    {
        if (!bundlesLoaded)
        {
            setBundles(bundleDAO.getBundles(this));
        }

        super.addBundle(b);
    }

    @Override
    public void removeBundle(Bundle b) throws AuthorizeException
    {
        if (!bundlesLoaded)
        {
            setBundles(bundleDAO.getBundles(this));
        }

        super.removeBundle(b);
    }

    @Override
    public List<DCValue> getMetadata()
    {
        if (!metadataLoaded)
        {
            dao.loadMetadata(this);
            this.metadataLoaded = true;
        }

        return super.getMetadata();
    }

    @Override
    public DCValue[] getMetadata(String mdString)
    {
        if (!metadataLoaded)
        {
            dao.loadMetadata(this);
            this.metadataLoaded = true;
        }

        return super.getMetadata(mdString);
    }

    @Override
    public DCValue[] getMetadata(String schema, String element,
            String qualifier, String language)
    {
        // Really, we should query the DAO for specific metadata only under
        // certain conditions, but if the value doesn't exist in memory, we
        // can't ever guarantee that it's not in the database unless we
        // actually check.
        if (!metadataLoaded)
        {
            dao.loadMetadata(this);
            this.metadataLoaded = true;
        }

        return super.getMetadata(schema, element, qualifier, language);
    }

    @Override
    public void setMetadata(List<DCValue> metadata)
    {
        this.metadata = metadata;
    }

    @Override
    public void addMetadata(String schema, String element, String qualifier,
            String lang, String... values)
    {
        if (!metadataLoaded)
        {
            dao.loadMetadata(this);
            this.metadataLoaded = true;
        }

        super.addMetadata(schema, element, qualifier, lang, values);
    }

    @Override
    public void clearMetadata(String schema, String element, String qualifier,
            String lang)
    {
        if (!metadataLoaded)
        {
            dao.loadMetadata(this);
            this.metadataLoaded = true;
        }

        super.clearMetadata(schema, element, qualifier, lang);
    }

    @Override
    public void setSubmitter(EPerson submitter)
    {
        this.submitterId = submitter.getID();
        this.submitter = submitter;
    }

    @Override
    public void setSubmitter(int submitterId)
    {
        this.submitterId = submitterId;
        submitter = null;
    }

    @Override
    public EPerson getSubmitter()
    {
        if (submitter == null && submitterId > -1)
        {
            submitter = epersonDAO.retrieve(submitterId);
        }
        return submitter;
    }

    @Override
    public DCValue[] getDC(String element, String qualifier, String lang)
    {
        return getMetadata(MetadataSchema.DC_SCHEMA, element, qualifier, lang);
    }

    @Override
    public void addDC(String element, String qualifier, String lang,
                      String... values)
    {
        addMetadata(MetadataSchema.DC_SCHEMA, element, qualifier, lang, values);
    }

    @Override
    public void clearDC(String element, String qualifier, String lang)
    {
        clearMetadata(MetadataSchema.DC_SCHEMA, element, qualifier, lang);
    }
}
