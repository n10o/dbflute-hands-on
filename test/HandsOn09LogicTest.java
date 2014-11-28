package org.dbflute.handson.logic;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.annotation.Resource;

import org.dbflute.handson.dbflute.allcommon.CDef;
import org.dbflute.handson.dbflute.cbean.MemberCB;
import org.dbflute.handson.dbflute.cbean.MemberServiceCB;
import org.dbflute.handson.dbflute.exbhv.MemberBhv;
import org.dbflute.handson.dbflute.exbhv.MemberServiceBhv;
import org.dbflute.handson.dbflute.exbhv.cursor.PurchaseMonthCursorCursor;
import org.dbflute.handson.dbflute.exbhv.pmbean.OutsideMemberPmb;
import org.dbflute.handson.dbflute.exbhv.pmbean.PartOfMemberPmb;
import org.dbflute.handson.dbflute.exbhv.pmbean.PurchaseMonthCursorPmb;
import org.dbflute.handson.dbflute.exbhv.pmbean.PurchaseMonthSummaryPmb;
import org.dbflute.handson.dbflute.exbhv.pmbean.SpInOutParameterPmb;
import org.dbflute.handson.dbflute.exbhv.pmbean.SpReturnResultSetPmb;
import org.dbflute.handson.dbflute.exentity.MemberService;
import org.dbflute.handson.dbflute.exentity.customize.OutsideMember;
import org.dbflute.handson.dbflute.exentity.customize.PartOfMember;
import org.dbflute.handson.dbflute.exentity.customize.PurchaseMonthSummary;
import org.dbflute.handson.dbflute.exentity.customize.SpReturnResultSetNotParamResult1;
import org.dbflute.handson.dbflute.exentity.customize.SpReturnResultSetNotParamResult2;
import org.dbflute.handson.exercise.phase1.HandsOn03Test;
import org.dbflute.handson.unit.UnitContainerTestCase;
import org.seasar.dbflute.cbean.PagingResultBean;
import org.seasar.dbflute.cbean.SubQuery;
import org.seasar.dbflute.helper.HandyDate;
import org.seasar.dbflute.helper.token.file.FileToken;
import org.seasar.dbflute.helper.token.file.FileTokenizingCallback;
import org.seasar.dbflute.helper.token.file.FileTokenizingOption;
import org.seasar.dbflute.helper.token.file.FileTokenizingRowResource;
import org.seasar.dbflute.infra.dfprop.DfPropFile;
import org.seasar.dbflute.infra.manage.refresh.DfRefreshResourceRequest;
import org.seasar.dbflute.unit.core.transaction.TransactionPerformer;
import org.seasar.dbflute.util.DfResourceUtil;

/**
 * @author nakamura
 */
public class HandsOn09LogicTest extends UnitContainerTestCase {

    @Resource
    protected MemberBhv memberBhv;

    @Resource
    protected MemberServiceBhv memberServiceBhv;

    public void test_letsOutside_会員が検索されること() {
        // ## Arrange ##
        HandsOn09Logic logic = new HandsOn09Logic();
        inject(logic);

        OutsideMemberPmb pmb = new OutsideMemberPmb();
        pmb.setMemberStatusCode_正式会員();
        pmb.setMemberName_PrefixSearch("S");

        // ## Act ##
        List<OutsideMember> memberList = logic.letsOutside(pmb);

        // ## Assert ##
        String regex = "S.*"; // Sで始まる文字列
        Pattern ptn = Pattern.compile(regex);

        assertHasAnyElement(memberList);
        for (OutsideMember member : memberList) {
            String status = member.getMemberStatusName();
            String name = member.getMemberName();
            log("Name:" + name + ". Status:" + status + ". SPoint" + member.getServicePointCount());
            assertEquals(CDef.MemberStatus.正式会員.name(), status);

            // Sで始まる会員名であることをアサート
            Matcher matcher = ptn.matcher(name);
            assertTrue(matcher.find());
        }
    }

