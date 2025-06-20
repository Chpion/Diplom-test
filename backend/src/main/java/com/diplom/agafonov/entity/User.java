package com.diplom.agafonov.entity;

import jakarta.persistence.*;
import lombok.Data;
import java.util.Date;
import java.util.Set;
import java.util.HashSet;

@Entity
@Table(name = "users")
@Data
public class User {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false)
    private String login; // Уникальный идентификатор для авторизации

    @Column(nullable = false)
    private String password;

    @Column(nullable = true)
    private String email; // Необязательный email

    @Column(nullable = true)
    private String lastName; // Фамилия

    @Column(nullable = true)
    private String firstName; // Имя

    @Column(nullable = true)
    private String middleName; // Отчество

    @Column(nullable = true)
    @Temporal(TemporalType.DATE)
    private Date dateOfBirth; // Дата рождения

    @ManyToMany(fetch = FetchType.EAGER)
    @JoinTable(
            name = "user_roles",
            joinColumns = @JoinColumn(name = "user_id"),
            inverseJoinColumns = @JoinColumn(name = "role_id")
    )
    private Set<Role> roles = new HashSet<>();

    @ElementCollection
    @CollectionTable(name = "user_search_queries", joinColumns = @JoinColumn(name = "user_id"))
    @Column(name = "query")
    private Set<String> searchQueries = new HashSet<>();

    public void setId(Long id) {
        this.id = id;
    }

    public void setLogin(String login) {
        this.login = login;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public void setLastName(String lastName) {
        this.lastName = lastName;
    }

    public void setFirstName(String firstName) {
        this.firstName = firstName;
    }

    public void setMiddleName(String middleName) {
        this.middleName = middleName;
    }

    public void setBirthDate(Date birthDate) {
        this.dateOfBirth = birthDate;
    }

    public void setRoles(Set<Role> roles) {
        this.roles = roles;
    }

    public void setSearchQueries(Set<String> searchQueries) {
        this.searchQueries = searchQueries;
    }

    public Long getId() {
        return id;
    }

    public String getLogin() {
        return login;
    }

    public String getPassword() {
        return password;
    }

    public String getEmail() {
        return email;
    }

    public String getLastName() {
        return lastName;
    }

    public String getFirstName() {
        return firstName;
    }

    public String getMiddleName() {
        return middleName;
    }

    public Date getDateOfBirth() {
        return dateOfBirth;
    }

    public Set<Role> getRoles() {
        return roles;
    }

    public Set<String> getSearchQueries() {
        return searchQueries;
    }
}