/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.repository;

import java.io.IOException;
import java.io.InputStream;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.UUID;
import javax.servlet.http.HttpServletRequest;

import org.dspace.app.rest.model.BitstreamRest;
import org.dspace.app.rest.model.patch.Patch;
import org.dspace.app.rest.projection.Projection;
import org.dspace.app.rest.repository.patch.DSpaceObjectPatch;
import org.dspace.authorize.AuthorizeException;
import org.dspace.content.Bitstream;
import org.dspace.content.Collection;
import org.dspace.content.Community;
import org.dspace.content.service.BitstreamService;
import org.dspace.content.service.CollectionService;
import org.dspace.content.service.CommunityService;
import org.dspace.core.Context;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.rest.webmvc.ResourceNotFoundException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Component;


/**
 * This is the repository responsible to manage Bitstream Rest object
 *
 * @author Andrea Bollini (andrea.bollini at 4science.it)
 */

@Component(BitstreamRest.CATEGORY + "." + BitstreamRest.NAME)
public class BitstreamRestRepository extends DSpaceObjectRestRepository<Bitstream, BitstreamRest> {

    private final BitstreamService bs;

    @Autowired
    private CollectionService collectionService;

    @Autowired
    private CommunityService communityService;

    @Autowired
    public BitstreamRestRepository(BitstreamService dsoService) {
        super(dsoService, new DSpaceObjectPatch<BitstreamRest>() { });
        this.bs = dsoService;
    }

    @Override
    @PreAuthorize("hasPermission(#id, 'BITSTREAM', 'READ')")
    public BitstreamRest findOne(Context context, UUID id) {
        Bitstream bit = null;
        try {
            bit = bs.find(context, id);
        } catch (SQLException e) {
            throw new RuntimeException(e.getMessage(), e);
        }
        if (bit == null) {
            return null;
        }
        try {
            if (bit.isDeleted() == true) {
                throw new ResourceNotFoundException();
            }
        } catch (SQLException e) {
            throw new RuntimeException(e.getMessage(), e);
        }
        return converter.toRest(bit, utils.obtainProjection());
    }

    @Override
    @PreAuthorize("hasAuthority('ADMIN')")
    public Page<BitstreamRest> findAll(Context context, Pageable pageable) {
        List<Bitstream> bit = new ArrayList<Bitstream>();
        Iterator<Bitstream> it = null;
        int total = 0;
        try {
            total = bs.countTotal(context);
            it = bs.findAll(context, pageable.getPageSize(), pageable.getOffset());
            while (it.hasNext()) {
                bit.add(it.next());
            }
        } catch (SQLException e) {
            throw new RuntimeException(e.getMessage(), e);
        }
        Projection projection = utils.obtainProjection(true);
        Page<BitstreamRest> page = new PageImpl<>(bit, pageable, total)
                .map((bitstream) -> converter.toRest(bitstream, projection));
        return page;
    }

    @Override
    @PreAuthorize("hasPermission(#id, 'BITSTREAM', 'WRITE')")
    protected void patch(Context context, HttpServletRequest request, String apiCategory, String model, UUID id,
                         Patch patch) throws AuthorizeException, SQLException {
        patchDSpaceObject(apiCategory, model, id, patch);
    }

    @Override
    public Class<BitstreamRest> getDomainClass() {
        return BitstreamRest.class;
    }

    @Override
    protected void delete(Context context, UUID id) throws AuthorizeException {
        Bitstream bit = null;
        try {
            bit = bs.find(context, id);
            if (bit == null) {
                throw new ResourceNotFoundException("The bitstream with uuid " + id + " could not be found");
            }
            if (bit.isDeleted()) {
                throw new ResourceNotFoundException("The bitstream with uuid " + id + " was already deleted");
            }
            Community community = bit.getCommunity();
            if (community != null) {
                communityService.setLogo(context, community, null);
            }
            Collection collection = bit.getCollection();
            if (collection != null) {
                collectionService.setLogo(context, collection, null);
            }
        } catch (SQLException | IOException e) {
            throw new RuntimeException(e.getMessage(), e);
        }
        try {
            bs.delete(context, bit);
        } catch (SQLException | IOException e) {
            throw new RuntimeException(e.getMessage(), e);
        }
    }

    public InputStream retrieve(UUID uuid) {
        Context context = obtainContext();
        Bitstream bit = null;
        try {
            bit = bs.find(context, uuid);
        } catch (SQLException e) {
            throw new RuntimeException(e.getMessage(), e);
        }
        if (bit == null) {
            return null;
        }
        InputStream is;
        try {
            is = bs.retrieve(context, bit);
        } catch (IOException | SQLException | AuthorizeException e) {
            throw new RuntimeException(e.getMessage(), e);
        }
        context.abort();
        return is;
    }
}
