package fi.helsinki.lib.simplerest.stubs;


import java.io.Serializable;

public class StubCommunity implements Serializable{
    
    private int id;
    private String name;
    private String short_description;
    private String introductory_text;
    private String copyright_text;
    private String side_bar_text;

    public StubCommunity(int id, String name, String short_description, String introductory_text, String copyright_text, String side_bar_text) {
        this.id = id;
        this.name = name;
        this.short_description = short_description;
        this.introductory_text = introductory_text;
        this.copyright_text = copyright_text;
        this.side_bar_text = side_bar_text;
    }

    public String getShort_description() {
        return short_description;
    }

    public void setShort_description(String short_description) {
        this.short_description = short_description;
    }

    public String getIntroductory_text() {
        return introductory_text;
    }

    public void setIntroductory_text(String introductory_text) {
        this.introductory_text = introductory_text;
    }

    public String getCopyright_text() {
        return copyright_text;
    }

    public void setCopyright_text(String copyright_text) {
        this.copyright_text = copyright_text;
    }

    public String getSide_bar_text() {
        return side_bar_text;
    }

    public void setSide_bar_text(String side_bar_text) {
        this.side_bar_text = side_bar_text;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }
    
}