package org.dspace.app.rest.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.List;

import org.dspace.app.rest.RestResourceController;

/**
 * The LDAP REST Resource
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class LdapRest extends DSpaceObjectRest {
    public static final String NAME = "ldap";
    public static final String CATEGORY = RestAddressableModel.EPERSON;

    public static final String EPERSONS = "epersons";
    public static final String OBJECT = "object";

    private String name;
    private List<GroupRest> groups;

    private List<UnitRest> matchedUnits;
    private List<String> unmatchedUnits;

    @Override
    public String getCategory() {
        return CATEGORY;
    }

    @Override
    public String getType() {
        return NAME;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    @Override
    @JsonIgnore
    public Class getController() {
        return RestResourceController.class;
    }

    private String firstName;
    private String lastName;
    private String phone;
    private Boolean isFaculty;
    private String email;
    private List<String> umAppointments;

    public String getFirstName() {
        return firstName;
    }

    public void setFirstName(String firstName) {
        this.firstName = firstName;
    }

    public String getLastName() {
        return lastName;
    }

    public void setLastName(String lastName) {
        this.lastName = lastName;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public Boolean getIsFaculty() {
        return isFaculty;
    }

    public void setIsFaculty(Boolean isFaculty) {
        this.isFaculty = isFaculty;
    }

    public List<String> getUmAppointments() {
        return umAppointments;
    }

    public void setUmAppointments(List<String> umAppointments) {
        this.umAppointments = umAppointments;
    }

    public void setGroups(List<GroupRest> groupList) {
        this.groups = groupList;
    }

    public List<GroupRest> getGroups() {
        return groups;
    }

    public void setMatchedUnits(List<UnitRest> unitList) {
        this.matchedUnits = unitList;
    }

    public List<UnitRest> getMatchedUnits() {
        return matchedUnits;
    }

    public void setUnmatchedUnits(List<String> unmatchedUnitsList) {
        this.unmatchedUnits = unmatchedUnitsList;
    }

    public List<String> getUnmatchedUnits() {
        return this.unmatchedUnits;
    }
}
