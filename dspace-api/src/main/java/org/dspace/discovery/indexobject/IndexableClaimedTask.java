/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.discovery.indexobject;

import org.dspace.discovery.IndexableObject;
import org.dspace.xmlworkflow.storedcomponents.ClaimedTask;

/**
 * ClaimedTask implementation for the IndexableObject
 *
 * @author Kevin Van de Velde (kevin at atmire dot com)
 */
public class IndexableClaimedTask implements IndexableObject<ClaimedTask, Integer> {

    private ClaimedTask claimedTask;
    public static final String TYPE = ClaimedTask.class.getSimpleName();

    public IndexableClaimedTask(ClaimedTask claimedTask) {
        this.claimedTask = claimedTask;
    }

    @Override
    public ClaimedTask getIndexedObject() {
        return claimedTask;
    }

    @Override
    public void setIndexedObject(ClaimedTask claimedTask) {
        this.claimedTask = claimedTask;
    }

    @Override
    public String getType() {
        return TYPE;
    }

    @Override
    public Integer getID() {
        return claimedTask.getID();
    }

    @Override
    public String getTypeText() {
        return "CLAIMEDTASK";
    }
}