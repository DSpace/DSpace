/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.suggestion;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.dspace.core.Context;
import org.springframework.stereotype.Service;

@Service
public class SuggestionServiceImpl implements SuggestionService {

    Map<UUID, SuggestionTarget> storage = new HashMap<UUID, SuggestionTarget>();

    @Override
    public void addSuggestionTarget(SuggestionTarget target) {
        storage.put(target.getID(), target);
    }

    @Override
    public SuggestionTarget find(Context context, UUID id) {
        return storage.get(id);
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
        for (SuggestionTarget t : storage.values()) {
            if (idx >= offset && idx < offset + pageSize) {
                results.add(t);
            } else {
                break;
            }
            idx++;
        }
        return results;
    }

    @Override
    public void deleteTarget(SuggestionTarget target) {
        storage.remove(target.getID());
    }

}
