/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.discovery.indexobject;

import org.dspace.discovery.IndexableObject;
import org.dspace.scripts.Process;

/**
 * {@link Process} implementation for the {@link IndexableObject}.
 */
public class IndexableProcess extends AbstractIndexableObject<Process, Integer> {

    public static final String TYPE = Process.class.getSimpleName();

    private Process process;

    public IndexableProcess(Process process) {
        this.process = process;
    }

    @Override
    public String getType() {
        return getTypeText();
    }

    @Override
    public Integer getID() {
        return process.getID();
    }

    @Override
    public Process getIndexedObject() {
        return process;
    }

    @Override
    public void setIndexedObject(Process object) {
        this.process = object;
    }

    @Override
    public String getTypeText() {
        return TYPE;
    }
}
