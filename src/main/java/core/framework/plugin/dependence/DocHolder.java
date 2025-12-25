package core.framework.plugin.dependence;

import com.intellij.openapi.editor.Document;
import com.intellij.openapi.vfs.VirtualFile;

/**
 * @author ebin
 */
public record DocHolder(Document document, VirtualFile virtualFile) {
}
