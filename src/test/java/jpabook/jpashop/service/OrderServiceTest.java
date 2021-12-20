package jpabook.jpashop.service;

import jpabook.jpashop.domain.Address;
import jpabook.jpashop.domain.Member;
import jpabook.jpashop.domain.Order;
import jpabook.jpashop.domain.OrderStatus;
import jpabook.jpashop.domain.item.Book;
import jpabook.jpashop.exception.NotEnoughStockException;
import jpabook.jpashop.repository.OrderRepository;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.transaction.annotation.Transactional;

import javax.persistence.EntityManager;

import static org.junit.Assert.*;

@RunWith(SpringRunner.class)
@SpringBootTest
@Transactional
public class OrderServiceTest {
    @Autowired EntityManager em;
    @Autowired OrderService orderService;
    @Autowired OrderRepository orderRepository;

    @Test
    public void 상품주문() throws Exception{
        //given
        Member member = createMember();

        Book book = createBook("OLD JPA",10, 35000);

        int orderCount = 3;
        //when
        Long orderId = orderService.order(member.getId(), book.getId(), orderCount);
        Order getOrder = orderRepository.findOne(orderId);
        //then
        assertEquals("상품주문 상태는 ORDER", OrderStatus.ORDER,getOrder.getStatus());
        assertEquals("상품주문 상품 종류 수가 정확해야 한다.", 1,getOrder.getOrderItems().size());
        assertEquals("주문 가격*수량", orderCount*35000, getOrder.getTotalPrice());
        assertEquals("주문수량만큼 재고가 줄어야 한다.", 7, book.getStockQuantity());

    }

    @Test(expected = NotEnoughStockException.class)
    public void 상품_주문_초과() throws Exception{
        //given
        Member member = createMember();
        Book book = createBook("OLD JPA",10, 35000);
        int orderCount = 13;
        //when
        Long orderId = orderService.order(member.getId(), book.getId(), orderCount);
        Order getOrder = orderRepository.findOne(orderId);

        //then
        fail("주문수량 초과시 에러발생");
    }
    
    @Test
    public void 주문취소() throws Exception{
        //given
        Member member = createMember();
        Book book = createBook("OLD JPA",10, 35000);
        int orderCount = 3;
        Long orderId = orderService.order(member.getId(), book.getId(), orderCount);

        //when
        orderService.cancelOrder(orderId);

        //then
        Order getOrder = orderRepository.findOne(orderId);
        assertEquals("주문상태 취소 CANCEL", OrderStatus.CANCEL, getOrder.getStatus());
        assertEquals("주문취소 후 재고상태가 증가해야한다.", 10, book.getStockQuantity());
    }

    private Book createBook(String name, int quantity, int price) {
        Book book = new Book();
        book.setName(name);
        book.setStockQuantity(quantity);
        book.setPrice(price);
        em.persist(book);
        return book;
    }

    private Member createMember() {
        Member member = new Member();
        member.setName("userA");
        member.setAddress(new Address("seoul", "뱅뱅사거리 35-10", "123-123"));
        em.persist(member);
        return member;
    }
}