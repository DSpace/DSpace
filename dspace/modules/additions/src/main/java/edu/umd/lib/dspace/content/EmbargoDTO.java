package edu.umd.lib.dspace.content;

import java.sql.Date;
import java.util.UUID;

/**
 * DTO object to get embargoed bitstreams with associated item and handle and other
 * relevant information.
 */
public class EmbargoDTO {

  private final String handle;
  private final UUID itemId;
  private final UUID bitstreamId;
  private final String title;
  private final String advisor;
  private final String author;
  private final String department;
  private final String type;
  private final Date endDate; 

  public EmbargoDTO(String handle, UUID itemId, UUID bitstreamId, String title, String advisor, String author, String department, String type, Date endDate) {
      this.handle = handle;
      this.itemId = itemId;
      this.bitstreamId = bitstreamId;
      this.title = title;
      this.advisor = advisor;
      this.author = author;
      this.department = department;
      this.type = type;
      this.endDate = endDate;
  }

  public String getHandle() {
      return this.handle;
  }

  public UUID getItemId() {
      return this.itemId;
  }

  public String getItemIdString() {
      return this.itemId.toString();
  }

  public UUID getBitstreamId() {
      return this.bitstreamId;
  }

  public String getBitstreamIdString() {
      return this.bitstreamId.toString();
  }

  public String getTitle() {
      return this.title;
  }

  public String getAdvisor() {
      return this.advisor;
  }

  public String getAuthor() {
      return this.author;
  }

  public String getDepartment() {
      return this.department;
  }

  public String getType() {
      return this.type;
  }

  public Date getEndDate() {
      return this.endDate;
  }

  public String getEndDateString() {
      if(endDate == null) {
        return null;
      }
      return this.endDate.toString();
  }
}