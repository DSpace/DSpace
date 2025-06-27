/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.alerts;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.List;

import org.apache.logging.log4j.Logger;
import org.dspace.alerts.dao.SystemWideAlertDAO;
import org.dspace.alerts.service.SystemWideAlertService;
import org.dspace.authorize.service.AuthorizeService;
import org.dspace.core.Context;
import org.dspace.eperson.EPerson;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

@MockitoSettings(strictness = Strictness.WARN)
@ExtendWith(MockitoExtension.class)
public class SystemWideAlertServiceTest {

    private static final Logger log = org.apache.logging.log4j.LogManager.getLogger(SystemWideAlertService.class);

    @InjectMocks
    private SystemWideAlertServiceImpl systemWideAlertService;

    @Mock
    private SystemWideAlertDAO systemWideAlertDAO;

    @Mock
    private AuthorizeService authorizeService;

    @Mock
    private Context context;

    @Mock
    private SystemWideAlert systemWideAlert;

    @Mock
    private EPerson eperson;


    @Test
    public void testCreate() throws Exception {
        // Mock admin state
        when(authorizeService.isAdmin(context)).thenReturn(true);

        // Declare objects utilized in unit test
        SystemWideAlert systemWideAlert = new SystemWideAlert();
        systemWideAlert.setMessage("Test message");
        systemWideAlert.setAllowSessions(AllowSessionsEnum.ALLOW_ALL_SESSIONS);
        systemWideAlert.setCountdownTo(null);
        systemWideAlert.setActive(true);

        // Mock DAO to return our defined SystemWideAlert
        when(systemWideAlertDAO.create(any(), any())).thenReturn(systemWideAlert);

        // The newly created SystemWideAlert's message should match our mocked SystemWideAlert's message
        SystemWideAlert result = systemWideAlertService.create(context, "Test message",
            AllowSessionsEnum.ALLOW_ALL_SESSIONS, null, true);
        assertEquals(systemWideAlert.getMessage(), result.getMessage(), "TestCreate 0");
        // The newly created SystemWideAlert should match our mocked SystemWideAlert
        assertEquals(systemWideAlert, result, "TestCreate 1");
    }


    @Test
    public void testFindAll() throws Exception {
        // Declare objects utilized in unit test
        List<SystemWideAlert> systemWideAlertList = new ArrayList<>();

        // The SystemWideAlert(s) reported from our mocked state should match our systemWideAlertList
        assertEquals(systemWideAlertList, systemWideAlertService.findAll(context), "TestFindAll 0");
    }

    @Test
    public void testFind() throws Exception {
        // Mock DAO to return our mocked SystemWideAlert
        when(systemWideAlertService.find(context, 0)).thenReturn(systemWideAlert);

        // The SystemWideAlert reported from our ID should match our mocked SystemWideAlert
        assertEquals(systemWideAlert, systemWideAlertService.find(context, 0), "TestFind 0");
    }

    @Test
    public void testFindAllActive() throws Exception {
        // Declare objects utilized in unit test
        List<SystemWideAlert> systemWideAlertList = new ArrayList<>();

        // The SystemWideAlert(s) reported from our mocked state should match our systemWideAlertList
        assertEquals(systemWideAlertList, systemWideAlertService.findAllActive(context, 10, 0), "TestFindAllActive 0");
    }


    @Test
    public void testUpdate() throws Exception {
        // Mock admin state
        when(authorizeService.isAdmin(context)).thenReturn(true);

        // Invoke impl of method update()
        systemWideAlertService.update(context, systemWideAlert);

        // Verify systemWideAlertDAO.save was invoked twice to confirm proper invocation of both impls of update()
        Mockito.verify(systemWideAlertDAO, times(1)).save(context, systemWideAlert);
    }

    @Test
    public void testDelete() throws Exception {
        // Mock admin state
        when(authorizeService.isAdmin(context)).thenReturn(true);

        // Invoke method delete()
        systemWideAlertService.delete(context, systemWideAlert);

        // Verify systemWideAlertDAO.delete() ran once to confirm proper invocation of delete()
        Mockito.verify(systemWideAlertDAO, times(1)).delete(context, systemWideAlert);
    }

    @Test
    public void canNonAdminUserLoginTrueTest() throws Exception {
        // Mock the alert state
        when(systemWideAlert.getAllowSessions()).thenReturn(AllowSessionsEnum.ALLOW_ALL_SESSIONS);

        // Mock DAO to return our defined systemWideAlertList
        List<SystemWideAlert> systemWideAlertList = new ArrayList<>();
        systemWideAlertList.add(systemWideAlert);
        when(systemWideAlertDAO.findAllActive(context, 1, 0)).thenReturn(systemWideAlertList);

        // Assert the non admin users can log in
        assertTrue(systemWideAlertService.canNonAdminUserLogin(context), "CanNonAdminUserLogin 0");
    }

    @Test
    public void canNonAdminUserLoginFalseTest() throws Exception {
        // Mock the alert state
        when(systemWideAlert.getAllowSessions()).thenReturn(AllowSessionsEnum.ALLOW_ADMIN_SESSIONS_ONLY);

        // Mock DAO to return our defined systemWideAlertList
        List<SystemWideAlert> systemWideAlertList = new ArrayList<>();
        systemWideAlertList.add(systemWideAlert);
        when(systemWideAlertDAO.findAllActive(context, 1, 0)).thenReturn(systemWideAlertList);

        // Assert the non admin users can log in
        assertFalse(systemWideAlertService.canNonAdminUserLogin(context), "CanNonAdminUserLogin 1");
    }

    @Test
    public void canUserMaintainSessionAdminTest() throws Exception {
        // Assert the admin user can log in
        assertTrue(systemWideAlertService.canNonAdminUserLogin(context), "CanUserMaintainSession 0");
    }

    @Test
    public void canUserMaintainSessionTrueTest() throws Exception {
        // Mock admin state
        when(authorizeService.isAdmin(context, eperson)).thenReturn(false);

        // Mock the alert state
        when(systemWideAlert.getAllowSessions()).thenReturn(AllowSessionsEnum.ALLOW_CURRENT_SESSIONS_ONLY);

        // Mock DAO to return our defined systemWideAlertList
        List<SystemWideAlert> systemWideAlertList = new ArrayList<>();
        systemWideAlertList.add(systemWideAlert);
        when(systemWideAlertDAO.findAllActive(context, 1, 0)).thenReturn(systemWideAlertList);

        // Assert the non admin users can main session
        assertTrue(systemWideAlertService.canUserMaintainSession(context, eperson), "CanUserMaintainSession 1");
    }

    @Test
    public void canUserMaintainSessionFalseTest() throws Exception {
        // Mock admin state
        when(authorizeService.isAdmin(context, eperson)).thenReturn(false);

        // Mock the alert state
        when(systemWideAlert.getAllowSessions()).thenReturn(AllowSessionsEnum.ALLOW_ADMIN_SESSIONS_ONLY);

        // Mock DAO to return our defined systemWideAlertList
        List<SystemWideAlert> systemWideAlertList = new ArrayList<>();
        systemWideAlertList.add(systemWideAlert);
        when(systemWideAlertDAO.findAllActive(context, 1, 0)).thenReturn(systemWideAlertList);

        // Assert the non admin users cannot main session
        assertFalse(systemWideAlertService.canUserMaintainSession(context, eperson), "CanUserMaintainSession 2");
    }


}
