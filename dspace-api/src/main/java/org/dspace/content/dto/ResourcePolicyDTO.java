/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.content.dto;

import java.time.LocalDate;

import org.dspace.authorize.ResourcePolicy;

/**
 * This class acts as Data transfer object in which we can store data like in a
 * regular ResourcePolicy object, but this one isn't saved in the DB. This can
 * freely be used to represent ResourcePolicy without it being saved in the
 * database, this will typically be used when transferring data.
 *
 * @author Luca Giamminonni (luca.giamminonni at 4Science)
 */
public class ResourcePolicyDTO {

    private final String name;

    private final String description;

    private final int action;

    private final String type;

    private final LocalDate startDate;

    private final LocalDate endDate;

    public ResourcePolicyDTO(ResourcePolicy resourcePolicy) {
        this(resourcePolicy.getRpName(), resourcePolicy.getRpDescription(), resourcePolicy.getAction(),
             resourcePolicy.getRpType(), resourcePolicy.getStartDate(), resourcePolicy.getEndDate());
    }

    public ResourcePolicyDTO(String name, String description, int action, String type, LocalDate startDate,
                             LocalDate endDate) {
        this.name = name;
        this.description = description;
        this.action = action;
        this.type = type;
        this.startDate = startDate;
        this.endDate = endDate;
    }

    public String getName() {
        return name;
    }

    public LocalDate getStartDate() {
        return startDate;
    }

    public LocalDate getEndDate() {
        return endDate;
    }

    public String getDescription() {
        return description;
    }

    public int getAction() {
        return action;
    }

    public String getType() {
        return type;
    }

}
