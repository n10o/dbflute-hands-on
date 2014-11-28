package org.dbflute.handson.exercise.phase1;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.annotation.Resource;

import org.dbflute.handson.dbflute.cbean.MemberCB;
import org.dbflute.handson.dbflute.cbean.MemberSecurityCB;
import org.dbflute.handson.dbflute.cbean.MemberStatusCB;
import org.dbflute.handson.dbflute.cbean.PurchaseCB;
import org.dbflute.handson.dbflute.exbhv.MemberBhv;
import org.dbflute.handson.dbflute.exbhv.MemberSecurityBhv;
import org.dbflute.handson.dbflute.exbhv.MemberStatusBhv;
import org.dbflute.handson.dbflute.exbhv.PurchaseBhv;
import org.dbflute.handson.dbflute.exentity.Member;
import org.dbflute.handson.dbflute.exentity.MemberSecurity;
import org.dbflute.handson.dbflute.exentity.MemberStatus;
import org.dbflute.handson.dbflute.exentity.Purchase;
import org.dbflute.handson.unit.UnitContainerTestCase;
import org.seasar.dbflute.cbean.EntityRowHandler;
import org.seasar.dbflute.cbean.ManualOrderBean;
import org.seasar.dbflute.cbean.PagingResultBean;
import org.seasar.dbflute.cbean.SpecifyQuery;
import org.seasar.dbflute.cbean.coption.ColumnConversionOption;
import org.seasar.dbflute.cbean.coption.FromToOption;
import org.seasar.dbflute.cbean.coption.LikeSearchOption;
import org.seasar.dbflute.helper.HandyDate;
import org.seasar.dbflute.helper.token.file.FileMakingCallback;
import org.seasar.dbflute.helper.token.file.FileMakingOption;
import org.seasar.dbflute.helper.token.file.FileMakingRowWriter;
import org.seasar.dbflute.helper.token.file.FileToken;
import org.seasar.dbflute.util.DfResourceUtil;

/**
 * @author nakamura
 */
public class HandsOn03Test extends UnitContainerTestCase {

    @Resource
    protected MemberBhv memberBhv;

    @Resource
    protected MemberStatusBhv memberStatusBhv;

    @Resource
    protected MemberSecurityBhv memberSecurityBhv;

    @Resource
    protected PurchaseBhv purchaseBhv;

    public void test_1searchMembernameStartsWithSAndBirthBefore19680101() throws Exception {
        // ## Arrange ##
        MemberCB cb = new MemberCB();
        cb.query().setMemberName_PrefixSearch("S");

        // おもいで
        //Calendar cal = Calendar.getInstance();
        //cal.set(1968, 0, 1);
        //Date targetDate = cal.getTime();
        Date targetDate = new HandyDate("1968/01/01").getDate();

        cb.query().setBirthdate_LessEqual(targetDate);
        cb.query().addOrderBy_Birthdate_Asc();

        // ## Act ##
        List<Member> memberList = memberBhv.selectList(cb);

        // ## Assert ##
        assertHasAnyElement(memberList);
        for (Member member : memberList) {
            Date birthdate = member.getBirthdate();
            log("Name:" + member.getMemberName() + ". Birth:" + birthdate);
            assertTrue(birthdate.before(targetDate) || birthdate.equals(targetDate));
            assertFalse(birthdate.after(targetDate));
            assertTrue(birthdate.compareTo(targetDate) <= 0);
            assertTrue(birthdate.getTime() <= targetDate.getTime());

            // おもいで
            //assertTrue(targetDate.after(member.getBirthdate());
        }
    }

    public void test_2searchMemberWithStatusAndSecurity() throws Exception {
        // ## Arrange ##
        MemberCB cb = new MemberCB();
        cb.setupSelect_MemberStatus();
        cb.setupSelect_MemberSecurityAsOne();

        cb.query().addOrderBy_Birthdate_Desc();
        cb.query().addOrderBy_MemberId_Asc();

        // ## Act ##
        List<Member> memberList = memberBhv.selectList(cb);

        // ## Assert ##
        assertHasAnyElement(memberList);
        for (Member member : memberList) {
            log("Name: " + member.getMemberName() + ". ID:" + member.getMemberId() + ". Birth:" + member.getBirthdate());
            assertNotNull(member.getMemberStatus()); // NotNullのFKカラム参照だから絶対に存在する
            assertNotNull(member.getMemberSecurityAsOne()); // 業務的に必ず存在する
        }
    }

