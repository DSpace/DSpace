/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.importer.external.scopus;

import org.dspace.content.*;
import org.dspace.importer.external.exception.*;

/**
 * Created by: Antoine Snyers (antoine at atmire dot com)
 * Date: 28 Oct 2014
 */
public interface GenerateQueryForItem_Scopus {
    public String generateQueryForItem(Item item) throws MetadataSourceException;
    public String generateFallbackQueryForItem(Item item) throws MetadataSourceException;
}
