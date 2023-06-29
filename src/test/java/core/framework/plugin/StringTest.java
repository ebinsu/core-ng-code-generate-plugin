package core.framework.plugin;

import org.apache.commons.lang.StringUtils;

/**
 * @author ebin
 */
public class StringTest {
    public static void main(String[] args) {
        System.out.println(StringUtils.getLevenshteinDistance("orderId", "wonderOrderId"));
        System.out.println(StringUtils.getLevenshteinDistance("orderId", "mpOrderId"));
        System.out.println(StringUtils.getLevenshteinDistance("orderId", "OrderId"));
        System.out.println(StringUtils.getLevenshteinDistance("orderAddress", "orderHDRAddress"));
    }
}