    public void test_letsOutside_条件値なしで全件検索されること() {
        // ## Arrange ##
        HandsOn09Logic logic = new HandsOn09Logic();
        inject(logic);

        // ## Act ##
        List<OutsideMember> memberList = logic.letsOutside(new OutsideMemberPmb());

        // ## Assert ##
        assertHasAnyElement(memberList);
        for (OutsideMember member : memberList) {
            // where句が存在しないことを確認した
            log("Name:" + member.getMemberName());
        }
        // 検索結果が全件であることをアサート
        MemberCB cb = new MemberCB();
        assertEquals(memberBhv.selectCount(cb), memberList.size());
    }

    public void test_selectPartOfMember_ページング検索されること() {
        // ## Arrange ##
        HandsOn09Logic logic = new HandsOn09Logic();
        inject(logic);

        int pageSize = 4;

        PartOfMemberPmb pmb = new PartOfMemberPmb();
        pmb.setMemberName_ContainSearch("vi");
        //pmb.setAkirakaniOkashiiKaramuMei(1000);
        pmb.setServicePointCount(1000);
        pmb.paging(pageSize, 1);

        // ## Act ##
        PagingResultBean<PartOfMember> page = logic.selectPartOfMember(pmb);

        // ## Assert ##
        int memberNumCounter = 0;
        assertHasAnyElement(page);
        for (PartOfMember member : page) {
            log("Name:" + member.getMemberName() + ". ID:" + member.getMemberId() + ". Point:"
                    + member.getServicePointCount() + ". Status:" + member.getMemberStatusName());
            memberNumCounter++;
        }
        log("AllRecord:" + page.getAllRecordCount() + ". AllPageCount:" + page.getAllPageCount());
        assertTrue(memberNumCounter <= pageSize);
    }

    // ボーナスステージでPagingに変更
    public void test_selectLetsSummary_集計が検索されること() {
        // ## Arrange ##
        HandsOn09Logic logic = new HandsOn09Logic();
        inject(logic);

        // ## Act ##
        PurchaseMonthSummaryPmb pmb = new PurchaseMonthSummaryPmb();
        pmb.setMemberName_ContainSearch("vi");
        pmb.paging(4, 1);

        PagingResultBean<PurchaseMonthSummary> summaryList = logic.selectLetsSummary(pmb);

        // ## Assert ##
        String regex = ".*vi.*"; // viが含まれる文字列
        Pattern ptn = Pattern.compile(regex);

        assertHasAnyElement(summaryList);
        for (PurchaseMonthSummary summary : summaryList) {
            String name = summary.getMemberName();
            log("ID:" + summary.getMemberId() + ". Name:" + name + ". Month:" + summary.getPurchaseDate()
                    + ". avgPrice:" + summary.getAvgPrice() + ". NumOfPurchase:" + summary.getNumOfPurchase());
            Matcher matcher = ptn.matcher(name);
            assertTrue(matcher.find());
        }
    }

    public void test_selectLetsCursor_集計が検索されること() throws SQLException {
        // ## Arrange ##
        //ReadCommittedにしない場合、ポイントを足す前と足したあとの値が同じになる。 -> MySQLのデフォルトのトランザクション分離レベルがRepeatable Readだから?
        adjustTransactionIsolationLevel_ReadCommitted();
        HandsOn09Logic logic = new HandsOn09Logic() {
            public void addServicePoint(final PurchaseMonthCursorCursor cursor) throws SQLException {
                performNewTransaction(new TransactionPerformer() {
                    public boolean perform() throws SQLException {
                        doSuperAddServicePoint(cursor);
                        return true;
                    }
                });
            }

            private void doSuperAddServicePoint(PurchaseMonthCursorCursor cursor) throws SQLException {
                super.addServicePoint(cursor);
            }
        };

        inject(logic);

        //誰か一人でもサービスポイント数が増えていることをアサート
        MemberService before = findFirstMemberService();
        //int beforeUpdate = before.getAkirakaniOkashiiKaramuMei();
        int beforeUpdate = before.getServicePointCount();

        // ## Act ##
        PurchaseMonthCursorPmb pmb = new PurchaseMonthCursorPmb();
        pmb.setMemberName_ContainSearch("vi");
        logic.selectLetsCursor(pmb);

        MemberService after = findFirstMemberService();
        //int afterUpdate = after.getAkirakaniOkashiiKaramuMei();
        int afterUpdate = after.getServicePointCount();

        log("ID:" + before.getMemberId() + ". beforePoint:" + beforeUpdate + ". afterPoint:" + afterUpdate);
        assertTrue(beforeUpdate < afterUpdate);
    }

