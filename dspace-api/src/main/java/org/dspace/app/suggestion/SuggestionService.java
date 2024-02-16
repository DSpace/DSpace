/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.suggestion;

import java.util.List;
import java.util.UUID;

import org.dspace.core.Context;

/**
 * Service that handles {@link Suggestion}.
 *
 * @author Andrea Bollini (andrea.bollini at 4science.it)
 */
public interface SuggestionService {

    /** find a {@link SuggestionTarget } by source name and suggestion id */
    public SuggestionTarget find(Context context, String source, UUID id);

    /** count all suggetion targets by suggestion source */
    public long countAll(Context context, String source);

    /** find all suggestion targets by source (paged) */
    public List<SuggestionTarget> findAllTargets(Context context, String source, int pageSize, long offset);

    /** count all (unprocessed) suggestions by the given target uuid */
    public long countAllByTarget(Context context, UUID target);

    /** find suggestion target by targeted item (paged) */
    public List<SuggestionTarget> findByTarget(Context context, UUID target, int pageSize, long offset);

    /** find suggestion source by source name */
    public SuggestionSource findSource(Context context, String source);

    /** count all suggestion sources */
    public long countSources(Context context);

    /** find all suggestion sources (paged) */
    public List<SuggestionSource> findAllSources(Context context, int pageSize, long offset);

    /** find unprocessed suggestion by id */
    public Suggestion findUnprocessedSuggestion(Context context, String id);

    /** reject a specific suggestion by its id */
    public void rejectSuggestion(Context context, String id);

    /** find all suggestions by targeted item and external source */
    public List<Suggestion> findByTargetAndSource(Context context, UUID target, String source, int pageSize,
            long offset, boolean ascending);

    /** count all suggestions by targeted item id and source name */
    public long countAllByTargetAndSource(Context context, String source, UUID target);

    /** returns all suggestion providers */
    public List<SuggestionProvider> getSuggestionProviders();
}
