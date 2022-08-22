package core.framework.plugin;

import org.apache.commons.lang.StringUtils;

/**
 * @author ebin
 */
public class StringTest {
    public static void main(String[] args) {
        String c = "agentEnum";
        System.out.println(StringUtils.getLevenshteinDistance("agentEnum", "a"));
        System.out.println(StringUtils.getLevenshteinDistance("agentEnum", "b"));
        System.out.println(StringUtils.getLevenshteinDistance("agentEnum", "c"));
        System.out.println(StringUtils.getLevenshteinDistance("agentEnum", "abc"));
        System.out.println(StringUtils.getLevenshteinDistance("agentEnum", "agentEn"));
        System.out.println(StringUtils.getLevenshteinDistance("agentEnum", "ag"));
        System.out.println(StringUtils.getLevenshteinDistance("agentEnum", "agentEnum1"));
        System.out.println(StringUtils.getLevenshteinDistance("orderId".toLowerCase(), "mp_order_id"));
    }
}
