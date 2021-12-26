package jpabook.jpashop.api;

import jpabook.jpashop.domain.Address;
import jpabook.jpashop.domain.Order;
import jpabook.jpashop.domain.OrderSearch;
import jpabook.jpashop.domain.OrderStatus;
import jpabook.jpashop.repository.OrderRepository;
import jpabook.jpashop.repository.order.simpleQuery.OrderSimpleQueryDto;
import jpabook.jpashop.repository.order.simpleQuery.OrderSimpleQueryRepository;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequiredArgsConstructor
public class OrderSimpleApiController {
    private final OrderRepository orderRepository;
    private final OrderSimpleQueryRepository orderSimpleQueryRepository;


    /**
     * 프렌젠테이션 단에서 엔티티 JsonIgnore 작업을 해줘야함
     * 엔티티를 외부로 노출하는것은 좋지 않다. > 스펙 변경시 다른 API 문제
     * Hibernate5Module 을 사용하기 보다는 DTO변환해서 사용하는 것이 더 좋은 방법
     * LAZY 대신 EAGER로 설정하면 안된다. > 다른 API에 문제
     *
     * 1. jackson parser ignore 문제
     * 2. 지연로딩 프록시 문제 > 강제 초기화 해주어야 한다
     *  > Hibernate5Module
     *  > 코드에서 강제초기화
     * @return
     */
    @GetMapping("/api/v1/simple-orders")
    public List<Order> ordersV1(){
        List<Order> all = orderRepository.findAllByString(new OrderSearch());
        for(Order order : all){
            order.getMember().getName();    //Lazy 강제 초기화
            order.getDelivery().getAddress();   //Lazy 강제 초기화
        }
        return all;
    }

    /**
     * DTO변환하는 일반적인 방법
     *
     * N+1문제 Lazy 로딩
     * @return
     */
    @GetMapping("/api/v2/simple-orders")
    public Result ordersV2(){
        List<SimpleOrderDto> collect = orderRepository.findAllByString(new OrderSearch()).stream()
                .map(SimpleOrderDto::new)
                .collect(Collectors.toList());
        return new Result(collect);
    }

    /**
     * join fetch
     * 
     * 장 : 한번에 이너조인, 패치기능, 재활용 가능
     * 단 : 컬럼 네트워크 비용 증가
     * @return
     */
    @GetMapping("/api/v3/simple-orders")
    public Result ordersV3(){
        List<SimpleOrderDto> collect = orderRepository.findAllWithMemberDelivery().stream()
                .map(SimpleOrderDto::new)
                .collect(Collectors.toList());
        return new Result(collect);
    }

    /**
     * 일반적인 SQL을 사용할때 처럼 원하는 값을 선택해서 조회
     * new명령어를 사용해서 JPQL의 결과를 DTO로 즉시 변환
     * 
     * 장 : 애플리케이션 네트웍 용량 최적화
     * 단 : 리포토리 재사용성떨어짐, API 스펙에 맞춘 코드가 리포지토리에 들넉나ㅡㄴ 단점
     * @return
     */
    @GetMapping("/api/v4/simple-orders")
    public Result ordersV4(){
        List<OrderSimpleQueryDto> orderDtos = orderSimpleQueryRepository.findOrderDtos();
        return new Result(orderDtos);
    }

    @Data
    @AllArgsConstructor
    static class Result<T>{
        private T data;
    }

    @Data
    static class SimpleOrderDto{
        private Long orderId;
        private String name;
        private LocalDateTime orderDate;
        private OrderStatus status;
        private Address address;

        public SimpleOrderDto(Order order){
            this.orderId = order.getId();
            this.name = order.getMember().getName();
            this.orderDate = order.getOrderDate();
            this.status = order.getStatus();
            this.address = order.getDelivery().getAddress();
        }
    }
}
