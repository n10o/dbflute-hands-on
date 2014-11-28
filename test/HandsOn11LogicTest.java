package org.dbflute.handson.logic;

import java.sql.Timestamp;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.annotation.Resource;

import org.dbflute.handson.dbflute.cbean.MemberCB;
import org.dbflute.handson.dbflute.cbean.MemberLoginCB;
import org.dbflute.handson.dbflute.cbean.MemberServiceCB;
import org.dbflute.handson.dbflute.cbean.PurchaseCB;
import org.dbflute.handson.dbflute.exbhv.MemberBhv;
import org.dbflute.handson.dbflute.exbhv.MemberLoginBhv;
import org.dbflute.handson.dbflute.exbhv.MemberServiceBhv;
import org.dbflute.handson.dbflute.exbhv.PurchaseBhv;
import org.dbflute.handson.dbflute.exentity.Member;
import org.dbflute.handson.dbflute.exentity.MemberLogin;
import org.dbflute.handson.dbflute.exentity.MemberService;
import org.dbflute.handson.dbflute.exentity.Purchase;
import org.dbflute.handson.dbflute.exentity.ServiceRank;
import org.dbflute.handson.unit.UnitContainerTestCase;
import org.seasar.dbflute.cbean.ListResultBean;
import org.seasar.dbflute.cbean.SubQuery;
import org.seasar.dbflute.cbean.coption.DerivedReferrerOption;

/**
 * @author nakamura
 */
public class HandsOn11LogicTest extends UnitContainerTestCase {

    @Resource
    protected MemberBhv memberBhv;

    @Resource
    protected MemberServiceBhv memberServiceBhv;

    @Resource
    protected MemberLoginBhv memberLoginBhv;

    @Resource
    protected PurchaseBhv purchaseBhv;

    final private String VI = "vi";

    // 会員名称が "vi" を含んでいる会員を検索
    // 支払済み購入が取得できていることをアサート
    public void test_selectMemberWithPurchase_会員と購入が検索されていること() {
        // ## Arrange ##
        HandsOn11Logic logic = new HandsOn11Logic();
        inject(logic);

        // ## Act ##
        List<Member> memberList = logic.selectMemberWithPurchase(VI);

        // ## Assert ##
        assertHasAnyElement(memberList);
        for (Member member : memberList) {
            log("Name:" + member.getMemberName() + ". purchase:" + member.getPurchaseList());
            List<Purchase> purchaseList = member.getPurchaseList();
            for (Purchase purchase : purchaseList) {
                assertTrue(purchase.isPaymentCompleteFlgTrue());
            }
        }
    }

    // 会員名称が "vi" を含んでいる会員を検索
    // 検索された会員が未払い購入を持っていることをアサート
    public void test_selectUnpaidMember_未払い購入がある会員が検索されていること() {
        // ## Arrange ##
        HandsOn11Logic logic = new HandsOn11Logic();
        inject(logic);

        // ## Act ##
        List<Member> memberList = logic.selectUnpaidMember(VI);

        // ## Assert ##
        assertHasAnyElement(memberList);
        for (Member member : memberList) {
            log("Name:" + member.getMemberName() + ". purchase:" + member.getPurchaseList());
            List<Purchase> purchaseList = member.getPurchaseList();
            for (Purchase purchase : purchaseList) {
                assertTrue(member.getMemberName().contains(VI));
                assertTrue(purchase.isPaymentCompleteFlgFalse());
            }
        }
    }

    // 会員名称が "vi" を含んでいる会員を検索
    // 会員の最終ログイン日時が本当に最終ログイン日時であることをアサート
    public void test_selectLoginedMember_会員と最終ログイン日時が検索されていること() {
        // ## Arrange ##
        HandsOn11Logic logic = new HandsOn11Logic();
        inject(logic);

        // ## Act ##
        List<Member> memberList = logic.selectLoginedMember(VI);

        // ## Assert ##
        assertHasAnyElement(memberList);
        for (Member member : memberList) {
            Timestamp newestLoginDate = member.getNewestLoginDatetime();
            Integer id = member.getMemberId();
            log("ID:" + id + ". Name:" + member.getMemberName() + ". Login:" + newestLoginDate);

            // 最終ログイン時間であることをアサート
            if (newestLoginDate != null) {
                log("Expected:" + newestLoginDate + ". Actual:" + findLatestLoginDatetimeByID(id));
                assertEquals(findLatestLoginDatetimeByID(id), newestLoginDate);
            }
        }
    }

