package org.datadryad.journalstatistics.statistics;

import org.datadryad.journalstatistics.extractor.ExtractorInterface;

/**
 *
 * @author Dan Leehr <dan.leehr@nescent.org>
 */
public class Statistic<T> {
    private T value;
    private ExtractorInterface<T> extractor;
    private String name;
    public Statistic(String name, ExtractorInterface<T> extractor) {
        this.name = name;
        this.extractor = extractor;
    }

    public void setValue(T value) {
        this.value = value;
    }

    public T getValue() {
        return value;
    }
    
    public String toString() {
        return String.format("%s - %s", getName(), value.toString());
    }

    public String getName() {
        return name;
    }

    public void extractAndStore(String journalName) {
        this.value = extractor.extract(journalName);
    }

}
