package org.dbflute.handson.exercise.phase1;

import java.util.List;

import javax.annotation.Resource;

import org.dbflute.handson.dbflute.cbean.MemberCB;
import org.dbflute.handson.dbflute.exbhv.MemberBhv;
import org.dbflute.handson.dbflute.exentity.Member;
import org.dbflute.handson.unit.UnitContainerTestCase;

/**
 * @author nakamura
 */
public class HandsOn02Test extends UnitContainerTestCase {

    @Resource
    protected MemberBhv memberBhv;

    public void test_existsTestData() throws Exception {
        // ## Arrange ##
        MemberCB cb = new MemberCB();

        // ## Act ##
        int count = memberBhv.selectCount(cb);

        System.out.println("count:" + count);
        // ## Assert ##
        assertTrue(count > 0);
    }

    public void test_searchMemberNameStartsWithS() throws Exception {
        // ## Arrange ##
        MemberCB cb = new MemberCB();
        cb.query().setMemberName_PrefixSearch("S");
        cb.query().addOrderBy_MemberName_Asc();

        // ## Act ##
        List<Member> memberList = memberBhv.selectList(cb);

        // ## Assert ##
        assertHasAnyElement(memberList);
        for (Member member : memberList) {
            System.out.println(member.getMemberName());
            assertTrue(member.getMemberName().startsWith("S"));
        }
    }

    public void test_searchFirstMember() throws Exception {
        // ## Arrange ##
        MemberCB cb = new MemberCB();
        cb.query().setMemberId_Equal(1);

        // ## Act ##
        Member member = memberBhv.selectEntityWithDeletedCheck(cb);

        // ## Assert ##
        assertEquals(1, member.getMemberId());
    }

    public void test_searchNoBirthdateMember() throws Exception {
        // ## Arrange ##
        MemberCB cb = new MemberCB();
        cb.query().setBirthdate_IsNull();
        cb.query().addOrderBy_UpdateDatetime_Desc();

        // ## Act ##
        List<Member> memberList = memberBhv.selectList(cb);

        // ## Assert ##
        assertHasAnyElement(memberList);
        for (Member member : memberList) {
            System.out.println(member.getMemberName());
            assertNull(member.getBirthdate());
        }
    }

}
