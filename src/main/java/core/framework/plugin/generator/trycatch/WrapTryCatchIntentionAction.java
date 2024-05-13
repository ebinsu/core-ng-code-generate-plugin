package core.framework.plugin.generator.trycatch;

import com.intellij.codeInsight.intention.IntentionAction;
import com.intellij.codeInsight.intention.PsiElementBaseIntentionAction;
import com.intellij.codeInspection.util.IntentionFamilyName;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.TextRange;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiField;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiManager;
import com.intellij.psi.impl.source.PsiJavaFileImpl;
import com.intellij.util.IncorrectOperationException;
import core.framework.plugin.utils.PsiUtils;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;

/**
 * @author ebin
 */
public class WrapTryCatchIntentionAction extends PsiElementBaseIntentionAction implements IntentionAction {
    public static final String TEMPLATE = """
            try {
                %1$s
            } catch (Exception e) {
                %2$s.warn(Markers.errorCode("ERROR_CODE"), "error message", e);
            }
        """;

    public static final String LOGGER_TEMPLATE = "    private static final Logger LOGGER = LoggerFactory.getLogger(%1$s.class);";
    public static final String LOGGER_CLASS_NAME = "org.slf4j.Logger";

    @Override
    public void invoke(@NotNull Project project, Editor editor, @NotNull PsiElement element) throws IncorrectOperationException {
        if (editor.getVirtualFile() == null) {
            return;
        }
        PsiFile psiFile = PsiManager.getInstance(project).findFile(editor.getVirtualFile());
        if (psiFile == null) {
            return;
        }
        PsiClass psiClass = ((PsiJavaFileImpl) psiFile).getClasses()[0];
        PsiField[] allFields = psiClass.getAllFields();
        String loggerVar = null;
        for (PsiField field : allFields) {
            if (LOGGER_CLASS_NAME.equals(field.getType().getCanonicalText())) {
                loggerVar = field.getName();
            }
        }
        String selectedText = editor.getSelectionModel().getSelectedText();
        Document document = editor.getDocument();
        document.replaceString(
            editor.getSelectionModel().getSelectionStart(),
            editor.getSelectionModel().getSelectionEnd(),
            String.format(TEMPLATE, selectedText, loggerVar == null ? "LOGGER" : loggerVar)
        );

        if (loggerVar == null) {
            String className = psiClass.getName();
            if (className == null) {
                return;
            }
            int lineCount = document.getLineCount();
            for (int i = 0; i < lineCount; i++) {
                TextRange textRange = new TextRange(document.getLineStartOffset(i), document.getLineEndOffset(i));
                String text = document.getText(textRange);
                if (text.contains(className)) {
                    document.insertString(document.getLineEndOffset(i), "\n");
                    document.insertString(document.getLineEndOffset(i + 1), String.format(LOGGER_TEMPLATE, psiClass.getName()));
                    break;
                }
            }
        }
    }

    @Override
    public boolean isAvailable(@NotNull Project project, Editor editor, @NotNull PsiElement element) {
        String selectedText = editor.getSelectionModel().getSelectedText();
        if (StringUtils.isEmpty(selectedText)) {
            return false;
        }
        return PsiUtils.findMethod(element).isPresent();
    }

    @Override
    public @NotNull @IntentionFamilyName String getFamilyName() {
        return "Wrap try catch";
    }


    @NotNull
    public String getText() {
        return getFamilyName();
    }

}
