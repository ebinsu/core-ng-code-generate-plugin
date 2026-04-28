package core.framework.plugin.api.dependence;

import com.intellij.openapi.util.TextRange;

/**
 * @author ebin
 */
public class WriteDocHandler implements IDocHandler {
    @Override
    public void handle(DocHolder docHolder, TextRange textRange, String oldDef, String newDef) {
        docHolder.document().replaceString(textRange.getStartOffset(), textRange.getEndOffset(), newDef);
    }
}
