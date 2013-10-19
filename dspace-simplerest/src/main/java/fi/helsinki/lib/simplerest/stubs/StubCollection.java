/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */

package fi.helsinki.lib.simplerest.stubs;

import java.io.Serializable;
import org.dspace.content.Bitstream;
import org.dspace.content.Collection;
import org.dspace.content.DCValue;

/**
 *
 * @author moubarik
 */
public class StubCollection implements Serializable{
    private int id;
    private String collectionName;
    private String short_description;
    private String introductory_text;
    private String provenance_description;
    private String license;
    private String copyright_text;
    private String side_bar_text;
    private Bitstream logo;

    public StubCollection(int id, String collectionName, String short_description, String introductory_text,
            String provenance_description, String license, String copyright_text, String side_bar_text, 
            Bitstream logo) {
        this.id = id;
        this.collectionName = collectionName;
        this.short_description = short_description;
        this.introductory_text = introductory_text;
        this.provenance_description = provenance_description;
        this.license = license;
        this.copyright_text = copyright_text;
        this.side_bar_text = side_bar_text;
        this.logo = logo;
    }
    
    public StubCollection(Collection c){
         this.id = c.getID();
         this.collectionName = c.getName();
         this.short_description = c.getMetadata("short_description");
         this.introductory_text = c.getMetadata("introductory_text");
         this.provenance_description = c.getMetadata("provenance_description");
         this.license = c.getLicense();
         this.copyright_text = c.getMetadata("copyright_text");
         this.side_bar_text = c.getMetadata("side_bar_text");
         this.logo = c.getLogo();
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getCollectionName() {
        return collectionName;
    }

    public void setCollectionName(String collectionName) {
        this.collectionName = collectionName;
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

    public String getProvenance_description() {
        return provenance_description;
    }

    public void setProvenance_description(String provenance_description) {
        this.provenance_description = provenance_description;
    }

    public String getLicense() {
        return license;
    }

    public void setLicense(String license) {
        this.license = license;
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
}
