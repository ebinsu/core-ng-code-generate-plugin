package core.framework.plugin.generator.format;

import com.intellij.codeInsight.intention.IntentionAction;
import com.intellij.codeInsight.intention.PsiElementBaseIntentionAction;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiElement;
import com.intellij.util.IncorrectOperationException;
import org.jetbrains.annotations.NotNull;

/**
 * @author ebin
 */
public abstract class AbstractFormatIntentionAction extends PsiElementBaseIntentionAction implements IntentionAction {
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
        Document document = editor.getDocument();
        document.replaceString(
            editor.getSelectionModel().getSelectionStart(),
            editor.getSelectionModel().getSelectionEnd(),
            formatString(selectedText)
        );
    }

    protected abstract String formatString(String selectedText);

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
