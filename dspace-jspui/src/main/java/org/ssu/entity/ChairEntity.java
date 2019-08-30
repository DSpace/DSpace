package org.ssu.entity;

import com.fasterxml.jackson.annotation.JsonBackReference;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonManagedReference;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;

import javax.persistence.*;
import java.util.List;

@Entity
@Table(name = "chair")
public class ChairEntity {
    @Id
    @Column(name = "chair_id")
    @JsonProperty("id")
    private Integer id;

    @Column(name = "chair_name")
    @JsonProperty("name")
    private String chairName;

    @OneToOne
    @JoinColumn(name = "faculty_id")
    @JsonBackReference
    private FacultyEntity facultyEntityName;


    @OneToMany(mappedBy = "chairEntity", fetch = FetchType.EAGER)
    @JsonProperty("specialities")
    @JsonManagedReference
    private List<Speciality> specialities;

    public ChairEntity() {
    }

    private ChairEntity(Builder builder) {
        setId(builder.id);
        setChairName(builder.chairName);
        setFacultyEntityName(builder.facultyEntityName);
    }

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public String getChairName() {
        return chairName;
    }

    public void setChairName(String chairName) {
        this.chairName = chairName;
    }

    @JsonIgnore
    public String getFacultyEntityName() {
        return facultyEntityName.getName();
    }
    @JsonIgnore
    public FacultyEntity getFacultyEntity() {
        return this.facultyEntityName;
    }
    @JsonIgnore
    public Integer getFacultyEntityId() {
        return facultyEntityName.getId();
    }

    public void setFacultyEntityName(FacultyEntity facultyEntityName) {
        this.facultyEntityName = facultyEntityName;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;

        if (o == null || getClass() != o.getClass()) return false;

        ChairEntity that = (ChairEntity) o;

        return new EqualsBuilder()
                .append(id, that.id)
                .append(chairName, that.chairName)
                .append(facultyEntityName, that.facultyEntityName)
                .isEquals();
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder(17, 37)
                .append(id)
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
            this.chairName = copy.getChairName();
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