    private Timestamp findLatestLoginDatetimeByID(final int id) {
        MemberLoginCB cb = new MemberLoginCB();
        cb.query().scalar_Equal().max(new SubQuery<MemberLoginCB>() {
            public void query(MemberLoginCB cb) {
                cb.specify().columnLoginDatetime();
                cb.query().setMemberId_Equal(id);
            }
        });

        MemberLogin login = memberLoginBhv.selectEntity(cb);
        if (login == null) {
            return null;
        }
        return login.getLoginDatetime();
    }

    /**
     * オンパレードの第一歩
     * 未払い購入の存在しない会員だけを検索
     * 未払い購入が存在しないことをアサート
     * 会員ステータス経由の会員ログインが取得できていることをアサート
     * ロジックの中で出力したログを見て期待通りであることを確認
     */
    public void test_selectOnParadeFirstStepMember_未払い購入の存在しない会員() {
        // ## Arrange ##
        HandsOn11Logic logic = new HandsOn11Logic();
        inject(logic);

        // ## Act ##
        List<Member> paidMemberList = logic.selectOnParadeFirstStepMember(true);

        // ## Assert ##
        assertHasAnyElement(paidMemberList);
        for (Member member : paidMemberList) {
            log("Name:" + member.getMemberName() + ". Login:" + member.getMemberStatus().getMemberLoginList());
            // 会員ステータス経由の会員ログインが取得できていることをアサート
            assertHasAnyElement(member.getMemberStatus().getMemberLoginList());
            for (Purchase purchase : member.getPurchaseList()) {
                log("Pur:" + purchase.getPurchaseId() + ". isPaid:" + purchase.getPaymentCompleteFlgName());
                // 未払い購入が存在しないことをアサート
                assertTrue(purchase.isPaymentCompleteFlgTrue());
            }
        }
    }

    // オンパレードはつづく
    // 商品も取得できることをアサート
    // 購入商品種類数が妥当であることをアサート
    // 生産中止の商品を買ったことのある会員が検索されていることをアサート
    public void test_selectOnParadeSecondStepMember_購入のみならず商品も検索() {
        // ## Arrange ##
        HandsOn11Logic logic = new HandsOn11Logic();
        inject(logic);

        // ## Act ##
        List<Member> memberList = logic.selectOnParadeSecondStepMember();

        // ## Assert ##
        assertHasAnyElement(memberList);
        for (Member member : memberList) {
            boolean purchasedRareProduct = false;
            int differNum = member.getCountDifferProductNum();
            log("Name:" + member.getMemberName() + ". DifferProduct:" + differNum);
            for (Purchase purchase : member.getPurchaseList()) {
                log("Product:" + purchase.getProduct().getProductName());
                // 生産中止の商品を買ったことのある会員が検索されていることをアサート
                if (purchase.getProduct().isProductStatusCode生産中止()) {
                    purchasedRareProduct = true;
                }
                // 購入商品種類数が妥当であることをアサート
                assertEquals(countDifferProductNumByID(member.getMemberId()), differNum);
            }
            // このアサートをパスするには、"生産中止の商品を買ったことがある"ことが必要なので、
            // "商品も取得できることをアサート"も同時に証明できる。
            assertTrue(purchasedRareProduct);
        }
    }

    protected int countDifferProductNumByID(int id) {
        PurchaseCB cb = new PurchaseCB();
        cb.query().setMemberId_Equal(id);

        ListResultBean<Purchase> purchaseList = purchaseBhv.selectList(cb);

        Set<Integer> productSet = new HashSet<Integer>();
        for (Purchase purchase : purchaseList) {
            productSet.add(purchase.getProductId());
        }
        return productSet.size();
    }

