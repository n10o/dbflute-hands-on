/*
 [df:title]
  月別購入サマリー

 [df:description]
"会員と購入月" ごとの購入の平均購入価格、合計購入数量を検索する
会員ID、会員名称、購入月、平均購入価格、合計購入数量という形で検索
支払済みの購入だけでも検索できるようにする
会員名称の曖昧検索(部分一致)ができるようにする
会員のサービスポイント数の大なり条件が指定できるようにする
会員IDの昇順、購入月の降順で並べる
*/

-- #df:entity#

-- !df:pmb extends Paging!
-- !!AutoDetect!!

/*IF pmb.isPaging()*/
select mem.MEMBER_ID -- // メンバーが持っているであろうIDらしきもの
     , mem.MEMBER_NAME -- // メンバーをメンバーたらしめる何か
     , DATE_FORMAT(pur.PURCHASE_DATETIME, '%Y%m') as purchaseDate -- // 月ごとの購入日
     , AVG(pur.PURCHASE_PRICE) as avgPrice -- // 平均価格 高ければリッチで安ければリッチではない
     , COUNT(*) as numOfPurchase -- // 購入した合計数
-- ELSE select count(*)
/*END*/
  from PURCHASE pur
    left outer join MEMBER mem
      on mem.MEMBER_ID = pur.MEMBER_ID
    left outer join MEMBER_SERVICE ser
      on mem.MEMBER_ID = ser.MEMBER_ID
/*BEGIN*/
 where
    /*IF pmb.paymentCompleteFlg != null*/
    pur.PAYMENT_COMPLETE_FLG = /*pmb.paymentCompleteFlg*/0
    /*END*/
    /*IF pmb.memberName != null*/
 	and mem.MEMBER_NAME like /*pmb.memberName*/'%M%'
    /*END*/
    /*IF pmb.servicePointCount != null*/
 	and ser.SERVICE_POINT_COUNT > /*pmb.servicePointCount*/0
    /*END*/
/*END*/
 group by mem.MEMBER_ID
        , mem.MEMBER_NAME
        , purchaseDate
/*IF pmb.isPaging()*/
 order by mem.MEMBER_ID asc
        , purchaseDate desc
 limit /*$pmb.pageStartIndex*/80, /*$pmb.fetchSize*/20
/*END*/