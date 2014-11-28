/*
 [df:title]
 シンプルな会員検索（ 会員ID、会員名称、生年月日、会員ステータス名称、サービスポイント数を取得）

 [df:description]
会員ID、会員名称、生年月日、会員ステータス名称、サービスポイント数(今は明らかにおかしいカラム名になっている)を取得
会員IDの等値、会員名称の前方一致、会員ステータスの等値を and で連結
それぞれ条件値がない場合は条件自体が無効になるように
全ての条件値がない場合は全件検索になるように

*/

-- #df:entity#

-- !df:pmb!
 -- !!AutoDetect!!

select mem.MEMBER_ID
     , mem.MEMBER_NAME
     , mem.BIRTHDATE
     , stat.MEMBER_STATUS_NAME
     , serv.SERVICE_POINT_COUNT
  from MEMBER mem
    left outer join MEMBER_STATUS stat
      on mem.MEMBER_STATUS_CODE = stat.MEMBER_STATUS_CODE
    left outer join MEMBER_SERVICE serv
  	  on mem.MEMBER_ID = serv.MEMBER_ID
/*BEGIN*/
 where
 	/*IF pmb.memberId != null*/
 	mem.MEMBER_ID = /*pmb.memberId:comment(メンバーのアイ・ディー)*/1
 	/*END*/
 	/*IF pmb.memberName != null*/
 	and mem.MEMBER_NAME like /*pmb.memberName:comment(メンバーのネーム)*/'M%'
 	/*END*/
 	/*IF pmb.memberStatusCode != null*/
 	and stat.MEMBER_STATUS_CODE = /*pmb.memberStatusCode:cls(MemberStatus)|comment(メンバーのステータスのネーム)*/'PRV'
 	/*END*/
/*END*/