package org.dspace.content.virtual;

import java.util.List;

public class Concatenate {

    private List<String> fields;
    private String separator;

    public List<String> getFields() {
        return fields;
    }

    public void setFields(List<String> fields) {
        this.fields = fields;
    }

    public String getSeparator() {
        return separator;
    }

    public void setSeparator(String separator) {
        this.separator = separator;
    }
}
