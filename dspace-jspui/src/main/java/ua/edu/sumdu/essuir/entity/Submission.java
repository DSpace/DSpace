package ua.edu.sumdu.essuir.entity;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

public class Submission implements Comparable<Submission> {
    private String author;
    private String email;
    private Date date;

    public Submission(String line) {
        email = line.substring(line.indexOf('(') + 1, line.indexOf(')'));
        author = line.substring("Submitted by ".length(), line.indexOf('(') - 1);
        String date = line.substring(line.indexOf(") on ") + ") on ".length(), line.indexOf(") on ") + ") on ".length() + "yyyy-mm-dd".length());
        SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd");
        try {
            this.date = format.parse(date);
        } catch (ParseException e) {
            e.printStackTrace();
        }
    }


    public String getAuthor() {
        return author;
    }

    public void setAuthor(String author) {
        this.author = author;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public Date getDate() {
        return date;
    }

    public void setDate(Date date) {
        this.date = date;
    }

    @Override
    public int compareTo(Submission submission) {
        return this.date.compareTo(submission.date);
    }
}
