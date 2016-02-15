package ua.edu.sumdu.essuir.entity;

import javax.persistence.*;


@Entity
@Table(name = "eperson")
public class EPerson {
    @Id
    @Column(name = "eperson_id")
    private Integer id;

    @Column(name = "email")
    private String email;

    @OneToOne
    @JoinColumn(name = "chair_id")
    private ChairEntity chairEntity;

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public ChairEntity getChairEntity() {
        return chairEntity;
    }

    public void setChairEntity(ChairEntity chairEntity) {
        this.chairEntity = chairEntity;
    }
}
