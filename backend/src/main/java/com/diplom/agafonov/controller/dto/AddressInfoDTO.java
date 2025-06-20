package com.diplom.agafonov.controller.dto;

import lombok.Data;
import java.util.List;

@Data
public class AddressInfoDTO {
    private String region;
    private String city;
    private String street;
    private List<String> houses;

    public AddressInfoDTO(String region, String city, String street, List<String> houses) {
        this.region = region;
        this.city = city;
        this.street = street;
        this.houses = houses;
    }
}
