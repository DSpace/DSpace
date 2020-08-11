/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.repository;

import java.sql.SQLException;
import java.util.Arrays;
import java.util.Iterator;
import javax.servlet.http.HttpServletRequest;

import org.apache.commons.lang3.StringUtils;
import org.dspace.app.rest.converter.BrowseEntryConverter;
import org.dspace.app.rest.model.BrowseEntryRest;
import org.dspace.app.rest.model.BrowseIndexRest;
import org.dspace.app.rest.projection.Projection;
import org.dspace.app.rest.utils.ScopeResolver;
import org.dspace.browse.BrowseEngine;
import org.dspace.browse.BrowseException;
import org.dspace.browse.BrowseIndex;
import org.dspace.browse.BrowseInfo;
import org.dspace.browse.BrowserScope;
import org.dspace.core.Context;
import org.dspace.discovery.IndexableObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.domain.Sort.Order;
import org.springframework.stereotype.Component;

/**
 * This is the repository responsible to retrieve the first level values
 * (Entries) of a metadata browse
 *
 * @author Andrea Bollini (andrea.bollini at 4science.it)
 */
@Component(BrowseIndexRest.CATEGORY + "." + BrowseIndexRest.NAME + "." + BrowseIndexRest.ENTRIES)
public class BrowseEntryLinkRepository extends AbstractDSpaceRestRepository
    implements LinkRestRepository {

    @Autowired
    BrowseEntryConverter browseEntryConverter;

    @Autowired
    ScopeResolver scopeResolver;

    // FIXME It will be nice to drive arguments binding by annotation as in normal spring controller methods
    public Page<BrowseEntryRest> listBrowseEntries(HttpServletRequest request, String browseName,
                                                   Pageable pageable, Projection projection)
        throws BrowseException, SQLException {
        // FIXME this should be bind automatically and available as method
        // argument
        String scope = null;
        String startsWith = null;

        if (request != null) {
            scope = request.getParameter("scope");
            startsWith = request.getParameter("startsWith");
        }


        Context context = obtainContext();
        BrowseEngine be = new BrowseEngine(context);
        BrowserScope bs = new BrowserScope(context);

        IndexableObject scopeObj = scopeResolver.resolveScope(context, scope);

        // process the input, performing some inline validation
        final BrowseIndex bi;
        if (StringUtils.isNotEmpty(browseName)) {
            bi = BrowseIndex.getBrowseIndex(browseName);
        } else {
            bi = null;
        }
        if (bi == null) {
            throw new IllegalArgumentException("Unknown browse index");
        }
        if (!bi.isMetadataIndex()) {
            throw new IllegalStateException("The requested browse haven't metadata entries");
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
                bs.setOrder(orders.next().getDirection().name());
            }
        }
        // bs.setFilterValue(value != null?value:authority);
        // bs.setFilterValueLang(valueLang);
        // bs.setJumpToItem(focus);
        // bs.setJumpToValue(valueFocus);
        // bs.setJumpToValueLang(valueFocusLang);
        bs.setStartsWith(startsWith);
        if (pageable != null) {
            bs.setOffset(Math.toIntExact(pageable.getOffset()));
            bs.setResultsPerPage(pageable.getPageSize());
        }
        // bs.setEtAl(etAl);
        // bs.setAuthorityValue(authority);

        if (scopeObj != null) {
            bs.setBrowseContainer(scopeObj);
        }

        BrowseInfo binfo = be.browse(bs);
        Pageable pageResultInfo = PageRequest.of((binfo.getStart() - 1) / binfo.getResultsPerPage(),
                                                  binfo.getResultsPerPage());
        Page<BrowseEntryRest> page = new PageImpl<>(Arrays.asList(binfo.getStringResults()), pageResultInfo,
                                                            binfo.getTotal()).map(browseEntryConverter);
        BrowseIndexRest biRest = converter.toRest(bi, projection);
        page.forEach(t -> t.setBrowseIndex(biRest));
        return page;
    }

    @Override
    public boolean isEmbeddableRelation(Object data, String name) {
        BrowseIndexRest bir = (BrowseIndexRest) data;
        if (bir.isMetadataBrowse() && "entries".equals(name)) {
            return true;
        }
        return false;
    }
}