    public void test_3searchMemberSecurityContain2IntoReminder() throws Exception {
        String magicalWordTwo = "2";
        // ## Arrange ##
        MemberCB cb = new MemberCB();

        cb.query().queryMemberSecurityAsOne()
                .setReminderQuestion_LikeSearch(magicalWordTwo, new LikeSearchOption().likeContain());
        // 以下の書き方との違いは何か->どちらもMemberSecurityテーブルの取得はしない。
        /*
        cb.query().existsMemberSecurityAsOne(new SubQuery<MemberSecurityCB>() {
            public void query(MemberSecurityCB subCB) {
                subCB.query().setReminderQuestion_LikeSearch("2", new LikeSearchOption().likeContain());
            }
        });
        */

        // ## Act ##
        List<Member> memberList = memberBhv.selectList(cb);

        // ## Assert ##
        // 一回検索version
        MemberSecurityCB securityCB = new MemberSecurityCB();

        // 条件をおうむ返しにしないでやってみて by jflute
        //securityCB.query().setReminderQuestion_LikeSearch("2", new LikeSearchOption().likeContain());
        List<Integer> idList = memberBhv.extractMemberIdList(memberList);
        securityCB.query().setMemberId_InScope(idList);

        // securityListでOK by jflute
        List<MemberSecurity> securityList = memberSecurityBhv.selectList(securityCB);

        // boolean型でOK by jflute
        //boolean isMatch = false;
        // 該当するQuestionのMemberIDを出して、実際に得られたメンバのMemberIDと比較
        assertHasAnyElement(securityList);
        assertHasAnyElement(memberList);

        Map<Integer, String> userIdAndSecurityQuestionMap = new HashMap<Integer, String>();

        for (MemberSecurity memberSecurity : securityList) {
            userIdAndSecurityQuestionMap.put(memberSecurity.getMemberId(), memberSecurity.getReminderQuestion()); // log出力用
            assertTrue(memberSecurity.getReminderQuestion().contains(magicalWordTwo)); //対象の文字が含まれていることをアサート
        }

        for (Member member : memberList) {
            log("MemberName:" + member.getMemberName() + " SecurityQuestion:"
                    + userIdAndSecurityQuestionMap.get(member.getMemberId()));
        }

        //        // ネステッドループではなく、Mapを使ってみましょう。securityMap by jflute
        //        for (Member member : memberList) {
        //            for (MemberSecurity memberSecurity : securityList) {
        //                Integer expectID = memberSecurity.getMemberId();
        //                Integer actualID = member.getMemberId();
        //
        //                if (expectID == actualID) {
        //                    log("MemberName:" + member.getMemberName() + ". ReminderQuestion:"
        //                            + memberSecurity.getReminderQuestion());
        //                    isMatch = true;
        //                    break;
        //                }
        //            }
        //            if (isMatch == false) {
        //                fail();
        //            }
        //            isMatch = false;
        //        }

        //検索複数回version
        /*
        assertHasAnyElement(memberList);
        for (Member member : memberList) {
            MemberSecurityCB securityCB = new MemberSecurityCB();
            securityCB.query().setMemberId_Equal(member.getMemberId());

            MemberSecurity securityEntity = memberSecurityBhv.selectEntity(securityCB);
            log(securityEntity.getReminderQuestion());

            assertThat(securityEntity.getReminderQuestion(), containsString("2"));
        }
        */
    }

