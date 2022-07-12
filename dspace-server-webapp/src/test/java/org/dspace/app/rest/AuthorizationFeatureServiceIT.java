/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.junit.Assert.assertThat;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.commons.lang3.ArrayUtils;
import org.dspace.app.rest.authorization.AlwaysFalseFeature;
import org.dspace.app.rest.authorization.AlwaysThrowExceptionFeature;
import org.dspace.app.rest.authorization.AlwaysTrueFeature;
import org.dspace.app.rest.authorization.AuthorizationFeature;
import org.dspace.app.rest.authorization.AuthorizationFeatureService;
import org.dspace.app.rest.authorization.TrueForAdminsFeature;
import org.dspace.app.rest.converter.SiteConverter;
import org.dspace.app.rest.model.CollectionRest;
import org.dspace.app.rest.model.SiteRest;
import org.dspace.app.rest.projection.DefaultProjection;
import org.dspace.app.rest.test.AbstractControllerIntegrationTest;
import org.dspace.content.Site;
import org.dspace.content.service.SiteService;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;


/**
 * Test for the Authorization Feature Service
 *
 * @author Andrea Bollini (andrea.bollini at 4science.it)
 *
 */
public class AuthorizationFeatureServiceIT extends AbstractControllerIntegrationTest {
    @Autowired
    private SiteService siteService;

    @Autowired
    private SiteConverter siteConverter;

    @Autowired
    private AuthorizationFeatureService authzFeatureService;

    @Test
    /**
     * All the features available in the Sprint Context should be returned
     *
     * @throws Exception
     */
    public void findAllTest() throws Exception {
        List<AuthorizationFeature> authzFeatureServiceFindAll = authzFeatureService.findAll();

        assertThat("We have at least our 7 mock features for testing",
                authzFeatureServiceFindAll.size(), greaterThanOrEqualTo(7));

        Set<String> featureNames = new HashSet<>();
        for (AuthorizationFeature f : authzFeatureServiceFindAll) {
            featureNames.add(f.getName());
        }

        assertThat("all the features must have unique name", authzFeatureServiceFindAll.size(),
                equalTo(featureNames.size()));
    }

    @Test
    /**
     * The find method should return existing feature and null in the case the feature doesn't exist
     *
     * @throws Exception
     */
    public void findTest() throws Exception {
        AuthorizationFeature aFeature = authzFeatureService.find(AlwaysTrueFeature.NAME);
        assertThat("check that one of our mock feature is retrieved", aFeature.getName(),
                equalTo(AlwaysTrueFeature.NAME));

        AuthorizationFeature aNotExistingFeature = authzFeatureService.find("this feature doesn't exist!");
        assertThat("check that not existing feature name return null", aNotExistingFeature, equalTo(null));
    }

    @Test
    /**
     * The findByResourceType must return only features that support the specified type
     *
     * @throws Exception
     */
    public void findByResourceTypeTest() throws Exception {
        // we have at least one feature that support the Site object
        final String siteUniqueType = SiteRest.CATEGORY + "." + SiteRest.NAME;
        List<AuthorizationFeature> siteFeatures = authzFeatureService.findByResourceType(siteUniqueType);
        assertThat(siteFeatures.size(), greaterThan(0));
        boolean alwaysTrueFound = false;
        for (AuthorizationFeature f : siteFeatures) {
            assertThat(ArrayUtils.contains(f.getSupportedTypes(), siteUniqueType), equalTo(true));
            alwaysTrueFound = alwaysTrueFound || AlwaysTrueFeature.NAME.equals(f.getName());
        }
        assertThat(alwaysTrueFound, equalTo(true));

        // we can check that the AlwaysTrueFeature is returned also when searching for a
        // type other than the Site (that is the first type supported by the feature)
        alwaysTrueFound = false;
        final String collectionUniqueType = CollectionRest.CATEGORY + "." + CollectionRest.NAME;
        List<AuthorizationFeature> collectionFeatures = authzFeatureService.findByResourceType(collectionUniqueType);
        for (AuthorizationFeature f : collectionFeatures) {
            assertThat(ArrayUtils.contains(f.getSupportedTypes(), collectionUniqueType), equalTo(true));
            alwaysTrueFound = alwaysTrueFound || AlwaysTrueFeature.NAME.equals(f.getName());
        }

        // finally check that not existing type will return an empty list
        final List<AuthorizationFeature> notExistingTypeFeatures = authzFeatureService
                .findByResourceType("NOT-EXISTING-TYPE");
        assertThat(notExistingTypeFeatures.size(), equalTo(0));
    }

    @Test
    /**
     * The isAuthorized must return true for authorized feature and false for not authorized feature
     *
     * @throws Exception
     */
    public void isAuthorizedTest() throws Exception {
        Site site = siteService.findSite(context);
        SiteRest siteRest = siteConverter.convert(site, DefaultProjection.DEFAULT);

        AuthorizationFeature alwaysTrue = authzFeatureService.find(AlwaysTrueFeature.NAME);
        AuthorizationFeature alwaysFalse = authzFeatureService.find(AlwaysFalseFeature.NAME);
        AuthorizationFeature alwaysThrowEx = authzFeatureService.find(AlwaysThrowExceptionFeature.NAME);
        AuthorizationFeature trueForAdmins = authzFeatureService.find(TrueForAdminsFeature.NAME);

        assertThat(authzFeatureService.isAuthorized(context, alwaysTrue, siteRest), equalTo(true));
        assertThat(authzFeatureService.isAuthorized(context, alwaysFalse, siteRest), equalTo(false));
        try {
            authzFeatureService.isAuthorized(context, alwaysThrowEx, siteRest);
            // this code should be not run as the previous one throw an exception that we expect to be re-thrown
            assertThat("the exception has been not re-thrown!", false, equalTo(true));
        } catch (Exception ex) {
            // if this code is executed the exception was re-thrown
            assertThat("exceptions are rethrown", true, equalTo(true));
        }
        assertThat(authzFeatureService.isAuthorized(context, trueForAdmins, siteRest), equalTo(false));
        // login our admin
        context.setCurrentUser(admin);
        assertThat(authzFeatureService.isAuthorized(context, trueForAdmins, siteRest), equalTo(true));
        // finally check that a null object will always result in false to be returned
        assertThat(authzFeatureService.isAuthorized(context, alwaysTrue, null), equalTo(false));
        // without call at all the authorizationFeature to prevent NPE
        assertThat(authzFeatureService.isAuthorized(context, alwaysThrowEx, null), equalTo(false));
    }

}
