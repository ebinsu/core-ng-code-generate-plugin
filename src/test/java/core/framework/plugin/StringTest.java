package core.framework.plugin;

import org.apache.commons.lang.StringUtils;

/**
 * @author ebin
 */
public class StringTest {
    public static void main(String[] args) {
        System.out.println(StringUtils.getLevenshteinDistance("agent", "a"));
        System.out.println(StringUtils.getLevenshteinDistance("agent", "b"));
        System.out.println(StringUtils.getLevenshteinDistance("agent", "c"));
        System.out.println(StringUtils.getLevenshteinDistance("agent", "abc"));
        System.out.println(StringUtils.getLevenshteinDistance("agent", "agentEn"));
        System.out.println(StringUtils.getLevenshteinDistance("agent", "ag"));
        System.out.println(StringUtils.getLevenshteinDistance("agent", "agentEnum"));

        System.out.println(StringUtils.getLevenshteinDistance("orderId", "wonderOrderId"));
        System.out.println(StringUtils.getLevenshteinDistance("orderId", "mpOrderId"));
        System.out.println(StringUtils.getLevenshteinDistance("orderId", "OrderId"));
        System.out.println(StringUtils.getLevenshteinDistance("orderAddress", "orderHDRAddress"));
    }
}
