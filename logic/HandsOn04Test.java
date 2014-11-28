package org.dbflute.handson.exercise.phase1;

import java.util.ArrayList;
import java.util.List;

import javax.annotation.Resource;

import org.dbflute.handson.dbflute.allcommon.CDef;
import org.dbflute.handson.dbflute.cbean.MemberCB;
import org.dbflute.handson.dbflute.cbean.PurchaseCB;
import org.dbflute.handson.dbflute.exbhv.MemberBhv;
import org.dbflute.handson.dbflute.exbhv.PurchaseBhv;
import org.dbflute.handson.dbflute.exentity.Member;
import org.dbflute.handson.dbflute.exentity.MemberWithdrawal;
import org.dbflute.handson.dbflute.exentity.Purchase;
import org.dbflute.handson.unit.UnitContainerTestCase;
import org.seasar.dbflute.bhv.ConditionBeanSetupper;
import org.seasar.dbflute.cbean.ListResultBean;
import org.seasar.dbflute.cbean.SubQuery;

/**
 * @author nakamura
 */
public class HandsOn04Test extends UnitContainerTestCase {

    @Resource
    protected MemberBhv memberBhv;
    @Resource
    protected PurchaseBhv purchaseBhv;

    // 退会会員の未払い購入を検索
    public void test_searchWdlPaymentUncompletePurchase() throws Exception {
        // ## Arrange ##
        PurchaseCB cb = new PurchaseCB();
        cb.setupSelect_Member();
        cb.setupSelect_Product();
        //cb.query().queryMember().setMemberStatusCode_Equal("WDL");
        cb.query().queryMember().setMemberStatusCode_Equal_退会会員();
        //cb.query().setPaymentCompleteFlg_Equal(0);
        cb.query().setPaymentCompleteFlg_Equal_False();
        cb.query().addOrderBy_PurchaseDatetime_Desc();

        // ## Act ##
        // Listを使った場合とListResultBeanの違いについてうかがう->ListResultBeanはEntityを取り扱うのに便利なメソッド
        ListResultBean<Purchase> purchaseList = purchaseBhv.selectList(cb);
        //List<Purchase> purchaseList = purchaseBhv.selectList(cb);

        // ## Assert ##
        assertHasAnyElement(purchaseList);
        for (Purchase purchase : purchaseList) {
            log("Name:" + purchase.getMember().getMemberName() + " Product:" + purchase.getProduct().getProductName());
            assertTrue(purchase.isPaymentCompleteFlgFalse());
        }
    }

    //会員退会情報も取得して会員を検索
    public void test_searchMemberWithWithdrawal() throws Exception {
        // ## Arrange ##
        MemberCB cb = new MemberCB();
        cb.setupSelect_MemberWithdrawalAsOne();

        // ## Act ##
        List<Member> memberList = memberBhv.selectList(cb);

        // ## Assert ##
        // 不意のバグや不意のデータ不備でもテストができるだけ成り立つこと
        boolean existWDL = false;
        boolean existNotWDL = false;
        assertHasAnyElement(memberList);
        for (Member member : memberList) {
            MemberWithdrawal memberWithdrawalAsOne = member.getMemberWithdrawalAsOne();
            log(member.getMemberName() + " , " + member.getMemberStatusCode() + " , " + memberWithdrawalAsOne);

            //if (member.getMemberStatusCode().equals("WDL")) {　  // ベタ書き版
            if (member.isMemberStatusCode退会会員()) {
                existWDL = true;
                // 退会会員は会員退会情報を持っていることをアサート
                assertNotNull(memberWithdrawalAsOne);
            } else {
                existNotWDL = true;
                // 退会会員ではない会員は、会員退会情報を持っていないことをアサート
                assertNull(memberWithdrawalAsOne);
            }
        }
        assertTrue(existWDL);
        assertTrue(existNotWDL);
    }

    /**
     * 区分値メソッドを使って実装編
     */

