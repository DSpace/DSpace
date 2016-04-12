package ua.edu.sumdu.essuir.entity;

public class AuthorData {
    private String email;
    private String chair;
    private String faculty;
    private String lastname;

    private String firstname;

    public String getFirstname() {
        return firstname;
    }

    public AuthorData setFirstname(String firstname) {
        this.firstname = firstname;
        return this;
    }

    public String getEmail() {
        return email;
    }

    public AuthorData setEmail(String email) {
        this.email = email;
        return this;
    }

    public String getChair() {
        return chair;
    }

    public AuthorData setChair(String chair) {
        this.chair = chair;
        return this;
    }

    public String getFaculty() {
        return faculty;
    }

    public AuthorData setFaculty(String faculty) {
        this.faculty = faculty;
        return this;
    }

    public String getLastname() {
        return lastname;
    }

    public AuthorData setLastname(String lastname) {
        this.lastname = lastname;
        return this;
    }
}
