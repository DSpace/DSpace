/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */

package org.dspace.eperson;

import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import org.apache.commons.lang3.StringUtils;
import org.dspace.AbstractDSpaceTest;
import org.dspace.authorize.AuthorizeException;
import org.dspace.authorize.service.AuthorizeService;
import org.dspace.content.DSpaceObject;
import org.dspace.core.Context;
import org.dspace.eperson.dao.SubscriptionDAO;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;

public class SubscribeServiceTest extends AbstractDSpaceTest {

    @Mock
    private SubscriptionDAO subscriptionDAO;

    @Mock
    private AuthorizeService authorizeService;

    @InjectMocks
    private SubscribeServiceImpl subscribeService;

    private final Context context = Mockito.mock(Context.class);

    @Test
    public void findAllWithValidResource() throws Exception {

        String resourceType = "Item";
        Integer limit = 10;
        Integer offset = 0;

        Subscription subscription = createSubscription("content",
                                                       getSubscriptionParameter("frequency", "W"));

        when(subscriptionDAO.findAllOrderedByIDAndResourceType(context, resourceType, limit, offset))
            .thenReturn(Collections.singletonList(subscription));

        List<Subscription> subscriptions = subscribeService.findAll(context, resourceType, limit, offset);

        assertEquals(subscriptions, Collections.singletonList(subscription));

    }

    @Test
    public void findAllWithoutResourceType() throws Exception {
        String resourceType = StringUtils.EMPTY;
        Integer limit = 10;
        Integer offset = 0;

        Subscription subscription = createSubscription("content",
                                                       getSubscriptionParameter("frequency", "W"));

        when(subscriptionDAO.findAllOrderedByDSO(context, limit, offset))
            .thenReturn(Collections.singletonList(subscription));

        List<Subscription> subscriptions = subscribeService.findAll(context, resourceType, limit, offset);

        assertEquals(subscriptions, Collections.singletonList(subscription));
    }

    @Test(expected = Exception.class)
    public void findAllWithInValidResource() throws Exception {

        String resourceType = "INVALID";
        Integer limit = 10;
        Integer offset = 0;

        subscribeService.findAll(context, resourceType, limit, offset);

        verifyNoInteractions(subscriptionDAO);

    }

    @Test
    public void newSubscriptionCreatedByAdmin() throws Exception {

        Subscription dbSubscription = createSubscription("content",
                                                       getSubscriptionParameter("frequency", "W"));

        when(authorizeService.isAdmin(context)).thenReturn(true);
        when(subscriptionDAO.create(eq(context), any(Subscription.class)))
            .thenReturn(dbSubscription);
        DSpaceObject dso = mock(DSpaceObject.class);

        List<SubscriptionParameter> parameters = Collections.singletonList(
            getSubscriptionParameter("frequency", "M"));

        Subscription subscription = subscribeService.subscribe(context, getePerson(),
                                                               dso, parameters, "content");

        assertEquals(subscription, dbSubscription);
    }

    @Test
    public void newSubscriptionCreatedByCurrentUser() throws Exception {

        Subscription dbSubscription = createSubscription("content",
                                                       getSubscriptionParameter("frequency", "W"));
        EPerson currentUser = getePerson();

        when(context.getCurrentUser()).thenReturn(currentUser);

        when(subscriptionDAO.create(eq(context), any(Subscription.class)))
            .thenReturn(dbSubscription);
        DSpaceObject dso = mock(DSpaceObject.class);

        List<SubscriptionParameter> parameters = Collections.singletonList(
            getSubscriptionParameter("frequency", "D"));

        Subscription subscription = subscribeService.subscribe(context, currentUser,
                                                               dso, parameters, "content");

        assertEquals(subscription, dbSubscription);
    }

    @Test(expected = AuthorizeException.class)
    public void nonAdminDifferentUserTriesToSubscribe() throws Exception {

        EPerson currentUser = getePerson();
        EPerson subscribingUser = getePerson();

        when(context.getCurrentUser()).thenReturn(currentUser);

        DSpaceObject dso = mock(DSpaceObject.class);

        List<SubscriptionParameter> parameters = Collections.singletonList(
            getSubscriptionParameter("frequency", "D"));

        subscribeService.subscribe(context, subscribingUser,dso, parameters, "content");

        verifyNoInteractions(subscriptionDAO);
    }

