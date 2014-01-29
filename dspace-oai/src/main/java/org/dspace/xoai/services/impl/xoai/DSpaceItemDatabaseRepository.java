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
import com.lyncode.xoai.dataprovider.core.ReferenceSet;
import com.lyncode.xoai.dataprovider.data.Item;
import com.lyncode.xoai.dataprovider.data.ItemIdentifier;
import com.lyncode.xoai.dataprovider.exceptions.IdDoesNotExistException;
import com.lyncode.xoai.dataprovider.exceptions.OAIException;
import com.lyncode.xoai.dataprovider.filter.ScopedFilter;
import com.lyncode.xoai.dataprovider.xml.xoai.Metadata;
import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.dspace.content.Collection;
import org.dspace.content.Community;
import org.dspace.content.DSpaceObject;
import org.dspace.content.ItemIterator;
import org.dspace.handle.HandleManager;
import org.dspace.storage.rdbms.DatabaseManager;
import org.dspace.storage.rdbms.TableRowIterator;
import org.dspace.xoai.data.DSpaceDatabaseItem;
import org.dspace.xoai.data.DSpaceSet;
import org.dspace.xoai.services.api.cache.XOAIItemCacheService;
import org.dspace.xoai.services.api.config.ConfigurationService;
import org.dspace.xoai.services.api.context.ContextService;
import org.dspace.xoai.services.api.context.ContextServiceException;
import org.dspace.xoai.services.api.database.*;
import org.dspace.xoai.util.ItemUtils;

import java.io.IOException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

/**
 * 
 * @author Lyncode Development Team <dspace@lyncode.com>
 * @author Domingo Iglesias <diglesias@ub.edu>
 */
public class DSpaceItemDatabaseRepository extends DSpaceItemRepository
{

    private static Logger log = LogManager.getLogger(DSpaceItemDatabaseRepository.class);

    private XOAIItemCacheService cacheService;
    private boolean useCache;
    private DatabaseQueryResolver queryResolver;
    private ContextService context;
    private CollectionsService collectionsService;
    private ConfigurationService configurationService;

    public DSpaceItemDatabaseRepository(ConfigurationService configurationService, CollectionsService collectionsService, HandleResolver handleResolver, XOAIItemCacheService cacheService, DatabaseQueryResolver queryResolver, ContextService context)
    {
        super(collectionsService, handleResolver);
        this.configurationService = configurationService;
        this.collectionsService = collectionsService;
        this.cacheService = cacheService;
        this.queryResolver = queryResolver;
        this.context = context;
        this.useCache = configurationService.getBooleanProperty("oai", "cache.enabled", true);
    }
    
    private Metadata getMetadata (org.dspace.content.Item item) throws IOException {
        if (this.useCache) {
            if (!cacheService.hasCache(item))
                cacheService.put(item, ItemUtils.retrieveMetadata(item));
            
            return cacheService.get(item);
        } else return ItemUtils.retrieveMetadata(item);
    }

    private List<ReferenceSet> getSets(org.dspace.content.Item item)
    {
        List<ReferenceSet> sets = new ArrayList<ReferenceSet>();
        List<Community> coms = new ArrayList<Community>();
        try
        {
            Collection[] itemCollections = item.getCollections();
            for (Collection col : itemCollections)
            {
                ReferenceSet s = new DSpaceSet(col);
                sets.add(s);
                for (Community com : collectionsService.flatParentCommunities(col))
                    if (!coms.contains(com))
                        coms.add(com);
            }
            for (Community com : coms)
            {
                ReferenceSet s = new DSpaceSet(com);
                sets.add(s);
            }
        }
        catch (SQLException e)
        {
            log.error(e.getMessage(), e);
        }
        return sets;
    }

    @Override
    public Item getItem(String id) throws IdDoesNotExistException, OAIException
    {
        try
        {
            String parts[] = id.split(Pattern.quote(":"));
            if (parts.length == 3)
            {
                DSpaceObject obj = HandleManager.resolveToObject(context.getContext(),
                        parts[2]);
                if (obj == null)
                    throw new IdDoesNotExistException();
                if (!(obj instanceof Item))
                    throw new IdDoesNotExistException();

                org.dspace.content.Item item = (org.dspace.content.Item) obj;
                return new DSpaceDatabaseItem(item, this.getMetadata(item), getSets(item));
            }
        }
        catch (NumberFormatException e)
        {
            log.debug(e.getMessage(), e);
            throw new IdDoesNotExistException();
        }
        catch (SQLException e)
        {
            throw new OAIException(e);
        } catch (IOException e) {
            throw new OAIException(e);
        } catch (ContextServiceException e) {
            throw new OAIException(e);
        }
        throw new IdDoesNotExistException();
    }


    @Override
    public ListItemsResults getItems(List<ScopedFilter> filters, int offset,
            int length) throws OAIException
    {
        List<Item> list = new ArrayList<Item>();
        try
        {
            DatabaseQuery databaseQuery = queryResolver.buildQuery(filters, offset, length);
            TableRowIterator rowIterator = DatabaseManager.queryTable(context.getContext(), "item",
                    databaseQuery.getQuery(), databaseQuery.getParameters().toArray());
            ItemIterator iterator = new ItemIterator(context.getContext(), rowIterator);
            int i = 0;
            while (iterator.hasNext() && i < length)
            {
                org.dspace.content.Item it = iterator.next();
                list.add(new DSpaceDatabaseItem(it, this.getMetadata(it), getSets(it)));
                i++;
            }
            return new ListItemsResults((databaseQuery.getTotal() > offset + length), list, databaseQuery.getTotal());
        }
        catch (SQLException e)
        {
            log.error(e.getMessage(), e);
        } catch (DatabaseQueryException e) {
            log.error(e.getMessage(), e);
        } catch (IOException e) {
            log.error(e.getMessage(), e);
        } catch (ContextServiceException e) {
            log.error(e.getMessage(), e);
        }
        return new ListItemsResults(false, list, 0);
    }

    @Override
    public ListItemIdentifiersResult getItemIdentifiers(
            List<ScopedFilter> filters, int offset, int length) throws OAIException
    {

        List<ItemIdentifier> list = new ArrayList<ItemIdentifier>();
        try
        {
            DatabaseQuery databaseQuery = queryResolver.buildQuery(filters, offset, length);
            TableRowIterator rowIterator = DatabaseManager.queryTable(context.getContext(), "item",
                    databaseQuery.getQuery(), databaseQuery.getParameters().toArray());
            ItemIterator iterator = new ItemIterator(context.getContext(), rowIterator);
            int i = 0;
            while (iterator.hasNext() && i < length)
            {
                org.dspace.content.Item it = iterator.next();
                list.add(new DSpaceDatabaseItem(it, this.getMetadata(it), getSets(it)));
                i++;
            }
            return new ListItemIdentifiersResult((databaseQuery.getTotal() > offset + length), list, databaseQuery.getTotal());
        }
        catch (SQLException e)
        {
            log.error(e.getMessage(), e);
        } catch (DatabaseQueryException e) {
            log.error(e.getMessage(), e);
        } catch (IOException e) {
            log.error(e.getMessage(), e);
        } catch (ContextServiceException e) {
            log.error(e.getMessage(), e);
        }
        return new ListItemIdentifiersResult(false, list, 0);
    }
}
