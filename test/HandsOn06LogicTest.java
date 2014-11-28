package org.dbflute.handson.logic;

import java.util.List;
import java.util.regex.Pattern;

import org.dbflute.handson.dbflute.exentity.Member;
import org.dbflute.handson.unit.UnitContainerTestCase;

/**
 * @author nakamura
 */
public class HandsOn06LogicTest extends UnitContainerTestCase {

    public void test_selectSuffixMemberList_指定したsuffixで検索されること() {
        // ## Arrange ##
        HandsOn06Logic logic = new HandsOn06Logic();
        inject(logic);

        // ## Act ##
        List<Member> memberList = logic.selectSuffixMemberList("vic");

        // ## Assert ##
        // 正規表現で末尾がvicであることをassert
        String regex = ".*vic";
        Pattern pattern = Pattern.compile(regex);

        assertHasAnyElement(memberList);
        for (Member member : memberList) {
            log("Name:" + member.getMemberName());
            assertTrue(pattern.matcher(member.getMemberName()).find());
        }
    }

    public void test_selectSuffixMemberList_suffixが無効な値なら例外が発生すること() {
        // ## Arrange ##
        HandsOn06Logic logic = new HandsOn06Logic();
        inject(logic);

        // 無効な値とは、nullと空文字とトリムして空文字になる値
        // おもいで
        // boolean checkNull = false;
        // boolean checkBlank = false;
        // boolean checkTrimBlank = false;

        // ## Act ##
        try {
            logic.selectSuffixMemberList(null);
            fail();
        } catch (IllegalArgumentException e) {
            // checkNull = true;
            // OK
        }

        try {
            logic.selectSuffixMemberList("");
            fail();
        } catch (IllegalArgumentException e) {
            // checkBlank = true;
            // OK
        }

        try {
            logic.selectSuffixMemberList("      ");
            fail();
        } catch (IllegalArgumentException e) {
            // checkTrimBlank = true;
            // OK
        }

        // assertTrue(checkNull);
        // assertTrue(checkBlank);
        // assertTrue(checkTrimBlank);
    }
}
