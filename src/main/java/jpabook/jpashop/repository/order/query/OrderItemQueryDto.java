package jpabook.jpashop.repository.order.query;

import lombok.Data;

@Data
public class OrderItemQueryDto {
    private Long orderId;
    private String name;
    private int orderPrice;
    private int count;

    public OrderItemQueryDto(Long orderId, String name, int orderPrice, int count) {
        this.orderId = orderId;
        this.name = name;
        this.orderPrice = orderPrice;
        this.count = count;
    }
}
