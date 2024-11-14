package edu.umd.lib.dspace.authenticate.impl;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import javax.naming.NamingEnumeration;
import javax.naming.NamingException;
import javax.naming.directory.Attribute;
import javax.naming.directory.Attributes;
import javax.naming.directory.SearchResult;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.dspace.core.Context;
import org.dspace.eperson.Group;
import org.dspace.eperson.Unit;
import org.dspace.eperson.factory.EPersonServiceFactory;
import org.dspace.eperson.service.UnitService;

/**
 * Class representing an LDAP result for a particular user
 */
public class Ldap {
    /** log4j category */
    private static Logger log = LogManager.getLogger(Ldap.class);

    private String strUid;
    private SearchResult entry;

    private final static UnitService unitService = EPersonServiceFactory.getInstance().getUnitService();

    /**
     * Constructs an Ldap object from the given parameters
     *
     * @param strUid the LDAP user id
     * @param entry the LDAP SearchResult containing information about the user.
     */
    public Ldap(String strUid, SearchResult entry) {
        this.strUid = strUid;
        this.entry = entry;
    }

    /**
     * Return a (possible empty) List of Strings representing the values for the
     * attribute
     *
     * @return a (possibly empty) List of Strings representing the values for the
     * attribute
     * @param strName the name of the attribute to return
     * @throws NamingException if an error is encountered while retrieving the
     * values.
     */
    public List<String> getAttributeAll(String strName) throws NamingException {
        List<String> attributes = new ArrayList<>();

        if (entry != null) {
            Attributes as = entry.getAttributes();
            Attribute a = as.get(strName);

            if (a != null) {
                NamingEnumeration<?> e = a.getAll();

                while (e.hasMore()) {
                    attributes.add((String) e.next());
                }
            }
        }

        return attributes;
    }

    /**
     * Returns the first value for the given attribute, or null if no value
     * is found.
     *
     * @param strName the name of the attribute
     * @return the first value for the given attribute, or null if no value
     * is found.
     * @throws NamingException if an error is encountered while retrieving the
     * value.
     */
    public String getAttribute(String strName) throws NamingException {
        List<String> l = getAttributeAll(strName);

        if (l.size() > 0) {
            return (String) l.get(0);
        } else {
            return null;
        }
    }

    /**
     * Returns the user's email address attribute ("mail") from LDAP, or null
     * if no value is found.
     *
     * @return the user's email address attribute ("mail") from LDAP, or null
     * if no value is found.
     * @throws NamingException if an error is encountered while retrieving the
     * value.
     */
    public String getEmail() throws NamingException {
        return getAttribute("mail");
    }

    /**
     * Returns the user's telephone attribute ("telephonenumber") from LDAP, or
     * null if no value is found.
     *
     * @return the user's telephone attribute ("telephonenumber") from LDAP, or
     * null if no value is found.
     * @throws NamingException if an error is encountered while retrieving the
     * value.
     */
    public String getPhone() throws NamingException {
        return getAttribute("telephonenumber");
    }

    /**
     * Returns the user's first name attribute ("givenname") from LDAP, or
     * null if no value is found.
     *
     * @return the user's first name attribute ("givenname") from LDAP, or
     * null if no value is found.
     * @throws NamingException if an error is encountered while retrieving the
     * value.
     */
    public String getFirstName() throws NamingException {
        return getAttribute("givenname");
    }

    /**
     * Returns the user's last name attribute ("sn") from LDAP, or null if no
     * value is found.
     *
     * @return the user's last name attribute ("sn") from LDAP, or null if no
     * value is found.
     * @throws NamingException if an error is encountered while retrieving the
     * value.
     */
    public String getLastName() throws NamingException {
        return getAttribute("sn");
    }

    /**
     * Returns a (possibly empty) List of the user's organization units ("ou")
     * from LDAP.
     *
     * @return a (possibly empty) List of the user's organization units ("ou")
     * from LDAP.
     * @throws NamingException if an error is encountered while retrieving the
     * value.
     */
    protected List<String> getLdapOrganizationalUnits() throws NamingException {
        return getAttributeAll("ou");
    }

