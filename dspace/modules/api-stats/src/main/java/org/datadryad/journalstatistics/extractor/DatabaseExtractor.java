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

    private Boolean isDateWithinRange(final Date date) {
        checkDateOrder();
        if (
                (date != null) &&
                (date.after(this.beginDate) || date.equals(this.beginDate)) &&
                (date.before(this.endDate) || date.equals(this.endDate))
                ) {
            return Boolean.TRUE;
        } else {
            return Boolean.FALSE;
        }
    }

    final void checkDateOrder() {
        if(filterOnDates) {
            if(beginDate.after(endDate)) {
                throw new IllegalStateException("beginDate must not be after endDate");
            }
        }
    }

    final Boolean passesDateFilter(final Date date) {
        if(this.filterOnDates) {
            return isDateWithinRange(date);
        } else {
            return Boolean.TRUE;
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
