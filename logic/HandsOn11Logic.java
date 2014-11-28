package org.dbflute.handson.logic;

import java.util.List;

import javax.annotation.Resource;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.dbflute.handson.dbflute.cbean.MemberCB;
import org.dbflute.handson.dbflute.cbean.MemberLoginCB;
import org.dbflute.handson.dbflute.cbean.MemberServiceCB;
import org.dbflute.handson.dbflute.cbean.PurchaseCB;
import org.dbflute.handson.dbflute.cbean.ServiceRankCB;
import org.dbflute.handson.dbflute.exbhv.MemberBhv;
import org.dbflute.handson.dbflute.exbhv.MemberServiceBhv;
import org.dbflute.handson.dbflute.exbhv.MemberStatusBhv;
import org.dbflute.handson.dbflute.exbhv.PurchaseBhv;
import org.dbflute.handson.dbflute.exbhv.ServiceRankBhv;
import org.dbflute.handson.dbflute.exentity.Member;
import org.dbflute.handson.dbflute.exentity.MemberService;
import org.dbflute.handson.dbflute.exentity.MemberStatus;
import org.dbflute.handson.dbflute.exentity.ServiceRank;
import org.seasar.dbflute.bhv.ConditionBeanSetupper;
import org.seasar.dbflute.bhv.EntityListSetupper;
import org.seasar.dbflute.bhv.LoadReferrerOption;
import org.seasar.dbflute.cbean.ListResultBean;
import org.seasar.dbflute.cbean.ScalarQuery;
import org.seasar.dbflute.cbean.SubQuery;
import org.seasar.dbflute.cbean.coption.DerivedReferrerOption;
import org.seasar.dbflute.cbean.coption.LikeSearchOption;

/**
 * @author nakamura
 */
public class HandsOn11Logic {

    // ===================================================================================
    //                                                                          Definition
    //                                                                          ==========
    private static final Log LOG = LogFactory.getLog(HandsOn11Logic.class);

    // ===================================================================================
    //                                                                           Attribute
    //                                                                           =========
    @Resource
    protected MemberBhv memberBhv;

    @Resource
    protected MemberStatusBhv memberStatusBhv;

    @Resource
    protected ServiceRankBhv serviceRankBhv;

    @Resource
    protected MemberServiceBhv memberServiceBhv;

    @Resource
    protected PurchaseBhv purchaseBhv;

    // ===================================================================================
    //                                                                      Basic Exercise
    //                                                                            ========
    /**
     * 子テーブルの取得
     * @param memberName 部分一致検索で使う会員名称のキーワード (NotNUll)
     * @return 購入情報付きの会員リスト (NotNull)
     */
    public List<Member> selectMemberWithPurchase(String memberName) {
        MemberCB cb = new MemberCB();
        cb.checkInvalidQuery();
        cb.query().setMemberName_LikeSearch(memberName, new LikeSearchOption().likeContain());
        ListResultBean<Member> memberList = memberBhv.selectList(cb);

        memberBhv.loadPurchaseList(memberList, new ConditionBeanSetupper<PurchaseCB>() {
            public void setup(PurchaseCB cb) {
                cb.query().setPaymentCompleteFlg_Equal_True();
            }
        });

        return memberList;
    }

    /**
     * 未払い購入のある会員を検索
     * @param memberName 部分一致検索で使う会員名称のキーワード (NotNUll)
     * @return 会員リスト (NotNull)
     */
    public List<Member> selectUnpaidMember(String memberName) {
        MemberCB cb = new MemberCB();
        cb.checkInvalidQuery();
        cb.query().setMemberName_LikeSearch(memberName, new LikeSearchOption().likeContain());
        cb.query().existsPurchaseList(new SubQuery<PurchaseCB>() {
            public void query(PurchaseCB cb) {
                cb.query().setPaymentCompleteFlg_Equal_False();
            }
        });

        return memberBhv.selectList(cb);
    }

    /**
     * 会員と最終ログイン日時を一緒に検索
     * @param memberName 部分一致検索で使う会員名称のキーワード (NotNUll)
     * @return 最終ログイン日時付きの会員リスト (NotNull)
     */
    public List<Member> selectLoginedMember(String memberName) {
        MemberCB cb = new MemberCB();
        cb.checkInvalidQuery();
        cb.query().setMemberName_LikeSearch(memberName, new LikeSearchOption().likeContain());
        cb.specify().derivedMemberLoginList().max(new SubQuery<MemberLoginCB>() {
            public void query(MemberLoginCB subCB) {
                subCB.specify().columnLoginDatetime();
            }
        }, Member.ALIAS_newestLoginDate);

        return memberBhv.selectList(cb);
    }

