package org.dbflute.handson.logic;

import javax.annotation.Resource;

import org.dbflute.handson.dbflute.cbean.MemberSecurityCB;
import org.dbflute.handson.dbflute.cbean.MemberServiceCB;
import org.dbflute.handson.dbflute.cbean.MemberWithdrawalCB;
import org.dbflute.handson.dbflute.exbhv.MemberSecurityBhv;
import org.dbflute.handson.dbflute.exbhv.MemberServiceBhv;
import org.dbflute.handson.dbflute.exbhv.MemberWithdrawalBhv;
import org.dbflute.handson.dbflute.exentity.Member;
import org.dbflute.handson.dbflute.exentity.MemberWithdrawal;
import org.dbflute.handson.unit.UnitContainerTestCase;
import org.seasar.dbflute.helper.HandyDate;

/**
 * @author nakamura
 */
public class HandsOn07LogicTest extends UnitContainerTestCase {

    @Resource
    protected MemberSecurityBhv memberSecurityBhv;

    @Resource
    protected MemberServiceBhv memberServiceBhv;

    @Resource
    protected MemberWithdrawalBhv memberWithdrawalBhv;

    public void test_insertMyselfMember_会員が登録されていること() {
        // ## Arrange ##
        HandsOn07Logic logic = new HandsOn07Logic();
        inject(logic);

        // ## Act ##
        Member member = logic.insertMyselfMember();

        // ## Assert ##
        log("Name:" + member.getMemberName() + ". Birth:" + member.getBirthdate());
        assertEquals("Muneovic", member.getMemberName());
        assertEquals(new HandyDate("1985/08/09").getDate(), member.getBirthdate());
    }

    public void test_insertYourselfMember_会員が登録されていること() {
        // ## Arrange ##
        HandsOn07Logic logic = new HandsOn07Logic();
        inject(logic);

        // ## Act ##
        Member member = logic.insertYourselfMember();

        // ## Assert ##
        log("Name:" + member.getMemberName() + ". Birth:" + member.getBirthdate());

        // 代表的なカラムだけを利用して登録されていることをアサート
        assertEquals("鈴木宗尾", member.getMemberName());
        assertEquals(new HandyDate("1960/06/06").getDate(), member.getBirthdate());
        assertEquals("muneo", member.getMemberAccount());
        assertTrue(member.isMemberStatusCode正式会員());

        // 関連テーブルの登録もアサート
        MemberSecurityCB cb = new MemberSecurityCB();
        cb.query().setMemberId_Equal(member.getMemberId());
        cb.query().setLoginPassword_Equal("a");
        cb.query().setReminderQuestion_Equal("b");
        cb.query().setReminderAnswer_Equal("c");
        assertNotNull(memberSecurityBhv.selectEntityWithDeletedCheck(cb));

        MemberServiceCB serviceCb = new MemberServiceCB();
        serviceCb.query().setMemberId_Equal(member.getMemberId());
        //serviceCb.query().setAkirakaniOkashiiKaramuMei_Equal(0);
        serviceCb.query().setServicePointCount_Equal(0);
        serviceCb.query().setServiceRankCode_Equal_Bronze();
        assertNotNull(memberServiceBhv.selectEntityWithDeletedCheck(serviceCb));

        // 登録していない関連テーブルが登録されていないこともアサート
        MemberWithdrawalCB withdrawalCb = new MemberWithdrawalCB();
        withdrawalCb.query().setMemberId_Equal(member.getMemberId());
        MemberWithdrawal entity = memberWithdrawalBhv.selectEntity(withdrawalCb);
        assertNull(entity);
    }
}