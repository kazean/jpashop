package jpabook.jpashop.api;

import jpabook.jpashop.domain.*;
import jpabook.jpashop.repository.OrderRepository;
import jpabook.jpashop.repository.order.query.OrderFlatDto;
import jpabook.jpashop.repository.order.query.OrderItemQueryDto;
import jpabook.jpashop.repository.order.query.OrderQueryDto;
import jpabook.jpashop.repository.order.query.OrderQueryRepository;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

import static java.util.stream.Collectors.*;

@RestController
@RequiredArgsConstructor
public class OrderApiController {

    private final OrderRepository orderRepository;
    private final OrderQueryRepository orderQueryRepository;

    /**
     * 엔티티 조회
     * order > member, delivery, orderItem 각 loop
     *  orderItem > Item 각 loop
     *
     *  OrderItem, Item을 직접 초기화하면 Hibernate5Module 설정에 의해 엔티티를 JSON으로 생성한다.
     *  양방향 연관관계면 무한루프에 빠지지 않게 @JsonIgnore설정해줘야 한다
     * @return
     */
    @GetMapping("/api/v1/orders")
    public List<Order> ordersV1(){
        List<Order> result = orderRepository.findAll(new OrderSearch());
        result.stream()
                .forEach(o->{
                    o.getMember().getName();
                    o.getDelivery().getStatus();
                    List<OrderItem> orderItems = o.getOrderItems();
                    orderItems.stream().forEach(oi->oi.getItem().getName());
                });
        return result;
    }

    /**
     * 엔티티 조회 > Dto 변환
     *
     * 지연로딩으로 너무 많은 SQL 실행
     *
     * SQL 실행 수
     * order 1번
     * member , address N번(order 조회 수 만큼)
     * orderItem N번(order 조회 수 만큼)
     * item N번(orderItem 조회 수 만큼)
     * @return
     */
    @GetMapping("/api/v2/orders")
    public List<OrderDto> ordersV2(){
        List<Order> orderList= orderRepository.findAll(new OrderSearch());
        List<OrderDto> result = orderList.stream()
                .map(OrderDto::new)
                .collect(toList());
        return result;
    }

    /**
     * 엔티티 조회 > Dto변환 + join fetch
     *
     * 페치 조인으로 SQL이 1번만 실행됨
     * distnct로 SQL + 애플리케이션 단 distnct
     *
     * 페이징 불가능
     *
     * @return
     */
    @GetMapping("/api/v3/orders")
    public List<OrderDto> orersV3(){
        List<Order> orderList= orderRepository.findAllWithItem();
        List<OrderDto> result = orderList.stream()
                .map(OrderDto::new)
                .collect(toList());
        return result;
    }

    /**
     * 엔티티 > Dto변환 + join fetch + default_batch_fetch_size : 100
     *
     * Order, Member, Delivery 함께조회
     * >ToOne관계는 패치조인한다
     * OrderItem, Item Lazy 전략 + default_batch_fetch_size
     * >ToMany관계는 Lazy Loading 전략
     *
     * 쿼리호출수 1+1
     * 페이징가능
     *
     * @param offest
     * @param limit
     * @return
     */
    @GetMapping("/api/v3.1/orders")
    public List<OrderDto> ordersV3_1(@RequestParam(value = "offset", defaultValue = "0") int offest,
                                     @RequestParam(value = "limit", defaultValue = "10") int limit){
        List<Order> orderList= orderRepository.findAllWithMemberDelivery(offest,limit);
        List<OrderDto> result = orderList.stream()
                .map(OrderDto::new)
                .collect(toList());
        return result;
    }

    /**
     * Dto 직접조회 inner join + equals
     *
     * root1번 컬렉션N번 N+1문제
     * ToOne 관계 한번에 조회후 ToMany관계는 별도로 처리한다.
     *
     * @return
     */
    @GetMapping("/api/v4/orders")
    public List<OrderQueryDto> orderv4(){
        return orderQueryRepository.findOrderQueryDtos();
    }

    /**
     * Dto 직접조회 inner join + in
     *
     * root1번 컬렉션 1번
     * ToOne관계들을 한번에 조회하고, 여기서 얻은 식별자들로 OrderItem을 한꺼번에 조회후 조합
     * Map을 사용해 매칭성능 향상
     * @return
     */
    @GetMapping("/api/v5/orders")
    public List<OrderQueryDto> orderv5(){
        return orderQueryRepository.findAllByDto_optimization();
    }


    /**
     * Dto 직접조회 플랫데이터 최적화
     *
     * 쿼리1번
     *
     * 쿼리는 1번이지만, 조인으로 인해서 DB에서 애플리케이션에 전달하는 중복데이터가 추가되므로 상황에따라 v5보다 느릴수 있다.
     * 애플리케이션에서 추가작업이 크다.
     * 페이징불가능
     */
    @GetMapping("/api/v6/orders")
    public List<OrderQueryDto> orderv6(){
        List<OrderFlatDto> flats = orderQueryRepository.findAllByDto_flat();
        List<OrderQueryDto> result = flats.stream()
                .collect(groupingBy(o -> new OrderQueryDto(o.getOrderId(), o.getName(), o.getOrderDate(), o.getOrderStatus(), o.getAddress())
                        , mapping(o -> new OrderItemQueryDto(o.getOrderId(), o.getItemName(), o.getOrderPrice(), o.getCount()), toList())))
                .entrySet().stream()
                .map(e -> new OrderQueryDto(e.getKey().getOrderId(), e.getKey().getName(), e.getKey().getOrderDate(), e.getKey().getOrderStatus(), e.getKey().getAddress(), e.getValue()))
                .collect(toList());

        return result;
    }

    @Data
    static class OrderDto{
        private Long orderId;
        private String name;
        private LocalDateTime orderDate;
        private OrderStatus orderStatus;
        private Address address;
        private List<OrderITemDto> orderItems;

        public OrderDto(Order order) {
            this.orderId = order.getId();
            this.name = order.getMember().getName();
            this.orderDate = order.getOrderDate();
            this.orderStatus = order.getStatus();
            this.address = order.getDelivery().getAddress();
            this.orderItems = order.getOrderItems().stream()
                    .map(OrderITemDto::new).collect(toList());
        }
    }

    @Data
    static class OrderITemDto{
        private String itemName;
        private int orderPrice;
        private int count;

        public OrderITemDto(OrderItem orderItem) {
            this.itemName = orderItem.getItem().getName();
            this.orderPrice = orderItem.getItem().getPrice();
            this.count = orderItem.getCount();
        }
    }
}
