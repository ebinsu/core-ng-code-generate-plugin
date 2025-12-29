package core.framework.plugin.generator.bean;

import com.intellij.psi.PsiMethod;
import com.intellij.psi.PsiStatement;

import java.util.List;

/**
 * @author ebin
 */
public class StatementHolder {
    public List<PsiStatement> statements;
    public List<PsiMethod> addMethods;

    public StatementHolder(List<PsiStatement> statements, List<PsiMethod> addMethods) {
        this.statements = statements;
        this.addMethods = addMethods;
    }
}
