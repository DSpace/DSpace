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
 * 
 *
 * @author Andrea Bollini (andrea.bollini at 4science.it)
 */
public interface SuggestionService {

    public void addSuggestionTarget(SuggestionTarget target);

    public SuggestionTarget find(Context context, String source, UUID id);

    public long countAll(Context context, String source);

    public List<SuggestionTarget> findAllTargets(Context context, String source, int pageSize, long offset);


    public void deleteTarget(SuggestionTarget target);
}
