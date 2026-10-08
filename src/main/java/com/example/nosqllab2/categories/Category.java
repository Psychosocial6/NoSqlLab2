package com.example.nosqllab2.categories;

import com.google.cloud.firestore.annotation.DocumentId;
import com.google.cloud.spring.data.firestore.Document;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;


@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
@Document(collectionName = "categories")
public class Category {
    @DocumentId
    private String id;
    private String name;
    private String description;

}