    public void test_4searchMemberStatusByDisplayOrder() throws Exception {
        // ## Arrange ##
        MemberCB cb = new MemberCB();

        cb.query().queryMemberStatus().addOrderBy_DisplayOrder_Asc();
        cb.query().addOrderBy_MemberId_Desc();

        // ## Act ##
        List<Member> memberList = memberBhv.selectList(cb);

        // ## Assert ##
        assertHasAnyElement(memberList);

        //        String previousStatusCode = null;
        //        // 会員が会員ステータス毎に固まって並んでいることをアサート
        //        // 以下はリライト
        //        for (Member member : memberList) {
        //            String currentStatusCode = member.getMemberStatusCode();
        //
        //            String fml = "FML";
        //            String wdl = "WDL";
        //            String prv = "PRV";
        //
        //            // 考え方　FML->WDL->PRVの順番は固定（中抜けも存在。e.g. WDL->PRV、FML->PRV）。WDL->FML等の逆行はエラー
        //            if (previousStatusCode == null) {
        //                // falling
        //            } else if ((previousStatusCode.equals(wdl) || previousStatusCode.equals(prv))
        //                    && currentStatusCode.equals(fml)) {
        //                fail();
        //            } else if (previousStatusCode.equals(prv)
        //                    && (currentStatusCode.equals(fml) || currentStatusCode.equals(wdl))) {
        //                fail();
        //            }
        //            previousStatusCode = member.getMemberStatusCode();
        //
        //            log(member.getMemberName());
        //            assertNull(member.getMemberStatus());
        //        }

        String currentStatusCode;
        String previousStatusCode = null;

        //        // 宿題1：会員ステータスが固まっていることをアサート
        //        List<String> statusCodeList = new ArrayList<String>();
        //        //
        //        for (Member member : memberList) {
        //            log("Name:" + member.getMemberName() + ". StatusCode:" + member.getMemberStatusCode());
        //            currentStatusCode = member.getMemberStatusCode();
        //            // 前回のステータスコードと今回のステータスコードが異なっていた場合　かつ　既にリストに今回のステータスコードが入っていた場合にエラー
        //            if (previousStatusCode != null && !currentStatusCode.equals(previousStatusCode)) {
        //                boolean alreadyExistStatusCode = statusCodeList.indexOf(currentStatusCode) != -1;
        //                if (alreadyExistStatusCode) {
        //                    fail();
        //                }
        //                statusCodeList.add(currentStatusCode);
        //            }
        //
        //            previousStatusCode = member.getMemberStatusCode();
        //        }

        // 宿題2:会員ステータスが固まっていること＋順番も正しいことをアサート。 + 元々の記述方法ではステータスが増えた時に対応できないので、それも対応できるように
        // 方針：宿題1にMEMBER_STATUSのDISPLAY_ORDERを組み合わせる
        MemberStatusCB statusCb = new MemberStatusCB();
        statusCb.query().addOrderBy_MemberStatusCode_Asc();

        List<MemberStatus> statusList = memberStatusBhv.selectList(statusCb);
        HashMap<String, Integer> statusMap = new HashMap<String, Integer>();
        for (MemberStatus memberStatus : statusList) {
            statusMap.put(memberStatus.getMemberStatusCode(), memberStatus.getDisplayOrder());
        }

        Integer currentDisplayNo = null;
        Integer previousDisplayNo = null;
        //List<String> alreadyExistsStatusCodeList = new ArrayList<String>();
        Set<String> existedStatusSet = new HashSet<String>();
        for (Member member : memberList) {
            log("Name:" + member.getMemberName() + ". StatusCode:" + member.getMemberStatusCode());
            currentStatusCode = member.getMemberStatusCode();
            currentDisplayNo = statusMap.get(member.getMemberStatusCode());

            // 前回のステータスコードと今回のステータスコードが異なっていた場合　かつ　既にリストに今回のステータスコードが入っていた場合にエラー
            if (previousStatusCode != null && !currentStatusCode.equals(previousStatusCode)) {
                // boolean alreadyExistStatusCode = statusCodeList.indexOf(currentStatusCode) > -1;
                assertFalse(existedStatusSet.contains(currentStatusCode));
                existedStatusSet.add(currentStatusCode);
            }

            // 前回のDisplayNumberよりも今回のDisplayNumberが小さい場合エラー
            assertTrue(previousDisplayNo == null || (currentDisplayNo >= previousDisplayNo));
            //            if (previousDisplayNumber != null && (currentDisplayNumber < previousDisplayNumber)) {
            //                fail();
            //            }

            previousStatusCode = member.getMemberStatusCode();
            previousDisplayNo = currentDisplayNo;
        }
    }

