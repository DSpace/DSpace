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
    public SuggestionTarget find(Context context, UUID id) {
        return storage.stream().filter(st -> st.getID().equals(id)).findFirst().orElse(null);
    }

    @Override
    public long countAll(Context context) {
        return storage.size();
    }

    @Override
    public List<SuggestionTarget> findAllTargets(Context context, int pageSize, long offset) {
        List<SuggestionTarget> results = new ArrayList<SuggestionTarget>();
        if (offset > storage.size()) {
            return null;
        }
        int idx = 0;
        for (SuggestionTarget t : storage) {
            if (idx >= offset && idx < offset + pageSize) {
                results.add(t);
            } else if (idx >= offset + pageSize) {
                break;
            }
            idx++;
        }
        return results;
    }

    @Override
    public void deleteTarget(SuggestionTarget target) {
        storage.remove(target);
    }

}
