package ua.edu.sumdu.essuir.entity;


import javax.persistence.*;

@Entity
@Table(name = "chair")
public class ChairEntity {
    @Id
    @Column(name = "chair_id")
    private Integer id;

    @Column(name = "chair_name")
    private String chairName;

    @OneToOne
    @JoinColumn(name = "faculty_id")
    private FacultyEntity facultyEntityName;

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

    public String getFacultyEntityName() {
        return facultyEntityName.getName();
    }

    public void setFacultyEntityName(FacultyEntity facultyEntityName) {
        this.facultyEntityName = facultyEntityName;
    }
}