    public void test_5searchMemberPurchaseHistoryExistsBirthdate() throws Exception {
        // ## Arrange ##
        /*
        //基点テーブルがMember版
        MemberCB cb = new MemberCB();

        cb.setupSelect_MemberStatus();
        cb.query().setBirthdate_IsNotNull();

        List<Member> memberList = memberBhv.selectList(cb);

        memberBhv.loadPurchaseList(memberList, new ConditionBeanSetupper<PurchaseCB>() {
            @Override
            public void setup(PurchaseCB cb) {
                cb.setupSelect_Product();
                cb.query().addOrderBy_PurchaseDatetime_Desc();
                cb.query().addOrderBy_PurchasePrice_Desc();
                cb.query().addOrderBy_ProductId_Asc();
                cb.query().addOrderBy_MemberId_Asc();
            }
        });

        assertHasAnyElement(memberList);
        for (Member member : memberList) {
            log("Name:" + member.getMemberName() + ". Status:" + member.getMemberStatus().getMemberStatusName());
            List<Purchase> purchaseList = member.getPurchaseList();

            for (Purchase purchase : purchaseList) {
                log("ProductName:" + purchase.getProduct().getProductName() + ". PurchaseDate:"
                        + purchase.getPurchaseDatetime() + ". Price:" + purchase.getPurchasePrice() + ". ItemID:"
                        + purchase.getProductId() + ". MemberID:" + purchase.getMemberId());
            }
            log("---");
            // ## Assert ##
            assertNotNull(member.getBirthdate());
        }
        */

        //基点テーブルがPurchase版
        PurchaseCB cb = new PurchaseCB();

        cb.setupSelect_Member().withMemberStatus();
        cb.setupSelect_Product();

        cb.query().queryMember().setBirthdate_IsNotNull();

        cb.query().addOrderBy_PurchaseDatetime_Desc();
        cb.query().addOrderBy_PurchasePrice_Desc();
        cb.query().addOrderBy_ProductId_Asc();
        cb.query().addOrderBy_MemberId_Asc();

        List<Purchase> purchaseList = purchaseBhv.selectList(cb);

        assertHasAnyElement(purchaseList);
        for (Purchase purchase : purchaseList) {
            log("Purchase:" + purchase.getPurchaseId() + ". Birthdate:" + purchase.getMember().getBirthdate()
                    + ". PurchaseDay:" + purchase.getPurchaseDatetime() + ". PurchasePrice:"
                    + purchase.getPurchasePrice() + ". ProductID:" + purchase.getProductId() + ". MemberID:"
                    + purchase.getMemberId());
            assertNotNull(purchase.getMember().getBirthdate());
        }
    }

    public void test_6searchMemberNameContainVi() throws Exception {
        // ## Arrange ##
        MemberCB cb = new MemberCB();
        cb.setupSelect_MemberStatus();
        cb.specify().specifyMemberStatus().columnMemberStatusName();

        // 検索後にやっても意味がない by jflute
        adjustMember_FormalizedDatetime_FirstOnly(toDate("2005/10/01"), "vi");

        cb.query().setMemberName_LikeSearch("vi", new LikeSearchOption().likeContain());

        Date fromDate = new HandyDate("2005/10/01").getDate();
        HandyDate toHandyDate = new HandyDate("2005/10/03");

        cb.query().setFormalizedDatetime_DateFromTo(fromDate, toHandyDate.getDate());

        // ## Act ##
        List<Member> memberList = memberBhv.selectList(cb);

        // ## Assert ##
        assertHasAnyElement(memberList);
        Date toDate = toHandyDate.addDay(1).getDate();
        for (Member member : memberList) {
            Date formalizedDatetime = member.getFormalizedDatetime();
            MemberStatus memberStatus = member.getMemberStatus();
            log("Name:" + member.getMemberName() + ". FormalizedDate:" + formalizedDatetime + ". StatusName:"
                    + memberStatus.getMemberStatusCode());
            assertNotNull(memberStatus.getMemberStatusCode());
            assertNotNull(memberStatus.getMemberStatusName());
            assertNull(memberStatus.getDescription());
            assertNull(memberStatus.getDisplayOrder());

            // 演算子を右側へ by jflute
            assertTrue(formalizedDatetime.compareTo(fromDate) >= 0);
            // addDayが積み重なってしまっている。ctrl + 1 -> enter で for の外へ by jflute
            assertTrue(formalizedDatetime.compareTo(toDate) < 0);
        }
    }