    // ===================================================================================
    //                                                                   OnParade Exercise
    //                                                                            ========
    /**
     * オンパレードの第一歩
     * 会員ステータス、会員サービス、サービスランク、購入、会員ステータス経由の会員ログイン、モバイルからのログイン情報付きの会員を検索
     * 購入は商品の定価の高い順、購入価格の高い順で並べる
     * @param completeOnly 未払い購入の存在しない会員だけを対象にするかどうか(NotNull)
     * @return 会員ステータス、会員サービス、サービスランク、購入、会員ステータス経由の会員ログイン、モバイルからのログイン情報付きの会員リスト(NotNull)
     */
    public List<Member> selectOnParadeFirstStepMember(boolean completeOnly) {
        MemberCB cb = new MemberCB();
        cb.checkInvalidQuery();
        cb.setupSelect_MemberStatus();
        cb.setupSelect_MemberServiceAsOne().withServiceRank();

        // モバイルからのログイン回数も導出して取得
        cb.specify().derivedMemberLoginList().count(new SubQuery<MemberLoginCB>() {
            public void query(MemberLoginCB cb) {
                // countを利用する場合は主キーを指定
                cb.specify().columnMemberLoginId();
                cb.query().setMobileLoginFlg_Equal_True();
            }
        }, Member.ALIAS_countMobileLogin);

        if (completeOnly) {
            cb.query().notExistsPurchaseList(new SubQuery<PurchaseCB>() {
                public void query(PurchaseCB subCB) {
                    subCB.query().setPaymentCompleteFlg_Equal_False();
                }
            });
        }

        ListResultBean<Member> memberList = memberBhv.selectList(cb);
        memberBhv.loadPurchaseList(memberList, new ConditionBeanSetupper<PurchaseCB>() {
            public void setup(PurchaseCB cb) {
                cb.query().queryProduct().addOrderBy_RegularPrice_Desc();
                cb.query().addOrderBy_PurchasePrice_Desc();
            }
        });

        if (LOG.isDebugEnabled()) {
            for (Member member : memberList) {
                // 会員ごとのログイン回数(モバイル)と購入一覧をログに出力する
                LOG.debug("Name:" + member.getMemberName() + ". LoginCount:" + member.getCountMobileLogin() + ". PL;"
                        + member.getPurchaseList());
            }
        }

        List<MemberStatus> statusList = memberBhv.pulloutMemberStatus(memberList);
        memberStatusBhv.loadMemberLoginList(statusList, new ConditionBeanSetupper<MemberLoginCB>() {
            public void setup(MemberLoginCB cb) {
            }
        });

        return memberList;
    }

    /**
     * オンパレードはつづく
     * 生産中止の商品を買ったことのある会員を検索
     * 購入は商品ステータスの表示順の昇順、購入日時の降順で並べる
     * @return　会員ステータス、購入と商品と購入商品種類数付きの会員リスト (NotNull)
     */
    public List<Member> selectOnParadeSecondStepMember() {
        MemberCB cb = new MemberCB();
        cb.checkInvalidQuery();
        cb.setupSelect_MemberStatus();

        cb.specify().derivedPurchaseList().countDistinct(new SubQuery<PurchaseCB>() {
            public void query(PurchaseCB subCB) {
                subCB.specify().columnProductId();
            }
        }, Member.ALIAS_countDifferProductNum);
        cb.query().existsPurchaseList(new SubQuery<PurchaseCB>() {
            public void query(PurchaseCB subCB) {
                subCB.query().queryProduct().queryProductStatus().setProductStatusCode_Equal_生産中止();
            }
        });

        ListResultBean<Member> memberList = memberBhv.selectList(cb);
        memberBhv.loadPurchaseList(memberList, new ConditionBeanSetupper<PurchaseCB>() {
            public void setup(PurchaseCB cb) {
                cb.setupSelect_Product();
                cb.query().queryProduct().queryProductStatus().addOrderBy_DisplayOrder_Asc();
                cb.query().addOrderBy_PurchaseDatetime_Desc();
            }
        });

        return memberList;
    }

