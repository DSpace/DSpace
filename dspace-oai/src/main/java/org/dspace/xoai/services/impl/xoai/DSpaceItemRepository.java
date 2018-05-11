/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.xoai.services.impl.xoai;

import com.lyncode.xoai.dataprovider.core.ListItemIdentifiersResult;
import com.lyncode.xoai.dataprovider.core.ListItemsResults;
import com.lyncode.xoai.dataprovider.data.Filter;
import com.lyncode.xoai.dataprovider.exceptions.OAIException;
import com.lyncode.xoai.dataprovider.filter.Scope;
import com.lyncode.xoai.dataprovider.filter.ScopedFilter;
import com.lyncode.xoai.dataprovider.filter.conditions.Condition;
import com.lyncode.xoai.dataprovider.services.api.ItemRepository;
import org.dspace.xoai.filter.DSpaceSetSpecFilter;
import org.dspace.xoai.filter.DateFromFilter;
import org.dspace.xoai.filter.DateUntilFilter;
import org.dspace.xoai.services.api.CollectionsService;
import org.dspace.xoai.services.api.HandleResolver;

import java.util.Date;
import java.util.List;

/**
 * 
 * @author Lyncode Development Team <dspace@lyncode.com>
 */
public abstract class DSpaceItemRepository implements ItemRepository
{
    private CollectionsService collectionsService;
    private HandleResolver handleResolver;

    protected DSpaceItemRepository(CollectionsService collectionsService, HandleResolver handleResolver) {
        this.collectionsService = collectionsService;
        this.handleResolver = handleResolver;
    }

    @Override
    public ListItemIdentifiersResult getItemIdentifiers(
            List<ScopedFilter> filters, int offset, int length, Date from) throws OAIException
    {
        filters.add(new ScopedFilter(getDateFromCondition(from), Scope.Query));
        return this.getItemIdentifiers(filters, offset, length);
    }

    @Override
    public ListItemIdentifiersResult getItemIdentifiers(
            List<ScopedFilter> filters, int offset, int length, String setSpec) throws OAIException
    {
        filters.add(new ScopedFilter(getDSpaceSetSpecFilter(setSpec), Scope.Query));
        return this.getItemIdentifiers(filters, offset, length);
    }

    @Override
	public ListItemIdentifiersResult getItemIdentifiers(
            List<ScopedFilter> filters, int offset, int length, Date from, Date until) throws OAIException
    {
        filters.add(new ScopedFilter(getDateFromCondition(from), Scope.Query));
        filters.add(new ScopedFilter(getDateUntilFilter(until), Scope.Query));
        return this.getItemIdentifiers(filters, offset, length);
    }

    @Override
    public ListItemIdentifiersResult getItemIdentifiers(
            List<ScopedFilter> filters, int offset, int length, String setSpec,
            Date from) throws OAIException
    {
        filters.add(new ScopedFilter(getDateFromCondition(from), Scope.Query));
        filters.add(new ScopedFilter(getDSpaceSetSpecFilter(setSpec),
                Scope.Query));
        return this.getItemIdentifiers(filters, offset, length);
    }

    @Override
    public ListItemIdentifiersResult getItemIdentifiers(
            List<ScopedFilter> filters, int offset, int length, String setSpec,
            Date from, Date until) throws OAIException
    {
        filters.add(new ScopedFilter(getDateFromCondition(from), Scope.Query));
        filters.add(new ScopedFilter(getDateUntilFilter(until), Scope.Query));
        filters.add(new ScopedFilter(getDSpaceSetSpecFilter(setSpec),
                Scope.Query));
        return this.getItemIdentifiers(filters, offset, length);
    }

    @Override
    public ListItemIdentifiersResult getItemIdentifiersUntil(
            List<ScopedFilter> filters, int offset, int length, Date until) throws OAIException
    {
        filters.add(new ScopedFilter(getDateUntilFilter(until), Scope.Query));
        return this.getItemIdentifiers(filters, offset, length);
    }

    @Override
    public ListItemIdentifiersResult getItemIdentifiersUntil(
            List<ScopedFilter> filters, int offset, int length, String setSpec,
            Date until) throws OAIException
    {
        filters.add(new ScopedFilter(getDateUntilFilter(until), Scope.Query));
        filters.add(new ScopedFilter(getDSpaceSetSpecFilter(setSpec),
                Scope.Query));
        return this.getItemIdentifiers(filters, offset, length);
    }

    @Override
    public ListItemsResults getItems(List<ScopedFilter> filters, int offset,
            int length, Date from) throws OAIException
    {
        filters.add(new ScopedFilter(getDateFromCondition(from), Scope.Query));
        return this.getItems(filters, offset, length);
    }

    @Override
    public ListItemsResults getItems(List<ScopedFilter> filters, int offset,
            int length, String setSpec) throws OAIException
    {
        filters.add(new ScopedFilter(getDSpaceSetSpecFilter(setSpec),
                Scope.Query));
        return this.getItems(filters, offset, length);
    }

    @Override
    public ListItemsResults getItems(List<ScopedFilter> filters, int offset,
            int length, Date from, Date until) throws OAIException
    {
        filters.add(new ScopedFilter(getDateFromCondition(from), Scope.Query));
        filters.add(new ScopedFilter(getDateUntilFilter(until), Scope.Query));
        return this.getItems(filters, offset, length);
    }

    @Override
    public ListItemsResults getItems(List<ScopedFilter> filters, int offset,
            int length, String setSpec, Date from) throws OAIException
    {
        filters.add(new ScopedFilter(getDateFromCondition(from), Scope.Query));
        filters.add(new ScopedFilter(getDSpaceSetSpecFilter(setSpec),
                Scope.Query));
        return this.getItems(filters, offset, length);
    }

    @Override
    public ListItemsResults getItems(List<ScopedFilter> filters, int offset,
            int length, String setSpec, Date from, Date until) throws OAIException
    {
        filters.add(new ScopedFilter(getDateFromCondition(from), Scope.Query));
        filters.add(new ScopedFilter(getDateUntilFilter(until), Scope.Query));
        filters.add(new ScopedFilter(getDSpaceSetSpecFilter(setSpec),
                Scope.Query));
        return this.getItems(filters, offset, length);
    }

    @Override
    public ListItemsResults getItemsUntil(List<ScopedFilter> filters, int offset,
            int length, Date until) throws OAIException
    {
        filters.add(new ScopedFilter(getDateUntilFilter(until), Scope.Query));
        return this.getItems(filters, offset, length);
    }

    @Override
    public ListItemsResults getItemsUntil(List<ScopedFilter> filters, int offset,
            int length, String setSpec, Date from) throws OAIException
    {
        filters.add(new ScopedFilter(getDateUntilFilter(from), Scope.Query));
        filters.add(new ScopedFilter(getDSpaceSetSpecFilter(setSpec),
                Scope.Query));
        return this.getItems(filters, offset, length);
    }

    private Condition getDateFromCondition(final Date from) {
        return new Condition() {
            @Override
            public Filter getFilter() {
                return new DateFromFilter(from);
            }
        };
    }

    private Condition getDSpaceSetSpecFilter(final String setSpec) {
        return new Condition() {
            @Override
            public Filter getFilter() {
                return new DSpaceSetSpecFilter(collectionsService, handleResolver, setSpec);
            }
        };
    }

    private Condition getDateUntilFilter(final Date until) {
        return new Condition() {
            @Override
            public Filter getFilter() {
                return new DateUntilFilter(until);
            }
        };
    }
}
