package core.framework.plugin.generator.bean;

import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiField;
import core.framework.plugin.utils.PsiUtils;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * @author ebin
 */
public class BeanDefinition {
    public static final String DISPLAY_NAME_SPLIT = " ";

    public String className;
    public String variableName;
    public String displayName;

    public Map<String, BeanField> fields = new LinkedHashMap<>();

    public BeanDefinition() {
    }

    public BeanDefinition(PsiClass typeClass, String variableName) {
        this.className = typeClass.getName();
        this.variableName = variableName;
        this.displayName = this.className + DISPLAY_NAME_SPLIT + variableName;

        List<PsiField> publicFields = PsiUtils.findPublicFields(typeClass);

        for (PsiField field : publicFields) {
            fields.put(field.getName(), new BeanField(field));
        }
    }

    public String getDisplayName() {
        return displayName;
    }

    public Optional<String> getSimilarityField(String filedName) {
        if (fields.containsKey(filedName)) {
            return Optional.of(fields.get(filedName).name);
        }
        String lowerCase = filedName.toLowerCase();
        return fields.keySet().stream().filter(f -> f.toLowerCase().equals(lowerCase)).findFirst();
    }

    public Optional<String> getFieldType(String filedName) {
        return Optional.ofNullable(fields.get(filedName)).map(m -> m.typeName);
    }

    public Optional<String> getSimpleFieldType(String filedName) {
        return Optional.ofNullable(fields.get(filedName)).map(m -> m.simpleTypeName);
    }

    public boolean hasField(String filedName, String fileType) {
        BeanField filed = fields.get(filedName);
        if (filed == null) {
            return false;
        } else {
            return filed.typeName.equals(fileType);
        }
    }
}
