package core.framework.plugin;

import com.intellij.codeInsight.intention.IntentionAction;
import com.intellij.codeInsight.intention.PsiElementBaseIntentionAction;
import com.intellij.codeInspection.util.IntentionFamilyName;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.fileEditor.FileDocumentManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.TextRange;
import com.intellij.openapi.vfs.VfsUtil;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiElement;
import com.intellij.testFramework.LightVirtualFile;
import com.intellij.util.IncorrectOperationException;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.io.IOException;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

/**
 * @author ebin
 */
public class CopyEnvPropertiesIntentionAction extends PsiElementBaseIntentionAction implements IntentionAction {
    private static final String LOCAL_ENV_PATH = "/src/main/resources";

    @Override
    public void invoke(@NotNull Project project, Editor editor, @NotNull PsiElement element) throws IncorrectOperationException {
        FileDocumentManager documentManager = FileDocumentManager.getInstance();
        VirtualFile virtualFile = getVirtualFile(editor, documentManager);
        if (virtualFile == null) {
            return;
        }
        String currentFilePath = virtualFile.getPath();

        Document document = editor.getDocument();
        TextRange selectionRange = editor.getCaretModel().getPrimaryCaret().getSelectionRange();
        int startLine = document.getLineNumber(selectionRange.getStartOffset());
        int endLine = document.getLineNumber(selectionRange.getEndOffset());
        String text = document.getText(new TextRange(document.getLineStartOffset(startLine), document.getLineEndOffset(endLine)));
        Properties toBeCopy = new Properties();
        try {
            toBeCopy.load(new StringReader(text));
        } catch (IOException ignored) {
        }
        if (toBeCopy.isEmpty()) {
            return;
        }
        List<String> selections = new ArrayList<>(toBeCopy.size());
        toBeCopy.keySet().stream().sorted().forEach(k -> selections.add((String) k + "=" + (String) toBeCopy.get(k)));

        CopyEnvPropertiesDialog dialog = new CopyEnvPropertiesDialog(selections, currentFilePath, project);
        dialog.show();
    }

    private VirtualFile getVirtualFile(Editor editor, FileDocumentManager documentManager) {
        VirtualFile virtualFile = documentManager.getFile(editor.getDocument());
        if (virtualFile instanceof LightVirtualFile lightVirtualFile) {
            return lightVirtualFile.getOriginalFile();
        } else {
            return virtualFile;
        }
    }

    @Override
    public boolean isAvailable(@NotNull Project project, Editor editor, @NotNull PsiElement element) {
        VirtualFile virtualFile = editor.getVirtualFile();
        if (virtualFile == null) {
            return false;
        }
        String filename = virtualFile.getName();
        if (!isPropertiesFile(filename)) {
            return false;
        }
        String currentFilePath = virtualFile.getPath();
        if (!isEnvFile(currentFilePath)) {
            return false;
        }
        Document document = editor.getDocument();
        TextRange selectionRange = editor.getCaretModel().getPrimaryCaret().getSelectionRange();
        int startLine = document.getLineNumber(selectionRange.getStartOffset());
        int endLine = document.getLineNumber(selectionRange.getEndOffset());
        String text = document.getText(new TextRange(document.getLineStartOffset(startLine), document.getLineEndOffset(endLine)));
        if (StringUtils.isEmpty(text)) {
            return false;
        }
        try {
            new Properties().load(new StringReader(text));
        } catch (IOException ignored) {
            return false;
        }
        return true;
    }

    @Override
    public @NotNull @IntentionFamilyName String getFamilyName() {
        return "Copy select properties to other env properties";
    }

    @NotNull
    public String getText() {
        return getFamilyName();
    }

    private boolean isPropertiesFile(String name) {
        String extension = FilenameUtils.getExtension(name);
        return extension.equals("properties");
    }


    private boolean isEnvFile(String path) {
        if (path.contains(LOCAL_ENV_PATH)) {
            return true;
        } else
            return path.contains("/resources") && (path.contains("dev") || path.contains("uat") || path.contains("prod"));
    }

    private Document getDoc(FileDocumentManager documentManager, String path) {
        Document doc = null;
        VirtualFile localFile = VfsUtil.findFileByIoFile(new File(path), true);
        if (localFile != null) {
            doc = documentManager.getDocument(localFile);
        }
        return doc;
    }

    @Override
    public boolean startInWriteAction() {
        return false;
    }
}
