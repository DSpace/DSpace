/*
 */
package org.datadryad.journalstatistics.extractor;

import org.dspace.core.Context;

/**
 *
 * @author Dan Leehr <dan.leehr@nescent.org>
 */
public class DataPackageCount extends DatabaseExtractor<Integer> {

    @Override
    public Integer extract(String journalName) {
        Context context = this.getContext();
        throw new RuntimeException("Not yet implemented");
    }

}
