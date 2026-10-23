package com.example.nosqllab2.products;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ProductSpecifications implements Serializable {
    private String brand;
    private String color;
    private String material;
}