package org.dbflute.handson.logic;

import java.util.List;

import javax.annotation.Resource;

import org.dbflute.handson.dbflute.cbean.MemberCB;
import org.dbflute.handson.dbflute.cbean.PurchaseCB;
import org.dbflute.handson.dbflute.exbhv.MemberBhv;
import org.dbflute.handson.dbflute.exbhv.PurchaseBhv;
import org.dbflute.handson.dbflute.exentity.Member;
import org.dbflute.handson.dbflute.exentity.Purchase;
import org.dbflute.handson.unit.UnitContainerTestCase;
import org.seasar.dbflute.exception.EntityAlreadyUpdatedException;
import org.seasar.dbflute.unit.core.thread.ThreadFireExecution;
import org.seasar.dbflute.unit.core.thread.ThreadFireOption;
import org.seasar.dbflute.unit.core.thread.ThreadFireResource;

/**
 * @author nakamura
 */
public class HandsOn08LogicTest extends UnitContainerTestCase {

    @Resource
    protected MemberBhv memberBhv;

    @Resource
    protected PurchaseBhv purchaseBhv;

    //    private static final int TARGETID = 2; // PRV User(それとも、事前に正式会員以外のユーザを検索でとってきたほうがいいですか？) -> Yes

    // 排他制御がかかるのはデフォルトではversionNoがあるテーブルに自動的にかかるという理解でよろしいですか？ -> Yes
    // また、versionNoが存在するテーブルのupdateにはversionNoの明示的なセットが必要（しないとエラー）という理解でいいですか？ -> Yes

    public void test_updateMemberChangedToFormalized_会員が更新されていること() {
        // ## Arrange ##
        HandsOn08Logic logic = new HandsOn08Logic();
        inject(logic);
        int targetId = findNotFMVUserId();

        // ## Assert ##
        // 作戦：念のためテスト対象会員が正式会員でないことをアサートしてから、ロジック実行後に正式会員になったことをアサート
        MemberCB cbBeforeUpdate = new MemberCB();
        cbBeforeUpdate.query().setMemberId_Equal(targetId);

        // ターゲットの会員が正式会員でないことをアサート
        Member entity = memberBhv.selectEntityWithDeletedCheck(cbBeforeUpdate);
        assertFalse(entity.isMemberStatusCode正式会員());

        // ## Act ##
        // 排他制御有りのUpdateをするときは、必ず事前にentityを検索しておくということでしょうか
        logic.updateMemberChangedToFormalized(targetId, entity.getVersionNo());

        MemberCB cbAfterUpdate = new MemberCB();
        cbAfterUpdate.query().setMemberStatusCode_Equal_正式会員();
        cbAfterUpdate.query().setMemberId_Equal(targetId);

        entity = memberBhv.selectEntityWithDeletedCheck(cbAfterUpdate);
        // ターゲットの会員が正式会員になったことをアサート
        assertTrue(entity.isMemberStatusCode正式会員());
        log("VNo:" + entity.getVersionNo());
    }

    public void test_updateMemberChangedToFormalized_排他制御例外が発生すること() {
        // ## Arrange ##
        HandsOn08Logic logic = new HandsOn08Logic();
        inject(logic);
        int targetId = findNotFMVUserId();

        // ## Act ##
        try {
            // デフォルトではversionNoが0であるため、既に更新済とみなされ、排他制御に引っかかる
            logic.updateMemberChangedToFormalized(targetId, 100L);
            // ## Assert ##
            fail();
        } catch (EntityAlreadyUpdatedException e) {
            log(e);
            // OK牧場
        }
    }

