package com.diplom.agafonov.entity.ES;

import org.springframework.data.annotation.Id;
import org.springframework.data.elasticsearch.annotations.Document;
import org.springframework.data.elasticsearch.annotations.Field;
import org.springframework.data.elasticsearch.annotations.FieldType;

@Document(indexName = "socrbase_new_v2")
public class SocrbaseES {
    @Id
    private String id;

    @Field(type = FieldType.Integer)
    private Integer level;

    @Field(type = FieldType.Keyword)
    private String scName;

    @Field(type = FieldType.Text)
    private String socrName;

    @Field(type = FieldType.Keyword)
    private String kodTSt;

    public SocrbaseES() {
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
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

    // Геттеры и сеттеры
}