    // すごいオンパレード
    // ログイン回数が 2 回より多い会員を検索し、結果がその通りであることをアサート
    // 最終ログイン日時の降順と会員IDの昇順で並んでいることをアサート
    // 支払済み購入の最大購入価格が妥当であることをアサート
    // 仮会員のときにログインをしたことのある会員であることをアサート
    // 自分だけが購入している商品を買ったことのある会員であることをアサート
    public void test_selectOnParadeXStepMember_オンパレードであること() {
        // ## Arrange ##
        HandsOn11Logic logic = new HandsOn11Logic();
        inject(logic);

        // ## Act ##
        int targetLoginCount = 3;
        List<Member> memberList = logic.selectOnParadeXStepMember(targetLoginCount);

        // ## Assert ##
        assertHasAnyElement(memberList);

        Timestamp previousDate = null;
        Integer previousId = null;
        for (Member member : memberList) {
            log("ID:" + member.getMemberId() + ". Name:" + member.getMemberName() + ". LoginCount:"
                    + member.getCountLogin() + ". NewestLogin:" + member.getNewestLoginDateFML() + ". MaxPrice:"
                    + member.getMaxPaidPurchasePrice());
            log("PurList:" + member.getPurchaseList() + ". LoginList:" + member.getMemberLoginList());

            // ログイン回数が 2 回より多い会員を検索し、結果がその通りであることをアサート
            assertEquals(countLoginById(member.getMemberId()), member.getCountLogin());
            assertTrue(member.getCountLogin() >= targetLoginCount);

            // 最終ログイン日時の降順と会員IDの昇順で並んでいることをアサート -> 最終ログイン日時は正式会員のときにログインした最終ログイン日時という解釈でいいですか？
            if (previousDate != null) {
                if (member.getNewestLoginDatetime().equals(previousDate)) {
                    assertTrue(previousId < member.getMemberId());
                } else {
                    assertTrue(member.getNewestLoginDatetime().before(previousDate));
                }
            }

            previousDate = member.getNewestLoginDatetime();
            previousId = member.getMemberId();

            // 支払済み購入の最大購入価格が妥当であることをアサート 何も購入していない場合はnullが入る。
            Integer maxPaidPurchasePriceById = findMaxPaidPurchasePriceById(member.getMemberId());
            if (maxPaidPurchasePriceById != null) {
                assertEquals(maxPaidPurchasePriceById.intValue(), member.getMaxPaidPurchasePrice());
            }

            // 仮会員のときにログインをしたことのある会員であることをアサート
            log("PRV Logined ID:" + hadLoginedWhenPRVById(member.getMemberId()));
            assertTrue(hadLoginedWhenPRVById(member.getMemberId()));

            // 自分だけが購入している商品を買ったことのある会員であることをアサート
            PurchaseCB cb = new PurchaseCB();
            cb.query().setMemberId_NotEqual(member.getMemberId());
            ListResultBean<Purchase> purchaseList = purchaseBhv.selectList(cb);
            Set<Integer> notMyParchaseAllSet = new HashSet<Integer>();
            for (Purchase purchase : purchaseList) {
                notMyParchaseAllSet.add(purchase.getProductId());
            }
            assertHasAnyElement(notMyParchaseAllSet);

            List<Purchase> myPurchaseList = member.getPurchaseList();
            assertHasAnyElement(myPurchaseList);
            boolean existsOnlyOne = false;
            for (Purchase purchase : myPurchaseList) {
                if (notMyParchaseAllSet.contains(purchase.getProductId()) == false) {
                    existsOnlyOne = true;
                    break;
                }
            }
            assertTrue(existsOnlyOne);
        }
    }

    private int countLoginById(int id) {
        MemberLoginCB cb = new MemberLoginCB();
        cb.query().setMemberId_Equal(id);
        return memberLoginBhv.selectCount(cb);
    }

