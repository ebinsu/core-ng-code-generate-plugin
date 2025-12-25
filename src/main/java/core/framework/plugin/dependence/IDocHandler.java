package core.framework.plugin.dependence;

import com.intellij.openapi.util.TextRange;

/**
 * @author ebin
 */
public interface IDocHandler {
    void handle(DocHolder docHolder, TextRange textRange, String oldDef, String newDef);
}
