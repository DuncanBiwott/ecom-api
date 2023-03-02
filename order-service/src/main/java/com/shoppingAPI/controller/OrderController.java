package com.shoppingAPI.controller;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.shoppingAPI.dto.OrderRequest;
import com.shoppingAPI.service.OrderService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;



@RestController
@RequestMapping("/api/order")
@RequiredArgsConstructor
@Slf4j
public class OrderController {

    private final OrderService orderService;
    private  final ObjectMapper objectMapper;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
   public String placeOrder(@RequestBody OrderRequest orderRequest) throws JsonProcessingException {

        log.info("This request as been received :{}",objectMapper.writeValueAsString(orderRequest));


        return  orderService.placeOrder(orderRequest);
    }

}
