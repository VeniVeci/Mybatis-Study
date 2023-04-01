package SQLBuilderTest;

import org.apache.ibatis.jdbc.SQL;
import org.junit.Test;

/**
 * @author PaiMon
 * @program: Mybatis-Study
 * @description:
 */
public class SQLTest {
    public static void main(String[] args) {
        String selectPersonSql = new SQLTest().selectPersonSql();

        System.out.println(selectPersonSql);
    }
    private String selectPersonSql() {
        return new SQL() {{
            SELECT("P.ID, P.USERNAME, P.PASSWORD, P.FULL_NAME");
            SELECT("P.LAST_NAME, P.CREATED_ON, P.UPDATED_ON");
            FROM("PERSON P");
            FROM("ACCOUNT A");
            INNER_JOIN("DEPARTMENT D on D.ID = P.DEPARTMENT_ID");
            INNER_JOIN("COMPANY C on D.COMPANY_ID = C.ID");
            WHERE("P.ID = A.ID");
            WHERE("P.FIRST_NAME like ?");
            OR();
            WHERE("P.LAST_NAME like ?");
            GROUP_BY("P.ID");
            HAVING("P.LAST_NAME like ?");
            OR();
            HAVING("P.FIRST_NAME like ?");
            ORDER_BY("P.ID");
            ORDER_BY("P.FULL_NAME");
        }}.toString();
    }

    @Test
    public void test1(){
        String s = new SQLTest().selectPersonLike("111", "kiki111", "alex");
        System.out.println(s);
    }
    // 动态条件（注意参数需要使用 final 修饰，以便匿名内部类对它们进行访问）
    public String selectPersonLike(final String id, final String firstName, final String lastName) {
        return new SQL() {{
            SELECT("P.ID, P.USERNAME, P.PASSWORD, P.FIRST_NAME, P.LAST_NAME");
            FROM("PERSON P");
            if (id != null) {
                WHERE("P.ID like #{id}");
            }
            if (firstName != null) {
                WHERE("P.FIRST_NAME like #{firstName}");
            }
            if (lastName != null) {
                WHERE("P.LAST_NAME like #{lastName}");
            }
            ORDER_BY("P.LAST_NAME");
        }}.toString();
    }
}
