/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.bulkimport.model;

import java.time.LocalDate;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import org.dspace.util.MultiFormatDateDeserializer;

/**
 * Class that model the value of ACCESS_CONDITION_CELL
 * of sheet BITSTREAM_METADATA of the Bulk import excel.
 *
 * @author Mohamed Eskander (mohamed.eskander at 4science.it)
 */
public class AccessCondition {

    private String name;

    private String description;

    @JsonDeserialize(using = MultiFormatDateDeserializer.class)
    private LocalDate startDate;

    @JsonDeserialize(using = MultiFormatDateDeserializer.class)
    private LocalDate endDate;

    public AccessCondition() {
    }

    public AccessCondition(String name, String description, LocalDate startDate, LocalDate endDate) {
        this.name = name;
        this.description = description;
        this.startDate = startDate;
        this.endDate = endDate;
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    public LocalDate getStartDate() {
        return startDate;
    }

    public LocalDate getEndDate() {
        return endDate;
    }
}