    // 正式会員になってから一週間以内の購入を検索
    public void test_7searchNewMemberWithinOneMonth() throws Exception {
        // ## Arrange ##
        PurchaseCB cb = new PurchaseCB();

        cb.setupSelect_Member().withMemberSecurityAsOne();
        cb.setupSelect_Member().withMemberStatus();
        cb.setupSelect_Product().withProductStatus();
        cb.setupSelect_Product().withProductCategory().withProductCategorySelf();

        //  検索結果が1増えるかどうか確認 -> done
        adjustPurchase_PurchaseDatetime_fromFormalizedDatetimeInWeek();

        cb.columnQuery(new SpecifyQuery<PurchaseCB>() {
            @Override
            public void specify(PurchaseCB cb) {
                cb.specify().columnPurchaseDatetime();
            }
        }).greaterEqual(new SpecifyQuery<PurchaseCB>() {
            @Override
            public void specify(PurchaseCB cb) {
                cb.specify().specifyMember().columnFormalizedDatetime();
            }
        });

        cb.columnQuery(new SpecifyQuery<PurchaseCB>() {
            @Override
            public void specify(PurchaseCB cb) {
                cb.specify().columnPurchaseDatetime();
            }
        }).lessThan(new SpecifyQuery<PurchaseCB>() {
            @Override
            public void specify(PurchaseCB cb) {
                cb.specify().specifyMember().columnFormalizedDatetime();
            }
        }).convert(new ColumnConversionOption().truncTime().addDay(8));
        // addDayではなく、丸７日後という形に書き換える。(0:00ジャストに)

        List<Purchase> purchaseList = purchaseBhv.selectList(cb);

        assertHasAnyElement(purchaseList);
        for (Purchase purchase : purchaseList) {
            Timestamp purchaseDatetime = purchase.getPurchaseDatetime();
            Member member = purchase.getMember();
            Timestamp formalizedDatetime = member.getFormalizedDatetime();
            log("PurchaseId:" + purchase.getPurchaseId() + ". Name:" + member.getMemberName() + ". FDate:"
                    + formalizedDatetime + ". PDate:" + purchaseDatetime);

            // 上位の商品カテゴリ名が取得できていることをアサート
            assertNotNull(purchase.getProduct().getProductCategory().getProductCategorySelf());
            // 購入日時が正式会員になってから一週間以内であることをアサート
            // 演算子を右側へ
            assertTrue(purchaseDatetime.compareTo(formalizedDatetime) > 0);
            assertTrue(purchaseDatetime.compareTo(new HandyDate(formalizedDatetime).moveToDayJustAdded(8).getDate()) < 0);
        }

        //苦戦の跡
        //cb.query().derivedPurchaseList()
        /*
                cb.columnQuery(new SpecifyQuery<MemberCB>() {
                    @Override
                    public void specify(MemberCB cb) {
                        cb.specify().columnFormalizedDatetime();
                    }
                }).lessEqual(new SpecifyQuery<MemberCB>() {
                    @Override
                    public void specify(MemberCB cb) {
                        cb.query().existsPurchaseList(new SubQuery<PurchaseCB>() {

                            @Override
                            public void query(PurchaseCB subCB) {
                                //subCB.query().setPurchaseDatetime_DateFromTo(fromDate, toDate)
                            }
                        });
                    }
                });
        */
        /*
        cb.query().existsPurchaseList(new SubQuery<PurchaseCB>() {

            @Override
            public void query(PurchaseCB subCB) {
                //subCB.query().setPurchaseDatetime_FromTo(cb.query().getFormalizedDatetime(), toDatetime, fromToOption)

                //この時点で正式会員になった日時がわかれば、query発行可能
                subCB.query().setPurchaseDatetime_FromTo(subCB.query().queryMember().getFormalizedDatetime(),
                        subCB.query().queryMember().getFormalizedDatetime(), new FromToOption().compareAsWeek());
            }
        });
        */

        //ConditionValue formalizedDatetime = cb.query().getFormalizedDatetime();
        //cb.query().setFormalizedDatetime_DateFromTo(cb.query().getFormalizedDatetime(), cb.query().getFormalizedDatetime());
        //cb.query().setFormalizedDatetime_FromTo(cb.query().getFormalizedDatetime(), cb.query().getFormalizedDatetime(),
        //        new FromToOption().compareAsWeek());
        //cb.query().setF
        /*
        assertHasAnyElement(memberList);
        for (Member member : memberList) {
            log("Name:" + member.getMemberName());
            List<Purchase> purchaseList = member.getPurchaseList();
            for (Purchase purchase : purchaseList) {
                log("ProductName:" + purchase.getProduct().getProductName());
                log(purchase.getProduct().getProductStatus());
                log(purchase.getProduct().getProductCategory());
                log(purchase.getProduct().getProductCategory().getProductCategorySelf());
                //member.getBirthdate().
                log(purchase.getPurchaseDatetime());
            }
        }
        */
    }

