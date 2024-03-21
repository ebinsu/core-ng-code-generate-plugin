package core.framework.plugin.generator.collection;

import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiType;
import core.framework.plugin.generator.bean.BeanDefinition;
import core.framework.plugin.utils.ClassUtils;

/**
 * @author ebin
 */
public class CollectToMapPopupStep extends SelectKeyPopupStep {
    private static final String TEMPLATE = "%1$s().stream().collect(Collectors.toMap(k -> k.%2$s, Function.identity()));";
    private static final String STEAM_TEMPLATE = "%1$s().collect(Collectors.toMap(k -> k.%2$s, Function.identity()));";

    public CollectToMapPopupStep(BeanDefinition beanDefinition, String variableName, Project project, Editor editor, PsiType type) {
        super(beanDefinition, variableName, project, editor, type);
    }

    @Override
    public String getTemplate() {
        return ClassUtils.isStream(type.getCanonicalText()) ? STEAM_TEMPLATE : TEMPLATE;
    }
}
