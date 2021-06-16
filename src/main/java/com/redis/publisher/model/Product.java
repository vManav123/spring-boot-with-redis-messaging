package com.redis.publisher.model;


import lombok.*;

@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@Data
public class Product {

    private int id;
    private String name;
    private int quantity;
    private long price;
}