    public void test_8searchMemberBirthdateUnknownOrBefore1974() throws Exception {
        // 1974でお願いします。日付操作も禁止(Arrange内では)。ヒント：まで検索 by jflute
        final String targetDateString = "1974/01/01";
        // ## Arrange ##
        MemberCB cb = new MemberCB();

        cb.setupSelect_MemberStatus();
        cb.setupSelect_MemberWithdrawalAsOne();
        cb.setupSelect_MemberSecurityAsOne();

        // 学び 日付を操作したい場合は、LessEqual等よりもFromTo。便利なFromToオプションが利用できる。
        cb.query().setBirthdate_FromTo(null, new HandyDate(targetDateString).getDate(),
                new FromToOption().compareAsYear().orIsNull());

        cb.query().addOrderBy_Birthdate_Desc().withNullsFirst();

        //        // orScopeQueryを使わない形にする ヒント：まで検索の応用
        //        cb.orScopeQuery(new OrQuery<MemberCB>() {
        //            @Override
        //            public void query(MemberCB orCB) {
        //                Date targetDate = new HandyDate(targetDateString).getDate();
        //                orCB.query().setBirthdate_IsNull();
        //                orCB.query().setBirthdate_LessThan(targetDate);
        //                // 外側へ。中で order by しない by jflute
        //                //orCB.query().addOrderBy_Birthdate_Desc().withNullsFirst();
        //            }
        //        });

        // ## Act ##
        List<Member> memberList = memberBhv.selectList(cb);

        // ## Assert ##
        // detected で by jflute
        boolean detectedFirstNotNullBirth = false;
        // existsで by jflute
        boolean existsBirth1974 = false;

        HandyDate targetDate = new HandyDate(targetDateString);
        HandyDate nextYearDate = new HandyDate(targetDate.getDate()).addYear(1);
        assertHasAnyElement(memberList);
        for (Member member : memberList) {
            String info = "BirthDate:" + member.getBirthdate() + ". StatusName:"
                    + member.getMemberStatus().getMemberStatusName() + ". ReminderQ:"
                    + member.getMemberSecurityAsOne().getReminderQuestion() + ". ReminderA:"
                    + member.getMemberSecurityAsOne().getReminderAnswer();
            if (member.getMemberWithdrawalAsOne() != null) {
                log(info + ". WithdrawalReason:" + member.getMemberWithdrawalAsOne().getWithdrawalReasonInputText());
            } else {
                log(info);
            }

            // 産まれが不明の会員が先頭になっていることをアサート
            if (member.getBirthdate() == null) {
                // 産まれが判明している会員が見つかったあとに、産まれが不明な会員が見つかったら、産まれが不明の会員が先頭になっていないのでfail
                assertFalse(detectedFirstNotNullBirth);
            } else {
                detectedFirstNotNullBirth = true;

                // 1974年生まれの人が含まれていることをアサート。一人もいなければfail
                if (targetDate.isYearSameAs(member.getBirthdate())) {
                    existsBirth1974 = true;
                }
                // 1974年までに生まれていることをアサート
                assertTrue(nextYearDate.isGreaterThan(member.getBirthdate()));
                //assertFalse(nextYearDate.isLessEqual(member.getBirthdate()));
            }

            // わかりづらいのでリライト
            //            if (member.getBirthdate() != null) {
            //                detectedFirstNotNullBirth = true;
            //                // 1974年生まれの人が含まれていることをアサート
            //                if (targetDate.isYearSameAs(member.getBirthdate())) {
            //                    existsBirth1974 = true;
            //                    // 1974年までに生まれていることをアサート
            //                } else if (targetDate.addYear(1).isLessEqual(member.getBirthdate())) {
            //                    fail();
            //                }
            //            } else if (detectedFirstNotNullBirth) {
            //                // nullでない誕生日が出たあとに、nullが見つかるとエラー（nullが１つもない場合はFAILしない）
            //                fail();
            //            }
        }
        assertTrue(existsBirth1974);
    }

