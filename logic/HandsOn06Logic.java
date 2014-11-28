package org.dbflute.handson.logic;

import java.util.List;

import javax.annotation.Resource;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.dbflute.handson.dbflute.cbean.MemberCB;
import org.dbflute.handson.dbflute.exbhv.MemberBhv;
import org.dbflute.handson.dbflute.exentity.Member;
import org.seasar.dbflute.cbean.ListResultBean;
import org.seasar.dbflute.cbean.coption.LikeSearchOption;

/**
 * @author nakamura
 */
public class HandsOn06Logic {

    private static final Log LOG = LogFactory.getLog(HandsOn06Logic.class);

    @Resource
    protected MemberBhv memberBhv;

    public List<Member> selectSuffixMemberList(String suffix) {
        if (suffix == null) {
            throw new IllegalArgumentException("Argument Illegal(null)");
        }
        suffix = suffix.trim();
        if (suffix.length() == 0) {
            throw new IllegalArgumentException("Argument Illegal(Blank)");
        }

        MemberCB cb = new MemberCB();
        cb.query().setMemberName_LikeSearch(suffix, new LikeSearchOption().likeSuffix());
        cb.query().addOrderBy_MemberName_Asc();

        ListResultBean<Member> memberList = memberBhv.selectList(cb);
        if (LOG.isDebugEnabled()) { // これを入れないと重くなる
            for (Member member : memberList) {
                // 会員名称、生年月日、正式会員日時をログに出す。
                LOG.debug("Name:" + member.getMemberName() + ". Birth:" + member.getBirthdate() + ". FormalizedDate:"
                        + member.getFormalizedDatetime());
            }
        }

        return memberBhv.selectList(cb);
    }
}
