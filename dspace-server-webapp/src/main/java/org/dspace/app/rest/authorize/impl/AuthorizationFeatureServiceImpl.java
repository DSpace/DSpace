/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.authorize.impl;

import java.sql.SQLException;
import java.util.List;
import java.util.stream.Collectors;

import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;
import org.dspace.app.rest.authorize.AuthorizationFeature;
import org.dspace.app.rest.authorize.AuthorizationFeatureService;
import org.dspace.core.Context;
import org.dspace.discovery.FindableObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * This is the default implementation of the {@link AuthorizationFeatureService}. It is based on the spring autowiring
 * feature to discover all the features available in the system
 * 
 * @author Andrea Bollini (andrea.bollini at 4science.it)
 *
 */
@Service
public class AuthorizationFeatureServiceImpl implements AuthorizationFeatureService {
    @Autowired
    private List<AuthorizationFeature> features;

    @Override
    public boolean isAuthorized(Context context, AuthorizationFeature feature, FindableObject object)
            throws SQLException {
        if (object == null) {
            // the authorization interface require that the object is not null
            return false;
        }

        if (feature == null || !ArrayUtils.contains(feature.getSupportedTypes(), object.getType())) {
            return false;
        }
        return feature.isAuthorized(context, object);
    }

    @Override
    public List<AuthorizationFeature> findAll() {
        return features;
    }

    @Override
    public AuthorizationFeature find(String name) {
        for (AuthorizationFeature feature : features) {
            if (StringUtils.equals(name, feature.getName())) {
                return feature;
            }
        }
        return null;
    }

    @Override
    public List<AuthorizationFeature> findByResourceType(int typeID) {
        return features
                .stream()
                .filter(f -> ArrayUtils.contains(f.getSupportedTypes(), typeID))
                .collect(Collectors.toList());
//        List<AuthorizationFeature> foundFeatures = new ArrayList<AuthorizationFeature>();
//        for (AuthorizationFeature f : features) {
//            if (ArrayUtils.contains(f.getSupportedTypes(), typeID)) {
//                foundFeatures.add(f);
//            }
//        }
//        return null;
    }
}
