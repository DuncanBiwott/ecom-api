package com.shoppingAPI.repository;


import com.shoppingAPI.model.Product;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface ProductRepository extends MongoRepository<Product, String> {
}
