package com.shoppingAPI.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.shoppingAPI.dto.InventoryResponse;
import com.shoppingAPI.dto.OrderLineItemsDto;
import com.shoppingAPI.dto.OrderRequest;
import com.shoppingAPI.event.OrderPlacedEvent;
import com.shoppingAPI.model.Order;
import com.shoppingAPI.model.OrderLineItems;
import com.shoppingAPI.repository.OrderRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.sleuth.Span;
import org.springframework.cloud.sleuth.Tracer;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import javax.transaction.Transactional;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;


@Service
@RequiredArgsConstructor
@Transactional
@Slf4j
public class OrderService {

    private final OrderRepository orderRepository;
    private final WebClient.Builder webClientBuilder;
    private final Tracer tracer;
    private  final  ObjectMapper objectMapper;
    private final KafkaTemplate<String, OrderPlacedEvent> kafkaTemplate;

    public String placeOrder(OrderRequest orderRequest) {
        Order order=new Order();
        order.setOrderNumber(UUID.randomUUID().toString());

        log.info("order ID :{} as been added",order.getOrderNumber());



        log.info("Calling the getOrderliesItems Method");


        List<OrderLineItems> orderLineItems = orderRequest.getOrderLineItemsDtoList()
                .stream()
                .map(this::mapToDto)
                .toList();

        order.setOrderLineItems(orderLineItems);

        List<String> skuCodes = order.getOrderLineItems().stream()
                .map(OrderLineItems::getSkuCode)
                .toList();
        Span inventorySpan=tracer.nextSpan().name("inventoryServiceCall");

        try (Tracer.SpanInScope isLookUp = tracer.withSpan(inventorySpan.start())){

            inventorySpan.tag("call", "inventory-service");

            InventoryResponse[] inventoryResponseArray = webClientBuilder.build().
                    get()
                    .uri("http://inventory-service/api/inventory",
                            uriBuilder -> uriBuilder.queryParam("skuCode", skuCodes).build())
                    .retrieve()
                    .bodyToMono(InventoryResponse[].class)
                    .block();

            boolean allProductsInStock = Arrays.stream(inventoryResponseArray)
                    .allMatch(InventoryResponse::isInStock);

            if (allProductsInStock) {
                orderRepository.save(order);
                log.info("Calling Notification Service");

                kafkaTemplate.send("notificationTopic", new OrderPlacedEvent(order.getOrderNumber()));
                log.info("Message has been send successfully");
                return "Order Placed Successfully";
            } else {
                throw new IllegalArgumentException("Product is not in stock, please try again later");
            }

        } finally {
            inventorySpan.end();
        }



    }

    private OrderLineItems mapToDto(OrderLineItemsDto orderLineItemsDto) {
        OrderLineItems orderLineItems = new OrderLineItems();
        orderLineItems.setPrice(orderLineItemsDto.getPrice());
        orderLineItems.setQuantity(orderLineItemsDto.getQuantity());
        orderLineItems.setSkuCode(orderLineItemsDto.getSkuCode());
        return orderLineItems;
    }

    public Page<Order> getOrder(Integer page, Integer pageNumber) {
        Pageable pageable= PageRequest.of(page,pageNumber, Sort.by(Sort.Direction.DESC,"orderNumber"));
        return orderRepository.findAll(pageable);
    }
}
