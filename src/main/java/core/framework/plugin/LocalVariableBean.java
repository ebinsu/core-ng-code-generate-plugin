package core.framework.plugin;

import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiField;
import com.intellij.psi.PsiType;

import static core.framework.plugin.SetBeanPropertiesGenerator.DISPLAY_NAME_SPLIT;

/**
 * @author ebin
 */
public class LocalVariableBean {
    public PsiType type;
    public PsiClass typeClass;
    public String name;
    public String displayName;

    public LocalVariableBean(PsiType type, PsiClass typeClass, String name) {
        this.type = type;
        this.typeClass = typeClass;
        this.name = name;
        this.displayName = type.getPresentableText() + DISPLAY_NAME_SPLIT + name;
    }

    public boolean isSameVariableType(PsiField targetFiled) {
        PsiField[] fields = typeClass.getFields();
        for (PsiField field : fields) {
            if (field.getName().equals(targetFiled.getName())) {
                return field.getType().getCanonicalText().equals(targetFiled.getType().getCanonicalText());
            }
        }
        return false;
    }
}
