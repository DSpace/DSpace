/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package fi.helsinki.lib.simplerest.stubs;

import java.io.Serializable;

/**
 *
 * @author moubarik
 */
public class StubBitstream implements Serializable{
    private int id;
    private String name;
    private String mimetype;
    private String description;
    private String userformatdescription;
    private int sequenceid;
    private Long sizebytes;

    public StubBitstream(int id, String name, String mimetype, String description, String userformatdescription, int sequenceid, Long sizebytes) {
        this.id = id;
        this.name = name;
        this.mimetype = mimetype;
        this.description = description;
        this.userformatdescription = userformatdescription;
        this.sequenceid = sequenceid;
        this.sizebytes = sizebytes;
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

    public String getMimetype() {
        return mimetype;
    }

    public void setMimetype(String mimetype) {
        this.mimetype = mimetype;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getUserformatdescription() {
        return userformatdescription;
    }

    public void setUserformatdescription(String userformatdescription) {
        this.userformatdescription = userformatdescription;
    }

    public int getSequenceid() {
        return sequenceid;
    }

    public void setSequenceid(int sequenceid) {
        this.sequenceid = sequenceid;
    }

    public Long getSizebytes() {
        return sizebytes;
    }

    public void setSizebytes(Long sizebytes) {
        this.sizebytes = sizebytes;
    }
   
}
