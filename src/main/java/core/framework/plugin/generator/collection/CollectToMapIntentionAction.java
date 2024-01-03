package core.framework.plugin.generator.collection;

import com.intellij.codeInspection.util.IntentionFamilyName;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.popup.JBPopupFactory;
import com.intellij.openapi.ui.popup.ListPopup;
import com.intellij.psi.PsiType;
import core.framework.plugin.generator.bean.BeanDefinition;
import org.jetbrains.annotations.NotNull;

/**
 * @author ebin
 */
public class CollectToMapIntentionAction extends AbstractMapIntentionAction {
    @Override
    protected ListPopup popup(@NotNull Project project, Editor editor, BeanDefinition beanDefinition, String name, PsiType type) {
        JBPopupFactory jbPopupFactory = JBPopupFactory.getInstance();
        return jbPopupFactory.createListPopup(new CollectToMapPopupStep(beanDefinition, name, project, editor, type));
    }

    @Override
    public @NotNull @IntentionFamilyName String getFamilyName() {
        return "Collect by map";
    }

}
