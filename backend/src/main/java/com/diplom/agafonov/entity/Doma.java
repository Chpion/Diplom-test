package com.diplom.agafonov.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "doma", uniqueConstraints = {
        @UniqueConstraint(columnNames = {"code"})
})
public class Doma {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "name", nullable = false, length = 200)
    private String name;

    @Column(name = "korp", length = 20)
    private String korp;

    @Column(name = "socr", nullable = false, length = 20)
    private String socr;

    @Column(name = "code", nullable = false, length = 20)
    private String code;

    @Column(name = "index", length = 6)
    private String postalIndex;

    @Column(name = "gninmb", length = 4)
    private String gninmb;

    @Column(name = "uno", length = 20)
    private String uno;

    @Column(name = "ocatd", length = 15)
    private String ocatd;

    public Doma() {
    }

    // Getters and setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getKorp() {
        return korp;
    }

    public void setKorp(String korp) {
        this.korp = korp;
    }

    public String getSocr() {
        return socr;
    }

    public void setSocr(String socr) {
        this.socr = socr;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public String getPostalIndex() {
        return postalIndex;
    }

    public void setPostalIndex(String postalIndex) {
        this.postalIndex = postalIndex;
    }

    public String getGninmb() {
        return gninmb;
    }

    public void setGninmb(String gninmb) {
        this.gninmb = gninmb;
    }

    public String getUno() {
        return uno;
    }

    public void setUno(String uno) {
        this.uno = uno;
    }

    public String getOcatd() {
        return ocatd;
    }

    public void setOcatd(String ocatd) {
        this.ocatd = ocatd;
    }
}
