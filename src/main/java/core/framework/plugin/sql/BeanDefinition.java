package core.framework.plugin.sql;

import com.intellij.lang.jvm.annotation.JvmAnnotationAttribute;
import com.intellij.psi.JavaPsiFacade;
import com.intellij.psi.PsiAnnotation;
import com.intellij.psi.PsiAnnotationMemberValue;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiField;
import com.intellij.psi.PsiLiteralExpression;
import com.intellij.psi.PsiReferenceExpression;
import com.intellij.psi.PsiType;
import com.intellij.psi.impl.source.tree.java.PsiNameValuePairImpl;
import com.intellij.psi.impl.source.tree.java.PsiReferenceExpressionImpl;
import com.intellij.psi.search.GlobalSearchScope;
import core.framework.plugin.utils.ClassUtils;
import core.framework.plugin.utils.PsiUtils;

import java.util.Arrays;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * @author ebin
 */
public class BeanDefinition {
    public static final String LINE_START = "   ";
    public static final String EMPTY_BLOCK = "      ";
    public static final String SINGLE_EMPTY_BLOCK = " ";
    public static final String COLUMN_NAME_FLAG = "`";
    public String tableName;
    // column name and type
    public Map<String, String> columns = new LinkedHashMap<>();
    public Set<String> primaryKeys = new LinkedHashSet<>();
    public Set<String> notNullFields = new HashSet<>();

    public int maxColumnNameLength = 0;
    public int maxColumnTypeLength = 0;

    public JavaPsiFacade javaPsiFacade;

    public BeanDefinition() {
    }

    public BeanDefinition(JavaPsiFacade javaPsiFacade, PsiClass beanClass) {
        this.javaPsiFacade = javaPsiFacade;
        this.tableName = findTableName(beanClass);
        PsiField[] classFields = beanClass.getFields();
        for (PsiField field : classFields) {
            String columnName = getColumnName(field);
            boolean isJson = isJson(field);
            if (columnName == null) {
                continue;
            }
            PsiType type = field.getType();

            PsiType[] superTypes = type.getSuperTypes();
            Optional<String> enumType = Arrays.stream(superTypes).filter(f -> f.getCanonicalText().contains(ClassUtils.ENUM)).findFirst().map(PsiType::getCanonicalText);
            String typeName = enumType.orElseGet(type::getCanonicalText);
            int i = typeName.indexOf("<");
            if (i != -1) {
                typeName = typeName.substring(0, i);
            }
            String columnType = MysqlDialect.INSTANCE.getType(typeName, columnName, isJson);
            columns.put(columnName, columnType);
            if (isNotNull(field)) {
                notNullFields.add(columnName);
            }
            if (isPrimaryKey(field)) {
                primaryKeys.add(columnName);
            }
            maxColumnNameLength = Math.max(columnName.length(), maxColumnNameLength);
            maxColumnTypeLength = Math.max(columnType.length(), maxColumnTypeLength);
        }
    }

    public String toSql() {
        StringBuilder sql = new StringBuilder("CREATE TABLE IF NOT EXISTS `");
        sql.append(this.tableName).append("`\n").append("(\n");

        for (Map.Entry<String, String> entry : columns.entrySet()) {
            StringBuilder columnSql = new StringBuilder(LINE_START);
            String columnName = entry.getKey();
            String columnType = entry.getValue();
            columnSql.append(COLUMN_NAME_FLAG).append(columnName).append(COLUMN_NAME_FLAG);
            if (columnName.length() < maxColumnNameLength) {
                int i = maxColumnNameLength - columnName.length();
                columnSql.append(SINGLE_EMPTY_BLOCK.repeat(i));
            }
            columnSql.append(EMPTY_BLOCK).append(columnType);
            if (columnType.length() < maxColumnTypeLength) {
                int i = maxColumnTypeLength - columnType.length();
                columnSql.append(SINGLE_EMPTY_BLOCK.repeat(i));
            }
            columnSql.append(EMPTY_BLOCK).append(this.notNullFields.contains(columnName) || this.primaryKeys.contains(columnName) ? "NOT NULL" : "NULL").append(",\n");
            sql.append(columnSql);
        }

        if (!primaryKeys.isEmpty()) {
            List<String> collect = primaryKeys.stream().map(pk -> COLUMN_NAME_FLAG + pk + COLUMN_NAME_FLAG).collect(Collectors.toList());
            sql.append(LINE_START).append("PRIMARY KEY (").append(String.join(", ", collect)).append(")\n");
        }

        sql.append(") ENGINE = InnoDB;");
        return sql.toString();
    }

