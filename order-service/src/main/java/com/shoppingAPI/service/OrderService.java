package com.shoppingAPI.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.shoppingAPI.dto.InventoryResponse;
import com.shoppingAPI.dto.OrderLineItemsDto;
import com.shoppingAPI.dto.OrderRequest;
import com.shoppingAPI.model.Order;
import com.shoppingAPI.model.OrderLineItems;
import com.shoppingAPI.repository.OrderRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional
@Slf4j
public class OrderService {

    private final OrderRepository orderRepository;
    private final WebClient.Builder webClientBuilder;
    private  final  ObjectMapper objectMapper;
    //private final KafkaTemplate<String, OrderPlacedEvent> kafkaTemplate;

    public String placeOrder(OrderRequest orderRequest) throws JsonProcessingException {
        Order order=new Order();
        order.setOrderNumber(UUID.randomUUID().toString());

        log.info("order ID :{} as been added",order.getOrderNumber());



        log.info("Calling the getOrderliesItems Method");

        log.info("OrderLineItems List :{}", objectMapper.writeValueAsString(orderRequest.getOrderLineItemsDtoList()));



        List<OrderLineItems> orderLineItems = orderRequest.getOrderLineItemsDtoList()
                .stream()
                .map(this::mapToDto)
                .toList();

        order.setOrderLineItems(orderLineItems);

        List<String> skuCodes = order.getOrderLineItems().stream()
                .map(OrderLineItems::getSkuCode)
                .toList();

       // Span inventoryServiceLookup = tracer.nextSpan().name("InventoryServiceLookup");
            // Call Inventory Service, and place order if product is in
            // stock
            InventoryResponse[] inventoryResponseArray = webClientBuilder.build().
                    get()
                    .uri("http://INVENTORY-SERVICE/api/inventory",
                            uriBuilder -> uriBuilder.queryParam("skuCode", skuCodes).build())
                    .retrieve()
                    .bodyToMono(InventoryResponse[].class)
                    .block();

        boolean allProductsInStock = Arrays.stream(inventoryResponseArray)
                    .allMatch(InventoryResponse::isInStock);

            if (allProductsInStock) {
                orderRepository.save(order);
               // kafkaTemplate.send("notificationTopic", new OrderPlacedEvent(order.getOrderNumber()));
                return "Order Placed Successfully";
            } else {
                throw new IllegalArgumentException("Product is not in stock, please try again later");
            }

    }

    private OrderLineItems mapToDto(OrderLineItemsDto orderLineItemsDto) {
        OrderLineItems orderLineItems = new OrderLineItems();
        orderLineItems.setPrice(orderLineItemsDto.getPrice());
        orderLineItems.setQuantity(orderLineItemsDto.getQuantity());
        orderLineItems.setSkuCode(orderLineItemsDto.getSkuCode());
        return orderLineItems;
    }
}
