/*
 */
package org.datadryad.journalstatistics.extractor;

/**
 *
 * @author Dan Leehr <dan.leehr@nescent.org>
 */
public interface ExtractorInterface<T> {
    public T extract(String journalName);
}
