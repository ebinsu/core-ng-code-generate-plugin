package core.framework.plugin;

import com.intellij.ide.highlighter.JavaFileType;
import com.intellij.lang.ASTNode;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.CommonDataKeys;
import com.intellij.openapi.actionSystem.PlatformDataKeys;
import com.intellij.openapi.command.WriteCommandAction;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.fileTypes.FileType;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import org.apache.commons.lang.StringUtils;
import org.jetbrains.annotations.NotNull;

/**
 * @author ebin
 */
public class WrapTryGenerator extends AnAction {
    public static final String TEMPLATE = """
            try {
                %1$s
            } catch (Exception e) {
                LOGGER.warn(Markers.errorCode("ERROR_CODE"), "error message", e);
            }
        """;

    @Override
    public void actionPerformed(@NotNull AnActionEvent e) {
        Project project = e.getData(PlatformDataKeys.PROJECT);
        PsiFile psiFile = e.getData(CommonDataKeys.PSI_FILE);
        if (project == null || psiFile == null) {
            return;
        }
        FileType fileType = psiFile.getFileType();
        if (!(fileType instanceof JavaFileType)) {
            return;
        }
        Editor editor = e.getData(PlatformDataKeys.EDITOR);
        if (editor == null) {
            return;
        }
        String selectedText = editor.getSelectionModel().getSelectedText();
        if (StringUtils.isEmpty(selectedText)) {
            return;
        }

        PsiElement element = psiFile.findElementAt(editor.getCaretModel().getOffset());
        if (element == null) {
            return;
        }
        PsiElement method = findMethod(element);
        if (method == null) {
            return;
        }
        WriteCommandAction.runWriteCommandAction(project, () -> {
            editor.getDocument().replaceString(editor.getSelectionModel().getSelectionStart(), editor.getSelectionModel().getSelectionEnd(), String.format(TEMPLATE, selectedText));
        });
    }

    private PsiElement findMethod(PsiElement statement) {
        PsiElement maybeMethod = statement;
        boolean isMethod;
        do {
            maybeMethod = maybeMethod.getParent();
            if (maybeMethod instanceof ASTNode) {
                isMethod = "METHOD".equals(((ASTNode) maybeMethod).getElementType().toString());
            } else {
                isMethod = true;
            }
        } while (!isMethod);
        return maybeMethod;
    }
}