    // 一番若い仮会員の会員を検索
    public void test_searchYoungestMember() {
        // ## Arrange ##
        MemberCB cb = new MemberCB();
        cb.setupSelect_MemberStatus();
        cb.query().setMemberStatusCode_Equal_仮会員();
        //cb.query().setBirthdate_IsNotNull(); // 不要
        // 同じ生年月日が２人いたら２人だす。
        //cb.fetchFirst(1);
        cb.query().scalar_Equal().max(new SubQuery<MemberCB>() {
            public void query(MemberCB subCB) {
                subCB.specify().columnBirthdate();
                subCB.query().setMemberStatusCode_Equal_仮会員();
            }
        });

        // ## Act ##
        List<Member> memberList = memberBhv.selectList(cb);

        // ## Assert ##
        assertHasAnyElement(memberList);
        for (Member member : memberList) {
            log("Name:" + member.getMemberName() + ". Status:" + member.getMemberStatus().getMemberStatusName()
                    + ". Birth:" + member.getBirthdate());
            assertTrue(member.isMemberStatusCode仮会員());
        }
    }

    // 支払い済みの購入の中で一番若い正式会員のものだけ検索
    public void test_searchYoungestFMLPurchased() {
        // 解釈1.とにかく一番若い正式会員を取得し、その会員の支払い済み購入を取得 -> 件数0なのではずれ
        // 解釈2.支払い済み購入を取得し、その中から一番若い会員の購入を取得
        // ## Arrange ##
        PurchaseCB cb = new PurchaseCB();
        //cb.setupSelect_Member(); // 不要
        cb.setupSelect_Member().withMemberStatus();
        cb.query().queryMember().setMemberStatusCode_Equal_正式会員();
        cb.query().setPaymentCompleteFlg_Equal_True();
        // 解釈2
        cb.query().queryMember().scalar_Equal().max(new SubQuery<MemberCB>() {
            public void query(MemberCB subCB) {
                subCB.specify().columnBirthdate();
                subCB.query().setMemberStatusCode_Equal_正式会員();
                subCB.query().existsPurchaseList(new SubQuery<PurchaseCB>() {
                    public void query(PurchaseCB subCB) {
                        subCB.query().setPaymentCompleteFlg_Equal_True();
                    }
                });
            }
        });
        cb.query().addOrderBy_PurchaseDatetime_Desc();

        // ## Act ##
        List<Purchase> purchaseList = purchaseBhv.selectList(cb);

        // ## Assert ##
        assertHasAnyElement(purchaseList);
        for (Purchase purchase : purchaseList) {
            Member member = purchase.getMember();
            String memberStatusName = member.getMemberStatus().getMemberStatusName();
            log("PurchaseID:" + purchase.getPurchaseId() + ". MemberName:" + member.getMemberName()
                    + ". MemberStatusName:" + memberStatusName + ". PaymentCompleteFlg:"
                    + purchase.getPaymentCompleteFlgName());
            assertTrue(member.getMemberStatus().isMemberStatusCode正式会員());
        }
    }

    // 生産販売可能な商品の購入を検索
    public void test_searchPurchaseOnlyAvailableProduct() {
        // ## Arrange ##
        PurchaseCB cb = new PurchaseCB();
        cb.setupSelect_Product();
        cb.setupSelect_Product().withProductStatus();
        cb.query().queryProduct().setProductStatusCode_Equal_生産販売可能();
        cb.query().addOrderBy_PurchasePrice_Desc();

        // ## Act ##
        List<Purchase> purchaseList = purchaseBhv.selectList(cb);

        // ## Assert ##
        assertHasAnyElement(purchaseList);
        for (Purchase purchase : purchaseList) {
            log("ProductStatusName:" + purchase.getProduct().getProductStatus().getProductStatusName());
            assertTrue(purchase.getProduct().isProductStatusCode生産販売可能());
        }
    }

    // 正式会員と退会会員の会員を検索
    public void test_searchFmlAndWdl() {
        // ## Arrange ##
        MemberCB cb = new MemberCB();
        cb.setupSelect_MemberStatus();
        // おもいで
        //        List<String> targetStatusCodeList = new ArrayList<String>();
        //        targetStatusCodeList.add(CDef.MemberStatus.正式会員.code());
        //        //targetStatusCodeList.add(CDef.MemberStatus.退会会員.toString());
        //        targetStatusCodeList.add(CDef.MemberStatus.退会会員.code());

        List<CDef.MemberStatus> memberStatusList = new ArrayList<CDef.MemberStatus>();
        memberStatusList.add(CDef.MemberStatus.正式会員);
        memberStatusList.add(CDef.MemberStatus.退会会員);
        cb.query().setMemberStatusCode_InScope_AsMemberStatus(memberStatusList);
        cb.query().queryMemberStatus().addOrderBy_DisplayOrder_Asc();

        // ## Act ##
        List<Member> memberList = memberBhv.selectList(cb);

        // ## Assert ##
        boolean existFML = false;
        boolean existWdl = false;
        assertHasAnyElement(memberList);
        for (Member member : memberList) {
            log("Name:" + member.getMemberName() + ". StatusCode:" + member.getMemberStatusCode());

            // 両方とも最低一個は存在していることをアサート
            if (member.isMemberStatusCode正式会員()) {
                existFML = true;
            } else if (member.isMemberStatusCode退会会員()) {
                existWdl = true;
            }

            // entity上だけで正式会員を退会会員に変更して、退会会員に変更されていることをアサート
            member.setMemberStatusCode_退会会員();
            assertTrue(member.isMemberStatusCode退会会員());
        }
        assertTrue(existFML);
        assertTrue(existWdl);
    }

