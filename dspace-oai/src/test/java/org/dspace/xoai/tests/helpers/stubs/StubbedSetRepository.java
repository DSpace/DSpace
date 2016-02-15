/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.xoai.tests.helpers.stubs;


import com.lyncode.xoai.dataprovider.core.ListSetsResult;
import com.lyncode.xoai.dataprovider.core.Set;
import com.lyncode.xoai.dataprovider.services.api.SetRepository;

import java.util.ArrayList;
import java.util.List;

import static java.lang.Math.min;
import static org.apache.commons.lang.RandomStringUtils.randomAlphabetic;

public class StubbedSetRepository implements SetRepository {
    private List<Set> sets = new ArrayList<Set>();
    private boolean supports = false;

    @Override
    public boolean supportSets() {
        return supports;
    }

    @Override
    public ListSetsResult retrieveSets(int offset, int length) {
        if (offset > sets.size()) return new ListSetsResult(false, new ArrayList<Set>(), sets.size());
        return new ListSetsResult(offset+length < sets.size(), sets.subList(offset, min(offset + length, sets.size())), sets.size());
    }

    @Override
    public boolean exists(String setSpec) {
        for (Set set : sets)
            if (set.getSetSpec().equals(setSpec))
                return true;

        return false;
    }

    public StubbedSetRepository doesSupportSets() {
        this.supports = true;
        return this;
    }
    public StubbedSetRepository doesNotSupportSets() {
        this.supports = false;
        return this;
    }
    public StubbedSetRepository withSet(String name, String spec) {
        this.sets.add(new Set(spec, name));
        return this;
    }

    public StubbedSetRepository withRandomlyGeneratedSets(int number) {
        for (int i=0;i<number;i++)
            this.sets.add(new Set(randomAlphabetic(10), randomAlphabetic(10)));
        return this;
    }

    public void clear() {
        this.sets.clear();
    }
}
