package core.framework.plugin.generator.properties;

import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiField;
import core.framework.plugin.utils.PsiUtils;
import org.apache.commons.lang.StringUtils;

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
            Optional<String> first = candidate.stream().filter(f -> finalGenFileType.contains(fields.get(f).typeName)).findFirst();
            if (first.isEmpty()) {
                first = candidate.stream().findFirst();
            }
            return first;
        } else {
            String lowerCase = filedName.toLowerCase();
            return fields.keySet().stream().filter(f -> f.toLowerCase().contains(lowerCase) || lowerCase.contains(f.toLowerCase())).findFirst();
        }
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