    // 排他制御無しロジックを利用
    public void test_updateMemberChangedToFormalizedNonstrict_会員が更新されていること() {
        // ## Arrange ##
        HandsOn08Logic logic = new HandsOn08Logic();
        inject(logic);
        int targetId = findNotFMVUserId();

        // ## Act ##
        logic.updateMemberChangedToFormalizedNonstrict(targetId);

        // ## Assert ##
        MemberCB cb = new MemberCB();
        cb.query().setMemberId_Equal(targetId);
        cb.query().setMemberStatusCode_Equal_正式会員();
        //assertNotNull(memberBhv.selectEntityWithDeletedCheck(cb));
        memberBhv.selectEntityWithDeletedCheck(cb); // expect existing
    }

    public void test_updateMemberChangedToFormalizedNonstrict_排他制御例外が発生しないこと() {
        // ## Arrange ##
        HandsOn08Logic logic = new HandsOn08Logic();
        inject(logic);
        int targetId = findNotFMVUserId();

        // ## Act ##
        // 通常なら排他制御例外が起きるはずの状況でも排他制御例外が発生しないことをアサート
        // 作戦：検索時よりVersionNoを上げた状態でUpdateすればいいので、事前にVersionNoを上げておく

        //　事前検索
        MemberCB cbBefore = new MemberCB();
        cbBefore.query().setMemberId_Equal(targetId);
        Member entity = memberBhv.selectEntityWithDeletedCheck(cbBefore);
        log("VNo:" + entity.getVersionNo());

        // VersionNoをUpdate
        Member member = new Member();
        member.setMemberId(targetId);
        member.setVersionNo(entity.getVersionNo());
        memberBhv.update(member);

        // ## Assert ##
        // 例外が発生しないことをアサート
        logic.updateMemberChangedToFormalizedNonstrict(targetId); // expect no exception

        MemberCB cb = new MemberCB();
        cb.query().setMemberId_Equal(targetId);
        cb.query().setMemberStatusCode_Equal_正式会員();

        entity = memberBhv.selectEntityWithDeletedCheck(cb);
        log("After VNo:" + entity.getVersionNo());
    }

    public void test_deletePurchaseSimply_購入が削除されていること() {
        // ## Arrange ##
        HandsOn08Logic logic = new HandsOn08Logic();
        inject(logic);

        // ## Assert ##
        // 削除前に要素が存在することをアサート
        PurchaseCB cb = new PurchaseCB();
        int targetId = 6;
        cb.query().setMemberId_Equal(targetId);
        List<Purchase> purchaseList = purchaseBhv.selectList(cb);
        assertTrue(purchaseList.size() > 0);

        // 削除後に要素が存在しないことをアサート
        logic.deletePurchaseSimply(targetId);
        PurchaseCB deletedCb = new PurchaseCB();
        deletedCb.query().setMemberId_Equal(targetId);
        List<Purchase> deletedPurchaseList = purchaseBhv.selectList(deletedCb);
        assertTrue(deletedPurchaseList.size() == 0);
    }

    public void test_IfYouLike_DeadLock() {
        threadFire(new ThreadFireExecution<Void>() {
            public Void execute(ThreadFireResource resource) {
                HandsOn08Logic logic = new HandsOn08Logic();
                inject(logic);

                long threadId = resource.getThreadId();
                if (threadId % 2 == 0) { // thread A
                    logic.updateMemberChangedToFormalizedNonstrict(1);
                    //updateFoo();
                    log("...Waiting A");
                    sleep(2000);
                    //updateBar();
                    logic.updateMemberChangedToFormalizedNonstrict(2);
                } else { // thread B
                    //updateBar();
                    logic.updateMemberChangedToFormalizedNonstrict(2);
                    log("...Waiting B");
                    sleep(2000);
                    //updateFoo();
                    logic.updateMemberChangedToFormalizedNonstrict(1);
                }
                return null;
            }
        }, new ThreadFireOption().threadCount(2).expectExceptionAny("Deadlock"));
    }

    private int findNotFMVUserId() {
        MemberCB cb = new MemberCB();
        cb.query().setMemberStatusCode_NotEqual_正式会員();
        cb.fetchFirst(1);

        return memberBhv.selectEntityWithDeletedCheck(cb).getMemberId();
    }
}
