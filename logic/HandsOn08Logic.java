package org.dbflute.handson.logic;

import javax.annotation.Resource;

import org.dbflute.handson.dbflute.cbean.PurchaseCB;
import org.dbflute.handson.dbflute.exbhv.MemberBhv;
import org.dbflute.handson.dbflute.exbhv.PurchaseBhv;
import org.dbflute.handson.dbflute.exentity.Member;

/**
 * @author nakamura
 */
public class HandsOn08Logic {

    @Resource
    protected MemberBhv memberBhv;

    @Resource
    protected PurchaseBhv purchaseBhv;

    // 排他制御有り版：指定された会員を正式会員に更新する 引数nullはException
    public void updateMemberChangedToFormalized(Integer memberId, Long versionNo) {
        if (memberId == null) {
            throw new IllegalArgumentException("MemberId is null");
        } else if (versionNo == null) {
            throw new IllegalArgumentException("VersionNo is null");
        }

        Member member = new Member();
        member.setMemberId(memberId);
        member.setMemberStatusCode_正式会員();
        member.setVersionNo(versionNo);
        memberBhv.update(member);
    }

    // 排他制御無し版：指定された会員を正式会員に更新する 引数nullはException
    public void updateMemberChangedToFormalizedNonstrict(Integer memberId) {
        if (memberId == null) {
            throw new IllegalArgumentException("Argument is null");
        }

        Member member = new Member();
        member.setMemberId(memberId);
        member.setMemberStatusCode_正式会員();
        memberBhv.updateNonstrict(member);
    }

    // 指定された会員の購入を削除する 引数nullは何もしない
    public void deletePurchaseSimply(Integer memberId) {
        if (memberId == null) {
            return;
        }

        //cb.checkInvalidQuery();も使える

        PurchaseCB cb = new PurchaseCB();
        cb.query().setMemberId_Equal(memberId);

        purchaseBhv.queryDelete(cb);
        // List<Purchase> purchaseList = purchaseBhv.selectList(cb);
        //        for (Purchase purchase : purchaseList) {
        //            purchaseBhv.deleteNonstrict(purchase);
        //        }
    }
}
