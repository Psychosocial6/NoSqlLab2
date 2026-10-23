package com.example.nosqllab2.products;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Date;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ProductStatus {
    private String code;
    private Date updatedAt;
    private String comment;
}