    private MemberService findFirstMemberService() {
        MemberServiceCB cb = new MemberServiceCB();
        cb.query().setMemberId_Equal(2); // 優良顧客のSavicevicさんを狙い撃ち
        cb.query().queryServiceRank().existsMemberServiceList(new SubQuery<MemberServiceCB>() {
            public void query(MemberServiceCB subCB) {
            }
        });
        cb.fetchFirst(1);

        return memberServiceBhv.selectEntityWithDeletedCheck(cb);
    }

    // スーパーボーナスステージ
    //    もし、ものすごい頑張れるのであれば、一行ずつフェッチでCSVにデータを出力してみてください。
    //    出力項目:会員名称、購入月、合計購入数量
    //    デリミタ文字:カンマ
    //    エンコーディング:UTF-8
    //    改行コード:LF
    //    カラムヘッダー:一行目にはカラム名のヘッダー
    //    出力ファイル:[PROJECT_ROOT]/target/hands-on-outside-bonus.csv
    //    CSV出力API:FileToken @since DBFlute-1.0.4F (それはもう、じっくりソースを読んで...)
    //    ※1: DfResourceUtil.getBuildDir(getClass())で target/classes の File インスタンスが取得できます
    //    Logicクラスの selectLetsCursor() にて、そのまま処理を追加し、 テストコードのアサートにて、同じく FileToken を使ってそのファイルを読み込んでログに出力してみましょう。
    public void test_selectLetsCursor_CSVにデータ出力() throws SQLException, FileNotFoundException, IOException {
        // ## Arrange ##
        adjustTransactionIsolationLevel_ReadCommitted();
        HandsOn09Logic logic = new HandsOn09Logic() {
            public void addServicePoint(final PurchaseMonthCursorCursor cursor) throws SQLException {
                performNewTransaction(new TransactionPerformer() {
                    public boolean perform() throws SQLException {
                        doSuperAddServicePoint(cursor);
                        return true;
                    }
                });
            }

            private void doSuperAddServicePoint(PurchaseMonthCursorCursor cursor) throws SQLException {
                super.addServicePoint(cursor);
            }
        };

        inject(logic);

        // ## Act ##
        PurchaseMonthCursorPmb pmb = new PurchaseMonthCursorPmb();
        pmb.setMemberName_ContainSearch("vi");
        logic.selectLetsCursor(pmb);

        // ## Assert ##
        File tsvFile = new File(DfResourceUtil.getBuildDir(getClass()).getParent(), "hands-on-outside-bonus.csv"); // input file
        FileToken fileToken = new FileToken();

        fileToken.tokenize(new FileInputStream(tsvFile), new FileTokenizingCallback() {
            public void handleRow(FileTokenizingRowResource resource) throws IOException, SQLException {
                log(resource.toColumnValueMap());
            }
        }, new FileTokenizingOption().delimitateByComma().encodeAsUTF8());

    }

