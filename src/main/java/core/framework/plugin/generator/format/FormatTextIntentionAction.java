package core.framework.plugin.generator.format;

import com.intellij.codeInsight.intention.IntentionAction;
import com.intellij.codeInsight.intention.PsiElementBaseIntentionAction;
import com.intellij.codeInspection.util.IntentionFamilyName;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.popup.JBPopupFactory;
import com.intellij.openapi.ui.popup.ListPopup;
import com.intellij.psi.PsiElement;
import com.intellij.util.IncorrectOperationException;
import core.framework.plugin.generator.bean.SetBeanPropertiesBaseListPopupStep;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;

/**
 * @author ebin
 */
public class FormatTextIntentionAction extends PsiElementBaseIntentionAction implements IntentionAction {
    @Override
    public void invoke(@NotNull Project project, Editor editor, @NotNull PsiElement element) throws IncorrectOperationException {
        if (editor == null) {
            return;
        }
        if (editor.getVirtualFile() == null) {
            return;
        }
        String selectedText = editor.getSelectionModel().getSelectedText();
        if (selectedText == null) {
            return;
        }

        JBPopupFactory jbPopupFactory = JBPopupFactory.getInstance();
        ListPopup listPopup = jbPopupFactory.createListPopup(new FormatTextPopupStep(project, editor, selectedText));
        listPopup.showInBestPositionFor(editor);
    }

    @Override
    public boolean isAvailable(@NotNull Project project, Editor editor, @NotNull PsiElement element) {
        String selectedText = editor.getSelectionModel().getSelectedText();
        if (!StringUtils.isEmpty(selectedText)) {
            return selectedText.contains("_") || selectedText.contains("-") || selectedText.contains(" ") || hasUpperCase(selectedText);
        }
        return false;
    }

    @Override
    public @NotNull @IntentionFamilyName String getFamilyName() {
        return "* Format text";
    }


    protected boolean hasUpperCase(String text) {
        if (text == null) {
            return false;
        }

        int len = text.length();
        for (int i = 0; i < len; i++) {
            char ch = text.charAt(i);
            if (Character.isUpperCase(ch)) {
                return true;
            }
        }
        return false;
    }

    @NotNull
    public String getText() {
        return getFamilyName();
    }
}
