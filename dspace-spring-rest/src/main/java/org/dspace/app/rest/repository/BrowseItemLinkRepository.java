/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.repository;

import java.sql.SQLException;
import java.util.Iterator;
import java.util.UUID;
import javax.servlet.http.HttpServletRequest;

import org.apache.commons.lang3.StringUtils;
import org.dspace.app.rest.converter.ItemConverter;
import org.dspace.app.rest.model.BrowseIndexRest;
import org.dspace.app.rest.model.ItemRest;
import org.dspace.app.rest.model.hateoas.ItemResource;
import org.dspace.browse.BrowseEngine;
import org.dspace.browse.BrowseException;
import org.dspace.browse.BrowseIndex;
import org.dspace.browse.BrowseInfo;
import org.dspace.browse.BrowserScope;
import org.dspace.content.DSpaceObject;
import org.dspace.content.Item;
import org.dspace.content.service.CollectionService;
import org.dspace.content.service.CommunityService;
import org.dspace.core.Context;
import org.dspace.sort.SortException;
import org.dspace.sort.SortOption;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.domain.Sort.Order;
import org.springframework.stereotype.Component;

/**
 * This is the repository to retrieve the items associated with a specific
 * browse index or entries
 * 
 * @author Andrea Bollini (andrea.bollini at 4science.it)
 *
 */
@Component(BrowseIndexRest.CATEGORY + "." + BrowseIndexRest.NAME + "." + BrowseIndexRest.ITEMS)
public class BrowseItemLinkRepository extends AbstractDSpaceRestRepository
		implements LinkRestRepository<ItemRest> {
	@Autowired
	ItemConverter converter;
	
	@Autowired
	ItemRestRepository itemRestRepository;

	@Autowired
	CollectionService collectionService;

	@Autowired
	CommunityService communityService;

	public Page<ItemRest> listBrowseItems(HttpServletRequest request, String browseName, Pageable pageable, String projection)
			throws BrowseException, SQLException {
		//FIXME these should be bind automatically and available as method arguments
		String scope = null;
		String filterValue = null;
		String filterAuthority = null;
		
		if (request != null) {		
			scope = request.getParameter("scope");
			filterValue = request.getParameter("filterValue");
			filterAuthority = request.getParameter("filterAuthority");
		}
		Context context = obtainContext();
		BrowseEngine be = new BrowseEngine(context);
		BrowserScope bs = new BrowserScope(context);
		DSpaceObject scopeObj = null;
		if (scope != null) {
			UUID uuid = UUID.fromString(scope);
			scopeObj = communityService.find(context, uuid);
			if (scopeObj == null) {
				scopeObj = collectionService.find(context, uuid);
			}
		}

		// process the input, performing some inline validation
		BrowseIndex bi = null;
		if (StringUtils.isNotEmpty(browseName)) {
			bi = BrowseIndex.getBrowseIndex(browseName);
		}
		if (bi == null) {
			throw new IllegalArgumentException("Unknown browse index");
		}
		if (!bi.isItemIndex() && (filterAuthority == null && filterValue==null)) {
			throw new IllegalStateException("The requested browse doesn't provide direct access to items you must specify a filter");
		}
		
		if (!bi.isMetadataIndex() && (filterAuthority != null || filterValue!=null)) {
			throw new IllegalStateException("The requested browse doesn't support filtering");
		}

		// set up a BrowseScope and start loading the values into it
		bs.setBrowseIndex(bi);
		Sort sort = null;
		if (pageable != null) {
			sort = pageable.getSort();
		}
		if (sort != null) {
			Iterator<Order> orders = sort.iterator();
			while (orders.hasNext()) {
				Order order = orders.next();
				bs.setOrder(order.getDirection().name());
				String sortBy;
				if (!StringUtils.equals("default", order.getProperty())) {
					sortBy = order.getProperty();
				}
				else {
					sortBy = bi.getDefaultOrder();
				}
				
				try {
					SortOption so = SortOption.getSortOption(sortBy);
					if (so != null) {
						bs.setSortBy(so.getNumber());
					}
				} catch (SortException e) {
					throw new RuntimeException(e.getMessage(), e);
				}
			}
		}
		
		if (filterValue != null || filterAuthority != null) {
			bs.setFilterValue(filterValue);
			bs.setAuthorityValue(filterAuthority);
			bs.setBrowseLevel(1);
		}
		// bs.setFilterValueLang(valueLang);
		// bs.setJumpToItem(focus);
		// bs.setJumpToValue(valueFocus);
		// bs.setJumpToValueLang(valueFocusLang);
		// bs.setStartsWith(startsWith);
		if (pageable != null) {
			bs.setOffset(pageable.getOffset());
			bs.setResultsPerPage(pageable.getPageSize());
		}
		
		if (scopeObj != null) {
			bs.setBrowseContainer(scopeObj);
		}
		
		// For second level browses on metadata indexes, we need to adjust the default sorting
        if (bi != null && bi.isMetadataIndex() && bs.isSecondLevel() && bs.getSortBy() <= 0)
        {
            bs.setSortBy(1);
        }

		BrowseInfo binfo = be.browse(bs);

		Pageable pageResultInfo = new PageRequest((binfo.getStart() -1) / binfo.getResultsPerPage(), binfo.getResultsPerPage());
		Page<ItemRest> page = new PageImpl<Item>(binfo.getBrowseItemResults(), pageResultInfo, binfo.getTotal())
				.map(converter);
		return page;
	}

	@Override
	public ItemResource wrapResource(ItemRest item, String... rels) {
		return itemRestRepository.wrapResource(item, rels);
	}
	
	@Override
	public boolean isEmbeddableRelation(Object data, String name) {
		BrowseIndexRest bir = (BrowseIndexRest) data;
		if (!bir.isMetadataBrowse() && "items".equals(name)) {
			return true;
		}
		return false;
	}
}