    // ミラクルボーナスステージ
    //    もし、ミラクルものすごい頑張れるのであれば、セクション３のカーソル検索でも出力してみましょう。(test_cursor)
    //    出力項目:会員名称、生年月日(yyyy/MM/dd)、正式会員日時(yyyy/MM/dd HH:mm:ss)
    //    デリミタ文字:タブ
    //    エンコーディング:UTF-8
    //    改行コード:LF
    //    カラムヘッダー:なし
    //    出力ファイル:[PROJECT_ROOT]/target/hands-on-cb-bonus.tsv
    //    TSV出力API:FileToken @since DBFlute-1.0.4F
    //    同じように、アサートで FileToken を使ってそのファイルを読み込んでログに出力してみてください。
    //    しかしながら、テストを実行するたびに target 配下を F5 するのは面倒ではありませんか？ そのテストの最後の処理でハンズオンのプロジェクトを自動が F5 されるようにしてみたらどうでしょう!?
    //    リフレッシュ情報dfprop:refreshDefinitionMap.dfprop
    //    dfprop読み込みAPI:DfPropFile InputStream から Map で読み込み
    //    F5 API (リフレッシュAPI):DfRefreshResourceRequest @since DBFlute-1.0.4F
    public void test_cursor_TSVにデータ出力() throws Exception {
        // ## Arrange ##
        HandsOn03Test logic = new HandsOn03Test();
        inject(logic);

        // ## Act ##
        logic.test_cursor();

        // ## Assert ##
        File tsvFile = new File(DfResourceUtil.getBuildDir(getClass()).getParent(), "hands-on-cb-bonus.csv"); // input file
        FileToken fileToken = new FileToken();

        fileToken.tokenize(new FileInputStream(tsvFile), new FileTokenizingCallback() {
            public void handleRow(FileTokenizingRowResource resource) throws IOException, SQLException {
                log(resource.toColumnValueMap());
            }
        }, new FileTokenizingOption().delimitateByComma().encodeAsUTF8());

        // 自動F5
        DfPropFile propFile = new DfPropFile();
        Map<String, Object> refMap = propFile.readMap(DfResourceUtil.getBuildDir(getClass()).getParent()
                + "/../dbflute_exampledb/dfprop/refreshDefinitionMap.dfprop", null);
        List<String> projectNameList = new ArrayList<String>();
        projectNameList.add(refMap.get("projectName").toString());
        DfRefreshResourceRequest request = new DfRefreshResourceRequest(projectNameList, refMap.get("requestUrl")
                .toString());
        request.refreshResources();
    }

    public void test_callInOutProcedure_値がへんてこりんになっていること() {
        // ## Arrange ##
        HandsOn09Logic logic = new HandsOn09Logic();
        inject(logic);

        String foo = "foo";
        String bar = "bar";

        SpInOutParameterPmb pmb = new SpInOutParameterPmb();
        pmb.setVInVarchar(foo);
        pmb.setVInoutVarchar(bar);

        // ## Act ##
        logic.callInOutProcedure(pmb);

        // ## Assert ##
        String vInoutVarchar = pmb.getVInoutVarchar();
        String vOutVarchar = pmb.getVOutVarchar();
        log("Out:" + vOutVarchar + ". InOut:" + vInoutVarchar);
        assertTrue(foo.equals(vInoutVarchar));
        assertTrue(bar.equals(vOutVarchar));
    }

    public void test_callResultSetProcedure_検索結果が取得できてること() {
        // ## Arrange ##
        HandsOn09Logic logic = new HandsOn09Logic();
        inject(logic);

        SpReturnResultSetPmb pmb = new SpReturnResultSetPmb();
        HandyDate targetDate = new HandyDate("1968-01-01");
        pmb.setBirthdateFrom(targetDate.getDate());

        // ## Act ##
        logic.callResultSetProcedure(pmb);

        // ## Assert ##
        Map<String, String> statusMap = new HashMap<String, String>();

        List<SpReturnResultSetNotParamResult2> statusList = pmb.getNotParamResult2();
        assertHasAnyElement(statusList);
        for (SpReturnResultSetNotParamResult2 status : statusList) {
            String code = status.getMemberStatusCode();
            String name = status.getMemberStatusName();
            log("Code:" + code + ". Name:" + name);
            statusMap.put(code, name);
        }

        List<SpReturnResultSetNotParamResult1> memberList = pmb.getNotParamResult1();
        assertHasAnyElement(memberList);
        for (SpReturnResultSetNotParamResult1 member : memberList) {
            // 会員名称と会員ステータス名称を一行のログで出力すること
            log("Name:" + member.getMemberName() + ". Birth:" + member.getBirthdate() + " Status:"
                    + statusMap.get(member.getMemberStatusCode()));
            // 生年月日が1968年以降であることをアサート
            assertTrue(targetDate.isLessEqual(member.getBirthdate()));
        }
    }
}