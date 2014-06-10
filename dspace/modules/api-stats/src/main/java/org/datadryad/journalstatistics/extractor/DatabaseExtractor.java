/*
 */
package org.datadryad.journalstatistics.extractor;

import org.dspace.core.Context;

/**
 * Base class for extracting statistics from a database
 * @author Dan Leehr <dan.leehr@nescent.org>
 */
public abstract class DatabaseExtractor<T> implements ExtractorInterface<T> {
    
    private Context context;
    public DatabaseExtractor(Context context) {
        this.context = context;
    }

    protected DatabaseExtractor() {
    }

    protected final Context getContext() {
        return context;
    }

    /**
     * Subclasses must override to do their work
     * @param journalName
     * @return
     */
    @Override
    public abstract T extract(String journalName);

}
