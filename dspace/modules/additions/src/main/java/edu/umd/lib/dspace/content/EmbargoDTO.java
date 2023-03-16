package edu.umd.lib.dspace.content;

import java.sql.Date;
import java.util.UUID;

/**
 * DTO object to get embargoed bitstreams with associated item and handle and other
 * relevant information.
 */
public class EmbargoDTO {

    private String handle;
    private UUID itemId;
    private UUID bitstreamId;
    private String title;
    private String advisor;
    private String author;
    private String department;
    private String type;
    private Date endDate;

    public EmbargoDTO() {
    }

    public String getHandle() {
        return this.handle;
    }

    public void setHandle(String handle) {
        this.handle = handle;
    }

    public UUID getItemId() {
        return this.itemId;
    }

    public void setItemId(UUID itemId) {
        this.itemId = itemId;
    }

    public String getItemIdString() {
        return this.itemId.toString();
    }

    public UUID getBitstreamId() {
        return this.bitstreamId;
    }

    public void setBitstreamId(UUID bitstreamId) {
        this.bitstreamId = bitstreamId;
    }

    public String getBitstreamIdString() {
        return this.bitstreamId.toString();
    }

    public String getTitle() {
        return this.title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getAdvisor() {
        return this.advisor;
    }

    public void setAdvisor(String advisor) {
        this.advisor = advisor;
    }

    public String getAuthor() {
        return this.author;
    }

    public void setAuthor(String author) {
        this.author = author;
    }

    public String getDepartment() {
        return this.department;
    }

    public void setDepartment(String department) {
        this.department = department;
    }

    public String getType() {
        return this.type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public Date getEndDate() {
        return this.endDate;
    }

    public void setEnddate(Date endDate) {
        this.endDate = endDate;
    }

    public String getEndDateString() {
        if (endDate == null) {
            return null;
        }
        return this.endDate.toString();
    }
}