    /**
     * すごいオンパレード
     * 仮会員のときのログインをしたことがあり、自分だけが購入している商品を買ったことのある会員を検索
     * 会員ログイン情報はログイン日時の降順
     * 最終ログイン日時の降順、会員IDの昇順で並べる
     * 購入は商品カテゴリの親カテゴリ名称の昇順、子カテゴリ名称の昇順、購入日時の降順
     * 会員ログイン情報はログイン日時の降順
     * @param leastLoginCount 最低ログイン回数。指定された回数より小さい会員は検索されない (NotNull)
     * @return 正式会員のときにログインした最終ログイン日時、ログイン回数、支払済み購入の最大購入価格、購入、商品、商品ステータス、商品カテゴリ、親商品カテゴリ、会員ログイン情報付きの会員リスト (NotNull)
     */
    public List<Member> selectOnParadeXStepMember(int leastLoginCount) {
        MemberCB cb = new MemberCB();
        cb.checkInvalidQuery();

        cb.specify().derivedMemberLoginList().max(new SubQuery<MemberLoginCB>() {
            public void query(MemberLoginCB cb) {
                cb.specify().columnLoginDatetime();
                cb.query().setLoginMemberStatusCode_Equal_正式会員();
            }
        }, Member.ALIAS_newestLoginDateFML);

        cb.specify().derivedMemberLoginList().count(new SubQuery<MemberLoginCB>() {
            public void query(MemberLoginCB cb) {
                cb.specify().columnMemberLoginId();
            }
        }, Member.ALIAS_countLogin);

        // ログイン回数が指定された回数以上で絞り込み
        cb.query().derivedMemberLoginList().count(new SubQuery<MemberLoginCB>() {
            public void query(MemberLoginCB cb) {
                cb.specify().columnMemberLoginId();
            }
        }).greaterEqual(leastLoginCount);

        cb.specify().derivedPurchaseList().max(new SubQuery<PurchaseCB>() {
            public void query(PurchaseCB cb) {
                cb.specify().columnPurchasePrice();
                cb.query().setPaymentCompleteFlg_Equal_True();
            }
        }, Member.ALIAS_maxPaidPurchasePrice);

        cb.query().existsMemberLoginList(new SubQuery<MemberLoginCB>() {
            public void query(MemberLoginCB cb) {
                cb.query().setLoginMemberStatusCode_Equal_仮会員();
            }
        });

        // 自分だけが購入している商品を買ったことのある会員を検索とは = 他に同じ商品を購入した会員がいない
        // 作戦：購入テーブルのある商品IDがある会員の中でしか出てこないものを検索
        cb.query().existsPurchaseList(new SubQuery<PurchaseCB>() {
            public void query(PurchaseCB subCB) {
                subCB.query().queryProduct().derivedPurchaseList().countDistinct(new SubQuery<PurchaseCB>() {
                    public void query(PurchaseCB subCB) {
                        subCB.specify().columnMemberId();
                    }
                }).equal(1);
            }
        });

        cb.query().queryMemberLoginAsLatest().addOrderBy_LoginDatetime_Desc();
        cb.query().addOrderBy_MemberId_Asc();

        ListResultBean<Member> memberList = memberBhv.selectList(cb);
        memberBhv.loadPurchaseList(memberList, new ConditionBeanSetupper<PurchaseCB>() {
            public void setup(PurchaseCB cb) {
                cb.setupSelect_Product().withProductCategory().withProductCategorySelf();
                cb.setupSelect_Product().withProductStatus();
                cb.query().queryProduct().queryProductCategory().queryProductCategorySelf()
                        .addOrderBy_ProductCategoryName_Asc();
                cb.query().queryProduct().queryProductCategory().addOrderBy_ProductCategoryName_Asc();
                cb.query().addOrderBy_PurchaseDatetime_Desc();
            }
        });

        memberBhv.loadMemberLoginList(memberList, new ConditionBeanSetupper<MemberLoginCB>() {
            public void setup(MemberLoginCB cb) {
                cb.query().addOrderBy_LoginDatetime_Desc();
            }
        });

        return memberList;
    }

