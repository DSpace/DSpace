/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.discovery.indexobject;

import org.dspace.content.InProgressSubmission;
import org.dspace.discovery.IndexableObject;

/**
 * InProgressSubmission implementation for the IndexableObject
 * @author Kevin Van de Velde (kevin at atmire dot com)
 */
public abstract class IndexableInProgressSubmission<T extends InProgressSubmission>
        implements IndexableObject<T, Integer> {

    protected T inProgressSubmission;

    public IndexableInProgressSubmission(T inProgressSubmission) {
        this.inProgressSubmission = inProgressSubmission;
    }

    @Override
    public T getIndexedObject() {
        return inProgressSubmission;
    }

    @Override
    public void setIndexedObject(T inProgressSubmission) {
        this.inProgressSubmission = inProgressSubmission;
    }
}
