package com.diplom.agafonov.entity;
import jakarta.persistence.*;

@Entity
@Table(name = "altnames", uniqueConstraints = {
        @UniqueConstraint(columnNames = {"oldcode"})
})
public class Altname {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "oldcode", nullable = false, length = 20)
    private String oldCode;

    @Column(name = "newcode", nullable = false, length = 20)
    private String newCode;

    @Column(name = "level", nullable = false)
    private Integer level;

    public Altname() {
    }

    // Getters and setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getOldCode() {
        return oldCode;
    }

    public void setOldCode(String oldCode) {
        this.oldCode = oldCode;
    }

    public String getNewCode() {
        return newCode;
    }

    public void setNewCode(String newCode) {
        this.newCode = newCode;
    }

    public Integer getLevel() {
        return level;
    }

    public void setLevel(Integer level) {
        this.level = level;
    }
}
