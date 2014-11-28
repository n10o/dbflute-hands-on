package org.dbflute.handson.exercise.phase1;

import java.util.List;

import javax.annotation.Resource;

import org.dbflute.handson.dbflute.cbean.MemberAddressCB;
import org.dbflute.handson.dbflute.cbean.MemberCB;
import org.dbflute.handson.dbflute.cbean.PurchaseCB;
import org.dbflute.handson.dbflute.exbhv.MemberAddressBhv;
import org.dbflute.handson.dbflute.exbhv.MemberBhv;
import org.dbflute.handson.dbflute.exbhv.PurchaseBhv;
import org.dbflute.handson.dbflute.exentity.Member;
import org.dbflute.handson.dbflute.exentity.MemberAddress;
import org.dbflute.handson.dbflute.exentity.Purchase;
import org.dbflute.handson.unit.UnitContainerTestCase;
import org.seasar.dbflute.cbean.ListResultBean;

/**
 * @author nakamura
 */
public class HandsOn05Test extends UnitContainerTestCase {

    @Resource
    protected MemberBhv memberBhv;

    @Resource
    protected PurchaseBhv purchaseBhv;

    @Resource
    protected MemberAddressBhv memberAddressBhv;

    // 会員住所情報を検索
    public void test_searchMemberAddress() {
        // ## Arrange ##
        MemberAddressCB cb = new MemberAddressCB();
        cb.setupSelect_Member();
        cb.setupSelect_Region();
        cb.query().addOrderBy_MemberId_Asc();
        cb.query().addOrderBy_ValidBeginDate_Desc();

        // ## Act ##
        ListResultBean<MemberAddress> addressList = memberAddressBhv.selectList(cb);

        // ## Assert ##
        assertHasAnyElement(addressList);
        for (MemberAddress address : addressList) {
            log("Name:" + address.getMember().getMemberName() + ". ValidBegin:" + address.getValidBeginDate()
                    + ". ValidEnd:" + address.getValidEndDate() + ". Address:" + address.getAddress() + ". Region:"
                    + address.getRegion().getRegionName());
            assertNotNull(address.getMember());
            assertNotNull(address.getRegion());
        }

        //      会員住所基点版(おもいで)
        //        // ## Arrange ##
        //        MemberCB cb = new MemberCB();
        //        cb.query().addOrderBy_MemberId_Asc();
        //        ListResultBean<Member> memberList = memberBhv.selectList(cb);
        //
        //        // ## Act ##
        //        memberBhv.loadMemberAddressList(memberList, new ConditionBeanSetupper<MemberAddressCB>() {
        //            public void setup(MemberAddressCB cb) {
        //                cb.query().addOrderBy_ValidBeginDate_Desc();
        //            }
        //        });
        //        // ## Assert ##
        //        for (Member member : memberList) {
        //            log("Name:" + member.getMemberName() + ". Address:" + member.getMemberAddressList());
        //        }
    }

    // additionalForeignKeyMapを編集後SchemaHTMLにて関連づいていることを確認 -> member_address(AsValid)

    // 会員と共に現在の住所を取得して検索
    public void test_searchMemberWithCurrentAddress() {
        // ## Arrange ##
        MemberCB cb = new MemberCB();
        cb.setupSelect_MemberAddressAsValid(currentDate());
        //cb.query().queryMemberAddressAsValid(currentDate()).setMemberAddressId_IsNotNull(); // この条件つけるのはセーフですか？ -> アウト

        // ## Act ##
        List<Member> memberList = memberBhv.selectList(cb);

        // ## Assert ##
        boolean existAddress = false;
        assertHasAnyElement(memberList);
        for (Member member : memberList) {
            MemberAddress validAddress = member.getMemberAddressAsValid();
            log("Name:" + member.getMemberName() + ". Address:" + validAddress);
            if (validAddress != null) {
                assertNotNull(validAddress.getAddress());
                existAddress = true;
            }
        }
        assertTrue(existAddress);
    }

    // 千葉に住んでいる会員の支払い済み購入を検索
    public void test_searchPurchaseCompletePaymentLivingChiba() {
        // ## Arrange ##
        PurchaseCB cb = new PurchaseCB();
        cb.setupSelect_Member().withMemberStatus();
        cb.setupSelect_Member().withMemberAddressAsValid(currentDate()).withRegion();
        cb.query().setPaymentCompleteFlg_Equal_True();
        cb.query().queryMember().queryMemberAddressAsValid(currentDate()).setRegionId_Equal_千葉();

        // ## Act ##
        List<Purchase> purchaseList = purchaseBhv.selectList(cb);

        // ## Assert ##v
        assertHasAnyElement(purchaseList);
        for (Purchase purchase : purchaseList) {
            Member member = purchase.getMember();
            MemberAddress validAddress = member.getMemberAddressAsValid();
            log("Name:" + member.getMemberName() + ". Status:" + member.getMemberStatus().getMemberStatusName()
                    + ". Address:" + validAddress.getAddress());
            assertTrue(validAddress.isRegionId千葉());
        }
    }

    // 最終ログイン時の会員ステータスを取得して会員を検索
    public void test_searchLastLoginMemberStatus() {
        // ## Arrange ##
        MemberCB cb = new MemberCB();
        cb.setupSelect_MemberLoginAsLatest();
        cb.setupSelect_MemberStatus();
        //cb.query().queryMemberLoginAsLatest().setMemberLoginId_IsNotNull(); // この条件つけるのはセーフですか？ -> アウト

        // ## Act ##
        List<Member> memberList = memberBhv.selectList(cb);

        // ## Assert ##
        boolean existLastLogin = false;
        assertHasAnyElement(memberList);
        for (Member member : memberList) {
            log("Name:" + member.getMemberName() + ". LastLoginDate:" + member.getMemberLoginAsLatest()
                    + ". LastStatus:" + member.getMemberStatus().getMemberStatusName());
            if (member.getMemberLoginAsLatest() != null) {
                assertNotNull(member.getMemberLoginAsLatest().getLoginDatetime());
                existLastLogin = true;
            }
        }
        assertTrue(existLastLogin);
    }
}