    public void test_9searchMembersNoBirthdate() throws Exception {
        // ## Arrange ##
        MemberCB cb = new MemberCB();

        cb.query().setMemberName_Equal(null);
        cb.query().setMemberAccount_Equal("");
        cb.query().setBirthdate_IsNull();

        Date targetMonth = new HandyDate("2005/06/01").getDate();

        // mob で by jflute
        ManualOrderBean mob = new ManualOrderBean();

        mob.when_FromTo(targetMonth, targetMonth, new FromToOption().compareAsMonth());
        cb.query().addOrderBy_FormalizedDatetime_Asc().withManualOrder(mob);

        cb.query().addOrderBy_MemberId_Desc();

        // ## Act ##
        List<Member> memberList = memberBhv.selectList(cb);

        // ## Assert ##
        HandyDate targetDate = new HandyDate(targetMonth);

        boolean passedBorder = false; // passedBorderという変数名はわかりやすくていいですね

        assertHasAnyElement(memberList);
        for (Member member : memberList) {
            Timestamp formalizedDatetime = member.getFormalizedDatetime();

            log("Name:" + member.getMemberName() + ". ID:" + member.getMemberId() + ". FormalizedDate:"
                    + formalizedDatetime);
            assertNull(member.getBirthdate());

            // 2005年6月に正式会員になった会員が先に並んでいること。2005年6月に正式会員になった会員がいない場合はエラーにしない実装
            if (formalizedDatetime != null && targetDate.isMonthOfYearSameAs(formalizedDatetime)) {
                assertFalse(passedBorder);
            } else {
                passedBorder = true;
            }
        }

        // 整理整頓すべし by jflute
        //        for (Member member : memberList) {
        //            Date formalizedDatetime = member.getFormalizedDatetime();
        //            log("Name:" + member.getMemberName() + ". ID:" + member.getMemberId() + ". FormalizedDate:"
        //                    + member.getFormalizedDatetime());
        //            assertNull(member.getBirthdate());
        //
        //            Date targetEndMonth = new HandyDate("2005/06/01").addMonth(1).getDate();
        //            // 2005年6月生まれでない会員が見つかった後に、2005年6月生まれの会員が見つかればFAIL
        //            // compareToを使う
        //            if (((formalizedDatetime != null) && (formalizedDatetime.after(targetMonth) && formalizedDatetime
        //                    .before(targetEndMonth))) == false) { // 対象外の場合
        //                existNotTarget = true;
        //            } else if (existNotTarget == null) {
        //                //falling
        //            } else if (existNotTarget == true) {
        //                fail();
        //            }
        //        }
    }

