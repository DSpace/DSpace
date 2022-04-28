/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.discovery.indexobject.factory;

import org.dspace.discovery.indexobject.IndexablePoolTask;
import org.dspace.xmlworkflow.storedcomponents.PoolTask;

/**
 * Factory interface for indexing/retrieving Pooltask objects in the search core
 *
 * @author Kevin Van de Velde (kevin at atmire dot com)
 */
public interface PoolTaskIndexFactory extends IndexFactory<IndexablePoolTask, PoolTask> {
}