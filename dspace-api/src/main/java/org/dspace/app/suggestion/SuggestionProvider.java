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
import org.dspace.external.model.ExternalDataObject;

/**
 * 
 * Interface for suggestion management like finding and counting.
 * @see org.dspace.app.suggestion.SuggestionTarget
 * @author Francesco Bacchelli (francesco.bacchelli at 4science.com)
 *
 */
public interface SuggestionProvider {

    /** find all suggestion targets
     * @see org.dspace.app.suggestion.SuggestionTarget
     * */
    public List<SuggestionTarget> findAllTargets(Context context, int pageSize, long offset);

    /** count all suggestion targets */
    public long countAllTargets(Context context);

    /** find a suggestion target by UUID */
    public SuggestionTarget findTarget(Context context, UUID target);

    /** find unprocessed suggestions (paged) by target UUID
     * @see org.dspace.app.suggestion.Suggestion
     * */
    public List<Suggestion> findAllUnprocessedSuggestions(Context context, UUID target, int pageSize, long offset,
            boolean ascending);

    /** find unprocessed suggestions by target UUID */
    public long countUnprocessedSuggestionByTarget(Context context, UUID target);

    /** find an unprocessed suggestion by target UUID and suggestion id */
    public Suggestion findUnprocessedSuggestion(Context context, UUID target, String id);

    /** reject a specific suggestion by target @param target and by suggestion id @param idPart */
    public void rejectSuggestion(Context context, UUID target, String idPart);

    /** flag a suggestion as processed */
    public void flagRelatedSuggestionsAsProcessed(Context context, ExternalDataObject externalDataObject);

}