    // CBでページングしてみよう
    public void test_paging() throws Exception {
        // ## Arrance ##
        int pageSize = 3;
        int pageNumber = 1;

        MemberCB cb = new MemberCB();
        cb.setupSelect_MemberStatus();
        cb.query().addOrderBy_MemberId_Desc();
        cb.paging(pageSize, pageNumber);

        // ## Act ##
        PagingResultBean<Member> page = memberBhv.selectPage(cb);

        // ## Assert ##
        int pageSizeNumberForAssertion = 0; // 検索結果が指定されたページサイズ分のデータだけであること

        // SQLのログでカウント検索時と実データ検索時の違いを確認 -> done
        for (Member member : page) {
            log("ID:" + member.getMemberId() + " Name:" + member.getMemberName() + " Status:"
                    + member.getMemberStatus().getMemberStatusName());
            pageSizeNumberForAssertion++;
        }

        MemberCB validationCb = new MemberCB();
        int allMemberNumber = memberBhv.selectCount(validationCb);
        assertEquals(allMemberNumber, page.getAllRecordCount()); // 総レコード件数が会員テーブルの全件であること

        int allPageNumber = allMemberNumber / pageSize; //総ページ数算出のため、総レコード数を1ページあたりのレコード数で割って余りがある場合は1を足す
        if (allMemberNumber % pageSize != 0) {
            allPageNumber++;
        }

        assertEquals(allPageNumber, page.getAllPageCount()); // 総ページ数が正しいか

        assertEquals(pageSize, page.getPageSize()); // 検索結果のページサイズが正しいか
        assertEquals(pageNumber, page.getCurrentPageNumber()); // ページ番号が指定されたもの（１）であること

        assertEquals(pageSize, pageSizeNumberForAssertion); // 検索結果が指定されたページサイズ分のデータだけであること

        page.setPageRangeSize(3);
        List<Integer> numberList = page.pageRange().createPageNumberList();
        List<Integer> expectedNumberList = Arrays.asList(1, 2, 3, 4);
        assertEquals(expectedNumberList, numberList); // [1,2,3,4]であることをアサート

        assertFalse(page.isExistPrePage());
        assertTrue(page.isExistNextPage());
    }

    // 会員ステータスの表示順カラムで会員を並べてカーソル検索
    public void test_cursor() throws Exception {
        File tsvFile = new File(DfResourceUtil.getBuildDir(getClass()).getParent(), "hands-on-cb-bonus.csv");
        List<String> columnNameList = new ArrayList<String>();
        columnNameList.add("会員名称");
        columnNameList.add("生年月日");
        columnNameList.add("正式会員日時");

        FileToken fileToken = new FileToken();

        fileToken.make(new FileOutputStream(tsvFile), new FileMakingCallback() {
            public void write(final FileMakingRowWriter writer) throws IOException, SQLException {
                // ## Arrange ##
                MemberCB cb = new MemberCB();
                cb.setupSelect_MemberStatus();
                cb.query().queryMemberStatus().addOrderBy_DisplayOrder_Asc();
                cb.query().addOrderBy_MemberId_Desc();

                // ## Act ##
                // 一件もヒットしない（null）場合はエラーにする
                // assertNotSame(0, memberBhv.readCount(cb));
                final Set<String> statusCodes = new HashSet<String>();

                memberBhv.selectCursor(cb, new EntityRowHandler<Member>() {
                    int statusCodeCounter = 1;
                    String pastStatusCode = null;

                    public void handle(Member entity) {
                        log("Name:" + entity.getMemberName());
                        assertNotNull(entity.getMemberStatus());

                        // ## Assert ##
                        // 会員が会員ステータスごとに固まって並んでいることをアサート
                        String currentStatusCode = entity.getMemberStatus().getMemberStatusCode();
                        if (pastStatusCode != null && !pastStatusCode.equals(currentStatusCode)) {
                            // 今と一個前のステータスコードを比較して、違ったらカウンタを回す
                            statusCodeCounter++;
                        }
                        // setは要素の重複を許さないので、ステータスコードの数が３つなら、何回登録しても最大３つしか要素が入らない
                        statusCodes.add(currentStatusCode);
                        pastStatusCode = currentStatusCode;

                        // カウンタが実際の会員ステータスの数以下であることをアサート
                        assertTrue(statusCodeCounter <= statusCodes.size());

                        List<String> valueList = new ArrayList<String>();
                        valueList.add(entity.getMemberName());
                        if (entity.getBirthdate() != null) {
                            // 日付に関してはtoStringを使わずに、フォーマットする。
                            valueList.add(new HandyDate(entity.getBirthdate()).toDisp("yyyy/MM/dd"));
                        } else {
                            valueList.add(null);
                        }

                        if (entity.getFormalizedDatetime() != null) {
                            valueList.add(entity.getFormalizedDatetime().toString());
                        } else {
                            valueList.add(null);
                        }

                        try {
                            writer.writeRow(valueList);
                        } catch (IOException e) {
                            //throw new IllegalStateException("IOException occured:" + filename, e);
                        }
                    }
                });
                assertFalse(statusCodes.isEmpty());
            }
        }, new FileMakingOption().delimitateByComma().encodeAsUTF8().headerInfo(columnNameList));
    }
}
