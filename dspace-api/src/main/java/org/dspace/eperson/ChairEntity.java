package org.dspace.eperson;


import com.fasterxml.jackson.annotation.JsonBackReference;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonManagedReference;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.dspace.content.DSpaceObject;
import org.dspace.eperson.essuir.DepositorDivision;

import javax.persistence.*;
import java.util.List;

@Entity
@Table(name = "chair")
public class ChairEntity extends DSpaceObject implements DepositorDivision {
    @Column(name = "chair_id")
    @JsonProperty("id")
    private Integer chairId;

    @Column(name = "chair_name")
    @JsonProperty("name")
    private String chairName = "";

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "faculty_id", referencedColumnName = "faculty_id")
    @JsonBackReference
    private FacultyEntity faculty;


    @OneToMany(fetch = FetchType.LAZY, mappedBy = "chairEntity")
    @JsonProperty("specialities")
    @JsonManagedReference
    private List<Speciality> specialities;

    public ChairEntity() {
    }

    @Override
    public int getType() {
        return 1338;
    }

    private ChairEntity(Builder builder) {
        setId(builder.id);
        setChairName(builder.chairName);
        setFacultyEntityName(builder.facultyEntityName);
    }

    public Integer getId() {
        return chairId;
    }

    public void setId(Integer id) {
        this.chairId = id;
    }

    public String getName() {
        return chairName;
    }

    public void setChairName(String chairName) {
        this.chairName = chairName;
    }

    @JsonIgnore
    public String getFacultyEntityName() {
        return faculty.getName();
    }
    @JsonIgnore
    public FacultyEntity getFacultyEntity() {
        return this.faculty;
    }
    @JsonIgnore
    public Integer getFacultyEntityId() {
        return faculty.getId();
    }

    public void setFacultyEntityName(FacultyEntity facultyEntityName) {
        this.faculty = facultyEntityName;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;

        if (o == null || getClass() != o.getClass()) return false;

        ChairEntity that = (ChairEntity) o;

        return new EqualsBuilder()
                .append(chairId, that.chairId)
                .append(chairName, that.chairName)
                .append(faculty, that.faculty)
                .isEquals();
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder(17, 37)
                .append(chairId)
                .append(chairName)
                .toHashCode();
    }

    public static final class Builder {
        private Integer id;
        private String chairName;
        private FacultyEntity facultyEntityName;

        public Builder() {
        }

        public Builder(ChairEntity copy) {
            this.id = copy.getId();
            this.chairName = copy.getName();
            this.facultyEntityName = copy.getFacultyEntity();
        }

        public Builder withId(Integer id) {
            this.id = id;
            return this;
        }

        public Builder withChairName(String chairName) {
            this.chairName = chairName;
            return this;
        }

        public Builder withFacultyEntityName(FacultyEntity facultyEntityName) {
            this.facultyEntityName = facultyEntityName;
            return this;
        }

        public ChairEntity build() {
            return new ChairEntity(this);
        }
    }
}
