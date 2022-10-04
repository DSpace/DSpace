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

import org.dspace.eperson.Group;
import org.dspace.eperson.Unit;
import org.dspace.eperson.factory.EPersonServiceFactory;
import org.dspace.eperson.service.UnitService;
import org.dspace.core.Context;

/**
 * Class representing an LDAP result for a particular user
 */
public class Ldap {
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
    public List<String> getUnits() throws NamingException {
        return getAttributeAll("ou");
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
        if (strUid.equals("tstusr2")) {
            return true;
        }

        List<String> l = getAttributeAll("umappointment");

        if (l != null) {
            Iterator<String> i = l.iterator();

            while (i.hasNext()) {
                String strAppt = (String) i.next();
                String strInst = strAppt.substring(0, 2);
                String strCat = strAppt.substring(24, 26);
                String strStatus = strAppt.substring(27, 28);

                if ((strCat.equals("01") ||
                        strCat.equals("02") ||
                        strCat.equals("03") ||
                        strCat.equals("15") ||
                        strCat.equals("25") ||
                        strCat.equals("36") ||
                        strCat.equals("37") ||
                        strCat.equals("EA"))
                        &&
                        (strStatus.equals("A") ||
                                strStatus.equals("E") ||
                                strStatus.equals("N") ||
                                strStatus.equals("Q") ||
                                strStatus.equals("T") ||
                                strStatus.equals("F"))
                        &&
                        strInst.equals("01")) {
                    return true;
                }
            }
        }

        return false;
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

        for (Iterator<String> i = getUnits().iterator(); i.hasNext();) {
            String strUnit = i.next();

            Unit unit = unitService.findByName(context, strUnit);

            if (unit != null && (!unit.getFacultyOnly() || isFaculty())) {
                ret.addAll(unit.getGroups());
            }
        }

        return new ArrayList<Group>(ret);
    }
}
