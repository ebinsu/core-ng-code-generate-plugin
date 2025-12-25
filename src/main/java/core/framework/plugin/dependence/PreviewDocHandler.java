package core.framework.plugin.dependence;

import com.intellij.openapi.util.TextRange;
import com.intellij.openapi.vfs.VirtualFile;
import org.apache.commons.lang3.tuple.Pair;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * @author ebin
 */
public class PreviewDocHandler implements IDocHandler {
    Map<String, List<Pair<String, String>>> changeHistories;

    public PreviewDocHandler(Map<String, List<Pair<String, String>>> changeHistories) {
        this.changeHistories = changeHistories;
    }

    @Override
    public void handle(DocHolder docHolder, TextRange textRange, String oldDef, String newDef) {
        String fileName = getFileName(docHolder.virtualFile());
        if (!oldDef.trim().equals(newDef.trim())) {
            changeHistories.computeIfAbsent(fileName, k -> new ArrayList<>()).add(Pair.of(oldDef, newDef));
        }
    }

    private String getFileName(VirtualFile virtualFile) {
        String parent = "";
        if (virtualFile.getParent() != null) {
            String parentUrl = virtualFile.getParent().toString();
            int parentBegin = parentUrl.lastIndexOf("/");
            if (parentBegin != -1) {
                parent = parentUrl.substring(parentBegin + 1);
            }
        }
        String string = virtualFile.toString();
        int begin = string.lastIndexOf("/");
        if (begin != -1) {
            return parent + string.substring(begin);
        } else {
            return string;
        }
    }
}