    @Test(expected = SQLException.class)
    public void exceptionWhileStoringSubscription() throws Exception {
        EPerson currentUser = getePerson();

        when(context.getCurrentUser()).thenReturn(currentUser);

        doThrow(new SQLException()).when(subscriptionDAO).create(eq(context), any(Subscription.class));

        DSpaceObject dso = mock(DSpaceObject.class);

        List<SubscriptionParameter> parameters = Collections.singletonList(
            getSubscriptionParameter("frequency", "D"));

        subscribeService.subscribe(context, currentUser,dso, parameters, "content");

    }

    @Test
    public void unsubscribeByAdmin() throws Exception {

        when(authorizeService.isAdmin(context)).thenReturn(true);
        DSpaceObject dso = mock(DSpaceObject.class);

        EPerson currentUser = getePerson();
        subscribeService.unsubscribe(context, currentUser,
                                     dso);
        verify(subscriptionDAO).deleteByDSOAndEPerson(context, dso, currentUser);
    }

    @Test
    public void unsubscribeByCurrentUser() throws Exception {

        EPerson currentUser = getePerson();
        when(authorizeService.isAdmin(context)).thenReturn(false);
        when(context.getCurrentUser()).thenReturn(currentUser);

        DSpaceObject dso = mock(DSpaceObject.class);

        subscribeService.unsubscribe(context, currentUser, dso);
        verify(subscriptionDAO).deleteByDSOAndEPerson(context, dso, currentUser);
    }

    @Test(expected = AuthorizeException.class)
    public void nonAdminDifferentUserTriesToUnSubscribe() throws Exception {
        EPerson currentUser = getePerson();
        EPerson unsubscribingUser = getePerson();

        when(authorizeService.isAdmin(context)).thenReturn(false);
        when(context.getCurrentUser()).thenReturn(currentUser);

        DSpaceObject dso = mock(DSpaceObject.class);

        subscribeService.unsubscribe(context, unsubscribingUser, dso);

        verifyNoInteractions(subscriptionDAO);
    }

    @Test(expected = SQLException.class)
    public void exceptionWhileDeletingSubscription() throws Exception {
        EPerson currentUser = getePerson();

        when(context.getCurrentUser()).thenReturn(currentUser);

        DSpaceObject dso = mock(DSpaceObject.class);

        doThrow(new SQLException()).when(subscriptionDAO).deleteByDSOAndEPerson(eq(context), eq(dso),
                                                                                eq(currentUser));

        subscribeService.unsubscribe(context, currentUser,dso);

    }

    @Test
    public void updateSubscription() throws Exception {


        int subscriptionId = 434123;

        Subscription subscription = createSubscription("original", getSubscriptionParameter("frequency", "D"),
                                                       getSubscriptionParameter("frequency", "M"));

        when(subscriptionDAO.findByID(context, Subscription.class, subscriptionId))
            .thenReturn(subscription);

        String updatedType = "updated";
        List<SubscriptionParameter> updatedParameters = Collections.singletonList(
            getSubscriptionParameter("frequency", "A")
        );

        Subscription updated = subscribeService.updateSubscription(context, subscriptionId, null, null,
                                                                        updatedParameters, updatedType);

        ArgumentCaptor<Subscription> captor = ArgumentCaptor.forClass(Subscription.class);
        verify(subscriptionDAO).save(eq(context), captor.capture());

        assertEquals(captor.getValue().getSubscriptionParameterList(), updatedParameters);
        assertEquals(captor.getValue().getSubscriptionType(), updatedType);

        assertEquals(updated.getSubscriptionType(), updatedType);
        assertEquals(updated.getSubscriptionParameterList(), updatedParameters);

    }

    @Test(expected = SQLException.class)
    public void errorWhileUpdating() throws Exception {


        int subscriptionId = 434;

        Subscription subscription = createSubscription("original", getSubscriptionParameter("frequency", "D"),
                                                       getSubscriptionParameter("frequency", "M"));

        when(subscriptionDAO.findByID(context, Subscription.class, subscriptionId))
            .thenReturn(subscription);
        doThrow(new SQLException()).when(subscriptionDAO).save(eq(context), any(Subscription.class));

        String updatedType = "updated";
        List<SubscriptionParameter> updatedParameters = Collections.singletonList(
            getSubscriptionParameter("frequency", "A")
        );

        subscribeService.updateSubscription(context, subscriptionId, null, null,
                                                                   updatedParameters, updatedType);

    }

