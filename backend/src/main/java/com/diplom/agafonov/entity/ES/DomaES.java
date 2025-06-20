package com.diplom.agafonov.entity.ES;

import jakarta.persistence.Column;
import org.springframework.data.annotation.Id;
import org.springframework.data.elasticsearch.annotations.Document;
import org.springframework.data.elasticsearch.annotations.Field;
import org.springframework.data.elasticsearch.annotations.FieldType;

@Document(indexName = "doma_new_v2")
public class DomaES {
    @Id
    private String id;

    @Field(type = FieldType.Text, analyzer = "autocomplete", searchAnalyzer = "standard")
    private String name;

    @Field(type = FieldType.Keyword)
    private String korp;

    @Field(type = FieldType.Keyword)
    private String socr;

    @Field(type = FieldType.Text)
    private String code;

    @Field(type = FieldType.Keyword)
    private String postalIndex;

    @Field(type = FieldType.Keyword)
    private String gninmb;

    @Field(type = FieldType.Keyword)
    private String uno;

    @Field(type = FieldType.Keyword)
    private String ocatd;

    public DomaES() {
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
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
