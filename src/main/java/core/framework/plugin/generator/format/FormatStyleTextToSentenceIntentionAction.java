package core.framework.plugin.generator.format;

import com.intellij.codeInspection.util.IntentionFamilyName;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiElement;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;

/**
 * @author ebin
 */
public class FormatStyleTextToSentenceIntentionAction extends AbstractFormatIntentionAction {
    @Override
    public boolean isAvailable(@NotNull Project project, Editor editor, @NotNull PsiElement element) {
        String selectedText = editor.getSelectionModel().getSelectedText();
        if (!StringUtils.isEmpty(selectedText)) {
            return selectedText.contains("_") || selectedText.contains("-") || hasUpperCase(selectedText);
        }
        return false;
    }

    @Override
    public @NotNull @IntentionFamilyName String getFamilyName() {
        return "* Format style text to sentence";
    }

    @Override
    protected String formatString(String selectedText) {
        return selectedText
            // 1. 驼峰边界：lower->Upper
            .replaceAll("([a-z])([A-Z])", "$1 $2")
            // 2. 驼峰边界：ALLCAPS->Capital
            .replaceAll("([A-Z]+)([A-Z][a-z])", "$1 $2")
            // 3. _ 和 - 替换为空格
            .replace('_', ' ')
            .replace('-', ' ')
            // 4. 多空格压缩
            .replaceAll("\\s+", " ")
            // 5. 统一小写
            .toLowerCase()
            .trim();
    }
}