    @Test
    public void parametersAdditionAndRemoval() throws Exception {

        int subscriptionId = 43123;

        SubscriptionParameter firstParameter = getSubscriptionParameter("key1", "value1");
        SubscriptionParameter secondParameter = getSubscriptionParameter("key2", "value2");

        Subscription existingSubscription = createSubscription("type",
                                                               firstParameter, secondParameter);

        when(subscriptionDAO.findByID(context, Subscription.class, subscriptionId))
            .thenReturn(existingSubscription);

        SubscriptionParameter addedParameter = getSubscriptionParameter("added", "add");

        Subscription updatedSubscription = subscribeService.addSubscriptionParameter(context, subscriptionId,
                                                                                     addedParameter);

        assertParametersAreUpdated(updatedSubscription, List.of(firstParameter, secondParameter,
                                                                addedParameter));

        updatedSubscription = subscribeService.removeSubscriptionParameter(context, subscriptionId,
                                                                        secondParameter);

        assertParametersAreUpdated(updatedSubscription, List.of(firstParameter, addedParameter));


    }

    @Test(expected = SQLException.class)
    public void exceptionWhileAddingParameters() throws Exception {
        int subscriptionId = 43123;

        SubscriptionParameter firstParameter = getSubscriptionParameter("key1", "value1");
        SubscriptionParameter secondParameter = getSubscriptionParameter("key2", "value2");

        Subscription existingSubscription = createSubscription("type",
                                                               firstParameter, secondParameter);

        when(subscriptionDAO.findByID(context, Subscription.class, subscriptionId))
            .thenReturn(existingSubscription);


        doThrow(new SQLException()).when(subscriptionDAO).save(eq(context), any(Subscription.class));

        subscribeService.addSubscriptionParameter(context, subscriptionId,
                                                  getSubscriptionParameter("added", "add"));
    }

    @Test(expected = SQLException.class)
    public void exceptionWhileRemovingParameters() throws Exception {
        int subscriptionId = 43127;

        SubscriptionParameter firstParameter = getSubscriptionParameter("key1", "value1");
        SubscriptionParameter secondParameter = getSubscriptionParameter("key2", "value2");

        Subscription existingSubscription = createSubscription("type",
                                                               firstParameter, secondParameter);

        when(subscriptionDAO.findByID(context, Subscription.class, subscriptionId))
            .thenReturn(existingSubscription);


        doThrow(new SQLException()).when(subscriptionDAO).save(eq(context), any(Subscription.class));

        subscribeService.removeSubscriptionParameter(context, subscriptionId,
                                                  secondParameter);
    }

    private static Subscription createSubscription(String type, SubscriptionParameter... parameters) {
        Subscription subscription = new Subscription();
        subscription.setSubscriptionType(type);

        subscription.setSubscriptionParameterList
                        (new ArrayList<>(Arrays.asList(parameters)));
        return subscription;
    }

    private void assertParametersAreUpdated(Subscription updatedSubscription,
                                                                       List<SubscriptionParameter> expectedParameters)
        throws SQLException {
        ArgumentCaptor<Subscription> captor = ArgumentCaptor.forClass(Subscription.class);

        verify(subscriptionDAO, atLeastOnce()).save(eq(context), captor.capture());


        assertEquals(captor.getValue().getSubscriptionParameterList(), expectedParameters);
        assertEquals(updatedSubscription.getSubscriptionParameterList(), expectedParameters);
    }

    private static SubscriptionParameter getSubscriptionParameter(String name, String value) {
        SubscriptionParameter parameter = new SubscriptionParameter();
        parameter.setName(name);
        parameter.setValue(value);
        return parameter;
    }

    private static EPerson getePerson() {
        EPerson ePerson = mock(EPerson.class);
        UUID uuid = UUID.randomUUID();
        doReturn(uuid).when(ePerson).getID();
        return ePerson;
    }
}