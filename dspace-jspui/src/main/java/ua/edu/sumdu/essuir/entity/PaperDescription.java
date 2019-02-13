package ua.edu.sumdu.essuir.entity;

import java.time.LocalDate;
import java.time.LocalDateTime;

public class PaperDescription {
    private Integer resourceId;
    private Speciality speciality;
    private LocalDate added;

    public Integer getResourceId() {
        return resourceId;
    }

    public Speciality getSpeciality() {
        return speciality;
    }

    public LocalDate getAdded() {
        return added;
    }

    private PaperDescription(Builder builder) {
        resourceId = builder.resourceId;
        speciality = builder.speciality;
        added = builder.added;
    }


    public static final class Builder {
        private Integer resourceId;
        private Speciality speciality;
        private LocalDate added;

        public Builder() {
        }

        public Builder(PaperDescription copy) {
            this.resourceId = copy.getResourceId();
            this.speciality = copy.getSpeciality();
            this.added = copy.getAdded();
        }

        public Builder withResourceId(Integer resourceId) {
            this.resourceId = resourceId;
            return this;
        }

        public Builder withSpeciality(Speciality speciality) {
            this.speciality = speciality;
            return this;
        }

        public Builder withAdded(LocalDate added) {
            this.added = added;
            return this;
        }

        public PaperDescription build() {
            return new PaperDescription(this);
        }
    }
}
