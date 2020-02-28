/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.model;

import java.util.Date;
import java.util.UUID;

import org.dspace.app.rest.model.step.UploadBitstreamRest;

/**
 * The UploadAccessConditionDTO is a partial representation of the DSpace
 * {@link ResourcePolicyRest} as used in the patch payload for the upload
 * submission section (see {@link UploadBitstreamRest}. The main reason for this
 * class is to have a DTO to use serialize/deserialize the REST model, that
 * include reference to the GroupRest and EPersonRest object, in the upload
 * section data in a simpler way where such reference are just UUID. Indeed, due
 * to the fact that the RestModel class are serialized according to the HAL
 * format and the reference are only exposed in the _links section of the
 * RestResource it was not possible to use the {@link ResourcePolicyRest} class
 * directly in the upload section
 *
 * @author Mykhaylo Boychuk (mykhaylo.boychuk at 4science.it)
 */
public class UploadBitstreamAccessConditionDTO  {

    private Integer id;

    private UUID groupUUID;

    private UUID epersonUUID;

    private String name;

    private String description;

    private Date startDate;

    private Date endDate;

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public UUID getGroupUUID() {
        return groupUUID;
    }

    public void setGroupUUID(UUID groupUUID) {
        this.groupUUID = groupUUID;
    }

    public UUID getEpersonUUID() {
        return epersonUUID;
    }

    public void setEpersonUUID(UUID epersonUUID) {
        this.epersonUUID = epersonUUID;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public Date getStartDate() {
        return startDate;
    }

    public void setStartDate(Date startDate) {
        this.startDate = startDate;
    }

    public Date getEndDate() {
        return endDate;
    }

    public void setEndDate(Date endDate) {
        this.endDate = endDate;
    }

}
