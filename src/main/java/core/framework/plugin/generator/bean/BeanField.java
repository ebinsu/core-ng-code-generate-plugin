package core.framework.plugin.generator.bean;

import com.intellij.psi.PsiField;
import com.intellij.psi.PsiType;
import core.framework.plugin.utils.ClassUtils;

import java.util.Arrays;
import java.util.Optional;

/**
 * @author ebin
 */
public class BeanField {
    public String typeName;
    public String simpleTypeName;
    public Boolean nullable;

    public String name;

    public BeanField(PsiField field) {
        PsiType type = field.getType();
        String canonicalText = type.getCanonicalText();

        PsiType[] superTypes = type.getSuperTypes();
        Optional<String> enumType = Arrays.stream(superTypes).filter(f -> f.getCanonicalText().contains(ClassUtils.ENUM)).findFirst().map(PsiType::getCanonicalText);
        this.typeName = enumType.orElse(canonicalText);
        this.simpleTypeName = type.getPresentableText();
        this.nullable = Arrays.stream(field.getAnnotations()).noneMatch(ann -> ann.getText().contains("NotNull"));
        this.name = field.getName();
    }

    public String getDisplayName() {
        return simpleTypeName + " " + name;
    }
}
