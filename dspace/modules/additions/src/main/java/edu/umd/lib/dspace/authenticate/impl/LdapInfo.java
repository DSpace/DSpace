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

public class LdapInfo {
  private String strUid;
  private SearchResult entry;

  // Begin UMD Customization
  private final static UnitService unitService = EPersonServiceFactory.getInstance().getUnitService();
  // End UMD Customization

  public LdapInfo(String strUid, SearchResult entry) {
    this.strUid = strUid;
    this.entry = entry;
  }
      /****************************************************** getAttributeAll */
    /**
     * get all instances of an attribute.
     */
    public List<String> getAttributeAll(String strName) throws NamingException {
      List<String> attributes = new ArrayList<>();

      if (entry != null) {
          Attributes as = entry.getAttributes();
          Attribute a = as.get(strName);

          if (a != null) {
              NamingEnumeration e = a.getAll();

              while (e.hasMore()) {
                  attributes.add((String)e.next());
              }
          }
      }

      return attributes;
  }

  /********************************************************* getAttribute */
  /**
   * get an attribute (first instance).
   */
  public String getAttribute(String strName) throws NamingException {
      List l = getAttributeAll(strName);

      if (l.size() > 0) {
          return (String)l.get(0);
      } else {
          return null;
      }
  }

  /************************************************************* getEmail */
  /**
   * user's email address
   */
  public String getEmail() throws NamingException {
      return getAttribute("mail");
  }

  /************************************************************* getPhone */
  /**
   * user's phone
   */
  public String getPhone() throws NamingException {
      return getAttribute("telephonenumber");
  }

  /********************************************************* getFirstName */
  /**
   * user's first name
   */
  public String getFirstName() throws NamingException {
      return getAttribute("givenname");
  }

  /********************************************************** getLastName */
  /**
   * user's last name
   */
  public String getLastName() throws NamingException {
      return getAttribute("sn");
  }

  /************************************************************** getUnits */
  /**
   * organization units
   */
  public List<String> getUnits() throws NamingException {
      return getAttributeAll("ou");
  }

    /************************************************************ isFaculty */
    /**
     * is the user CP faculty with an acceptable status?
     */
    public boolean isFaculty() throws NamingException {
      if (strUid.equals("tstusr2")) {
          return true;
      }

      List l = getAttributeAll("umappointment");

      if (l != null) {
          Iterator i = l.iterator();

          while (i.hasNext()) {
              String strAppt = (String)i.next();
              String strInst = strAppt.substring(0,2);
              String strCat = strAppt.substring(24,26);
              String strStatus = strAppt.substring(27,28);

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

  /************************************************************* getGroups */
  /**
   * Groups mapped by the Units for faculty.
   */
  public List<Group> getGroups(Context context) throws NamingException, java.sql.SQLException {
    HashSet<Group> ret = new HashSet();

    for (Iterator i = getUnits().iterator(); i.hasNext(); ) {
        String strUnit = (String) i.next();

        Unit unit = unitService.findByName(context, strUnit);

        if (unit != null && (!unit.getFacultyOnly() || isFaculty())) {
            ret.addAll(unit.getGroups());
        }
    }

    return new ArrayList<Group>(ret);
}
}
