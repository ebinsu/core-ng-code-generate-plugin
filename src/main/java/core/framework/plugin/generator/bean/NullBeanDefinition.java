package core.framework.plugin.generator.bean;

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
