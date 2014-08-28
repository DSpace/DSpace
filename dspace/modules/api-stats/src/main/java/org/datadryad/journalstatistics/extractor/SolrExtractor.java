/*
 */
package org.datadryad.journalstatistics.extractor;

/**
 *
 * @author Dan Leehr <dan.leehr@nescent.org>
 */
public abstract class SolrExtractor<T> implements ExtractorInterface<T> {

    public SolrExtractor(Object solr) {
        // TODO: Fill this in with a solr?
    }
    /**
     * Subclasses must override to do their work
     * @param journalName
     * @return 
     */
    @Override
    public abstract T extract(String journalName);

}
