/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.builder;

import java.util.HashMap;
import java.util.Map;

import org.dspace.app.suggestion.SuggestionService;
import org.dspace.app.suggestion.SuggestionTarget;
import org.dspace.content.Collection;
import org.dspace.content.Item;
import org.dspace.core.Context;
import org.dspace.eperson.EPerson;

/**
 * Builder to construct Item objects
 *
 * @author Andrea Bollini (andrea.bollini at 4science.it)
 */
public class SuggestionTargetBuilder extends AbstractBuilder<SuggestionTarget, SuggestionService> {

    private ItemBuilder itemBuilder;
    private SuggestionTarget target;
    private Map<String, Integer> totals = new HashMap<String, Integer>();

    protected SuggestionTargetBuilder(Context context) {
        super(context);
    }

    public static SuggestionTargetBuilder createTarget(final Context context, final Collection col) {
        SuggestionTargetBuilder builder = new SuggestionTargetBuilder(context);
        return builder.create(context, col);
    }

    private SuggestionTargetBuilder create(final Context context, final Collection col) {
        this.context = context;

        try {
            itemBuilder = ItemBuilder.createItem(context, col);
        } catch (Exception e) {
            return handleException(e);
        }

        return this;
    }

    public SuggestionTargetBuilder withPreferredName(final String title) {
        itemBuilder = itemBuilder.withTitle(title);
        return this;
    }

    public SuggestionTargetBuilder withSuggestionCount(final String source, final int count) {
        totals.put(source, count);
        return this;
    }

    public SuggestionTargetBuilder withOwner(EPerson eperson) {
        itemBuilder = itemBuilder.withCrisOwner(eperson.getFullName(), eperson.getID().toString());
        return this;
    }

    @Override
    public SuggestionTarget build() {
        try {
            Item item = itemBuilder.build();
            context.dispatchEvents();
            indexingService.commit();
            target = new SuggestionTarget(item);
            target.setTotals(totals);
            suggestionService.addSuggestionTarget(target);
            return target;
        } catch (Exception e) {
            return handleException(e);
        }
    }

    @Override
    public void cleanup() throws Exception {
        suggestionService.deleteTarget(target);
    }

    @Override
    protected SuggestionService getService() {
        return suggestionService;
    }

    @Override
    public void delete(Context c, SuggestionTarget dso) throws Exception {
        // TODO Auto-generated method stub
    }
}
