/*
 */
package org.datadryad.journalstatistics.extractor;

import java.util.Date;

/**
 *
 * @author Dan Leehr <dan.leehr@nescent.org>
 */
public interface ExtractorInterface<T> {
    public T extract(String journalName);
    public void setBeginDate(Date beginDate);
    public void setEndDate(Date endDate);
}
