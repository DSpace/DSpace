/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.discovery.indexobject;

import org.dspace.discovery.IndexableObject;
import org.dspace.xmlworkflow.storedcomponents.PoolTask;

/**
 * PoolTask implementation for the IndexableObject
 * @author Kevin Van de Velde (kevin at atmire dot com)
 */
public class IndexablePoolTask implements IndexableObject<PoolTask, Integer> {

    public static final String TYPE = PoolTask.class.getSimpleName();

    private PoolTask poolTask;

    public IndexablePoolTask(PoolTask poolTask) {
        this.poolTask = poolTask;
    }

    @Override
    public PoolTask getIndexedObject() {
        return poolTask;
    }

    @Override
    public void setIndexedObject(PoolTask poolTask) {
        this.poolTask = poolTask;
    }

    @Override
    public String getType() {
        return TYPE;
    }

    @Override
    public String getTypeText() {
        return "POOLTASK";
    }

    @Override
    public Integer getID() {
        return poolTask.getID();
    }
}