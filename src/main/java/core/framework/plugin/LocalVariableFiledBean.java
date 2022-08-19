package core.framework.plugin;

import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiField;
import com.intellij.psi.PsiPrimitiveType;
import com.intellij.psi.PsiType;

import static core.framework.plugin.SetBeanPropertiesGenerator.JAVA_PACKAGE;

/**
 * @author ebin
 */
public class LocalVariableFiledBean {
    public PsiField field;
    public PsiType type;
    public PsiClass typeClass;

    public LocalVariableFiledBean(PsiField field, PsiType type, PsiClass typeClass) {
        this.field = field;
        this.type = type;
        this.typeClass = typeClass;
    }

    public PsiType getType() {
        return type;
    }

    public PsiClass getTypeClass() {
        return typeClass;
    }

    public String getName() {
        return field.getName();
    }

    public boolean isEnum() {
        return typeClass != null && typeClass.isEnum();
    }

    public boolean isList() {
        return "java.util.List".equals(type.getCanonicalText());
    }

    public boolean isSet() {
        return "java.util.Set".equals(type.getCanonicalText());
    }

    public boolean isJavaBean() {
        return !(type instanceof PsiPrimitiveType) && !type.getCanonicalText().startsWith(JAVA_PACKAGE);
    }
}
