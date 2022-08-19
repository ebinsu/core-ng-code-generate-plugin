package core.framework.plugin;

import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiField;
import com.intellij.psi.PsiType;

import java.util.HashMap;
import java.util.Map;

import static core.framework.plugin.SetBeanPropertiesGenerator.DISPLAY_NAME_SPLIT;

/**
 * @author ebin
 */
public class LocalVariableBean {
    public PsiType type;
    public PsiClass typeClass;
    public String name;
    public String displayName;
    public Map<String, String> fields = new HashMap<>();

    public LocalVariableBean(PsiType type, PsiClass typeClass, String name) {
        this.type = type;
        this.typeClass = typeClass;
        this.name = name;
        this.displayName = type.getPresentableText() + DISPLAY_NAME_SPLIT + name;
        PsiField[] classFields = typeClass.getFields();
        for (PsiField field : classFields) {
            fields.put(field.getName(), field.getType().getCanonicalText());
        }
    }

    public boolean isSameVariableType(PsiField targetFiled) {
        String filedName = fields.get(targetFiled.getName());
        return targetFiled.getType().getCanonicalText().equals(filedName);
    }
}
