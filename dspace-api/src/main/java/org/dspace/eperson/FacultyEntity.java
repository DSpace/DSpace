package org.dspace.eperson;


import com.fasterxml.jackson.annotation.JsonManagedReference;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.dspace.content.DSpaceObject;
import org.dspace.eperson.essuir.DepositorDivision;
import org.hibernate.annotations.CacheConcurrencyStrategy;
import org.hibernate.annotations.LazyCollection;
import org.hibernate.annotations.LazyCollectionOption;

import javax.persistence.*;
import java.util.List;

@Entity
//@Cacheable
//@org.hibernate.annotations.Cache(usage = CacheConcurrencyStrategy.NONSTRICT_READ_WRITE, include = "non-lazy")
@Table(name = "faculty")
public class FacultyEntity extends DSpaceObject implements DepositorDivision {
    @Column(name = "faculty_id")
    @JsonProperty("id")
    private Integer facultyId;

    @Column(name = "faculty_name")
    @JsonProperty("name")
    private String name;

    @OneToMany(fetch = FetchType.LAZY, mappedBy = "faculty")
    @JsonProperty("chairs")
    @JsonManagedReference
    private List<ChairEntity> chairs;

    private FacultyEntity(Builder builder) {
        setId(builder.facultyId);
        setName(builder.name);
    }

    public FacultyEntity() {
    }

    @Override
    public int getType() {
        return 1337;
    }

    public Integer getId() {
        return facultyId;
    }

    public void setId(Integer id) {
        this.facultyId = facultyId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public List<ChairEntity> getChairs() {
        return chairs;
    }

    public void setChairs(List<ChairEntity> chairs) {
        this.chairs = chairs;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;

        if (o == null || getClass() != o.getClass()) return false;

        FacultyEntity that = (FacultyEntity) o;

        return new EqualsBuilder()
                .append(id, that.id)
                .append(name, that.name)
                .isEquals();
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder(17, 37)
                .append(id)
                .append(name)
                .toHashCode();
    }

    public static final class Builder {
        private Integer facultyId;
        private String name;

        public Builder() {
        }

        public Builder(FacultyEntity copy) {
            this.facultyId = copy.getId();
            this.name = copy.getName();
        }

        public Builder withId(Integer id) {
            this.facultyId = id;
            return this;
        }

        public Builder withName(String name) {
            this.name = name;
            return this;
        }

        public FacultyEntity build() {
            return new FacultyEntity(this);
        }
    }
}