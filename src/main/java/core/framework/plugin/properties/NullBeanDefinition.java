package core.framework.plugin.properties;

/**
 * @author ebin
 */
public class NullBeanDefinition extends BeanDefinition {
    public NullBeanDefinition() {
        super();
    }

    @Override
    public String getDisplayName() {
        return "None";
    }
}
