package core.framework.plugin.generator.collection;

import com.intellij.openapi.command.WriteCommandAction;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.popup.PopupStep;
import com.intellij.openapi.ui.popup.util.BaseListPopupStep;
import com.intellij.openapi.util.TextRange;
import com.intellij.psi.PsiType;
import core.framework.plugin.generator.bean.BeanDefinition;
import core.framework.plugin.generator.bean.BeanField;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;

/**
 * @author ebin
 */
public abstract class SelectKeyPopupStep extends BaseListPopupStep<BeanField> {
    protected final String variableName;
    protected final Editor editor;
    protected final Project project;
    protected final PsiType type;

    public SelectKeyPopupStep(BeanDefinition beanDefinition, String variableName, Project project, Editor editor, PsiType type) {
        this.variableName = variableName;
        this.editor = editor;
        this.project = project;
        this.type = type;
        init("Select Key", new ArrayList<>(beanDefinition.fields.values()), null);
    }

    @Override
    public @NotNull String getTextFor(BeanField value) {
        return value.getDisplayName();
    }

    @Override
    public @Nullable PopupStep<?> onChosen(BeanField selectedValue, boolean finalChoice) {
        String format = String.format(getTemplate(), variableName, selectedValue.name);
        Document document = editor.getDocument();
        int lineNumber = document.getLineNumber(editor.getSelectionModel().getSelectionStart());
        int startOffset = document.getLineStartOffset(lineNumber);
        int endOffset = document.getLineEndOffset(lineNumber);
        String escapeExprSpecialWord = escapeExprSpecialWord(variableName);
        String line = editor.getDocument().getText(new TextRange(startOffset, endOffset)).replaceFirst("(?s)" + escapeExprSpecialWord + "(?!.*?" + escapeExprSpecialWord + ")", format);
        if (line.endsWith("()")) {
            line = line.substring(0, line.length() - 2);
        } else if (line.endsWith("();")) {
            line = line.substring(0, line.length() - 3);
        }
        String finalLine = line;
        WriteCommandAction.runWriteCommandAction(project, () -> document.replaceString(
            startOffset,
            endOffset,
            finalLine
        ));

        return super.onChosen(selectedValue, finalChoice);
    }

    public String escapeExprSpecialWord(String keyword) {
        if (StringUtils.isNotBlank(keyword)) {
            String[] fbsArr = {"\\", "$", "(", ")", "*", "+", ".", "[", "]", "?", "^", "{", "}", "|"};
            for (String key : fbsArr) {
                if (keyword.contains(key)) {
                    keyword = keyword.replace(key, "\\" + key);
                }
            }
        }
        return keyword;
    }

    public abstract String getTemplate();
}
