package com.crafter.lol.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ItemOption {

    private String itemId;
    private String name;
    private String imageUrl;
    private Integer cost;
}