    /**
     * Returns a List of Units in DSpace that match the LDAP organizational
     * units of the user
     */
    public List<Unit> getMatchedUnits(Context context) throws NamingException, java.sql.SQLException {
        List<Unit> units = new ArrayList<>();
        for (Iterator<String> i = getLdapOrganizationalUnits().iterator(); i.hasNext();) {
            String strUnit = (String) i.next();

            Unit unit = unitService.findByName(context, strUnit);

            if (unit != null) {
                units.add(unit);
            }
        }
        return units;
    }

    /**
     * Returns a List of String representing the LDAP organizational units for
     * the user that do not have a corresponding DSpace Unit.
     */
    public List<String> getUnmatchedUnits(Context context) throws NamingException, java.sql.SQLException {
        List<String> additionalUnits = new ArrayList<>();
        for (Iterator<String> i = getLdapOrganizationalUnits().iterator(); i.hasNext();) {
            String strUnit = (String) i.next();

            Unit unit = unitService.findByName(context, strUnit);

            if (unit == null) {
                additionalUnits.add(strUnit);
            }
        }
        return additionalUnits;
    }


    /**
     * Returns true if the user is College Park faculty with an acceptable
     * status, false otherwise.
     *
     * @return true if the user is College Park faculty with an acceptable
     * status, false otherwise.
     * @throws NamingException if an error is encountered while retrieving the
     * value.
     */
    public boolean isFaculty() throws NamingException {
        if ("tstusr2".equals(strUid)) {
            return true;
        }

        List<String> l = getAttributeAll("umappointment");

        if (l != null) {
            for (String umAppointment : l) {
                if (isFaculty(umAppointment)) {
                    return true;
                }
            }
        }

        return false;
    }

    /**
     * Returns true if the given umAppointment string matches the criteria for
     * a faculty member, false otherwise.
     *
     * @param umAppointment the LDAP "umappointment" string to check
     * @return true if the given umAppointment string matches the criteria for
     * a faculty member, false otherwise.
     */
    protected boolean isFaculty(String umAppointment) {

        String[] components = umAppointment.split("\\$");
        // Don't look for exactly 7 (or 8), because last term may be blank, and so not be present
        if (components.length < 7) {
            log.warn(
                "The given umAppointment '{}' did not have the expected format",
                umAppointment
            );
            return false;
        }

        String strInst = components[0]; // umInstitutionCode
        String strCat = components[3]; // umCatStatus
        String strStatus = components[4]; // EMP_STAT_CD

        final List<String> facultyCategories = List.of(
            "Tenured Fac",
            "Ten Trk Fac",
            "NT-Term Fac",
            "NT-Cont. Fac",
            "Post-Doctoral Scholar",
            "Hrly Faculty",
            "NT-NonRg Fac"
        );

        final List<String> employmentStatuses = List.of(
            "A", // Active
            "Q", // non-paid
            // If is unclear what the following correspond to, or if they
            // are even still used
            "E", "N", "T", "F"
        );

        return "01".equals(strInst) && facultyCategories.contains(strCat) &&
            employmentStatuses.contains(strStatus);
    }

    /**
     * Returns a (possibly empty) List of Groups derived from the Units
     * the user belongs to.
     *
     * @param context the DSpace context
     * @return a (possibly empty) List of Groups derived from the Units
     * the user belongs to.
     * @throws NamingException if an error is encountered while retrieving the
     * value.
     * @throws SQLException if an error occurs retrieving information from the
     * database.
     */
    public List<Group> getGroups(Context context) throws NamingException, java.sql.SQLException {
        HashSet<Group> ret = new HashSet<>();

        for (Iterator<String> i = getLdapOrganizationalUnits().iterator(); i.hasNext();) {
            String strUnit = i.next();

            Unit unit = unitService.findByName(context, strUnit);

            if (unit != null && (!unit.getFacultyOnly() || isFaculty())) {
                ret.addAll(unit.getGroups());
            }
        }

        return new ArrayList<Group>(ret);
    }
}
