package com.shoppingAPI.fallBack;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class FallBackController {
    @GetMapping("/orderServiceFallBack")
    public  String orderServiceFallBack(){
        return "Order Service is taking longer than expected to respond please try again after some time";

    }

    @GetMapping("/productServiceFallBack")
    public  String productServiceFallBack(){
        return "Product Service is taking longer than expected to respond please try again after some time";

    }
}