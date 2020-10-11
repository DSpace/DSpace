/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.suggestion;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;
import org.dspace.core.Context;
import org.springframework.stereotype.Service;

@Service
public class SuggestionServiceImpl implements SuggestionService {

    List<SuggestionTarget> storage = new ArrayList<>();

    @Override
    public void addSuggestionTarget(SuggestionTarget target) {
        storage.add(target);
    }

    @Override
    public SuggestionTarget find(Context context, String source, UUID id) {
        return storage.stream().filter(st -> st.getID().toString().equals(source + ":" + id)).findFirst().orElse(null);
    }

    @Override
    public long countAll(Context context, String source) {
        return storage.stream().filter(st -> StringUtils.equals(st.getSource(), source)).count();
    }

    @Override
    public List<SuggestionTarget> findAllTargets(Context context, String source, int pageSize, long offset) {
        List<SuggestionTarget> results = new ArrayList<SuggestionTarget>();
        List<SuggestionTarget> fullSourceTargets = storage.stream()
                .filter(st -> StringUtils.equals(st.getSource(), source)).skip(offset).limit(pageSize)
                .collect(Collectors.toList());
        return fullSourceTargets;
    }

    @Override
    public void deleteTarget(SuggestionTarget target) {
        storage.remove(target);
    }

}
