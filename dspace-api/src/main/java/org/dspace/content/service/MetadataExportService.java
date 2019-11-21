/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.content.service;

import java.util.Iterator;

import org.dspace.app.bulkedit.DSpaceCSV;
import org.dspace.content.Community;
import org.dspace.content.Item;
import org.dspace.core.Context;

public interface MetadataExportService {

    public DSpaceCSV handleExport(Context context, boolean exportAllItems, boolean exportAllMetadata, String handle)
        throws Exception;

    public DSpaceCSV export(Context context, Iterator<Item> toExport, boolean exportAll) throws Exception;

    public DSpaceCSV export(Context context, Community community, boolean exportAll) throws Exception;

}