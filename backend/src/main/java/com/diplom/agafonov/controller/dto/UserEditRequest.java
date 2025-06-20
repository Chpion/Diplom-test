package com.diplom.agafonov.controller.dto;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

public class UserEditRequest {
    private String login;
    private String email;
    private String lastName;
    private String firstName;
    private String middleName;
    private Date dateOfBirth;

    public String getLogin() { return login; }
    public void setLogin(String login) { this.login = login; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public String getLastName() { return lastName; }
    public void setLastName(String lastName) { this.lastName = lastName; }
    public String getFirstName() { return firstName; }
    public void setFirstName(String firstName) { this.firstName = firstName; }
    public String getMiddleName() { return middleName; }
    public void setMiddleName(String middleName) { this.middleName = middleName; }
    public Date getDateOfBirth() { return dateOfBirth; }
    public void setDateOfBirth(String dateOfBirth) {
        try {
            SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy.MM.dd");
            this.dateOfBirth = dateFormat.parse(dateOfBirth);
        } catch (ParseException e) {
            throw new RuntimeException("Неверный формат даты: " + dateOfBirth);
        }
    }
}
