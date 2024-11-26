package core.framework.plugin.generator.format;

import com.google.common.base.CaseFormat;
import com.intellij.codeInspection.util.IntentionFamilyName;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiElement;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;

/**
 * @author ebin
 */
public class FormatUnderLineToCamelIntentionAction extends AbstractFormatIntentionAction {
    @Override
    public boolean isAvailable(@NotNull Project project, Editor editor, @NotNull PsiElement element) {
        String selectedText = editor.getSelectionModel().getSelectedText();
        if (!StringUtils.isEmpty(selectedText)) {
            return selectedText.contains("_");
        }
        return false;
    }

    @Override
    public @NotNull @IntentionFamilyName String getFamilyName() {
        return "* Format underline to camel";
    }

    @Override
    protected String formatString(String selectedText) {
        return CaseFormat.LOWER_UNDERSCORE.to(CaseFormat.LOWER_CAMEL, selectedText);
    }
}
