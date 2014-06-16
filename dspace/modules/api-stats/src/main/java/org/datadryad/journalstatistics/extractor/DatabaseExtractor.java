/*
 */
package org.datadryad.journalstatistics.extractor;

import java.util.Date;
import org.dspace.core.Context;

/**
 * Base class for extracting statistics from a database
 * @author Dan Leehr <dan.leehr@nescent.org>
 */
public abstract class DatabaseExtractor<T> implements ExtractorInterface<T> {
    static final String JOURNAL_ELEMENT = "publicationName";
    static final String JOURNAL_QUALIFIER = null;
    static final String JOURNAL_SCHEMA = "prism";
    static final Date DISTANT_PAST = new Date(Long.MIN_VALUE);
    static final Date DISTANT_FUTURE = new Date(Long.MAX_VALUE);
    
    Date beginDate = DISTANT_PAST;
    Date endDate = DISTANT_FUTURE;
    Boolean filterOnDates = Boolean.FALSE;
    
    private Context context;
    public DatabaseExtractor(Context context) {
        this.context = context;
    }

    public void setBeginDate(Date beginDate) {
        this.beginDate = beginDate;
        this.filterOnDates = Boolean.TRUE;
    }
    public Date getBeginDate() { return beginDate; }
    public void setEndDate(Date endDate) {
        this.endDate = endDate;
        this.filterOnDates = Boolean.TRUE;
    }
    public Date getEndDate() { return endDate; }

    protected DatabaseExtractor() {
    }

    final Context getContext() {
        return context;
    }

    final Boolean isDateWithinRange(final Date date) {
        if (
                (date != null) &&
                (date.compareTo(this.beginDate) >= 0) &&
                (date.compareTo(this.endDate) <= 0)) {
            return Boolean.TRUE;
        } else {
            return Boolean.FALSE;
        }
    }

    /**
     * Subclasses must override to do their work
     * @param journalName
     * @return
     */
    @Override
    public abstract T extract(String journalName);

}
