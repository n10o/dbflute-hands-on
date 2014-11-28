package org.dbflute.handson.logic;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import javax.annotation.Resource;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;

import org.dbflute.handson.dbflute.cbean.MemberServiceCB;
import org.dbflute.handson.dbflute.exbhv.MemberBhv;
import org.dbflute.handson.dbflute.exbhv.MemberServiceBhv;
import org.dbflute.handson.dbflute.exbhv.PurchaseBhv;
import org.dbflute.handson.dbflute.exbhv.cursor.PurchaseMonthCursorCursor;
import org.dbflute.handson.dbflute.exbhv.cursor.PurchaseMonthCursorCursorHandler;
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
import org.seasar.dbflute.bhv.UpdateOption;
import org.seasar.dbflute.cbean.PagingResultBean;
import org.seasar.dbflute.cbean.SpecifyQuery;
import org.seasar.dbflute.helper.token.file.FileMakingCallback;
import org.seasar.dbflute.helper.token.file.FileMakingOption;
import org.seasar.dbflute.helper.token.file.FileMakingRowWriter;
import org.seasar.dbflute.helper.token.file.FileToken;
import org.seasar.dbflute.jdbc.StatementConfig;
import org.seasar.dbflute.util.DfResourceUtil;

/**
 * @author nakamura
 */
public class HandsOn09Logic {

    @Resource
    protected MemberBhv memberBhv;

    @Resource
    protected PurchaseBhv purchaseBhv;

    @Resource
    protected MemberServiceBhv memberServiceBhv;

    public List<OutsideMember> letsOutside(OutsideMemberPmb pmb) {
        if (pmb == null) {
            throw new IllegalArgumentException("Argument 'pmb' should not be null");
        }

        return memberBhv.outsideSql().selectList(pmb);
    }

    public PagingResultBean<PartOfMember> selectPartOfMember(PartOfMemberPmb pmb) {
        if (pmb == null) {
            throw new IllegalArgumentException("Argument 'pmb' should not be null");
        }

        return memberBhv.outsideSql().manualPaging().selectPage(pmb);
    }

    public PagingResultBean<PurchaseMonthSummary> selectLetsSummary(PurchaseMonthSummaryPmb pmb) {
        if (pmb == null) {
            throw new IllegalArgumentException("Argument 'pmb' should not be null");
        }

        // ボーナスステージ　SQLをページング検索に
        // return purchaseBhv.outsideSql().selectList(pmb);
        return purchaseBhv.outsideSql().manualPaging().selectPage(pmb);
    }

    public void selectLetsCursor(final PurchaseMonthCursorPmb pmb) {
        if (pmb == null) {
            throw new IllegalArgumentException("Argument 'pmb' should not be null");
        }

        StatementConfig conf = new StatementConfig().fetchSize(Integer.MIN_VALUE);

        // 平均購入価格の分、その会員のサービスポイント数を足す
        //　足す際、パフォーマンス考慮のために事前selectはせず、updateだけで足す (varyingUpdat...)
        purchaseBhv.outsideSql().configure(conf).cursorHandling()
                .selectCursor(pmb, new PurchaseMonthCursorCursorHandler() {
                    protected Object fetchCursor(final PurchaseMonthCursorCursor cursor) throws SQLException {
                        File tsvFile = new File(DfResourceUtil.getBuildDir(getClass()).getParent(),
                                "hands-on-outside-bonus.csv");
                        List<String> columnNameList = new ArrayList<String>();
                        columnNameList.add("会員名称");
                        columnNameList.add("購入月");
                        columnNameList.add("合計購入数量");

                        // ソースが読めない現象再び -> sources.jarをzipにしたらなぜか読めるように
                        FileToken fileToken = new FileToken();
                        try {
                            fileToken.make(new FileOutputStream(tsvFile), new FileMakingCallback() {
                                public void write(FileMakingRowWriter writer) throws IOException, SQLException {
                                    while (cursor.next()) {
                                        addServicePoint(cursor);
                                        List<String> valueList = new ArrayList<String>();
                                        valueList.add(cursor.getMemberName());
                                        valueList.add(cursor.getPurchaseDate());
                                        valueList.add(cursor.getNumOfPurchase().toString());
                                        writer.writeRow(valueList);
                                    }
                                }
                            }, new FileMakingOption().delimitateByComma().encodeAsUTF8().headerInfo(columnNameList));
                        } catch (FileNotFoundException e) {
                            throw new IllegalStateException("Not found the file: " + tsvFile + ", pmb=" + pmb, e);
                        } catch (IOException e) {
                            throw new IllegalStateException("IOException", e); // IOExceptionをそのまま投げずに、ラップする理由はなんでしょう？ -> 仕様上それしかできない
                        }

                        return null;
                    }
                });
    }

    // mainとして動かすときには、以下のアノテーションが必要
    @TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
    public void addServicePoint(PurchaseMonthCursorCursor cursor) throws SQLException {
        MemberService service = new MemberService();
        service.setMemberId(cursor.getMemberId());
        service.setMemberServiceId(cursor.getMemberServiceId());
        // service.setAkirakaniOkashiiKaramuMei(cursor.getAvgPrice().intValue()); // DB変更によるカラム名の変更

        UpdateOption<MemberServiceCB> option = new UpdateOption<MemberServiceCB>();
        option.self(new SpecifyQuery<MemberServiceCB>() {
            public void specify(MemberServiceCB cb) {
                //cb.specify().columnAkirakaniOkashiiKaramuMei(); // DB変更によるカラム名の変更
                cb.specify().columnServicePointCount();
            }
        }).plus(cursor.getAvgPrice());

        memberServiceBhv.varyingUpdateNonstrict(service, option);
    }

    // show procedure statusで登録済みのストアドプロシージャを閲覧可能
    public void callInOutProcedure(SpInOutParameterPmb pmb) {
        memberBhv.outsideSql().call(pmb);
    }

    public void callResultSetProcedure(SpReturnResultSetPmb pmb) {
        memberBhv.outsideSql().call(pmb);
    }
}