    private String findTableName(PsiClass beanClass) {
        PsiAnnotation[] annotations = beanClass.getAnnotations();
        for (PsiAnnotation annotation : annotations) {
            String qualifiedName = annotation.getQualifiedName();
            if (qualifiedName != null && annotation.getQualifiedName().contains("Table")) {
                List<JvmAnnotationAttribute> attributes = annotation.getAttributes();
                for (JvmAnnotationAttribute attribute : attributes) {
                    if ("name".equals(attribute.getAttributeName())) {
                        if (attribute instanceof PsiNameValuePairImpl pair) {
                            PsiAnnotationMemberValue value = pair.getValue();
                            if (value instanceof PsiReferenceExpression referenceExpression) {
                                PsiClass refClass = javaPsiFacade.findClass(
                                    ((PsiReferenceExpressionImpl) referenceExpression.getFirstChild()).getCanonicalText(),
                                    GlobalSearchScope.allScope(beanClass.getProject())
                                );
                                if (refClass == null) {
                                    return null;
                                }
                                PsiField fieldByName = refClass.findFieldByName(referenceExpression.getReferenceName(), true);
                                if (fieldByName == null) {
                                    return null;
                                }
                                return Arrays.stream(fieldByName.getChildren()).filter(f -> f instanceof PsiLiteralExpression).findFirst().map(m -> {
                                    String text = m.getText();
                                    if (text.contains("\"")) {
                                        text = text.replace("\"", "");
                                    }
                                    return text;
                                }).orElse(null);
                            } else {
                                return pair.getLiteralValue();
                            }
                        }
                    }
                }
            }
        }
        return null;
    }

    private String getColumnName(PsiField field) {
        PsiAnnotation[] filedAnnotations = field.getAnnotations();
        for (PsiAnnotation filedAnnotation : filedAnnotations) {
            String qualifiedName = filedAnnotation.getQualifiedName();
            if (qualifiedName != null && filedAnnotation.getQualifiedName().contains("Column")) {
                List<JvmAnnotationAttribute> attributes = filedAnnotation.getAttributes();
                for (JvmAnnotationAttribute attribute : attributes) {
                    if ("name".equals(attribute.getAttributeName())) {
                        if (attribute instanceof PsiNameValuePairImpl) {
                            return ((PsiNameValuePairImpl) attribute).getLiteralValue();
                        }
                    }
                }
            }
        }
        return null;
    }

    private boolean isJson(PsiField field) {
        PsiAnnotation[] filedAnnotations = field.getAnnotations();
        for (PsiAnnotation filedAnnotation : filedAnnotations) {
            String qualifiedName = filedAnnotation.getQualifiedName();
            if (qualifiedName != null && filedAnnotation.getQualifiedName().contains("Column")) {
                List<JvmAnnotationAttribute> attributes = filedAnnotation.getAttributes();
                for (JvmAnnotationAttribute attribute : attributes) {
                    if ("json".equals(attribute.getAttributeName())) {
                        if (attribute instanceof PsiNameValuePairImpl) {
                            return Objects.equals(((PsiNameValuePairImpl) attribute).getLiteralValue(), "true");
                        }
                    }
                }
            }
        }
        return false;
    }

    private boolean isNotNull(PsiField field) {
        return PsiUtils.hasAnnotation(field, "NotNull");
    }

    private boolean isPrimaryKey(PsiField field) {
        return PsiUtils.hasAnnotation(field, "PrimaryKey");
    }
}
