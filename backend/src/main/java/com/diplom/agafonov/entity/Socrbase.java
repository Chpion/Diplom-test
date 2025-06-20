package com.diplom.agafonov.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "socrbase")
public class Socrbase {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "level", nullable = false)
    private Integer level;

    @Column(name = "scname", nullable = false, length = 20)
    private String scName;

    @Column(name = "socrname", nullable = false, length = 50)
    private String socrName;

    @Column(name = "kod_t_st", length = 10)
    private String kodTSt;

    public Socrbase() {
    }

    // Getters and setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Integer getLevel() {
        return level;
    }

    public void setLevel(Integer level) {
        this.level = level;
    }

    public String getScName() {
        return scName;
    }

    public void setScName(String scName) {
        this.scName = scName;
    }

    public String getSocrName() {
        return socrName;
    }

    public void setSocrName(String socrName) {
        this.socrName = socrName;
    }

    public String getKodTSt() {
        return kodTSt;
    }

    public void setKodTSt(String kodTSt) {
        this.kodTSt = kodTSt;
    }
}