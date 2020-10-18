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
    public void deleteTarget(SuggestionTarget target) {
        storage.remove(target);
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
        List<SuggestionTarget> fullSourceTargets = storage.stream()
                .filter(st -> StringUtils.equals(st.getSource(), source)).skip(offset).limit(pageSize)
                .collect(Collectors.toList());
        return fullSourceTargets;
    }

    @Override
    public long countAllByTarget(Context context, UUID target) {
        return storage.stream().filter(st -> target.equals(st.getTarget().getID())).count();
    }

    @Override
    public List<SuggestionTarget> findByTarget(Context context, UUID target, int pageSize, long offset) {
        List<SuggestionTarget> fullSourceTargets = storage.stream().filter(st -> target.equals(st.getTarget().getID()))
                .skip(offset).limit(pageSize).collect(Collectors.toList());
        return fullSourceTargets;
    }

    @Override
    public long countSources(Context context) {
        List<SuggestionSource> results = getSources();
        return results.size();
    }

    @Override
    public SuggestionSource findSource(Context context, String source) {
        return getSources().stream().filter(st -> StringUtils.equals(source, st.getID())).findFirst().orElse(null);
    }

    @Override
    public List<SuggestionSource> findAllSources(Context context, int pageSize, long offset) {
        List<SuggestionSource> fullSources = getSources().stream()
                .skip(offset).limit(pageSize)
                .collect(Collectors.toList());
        return fullSources;
    }

    private List<SuggestionSource> getSources() {
        List<SuggestionSource> results = new ArrayList<SuggestionSource>();
        for (SuggestionTarget t : storage) {
            SuggestionSource s = null;
            for (SuggestionSource ss : results) {
                if (StringUtils.equals(ss.getID(), t.getSource())) {
                    s = ss;
                    s.setTotal(s.getTotal() + 1);
                }
            }
            if (s == null) {
                s = new SuggestionSource(t.getSource());
                s.setTotal(1);
                results.add(s);
            }

        }
        return results;
    }

}