    // return value -> null allow
    private Integer findMaxPaidPurchasePriceById(int id) {
        PurchaseCB cb = new PurchaseCB();
        cb.query().setMemberId_Equal(id);
        cb.query().setPaymentCompleteFlg_Equal_True();
        cb.query().scalar_Equal().max(new SubQuery<PurchaseCB>() {
            public void query(PurchaseCB cb) {
                cb.specify().columnPurchasePrice();
            }
        });
        cb.fetchFirst(1); // 最大価格が同じものが複数あった場合にもselectEntityを使うため
        Purchase purchase = purchaseBhv.selectEntity(cb);
        if (purchase == null) {
            return null;
        }
        return purchase.getPurchasePrice();
    }

    // 仮会員のときにログインをしたことのある会員かどうか
    private boolean hadLoginedWhenPRVById(int id) {
        MemberLoginCB cb = new MemberLoginCB();
        cb.query().setMemberId_Equal(id);
        cb.query().setLoginMemberStatusCode_Equal_仮会員();
        return memberLoginBhv.selectCount(cb) > 0;
    }

    // シンプルな応用編
    // 会員数が妥当であることをアサート
    // 検索した内容をログに綺麗に出して目視で確認すること
    public void test_selectServiceRankSummary_集計されていること() {
        // ## Arrange ##
        HandsOn11Logic logic = new HandsOn11Logic();
        inject(logic);

        // ## Act ##
        List<ServiceRank> serviceRankList = logic.selectServiceRankSummary();

        for (ServiceRank rank : serviceRankList) {
            for (MemberService service : rank.getMemberServiceList()) {
                log(service.getMember().getPurchaseList());
                log(service.getMember().getMemberLoginList());
            }
            log("SRN:" + rank.getServiceRankName() + ". :" + rank.getMemberServiceList());
            log("MSL:" + rank.getMemberServiceList());
            log("ServiceRank:" + rank.getServiceRankName() + ". MemberNum:" + rank.getCountMember() + ". SumPrice:"
                    + rank.getSumPurchasePrice() + ". AveMaxPrice:" + rank.getAveMaxPurchasePrice() + ". LoginCount:"
                    + rank.getCountLogin());

            // 会員数が妥当であることをアサート
            assertEquals(countMemberPerRank(rank.getServiceRankCodeAsServiceRank()), rank.getCountMember());
        }
    }

    private int countMemberPerRank(org.dbflute.handson.dbflute.allcommon.CDef.ServiceRank serviceRank) {
        MemberServiceCB cb = new MemberServiceCB();
        cb.query().setServiceRankCode_Equal_AsServiceRank(serviceRank);
        return memberServiceBhv.readCount(cb);
    }

    // さらにシンプルな応用編
    // 平均の最大価格に該当する会員が存在することをアサート
    public void test_selectMaxAvgPurchasePrice_平均の最大の会員() {
        // ## Arrange ##
        HandsOn11Logic logic = new HandsOn11Logic();
        inject(logic);

        // ## Act ##
        Integer maxAvgPrice = logic.selectMaxAvgPurchasePrice();

        // ## Assert ##
        log("Max price:" + maxAvgPrice);

        // 平均の最大価格に該当する会員が存在することをアサート
        MemberCB cb = new MemberCB();
        cb.specify().derivedPurchaseList().avg(new SubQuery<PurchaseCB>() {
            public void query(PurchaseCB subCB) {
                subCB.specify().columnPurchasePrice();
            }
        }, Member.ALIAS_maxPurchasePrice, new DerivedReferrerOption().coalesce(0));

        ListResultBean<Member> memberList = memberBhv.selectList(cb);
        boolean existsTarget = false;
        for (Member member : memberList) {
            log("Name:" + member.getMemberName() + ". Price:" + member.getMaxPurchasePrice());
            if (member.getMaxPurchasePrice().equals(maxAvgPrice)) {
                existsTarget = true;
            }
        }
        assertTrue(existsTarget);
    }
}
