package core.framework.plugin.api.link;

import java.util.UUID;

/**
 * @author ebin
 */
public class KibanaLink {
    public static final String TPL = "app/discover#/?_tab=(tabId:%1$s)&_g=(filters:!(),refreshInterval:(pause:!t,value:60000),time:(from:now-15m,to:now))&_a=(columns:!(),dataSource:(dataViewId:action-pattern,type:dataView),filters:!(),hideChart:!t,interval:auto,query:(language:kuery,query:'app:\"%2$s\" and action:\"%3$s\" '),sort:!(!('@timestamp',desc)))";
    public String env;
    public String url;

    public KibanaLink(String env, String serviceName, String action) {
        String baseUrl;
        if (env.equals("qa")) {
            baseUrl = "https://kibana.foodtruck-qa.com/";
        } else if (env.equals("uat")) {
            baseUrl = "https://kibana.foodtruck-uat.com/";
        } else {
            baseUrl = "https://kibana.remarkablefoods.net/";
        }
        this.env = env;
        this.url = baseUrl + String.format(TPL, UUID.randomUUID(), serviceName, action);
    }
}
