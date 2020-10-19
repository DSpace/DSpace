/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.builder;

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

    private Item item;
    private SuggestionTarget target;
    private String source;
    private int total;

    protected SuggestionTargetBuilder(Context context) {
        super(context);
    }

    public static SuggestionTargetBuilder createTarget(final Context context, final Collection col, final String name) {
        return createTarget(context, col, name, null);
    }

    public static SuggestionTargetBuilder createTarget(final Context context, final Collection col, final String name,
            final EPerson eperson) {
        SuggestionTargetBuilder builder = new SuggestionTargetBuilder(context);
        return builder.create(context, col, name, eperson);
    }

    public static SuggestionTargetBuilder createTarget(final Context context, final Item item) {
        SuggestionTargetBuilder builder = new SuggestionTargetBuilder(context);
        return builder.create(context, item);
    }

    private SuggestionTargetBuilder create(final Context context, final Collection col, final String name) {
        return create(context, col, name, null);
    }

    private SuggestionTargetBuilder create(final Context context, final Collection col, final String name,
            final EPerson eperson) {
        this.context = context;

        try {
            ItemBuilder itemBuilder = ItemBuilder.createItem(context, col).withTitle(name);
            if (eperson != null) {
                itemBuilder = itemBuilder.withCrisOwner(eperson.getFullName(), eperson.getID().toString());
            }
            item = itemBuilder.build();
            context.dispatchEvents();
            indexingService.commit();
        } catch (Exception e) {
            return handleException(e);
        }
        return this;
    }

    private SuggestionTargetBuilder create(final Context context, final Item item) {
        this.context = context;
        this.item = item;
        return this;
    }

    public SuggestionTargetBuilder withSuggestionCount(final String source, final int total) {
        this.source = source;
        this.total = total;
        return this;
    }

    @Override
    public SuggestionTarget build() {
        target = new SuggestionTarget(item);
        target.setTotal(total);
        target.setSource(source);
        suggestionService.addSuggestionTarget(target);
        return target;
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
        suggestionService.deleteTarget(dso);
    }
}
