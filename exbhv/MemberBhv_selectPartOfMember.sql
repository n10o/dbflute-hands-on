/*
 [df:title]
 シンプルな会員検索（ 会員ID、会員名称、生年月日、会員ステータス名称、サービスポイント数を取得）

 [df:description]
(Manual)ページング検索すること
基本は "初めての外だしSQL" のSQLと同じで...
会員ステータスの等値条件は要らない
会員サービスのサービスポイント数の大なり条件を追加
結合に関して、カウント検索のパフォーマンスを最大限考慮すること
IFコメントに記述する条件が複雑にならないように (代理判定メソッドを使う)

*/

-- #df:entity#

-- !df:pmb extends Paging!
-- !!AutoDetect!!

/*IF pmb.isPaging()*/
select mem.MEMBER_ID
     , mem.MEMBER_NAME
     , mem.BIRTHDATE
     , stat.MEMBER_STATUS_NAME
     , serv.SERVICE_POINT_COUNT
-- ELSE select count(*)
/*END*/
  from MEMBER mem
    /*IF pmb.isPaging()*/
    left outer join MEMBER_STATUS stat
  		on mem.MEMBER_STATUS_CODE = stat.MEMBER_STATUS_CODE
  	/*END*/
    /*IF pmb.isUseService()*/
  	left outer join MEMBER_SERVICE serv
  		on mem.MEMBER_ID = serv.MEMBER_ID
  	/*END*/
/*BEGIN*/
 where
    /*IF pmb.memberId != null*/
 	mem.MEMBER_ID = /*pmb.memberId*/1
 	/*END*/
 	/*IF pmb.memberName != null*/
    and mem.MEMBER_NAME like /*pmb.memberName*/'%M%'
    /*END*/
    /*IF pmb.servicePointCount != null*/
 	and serv.SERVICE_POINT_COUNT > /*pmb.servicePointCount*/100
    /*END*/
/*END*/
/*IF pmb.isPaging()*/
  limit /*pmb.pageStartIndex*/80, /*pmb.fetchSize*/20
/*END*/
