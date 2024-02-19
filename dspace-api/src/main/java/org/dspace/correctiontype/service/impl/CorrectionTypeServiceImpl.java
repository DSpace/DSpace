/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.correctiontype.service.impl;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.collections4.CollectionUtils;
import org.dspace.authorize.AuthorizeException;
import org.dspace.content.Item;
import org.dspace.core.Context;
import org.dspace.correctiontype.CorrectionType;
import org.dspace.correctiontype.service.CorrectionTypeService;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Service implementation class for the CorrectionType object.
 *
 * @author Mohamed Eskander (mohamed.eskander at 4science.com)
 */
public class CorrectionTypeServiceImpl implements CorrectionTypeService {

    @Autowired
    private List<CorrectionType> correctionTypes;

    @Override
    public CorrectionType findOne(String id) {
        return findAll().stream()
                        .filter(correctionType -> correctionType.getId().equals(id))
                        .findFirst()
                        .orElse(null);
    }

    @Override
    public List<CorrectionType> findAll() {
        return CollectionUtils.isNotEmpty(correctionTypes) ? correctionTypes : List.of();
    }

    @Override
    public List<CorrectionType> findByItem(Context context, Item item) throws AuthorizeException, SQLException {
        List<CorrectionType> correctionTypes = new ArrayList<>();
        for (CorrectionType correctionType : findAll()) {
            if (correctionType.isAllowed(context, item)) {
                correctionTypes.add(correctionType);
            }
        }
        return correctionTypes;
    }

    @Override
    public CorrectionType findByTopic(String topic) {
        return findAll().stream()
                        .filter(correctionType -> correctionType.getTopic().equals(topic))
                        .findFirst()
                        .orElse(null);
    }

}
