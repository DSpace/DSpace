package org.dspace.eperson;


import com.fasterxml.jackson.annotation.JsonProperty;

public class SpecialityDetailedInfo {
    @JsonProperty("code")
    private Integer code;
    @JsonProperty("name")
    private String name;

    public SpecialityDetailedInfo() {
    }

    public Integer getCode() {
        return code;
    }

    public String getName() {
        return name;
    }
}