    // 会員ステータスコードHANを追加し、以下のテストメソッドを実行し、動作を確認。その後、HANを削除したので以下は動作しなくなりました
    //    // 区分値の追加と変更でテストメソッドを１つ作成
    //    public void test_searchHanMember() {
    //        MemberCB cb = new MemberCB();
    //        cb.query().setMemberStatusCode_Equal_ハンズオン();
    //
    //        Member entity = memberBhv.selectEntity(cb);
    //        assertNull(entity);
    //    }

    // サービスが利用できる会員を検索（グルーピング判定）
    public void test_searchServiceAvailableMember() {
        // ## Arrange ##
        MemberCB cb = new MemberCB();
        cb.setupSelect_MemberStatus();
        cb.query().setMemberStatusCode_InScope_ServiceAvailable();
        cb.query().queryMemberStatus().addOrderBy_DisplayOrder_Asc();

        // ## Act ##
        List<Member> memberList = memberBhv.selectList(cb);

        // ## Assert ##
        assertHasAnyElement(memberList);
        for (Member member : memberList) {
            log("Name:" + member.getMemberName() + ". Status:" + member.getMemberStatus().getMemberStatusName());
            assertTrue(member.isMemberStatusCode_ServiceAvailable());
        }
    }

    // 姉妹コードの利用(未払い購入のある会員を検索)
    public void test_searchPaymentUncompletedMember() {
        // ## Arrange ##
        MemberCB cb = new MemberCB();
        cb.query().existsPurchaseList(new SubQuery<PurchaseCB>() {
            // Override不要
            public void query(PurchaseCB subCB) {
                subCB.query().setPaymentCompleteFlg_Equal_False();
            }
        });
        cb.query().addOrderBy_FormalizedDatetime_Desc().withNullsLast();
        cb.query().addOrderBy_MemberId_Asc();

        // ## Act ##
        // Loadrefererを利用(memberからpurchaseListを取得できる)
        List<Member> memberList = memberBhv.selectList(cb);
        memberBhv.loadPurchaseList(memberList, new ConditionBeanSetupper<PurchaseCB>() {
            public void setup(PurchaseCB cb) {
                //cb.query().setPaymentCompleteFlg_Equal_False();
                cb.query().setPaymentCompleteFlg_Equal_AsBoolean(false);
            }
        });

        // ## Assert ##
        assertHasAnyElement(memberList);
        for (Member member : memberList) {
            log("Name+:" + member.getMemberName() + ". PaymentCompleteFlg" + member.getPurchaseList());
            //assertNotNull(member.getPurchaseList()); //nullを返さない
            assertHasAnyElement(member.getPurchaseList());
        }
    }

    public void test_searchMemberUseDisplayOrder() {
        // ## Arrange ##
        MemberCB cb = new MemberCB();
        cb.query().queryMemberStatus().addOrderBy_DisplayOrder_Asc();
        cb.query().addOrderBy_MemberId_Desc();

        // ## Act ##
        List<Member> memberList = memberBhv.selectList(cb);

        // ## Assert ##
        assertHasAnyElement(memberList);
        Integer previousOrder = null;
        for (Member member : memberList) {
            // 会員ステータスのデータが取れていないことをアサート
            assertNull(member.getMemberStatus());

            // 会員が会員ステータスの表示順ごとに並んでいることをアサート
            log("Name:" + member.getMemberName() + ". Order:"
                    + member.getMemberStatusCodeAsMemberStatus().displayOrder());
            int currentOrder = Integer.parseInt(member.getMemberStatusCodeAsMemberStatus().displayOrder());
            if (previousOrder != null) {
                assertTrue(previousOrder <= currentOrder);
            }
            previousOrder = currentOrder;
        }
    }
}
