package org.dbflute.handson.logic;

import javax.annotation.Resource;

import org.dbflute.handson.dbflute.exbhv.MemberBhv;
import org.dbflute.handson.dbflute.exbhv.MemberSecurityBhv;
import org.dbflute.handson.dbflute.exbhv.MemberServiceBhv;
import org.dbflute.handson.dbflute.exentity.Member;
import org.dbflute.handson.dbflute.exentity.MemberSecurity;
import org.dbflute.handson.dbflute.exentity.MemberService;
import org.seasar.dbflute.helper.HandyDate;

/**
 * @author nakamura
 */
public class HandsOn07Logic {

    @Resource
    protected MemberBhv memberBhv;

    @Resource
    protected MemberSecurityBhv memberSecurityBhv;

    @Resource
    protected MemberServiceBhv memberServiceBhv;

    // 自分自身の会員を登録
    public Member insertMyselfMember() {
        Member member = new Member();
        member.setMemberName("Muneovic");
        member.setBirthdate(new HandyDate("1985/08/09").getDate());
        member.setMemberAccount("muneo");
        member.setMemberStatusCode_正式会員();
        //        member.setRegisterDatetime(new Timestamp(0));
        //        member.setRegisterUser("muneko");
        //        member.setUpdateDatetime(new Timestamp(100));
        //        member.setUpdateUser("munetaro");
        member.setVersionNo(1L);
        memberBhv.insert(member);

        return member;
    }

    // 誰かを正式会員として登録 & 業務的に必須の関連テーブルも登録
    public Member insertYourselfMember() {
        Member member = new Member();
        member.setMemberName("鈴木宗尾");
        member.setBirthdate(new HandyDate("1960/06/06").getDate());
        member.setMemberAccount("muneo");
        member.setMemberStatusCode_正式会員();
        memberBhv.insert(member);

        // 業務的に必須の関連テーブルの登録
        MemberSecurity security = new MemberSecurity();
        security.setMemberId(member.getMemberId());
        security.setLoginPassword("a");
        security.setReminderQuestion("b");
        security.setReminderAnswer("C");
        security.setReminderUseCount(1); // Section10でカラムが増えたため、暫定的に対処
        memberSecurityBhv.insert(security);

        MemberService service = new MemberService();
        service.setMemberId(member.getMemberId());
        //service.setAkirakaniOkashiiKaramuMei(0); // 明らかにおかしいカラム名
        service.setServicePointCount(0);
        service.setServiceRankCode_Bronze();
        memberServiceBhv.insert(service);

        return member;
    }
}
