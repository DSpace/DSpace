/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.suggestion;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;
import org.dspace.content.Item;
import org.dspace.content.dto.MetadataValueDTO;
import org.dspace.content.service.ItemService;
import org.dspace.core.Context;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class SuggestionServiceImpl implements SuggestionService {

    List<SuggestionTarget> storage = new ArrayList<>();

    @Autowired
    private ItemService itemService;

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
        List<SuggestionSource> fullSources = getSources().stream().skip(offset).limit(pageSize)
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

    @Override
    public long countAllByTargetAndSource(Context context, String source, UUID target) {
        SuggestionTarget targetSuggestion = find(context, source, target);
        if (targetSuggestion != null) {
            return targetSuggestion.getTotal();
        }
        return 0;
    }

    @Override
    public List<Suggestion> findByTargetAndSource(Context context, UUID target, String source, int pageSize,
            long offset) {
        SuggestionTarget targetSuggestion = find(context, source, target);
        if (targetSuggestion != null) {
            List<Suggestion> allSuggestions = generateAllSuggestion(context, target, source, targetSuggestion);
            List<Suggestion> pageSuggestion = allSuggestions.stream().skip(offset).limit(pageSize)
                    .collect(Collectors.toList());
            return pageSuggestion;
        }
        return null;
    }

    @Override
    public Suggestion findSuggestion(Context context, String id) {
        String source = id.split(":", 3)[0];
        UUID target = UUID.fromString(id.split(":", 3)[1]);
        SuggestionTarget targetSuggestion = find(context, source, target);
        if (targetSuggestion != null) {
            List<Suggestion> allSuggestions = generateAllSuggestion(context, target, source, targetSuggestion);
            return allSuggestions.stream().filter(st -> StringUtils.equals(id, st.getID())).findFirst().orElse(null);
        }
        return null;
    }

    private List<Suggestion> generateAllSuggestion(Context context, UUID target, String source,
            SuggestionTarget targetSuggestion) {
        List<Suggestion> allSuggestions = new ArrayList<Suggestion>();
        Item itemTarget;
        try {
            itemTarget = itemService.find(context, target);
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
        for (int idx = 0; idx < targetSuggestion.getTotal(); idx++) {
            String idPartStr = String.valueOf(idx + 1);
            Suggestion sug = new Suggestion(source, itemTarget, idPartStr);
            sug.setDisplay("Suggestion " + source + " " + idPartStr);
            MetadataValueDTO mTitle = new MetadataValueDTO();
            mTitle.setSchema("dc");
            mTitle.setElement("title");
            mTitle.setValue("Title Suggestion " + idPartStr);

            MetadataValueDTO mSource1 = new MetadataValueDTO();
            mSource1.setSchema("dc");
            mSource1.setElement("source");
            mSource1.setValue("Source 1");

            MetadataValueDTO mSource2 = new MetadataValueDTO();
            mSource2.setSchema("dc");
            mSource2.setElement("source");
            mSource2.setValue("Source 2");

            sug.getMetadata().add(mTitle);
            sug.getMetadata().add(mSource1);
            sug.getMetadata().add(mSource2);

            allSuggestions.add(sug);
        }
        return allSuggestions;
    }
}
