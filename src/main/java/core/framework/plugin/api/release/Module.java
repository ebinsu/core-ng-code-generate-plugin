package core.framework.plugin.api.release;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;

/**
 * @author ebin
 */
@JsonNaming(PropertyNamingStrategies.LowerCamelCaseStrategy.class)
public class Module {
    public String name;
    public String artifactId;
    public String version;
}
