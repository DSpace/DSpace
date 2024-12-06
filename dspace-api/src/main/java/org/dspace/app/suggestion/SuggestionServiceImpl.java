/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.suggestion;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

import jakarta.annotation.Resource;
import org.apache.logging.log4j.Logger;
import org.dspace.core.Context;
import org.springframework.stereotype.Service;

@Service
public class SuggestionServiceImpl implements SuggestionService {
    private static final Logger log = org.apache.logging.log4j.LogManager.getLogger(SuggestionServiceImpl.class);

    @Resource(name = "suggestionProviders")
    private Map<String, SuggestionProvider> providersMap;

    @Override
    public List<SuggestionProvider> getSuggestionProviders() {
        if (providersMap != null) {
            return providersMap.values().stream().collect(Collectors.toList());
        }
        return null;
    }

    @Override
    public SuggestionTarget find(Context context, String source, UUID id) {
        if (providersMap.containsKey(source)) {
            return providersMap.get(source).findTarget(context, id);
        } else {
            return null;
        }
    }

    @Override
    public long countAll(Context context, String source) {
        if (providersMap.containsKey(source)) {
            return providersMap.get(source).countAllTargets(context);
        } else {
            return 0;
        }
    }

    @Override
    public List<SuggestionTarget> findAllTargets(Context context, String source, int pageSize, long offset) {
        if (providersMap.containsKey(source)) {
            return providersMap.get(source).findAllTargets(context, pageSize, offset);
        } else {
            return null;
        }
    }

    @Override
    public long countAllByTarget(Context context, UUID target) {
        int count = 0;
        for (String provider : providersMap.keySet()) {
            if (providersMap.get(provider).countUnprocessedSuggestionByTarget(context, target) > 0) {
                count++;
            }
        }
        return count;
    }

    @Override
    public List<SuggestionTarget> findByTarget(Context context, UUID target, int pageSize, long offset) {
        List<SuggestionTarget> fullSourceTargets = new ArrayList<SuggestionTarget>();
        for (String source : providersMap.keySet()) {
            // all the suggestion target will be related to the same target (i.e. the same researcher - person item)
            SuggestionTarget sTarget = providersMap.get(source).findTarget(context, target);
            if (sTarget != null && sTarget.getTotal() > 0) {
                fullSourceTargets.add(sTarget);
            }
        }
        fullSourceTargets.sort(new Comparator<SuggestionTarget>() {
            @Override
            public int compare(SuggestionTarget arg0, SuggestionTarget arg1) {
                return -(arg0.getTotal() - arg1.getTotal());
            }
        }
        );
        // this list will be as large as the number of sources available in the repository so it is unlikely that
        // real pagination will occur
        return fullSourceTargets.stream().skip(offset).limit(pageSize).collect(Collectors.toList());
    }

    @Override
    public long countSources(Context context) {
        return providersMap.size();
    }

    @Override
    public SuggestionSource findSource(Context context, String source) {
        if (providersMap.containsKey(source)) {
            SuggestionSource ssource = new SuggestionSource(source);
            ssource.setTotal((int) providersMap.get(source).countAllTargets(context));
            return ssource;
        } else {
            return null;
        }
    }

    @Override
    public List<SuggestionSource> findAllSources(Context context, int pageSize, long offset) {
        List<SuggestionSource> fullSources = getSources(context).stream().skip(offset).limit(pageSize)
                .collect(Collectors.toList());
        return fullSources;
    }

    private List<SuggestionSource> getSources(Context context) {
        List<SuggestionSource> results = new ArrayList<SuggestionSource>();
        for (String source : providersMap.keySet()) {
            SuggestionSource ssource = new SuggestionSource(source);
            ssource.setTotal((int) providersMap.get(source).countAllTargets(context));
            results.add(ssource);
        }
        return results;
    }

    @Override
    public long countAllByTargetAndSource(Context context, String source, UUID target) {
        if (providersMap.containsKey(source)) {
            return providersMap.get(source).countUnprocessedSuggestionByTarget(context, target);
        }
        return 0;
    }

    @Override
    public List<Suggestion> findByTargetAndSource(Context context, UUID target, String source, int pageSize,
            long offset, boolean ascending) {
        if (providersMap.containsKey(source)) {
            return providersMap.get(source).findAllUnprocessedSuggestions(context, target, pageSize, offset, ascending);
        }
        return null;
    }

    @Override
    public Suggestion findUnprocessedSuggestion(Context context, String id) {
        String source = null;
        UUID target = null;
        String idPart = null;
        String[] split;
        try {
            split = id.split(":", 3);
            source = split[0];
            target = UUID.fromString(split[1]);
            idPart = split[2];
        } catch (Exception e) {
            log.warn("findSuggestion got an invalid id " + id + ", return null");
            return null;
        }
        if (split.length != 3) {
            return null;
        }
        if (providersMap.containsKey(source)) {
            return providersMap.get(source).findUnprocessedSuggestion(context, target, idPart);
        }
        return null;
    }

    @Override
    public void rejectSuggestion(Context context, String id) {
        String source = null;
        UUID target = null;
        String idPart = null;
        String[] split;
        try {
            split = id.split(":", 3);
            source = split[0];
            target = UUID.fromString(split[1]);
            idPart = split[2];
        } catch (Exception e) {
            log.warn("rejectSuggestion got an invalid id " + id + ", doing nothing");
            return;
        }
        if (split.length != 3) {
            return;
        }
        if (providersMap.containsKey(source)) {
            providersMap.get(source).rejectSuggestion(context, target, idPart);
        }

    }
}
