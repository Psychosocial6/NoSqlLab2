package com.example.nosqllab2.products;

import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ProductSpecifications implements Serializable {
    @Size(max = 256)
    private String brand;
    @Size(max = 256)
    private String color;
    @Size(max = 256)
    private String material;
}
