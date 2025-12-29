package core.framework.plugin.generator.bean;

import com.intellij.psi.PsiField;
import com.intellij.psi.PsiType;
import core.framework.plugin.utils.ClassUtils;

import java.util.Arrays;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @author ebin
 */
public class BeanField {
    private static final Pattern GENERIC_PATTERN = Pattern.compile("<([^<>]+)>");
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


    public String getGenericType() {
        Matcher matcher = GENERIC_PATTERN.matcher(typeName);
        if (matcher.find()) {
            return matcher.group(1);
        }
        return typeName;
    }
}
