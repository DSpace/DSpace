package edu.umd.lib.dspace.authenticate.impl;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;

import java.util.List;

import org.dspace.AbstractDSpaceTest;
import org.junit.Test;

public class LdapTest extends AbstractDSpaceTest {
    @Test
    public void isFaculty_tstusr2_returnsTrue() throws Exception {
        Ldap ldap = spy(new Ldap("tstusr2", null));

        assertTrue(ldap.isFaculty());
    }

    @Test
    public void isFaculty_nullUmAppointment_returnsFalse() throws Exception {
        Ldap ldap = spy(new Ldap("nullUmAppointment", null));
        when(ldap.getAttributeAll("umappointment")).thenReturn(null);

        assertFalse(ldap.isFaculty());
    }

    @Test
    public void isFaculty_emptyUmAppointment_returnsFalse() throws Exception {
        List<String> umAppointmentsList = List.of("");

        Ldap ldap = spy(new Ldap("emptyUmAppointment", null));
        when(ldap.getAttributeAll("umappointment")).thenReturn(umAppointmentsList);

        assertFalse(ldap.isFaculty());
    }

    @Test
    public void isFaculty_notFacultyUmAppointment_returnsFalse() throws Exception {
        List<String> umAppointmentsList = List.of("01$SO001651$SO001650$Grad Asst$A$Y$Y$");
        Ldap ldap = spy(new Ldap("notFacultyUmAppointment", null));
        when(ldap.getAttributeAll("umappointment")).thenReturn(umAppointmentsList);

        assertFalse(ldap.isFaculty());
    }

    @Test
    public void isFaculty_facultyUmAppointment_returnsTrue() throws Exception {
        List<String> umAppointmentsList = List.of("01$SO003205$SO003123$NT-Cont. Fac$A$Y$Y$");
        Ldap ldap = spy(new Ldap("multipleFacultyUmAppointment", null));
        doReturn(umAppointmentsList).when(ldap).getAttributeAll("umappointment");


        boolean result = ldap.isFaculty();
        assertTrue("Expected " + umAppointmentsList + " to return true", result);
    }

    @Test
    public void isFaculty_facultyWithMultipleUmAppointments_returnsTrue() throws Exception {
        List<String> umAppointmentsList = List.of(
            "01$SO002367$SO002206$Affiliate with Librarian$A$Y$N$",
            "01$SO001740$SO001650$NT-Cont. Fac$A$Y$Y$");
        Ldap ldap = spy(new Ldap("multipleFacultyUmAppointment", null));
        doReturn(umAppointmentsList).when(ldap).getAttributeAll("umappointment");

        assertTrue(ldap.isFaculty());
    }
}
