package core.framework.plugin.properties;

import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiField;
import com.intellij.psi.PsiModifier;
import com.intellij.psi.PsiType;
import core.framework.plugin.utils.ClassUtils;
import org.apache.commons.lang.StringUtils;

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.OptionalInt;
import java.util.stream.Collectors;

/**
 * @author ebin
 */
public class BeanDefinition {
    public static final String DISPLAY_NAME_SPLIT = " ";

    public String className;
    public String variableName;
    public String displayName;
    // filed name and type
    public Map<String, String> fields = new LinkedHashMap<>();
    // filed name and simple type
    public Map<String, String> fieldSimpleNames = new LinkedHashMap<>();
    // filed name and nullable
    public Map<String, Boolean> fieldNullables = new LinkedHashMap<>();

    public BeanDefinition() {
    }

    public BeanDefinition(PsiClass typeClass, String variableName) {
        this.className = typeClass.getName();
        this.variableName = variableName;
        this.displayName = this.className + DISPLAY_NAME_SPLIT + variableName;
        PsiField[] classFields = typeClass.getFields();
        List<PsiField> publicFields = Arrays.stream(classFields).filter(filed -> filed.getModifierList() != null)
            .filter(field -> !field.getModifierList().hasModifierProperty(PsiModifier.PRIVATE)
                && !field.getModifierList().hasModifierProperty(PsiModifier.STATIC)
                && !field.getModifierList().hasModifierProperty(PsiModifier.FINAL)
            ).toList();
        for (PsiField field : publicFields) {
            PsiType type = field.getType();
            String canonicalText = type.getCanonicalText();

            PsiType[] superTypes = type.getSuperTypes();
            Optional<String> enumType = Arrays.stream(superTypes).filter(f -> f.getCanonicalText().contains(ClassUtils.ENUM)).findFirst().map(PsiType::getCanonicalText);
            if (enumType.isPresent()) {
                fields.put(field.getName(), enumType.get());
            } else {
                fields.put(field.getName(), canonicalText);
            }
            fieldSimpleNames.put(field.getName(), type.getPresentableText());
            fieldNullables.put(field.getName(), Arrays.stream(field.getAnnotations()).noneMatch(ann -> ann.getText().contains("NotNull")));
        }
    }

    public String getDisplayName() {
        return displayName;
    }

    public Optional<String> getSimilarityField(String filedName, String fileType) {
        int i = fileType.indexOf("<");
        String genFileType = fileType;
        if (i != -1) {
            genFileType = genFileType.substring(0, i);
        }
        Map<Integer, List<String>> collect = fields.keySet().stream().collect(Collectors.groupingBy((k -> StringUtils.getLevenshteinDistance(filedName, k))));
        OptionalInt min = collect.keySet().stream().mapToInt(k -> k).min();
        if (min.isPresent() && min.getAsInt() <= 3) {
            List<String> candidate = collect.get(min.getAsInt());
            if (candidate.isEmpty()) {
                return Optional.empty();
            }
            String finalGenFileType = genFileType;
            Optional<String> first = candidate.stream().filter(f -> finalGenFileType.contains(fields.get(f))).findFirst();
            if (first.isEmpty()) {
                first = candidate.stream().findFirst();
            }
            return first;
        } else {
            String lowerCase = filedName.toLowerCase();
            return fields.keySet().stream().filter(f -> f.toLowerCase().contains(lowerCase)).findFirst();
        }
    }

    public Optional<String> getFieldType(String filedName) {
        return Optional.ofNullable(fields.get(filedName));
    }

    public Optional<String> getSimpleFieldType(String filedName) {
        return Optional.ofNullable(fieldSimpleNames.get(filedName));
    }

    public boolean hasField(String filedName, String fileType) {
        String type = fields.get(filedName);
        if (type == null) {
            return false;
        } else {
            return type.equals(fileType);
        }
    }
}