    // ===================================================================================
    //                                                                     Simple Exercise
    //                                                                            ========
    /**
     * シンプルな応用編
     * サービスランクごとの会員数、合計購入価格、平均最大購入価格を検索
     * 会員数の多い順に並べる
     * @return 紐付く会員とその会員に紐付く購入と会員ログイン情報付きのサービスランクごとの会員数、合計購入価格、平均最大購入価格、ログイン数ランクリスト (NotNull)
     */
    public List<ServiceRank> selectServiceRankSummary() {
        ServiceRankCB cb = new ServiceRankCB();
        cb.checkInvalidQuery();

        // サービスランクごとの会員数
        cb.specify().derivedMemberServiceList().count(new SubQuery<MemberServiceCB>() {
            public void query(MemberServiceCB cb) {
                cb.specify().columnMemberServiceId();
            }
        }, ServiceRank.ALIAS_countMember);

        // サービスランクごとの合計購入価格
        cb.specify().derivedMemberServiceList().sum(new SubQuery<MemberServiceCB>() {
            public void query(MemberServiceCB cb) {
                cb.specify().specifyMember().derivedPurchaseList().sum(new SubQuery<PurchaseCB>() {
                    public void query(PurchaseCB cb) {
                        cb.specify().columnPurchasePrice();
                    }
                }, null);
            }
        }, ServiceRank.ALIAS_sumPurchasePrice, new DerivedReferrerOption().coalesce(0));

        // サービスランクごとの平均最大購入価格
        cb.specify().derivedMemberServiceList().avg(new SubQuery<MemberServiceCB>() {
            public void query(MemberServiceCB cb) {
                cb.specify().specifyMember().derivedPurchaseList().max(new SubQuery<PurchaseCB>() {
                    public void query(PurchaseCB cb) {
                        cb.specify().columnPurchasePrice();
                    }
                }, null);
            }
        }, ServiceRank.ALIAS_aveMaxPurchasePrice, new DerivedReferrerOption().coalesce(0));

        // サービスランクごとのログイン数を検索
        cb.specify().derivedMemberServiceList().sum(new SubQuery<MemberServiceCB>() {
            public void query(MemberServiceCB cb) {
                cb.specify().specifyMember().derivedMemberLoginList().count(new SubQuery<MemberLoginCB>() {
                    public void query(MemberLoginCB cb) {
                        cb.specify().columnMemberLoginId();
                    }
                }, null);
            }
        }, ServiceRank.ALIAS_countLogin, new DerivedReferrerOption().coalesce(0));

        // 会員数の多い順に並べる
        cb.query().addSpecifiedDerivedOrderBy_Desc(ServiceRank.ALIAS_countMember);

        List<ServiceRank> serviceRankList = serviceRankBhv.selectList(cb);

        LoadReferrerOption<MemberServiceCB, MemberService> option = new LoadReferrerOption<MemberServiceCB, MemberService>();
        option.setConditionBeanSetupper(new ConditionBeanSetupper<MemberServiceCB>() {
            public void setup(MemberServiceCB cb) {
                cb.setupSelect_Member();
            }
        });

        option.setEntityListSetupper(new EntityListSetupper<MemberService>() {
            public void setup(List<MemberService> entityList) {
                memberBhv.loadPurchaseList(memberServiceBhv.pulloutMember(entityList),
                        new ConditionBeanSetupper<PurchaseCB>() {
                            public void setup(PurchaseCB cb) {
                            }
                        });
                memberBhv.loadMemberLoginList(memberServiceBhv.pulloutMember(entityList),
                        new ConditionBeanSetupper<MemberLoginCB>() {
                            public void setup(MemberLoginCB cb) {
                            }
                        });
            }
        });

        serviceRankBhv.loadMemberServiceList(serviceRankList, option);

        return serviceRankList;
    }

    /**
     * さらにシンプルな応用編
     * それぞれの会員の平均購入価格の会員全体での最大値を検索
     * @return それぞれの会員の平均購入価格の会員全体の最大値 (NotNull)
     */
    public Integer selectMaxAvgPurchasePrice() {
        return memberBhv.scalarSelect(Integer.class).max(new ScalarQuery<MemberCB>() {
            public void query(MemberCB cb) {
                cb.specify().derivedPurchaseList().avg(new SubQuery<PurchaseCB>() {
                    public void query(PurchaseCB subCB) {
                        subCB.specify().columnPurchasePrice();
                    }
                }, null, new DerivedReferrerOption().coalesce(0));
            }
        });
    }
}
