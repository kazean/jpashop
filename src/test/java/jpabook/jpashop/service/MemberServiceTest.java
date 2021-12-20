package jpabook.jpashop.service;

import jpabook.jpashop.domain.Member;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.Rollback;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.transaction.annotation.Transactional;

import static org.junit.Assert.*;

@RunWith(SpringRunner.class)
@SpringBootTest
@Transactional
public class MemberServiceTest {
    @Autowired MemberService memberService;

    @Test
    @Rollback(value = false)
    public void 회원가입() throws Exception{
        //given
        Member member = new Member();
        member.setName("memberA");
        Long memberId = memberService.join(member);

        //when
        Member findmember = memberService.findOne(memberId);
        
        //then
        assertEquals("회원가입 정상동작확인", memberId, findmember.getId());
    }
    
    @Test(expected = IllegalStateException.class)
    public void 중복_회원_예외() throws Exception{
        //given
        Member member1 = new Member();
        member1.setName("memberA");
        
        Member member2 = new Member();
        member2.setName("memberA");
        //when
        memberService.join(member1);
        memberService.join(member2);
        
        //then
        fail("중복회원예외가 발생하지 않았음");
    }
}