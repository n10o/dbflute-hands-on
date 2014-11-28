/*
 [df:title]
  月別購入サマリー(外だしSQLでカーソル検索ってみる)

 [df:description]
"さらに外だし" と同じ仕様(ページングは不要)
"会員と購入月" ごとの購入の平均購入価格、合計購入数量を検索する
会員ID、会員名称、購入月、平均購入価格、合計購入数量という形で検索
支払済みの購入だけでも検索できるようにする
会員名称の曖昧検索(部分一致)ができるようにする
会員のサービスポイント数の大なり条件が指定できるようにする
会員IDの昇順、購入月の降順で並べる

カーソル検索として作成すること
明らかにおかしいカラム名のカラムは取得してはいけない (後は何を取ってもOKですが、これだけはっ)

*/

-- #df:entity#
-- +cursor+

-- !df:pmb!
-- !!AutoDetect!!

select mem.MEMBER_ID -- // メンバーが持っているであろうIDらしきもの
	 , ser.MEMBER_SERVICE_ID -- // メンバーが持っているサービスの中に振られたあなただけの特別なID
     , mem.MEMBER_NAME -- // メンバーをメンバーたらしめる何か
     , DATE_FORMAT(pur.PURCHASE_DATETIME, '%Y%m') as purchaseDate -- // 月ごとの購入日
     , AVG(pur.PURCHASE_PRICE) as avgPrice -- // 平均価格 高ければリッチで安ければリッチではない
     , COUNT(*) as numOfPurchase -- // 購入した合計数
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
        , ser.MEMBER_SERVICE_ID
        , mem.MEMBER_NAME
        , DATE_FORMAT(pur.PURCHASE_DATETIME, '%Y%m')
 order by mem.MEMBER_ID asc
        , DATE_FORMAT(pur.PURCHASE_DATETIME, '%Y%m') desc
