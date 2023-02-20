package edu.umd.lib.dspace.authenticate.impl;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;

import java.util.List;

import org.dspace.AbstractDSpaceTest;
import org.junit.Before;
import org.junit.Test;

public class LdapTest extends AbstractDSpaceTest {
    Ldap ldap;
    @Before
    public void setUp() {
    }

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

    @Test(expected = StringIndexOutOfBoundsException.class)
    public void isFaculty_emptyUmAppointment_throwsException() throws Exception {
        List<String> umAppointmentsList = List.of("");

        Ldap ldap = spy(new Ldap("emptyUmAppointment", null));
        when(ldap.getAttributeAll("umappointment")).thenReturn(umAppointmentsList);

        ldap.isFaculty();
    }

    @Test
    public void isFaculty_notFacultyUmAppointment_returnsFalse() throws Exception {
        // instCode: 01, strCat: 04, strStatus: A -- strCat does not match
        List<String> umAppointmentsList = List.of("012023001230101$9936503$04$A$ $Y$Y$");
        Ldap ldap = spy(new Ldap("notFacultyUmAppointment", null));
        when(ldap.getAttributeAll("umappointment")).thenReturn(umAppointmentsList);

        assertFalse(ldap.isFaculty());
    }

    @Test
    public void isFaculty_facultyUmAppointment_returnsTrue() throws Exception {
        // instCode: 01, strCat: 15, strStatus: A
        List<String> umAppointmentsList = List.of("012030001300301$9533010$15$A$ $Y$Y$");
        Ldap ldap = spy(new Ldap("notFacultyUmAppointment", null));
        when(ldap.getAttributeAll("umappointment")).thenReturn(umAppointmentsList);

        assertTrue(ldap.isFaculty());
    }

    @Test
    public void isFaculty_facultyWithMultipleUmAppointments_returnsTrue() throws Exception {
        List<String> umAppointmentsList = List.of(
            "012023001230101$9542103$15$A$ $Y$Y$",
            "012027001272901$9543906$15$A$ $Y$N$");
        Ldap ldap = spy(new Ldap("notFacultyUmAppointment", null));
        when(ldap.getAttributeAll("umappointment")).thenReturn(umAppointmentsList);

        assertTrue(ldap.isFaculty());
    }
}
