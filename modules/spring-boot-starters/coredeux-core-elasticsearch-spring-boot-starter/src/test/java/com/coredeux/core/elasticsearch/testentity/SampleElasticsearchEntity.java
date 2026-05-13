package com.coredeux.core.elasticsearch.testentity;

import org.springframework.data.annotation.Id;

public class SampleElasticsearchEntity {

    @Id
    private String id;

    private String name;

    private Integer age;

    private String payload;

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

    public Integer getAge() {
        return age;
    }

    public void setAge(Integer age) {
        this.age = age;
    }

    public String getPayload() {
        return payload;
    }

    public void setPayload(String payload) {
        this.payload = payload;
    }
}
