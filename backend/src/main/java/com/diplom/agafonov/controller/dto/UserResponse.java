package com.diplom.agafonov.controller.dto;

import java.util.Date;
import java.util.Set;
import java.util.stream.Collectors;

public class UserResponse {
    private String login;
    private String email;
    private String lastName;
    private String firstName;
    private String middleName;
    private Date dateOfBirth;
    private Set<String> roles;

    public UserResponse(String login, String email, String lastName, String firstName, String middleName, Date dateOfBirth, Set<String> roles) {
        this.login = login;
        this.email = email;
        this.lastName = lastName;
        this.firstName = firstName;
        this.middleName = middleName;
        this.dateOfBirth = dateOfBirth;
        this.roles = roles;
    }

    // Геттеры и сеттеры
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
    public void setDateOfBirth(Date dateOfBirth) { this.dateOfBirth = dateOfBirth; }
    public Set<String> getRoles() { return roles; }
    public void setRoles(Set<String> roles) { this.roles = roles; }
}
