package core.framework.plugin.generator.bean;

import com.intellij.openapi.command.WriteCommandAction;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.popup.PopupStep;
import com.intellij.openapi.ui.popup.util.BaseListPopupStep;
import com.intellij.psi.JavaPsiFacade;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiElementFactory;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiStatement;
import com.intellij.psi.search.GlobalSearchScope;
import core.framework.plugin.utils.ClassUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

/**
 * @author ebin
 */
public class SetBeanPropertiesBaseListPopupStep extends BaseListPopupStep<BeanDefinition> {
    public static final String NON_PROPERTIES_TEMPLATE = "%1$s.%2$s=null;";
    public static final String NON_PROPERTIES_JAVA_BEAN_TEMPLATE = "%1$s.%2$s=new %3$s();";
    public static final String NON_PROPERTIES_JAVA_BEAN_TEMPLATE_V2 = "%1$s %2$s=new %1$s();";
    public static final String NON_PROPERTIES_JAVA_BEAN_SET_TEMPLATE = "%1$s.%2$s=%2$s;";
    public static final String COPY_TEMPLATE = "%1$s.%2$s=%3$s.%4$s;";
    public static final String COPY_TEMPLATE_V2 = "%1$s.%2$s=%3$s.%2$s;";
    public static final String SET_ENUM_TEMPLATE = "%1$s.%2$s=%3$s.valueOf(%4$s.%5$s.name());";
    public static final String SET_ENUM_NULLABLE_TEMPLATE = "%1$s.%2$s=Optional.ofNullable(%4$s.%5$s).map(e -> %3$s.valueOf(e.name())).orElse(null);";
    public static final String SET_LIST_TEMPLATE = "%1$s.%2$s=%3$s.%4$s.stream().map(this::%2$s).toList();";
    public static final String SET_LIST_NULLABLE_TEMPLATE = "%1$s.%2$s=Optional.ofNullable(%3$s.%4$s).map(list-> list.stream().map(this::%2$s).toList()).orElse(null);";
    public static final String SET_SET_TEMPLATE = "%1$s.%2$s=%3$s.%4$s.stream().map(this::%2$s).collect(Collectors.toSet());";
    public static final String SET_SET_NULLABLE_TEMPLATE = "%1$s.%2$s=Optional.ofNullable(%3$s.%4$s).map(set-> set.stream().map(this::%2$s).collect(Collectors.toSet())).orElse(null);";
    public static final String SET_DIFFERENT_BEAN_TEMPLATE = "%1$s.%2$s=this.%2$s(%3$s.%4$s);";

    public static final String DOUBLE_TO_BIG_DECIMAL_TEMPLATE = "%1$s.%2$s=Maths.toBigDecimal(%3$s.%4$s);";
    public static final String BIG_DECIMAL_TO_DOUBLE_TEMPLATE = "%1$s.%2$s=Maths.toDouble(%3$s.%4$s);";

    private final Project project;
    private final PsiFile psiFile;
    private final PsiElement statement;
    private final PsiElementFactory elementFactory;
    private final BeanDefinition target;
    private final JavaPsiFacade javaPsiFacade;
    private final List<String> alreadyAssignedFiledNames;

    public SetBeanPropertiesBaseListPopupStep(List<BeanDefinition> listValues, Project project, JavaPsiFacade javaPsiFacade, PsiFile psiFile,
                                              PsiElement statement, BeanDefinition target, List<String> alreadyAssignedFiledNames) {
        this.project = project;
        this.javaPsiFacade = javaPsiFacade;
        this.psiFile = psiFile;
        this.statement = statement;
        this.target = target;
        this.alreadyAssignedFiledNames = alreadyAssignedFiledNames;
        this.elementFactory = PsiElementFactory.getInstance(project);
        listValues.add(new NullBeanDefinition());
        init("Select Local Variable To Set Properties :", listValues, null);
    }

    @Override
    public @NotNull String getTextFor(BeanDefinition value) {
        return value.getDisplayName();
    }

    @Override
    public @Nullable PopupStep<?> onChosen(BeanDefinition selectedValue, boolean finalChoice) {
        if (selectedValue instanceof NullBeanDefinition) {
            generateExpandPropertiesSetNull();
        } else {
            generate(selectedValue);
        }
        return super.onChosen(selectedValue, finalChoice);
    }

    private void generateExpandPropertiesSetNull() {
        List<PsiStatement> statements = new ArrayList<>();
        target.fields.forEach((fieldName, type) -> {
            if (alreadyAssignedFiledNames.contains(fieldName)) {
                return;
            }
            if (ClassUtils.isJavaBean(type.typeName)) {
                statements.addAll(buildBeanExpandAndSetNullStatement(type.typeName, fieldName));
            } else {
                String statementStr = String.format(NON_PROPERTIES_TEMPLATE, target.variableName, fieldName);
                statements.add(elementFactory.createStatementFromText(statementStr, psiFile.getContext()));
            }
        });
        WriteCommandAction.runWriteCommandAction(project, () -> {
            Collections.reverse(statements);
            for (PsiStatement addStatement : statements) {
                statement.getParent().addAfter(addStatement, statement);
            }
        });
    }

    public void generate(BeanDefinition source) {
        List<PsiStatement> statements = new ArrayList<>();

        target.fields.forEach((targetFieldName, targetBeanField) -> {
            if (alreadyAssignedFiledNames.contains(targetFieldName)) {
                return;
            }
            statements.addAll(buildStatements(source, target.variableName, targetFieldName, targetBeanField));
        });
        WriteCommandAction.runWriteCommandAction(project, () -> {
            Collections.reverse(statements);
            for (PsiStatement addStatement : statements) {
                statement.getParent().addAfter(addStatement, statement);
            }
        });
    }

    private List<PsiStatement> buildStatements(BeanDefinition source, String targetVariableName, String targetFieldName, BeanField targetBeanField) {
        String sourceVariableName = source.variableName;
        List<PsiStatement> statements = new ArrayList<>();
        if (source.hasField(targetFieldName, targetBeanField.typeName)) {
            String statement = String.format(COPY_TEMPLATE_V2, targetVariableName, targetFieldName, sourceVariableName);
            statements.add(elementFactory.createStatementFromText(statement, psiFile.getContext()));
            return statements;
        } else {
            Optional<String> similarityFieldOptional = source.getSimilarityField(targetFieldName);
            if (similarityFieldOptional.isPresent()) {
                String sourceField = similarityFieldOptional.get();
                String sourceFieldType = source.getFieldType(sourceField).get();
                Boolean nullable = source.fields.get(sourceField).nullable;

                if (ClassUtils.isEnum(targetBeanField.typeName)) {
                    String statement = String.format(nullable ? SET_ENUM_NULLABLE_TEMPLATE : SET_ENUM_TEMPLATE,
                        targetVariableName,
                        targetFieldName,
                        targetBeanField.simpleTypeName,
                        sourceVariableName,
                        sourceField);
                    statements.add(elementFactory.createStatementFromText(statement, psiFile.getContext()));
                } else if (ClassUtils.isList(targetBeanField.typeName)) {
                    String statement = String.format(nullable ? SET_LIST_NULLABLE_TEMPLATE : SET_LIST_TEMPLATE,
                        targetVariableName,
                        targetFieldName,
                        sourceVariableName,
                        sourceField);
                    statements.add(elementFactory.createStatementFromText(statement, psiFile.getContext()));
                } else if (ClassUtils.isSet(targetBeanField.typeName)) {
                    String statement = String.format(nullable ? SET_SET_NULLABLE_TEMPLATE : SET_SET_TEMPLATE,
                        targetVariableName,
                        targetFieldName, sourceVariableName,
                        sourceField);
                    statements.add(elementFactory.createStatementFromText(statement, psiFile.getContext()));
                } else if (ClassUtils.isDouble(sourceFieldType) && ClassUtils.isBigDecimal(targetBeanField.typeName)) {
                    String statement = String.format(DOUBLE_TO_BIG_DECIMAL_TEMPLATE, targetVariableName, targetFieldName, sourceVariableName, sourceField);
                    statements.add(elementFactory.createStatementFromText(statement, psiFile.getContext()));
                } else if (ClassUtils.isBigDecimal(sourceFieldType) && ClassUtils.isDouble(targetBeanField.typeName)) {
                    String statement = String.format(BIG_DECIMAL_TO_DOUBLE_TEMPLATE, targetVariableName, targetFieldName, sourceVariableName, sourceField);
                    statements.add(elementFactory.createStatementFromText(statement, psiFile.getContext()));
                } else if (ClassUtils.isJavaBean(targetBeanField.typeName)) {
                    PsiClass sourceBeanClass = javaPsiFacade.findClass(sourceFieldType, GlobalSearchScope.allScope(project));
                    PsiClass targetBeanClass = javaPsiFacade.findClass(targetBeanField.typeName, GlobalSearchScope.allScope(project));
                    if (sourceBeanClass == null || targetBeanClass == null) {
                        statements.addAll(buildBeanExpandAndSetNullStatement(targetBeanField.typeName, targetFieldName));
                    } else {
                        statements.addAll(fillJavaBeanProperties(targetFieldName, targetBeanField, targetBeanClass, sourceBeanClass, source.variableName));
                    }
                } else {
                    String statement = String.format(NON_PROPERTIES_TEMPLATE, targetVariableName, targetFieldName);
                    statements.add(elementFactory.createStatementFromText(statement, psiFile.getContext()));
                }
            } else {
                if (ClassUtils.isJavaBean(targetBeanField.typeName)) {
                    statements.addAll(buildBeanExpandAndSetNullStatement(targetBeanField.typeName, targetFieldName));
                } else {
                    String statement = String.format(NON_PROPERTIES_TEMPLATE, targetVariableName, targetFieldName);
                    statements.add(elementFactory.createStatementFromText(statement, psiFile.getContext()));
                }
            }
            return statements;
        }
    }

    private List<PsiStatement> fillJavaBeanProperties(String targetFieldName, BeanField targetFiledBeanField, PsiClass targetBeanClass, PsiClass sourceBeanClass, String sourceVariableName) {
        List<PsiStatement> statements = new ArrayList<>();
        String statementStr = String.format(NON_PROPERTIES_JAVA_BEAN_TEMPLATE_V2, targetFiledBeanField.typeName, targetFieldName);
        statements.add(elementFactory.createStatementFromText(statementStr, psiFile.getContext()));

        BeanDefinition targetFiledBeanDefinition = new BeanDefinition(targetBeanClass, targetFieldName);
        BeanDefinition sourceBeanDefinition = new BeanDefinition(sourceBeanClass, sourceVariableName + "." + targetFieldName);
        targetFiledBeanDefinition.fields.forEach((subFieldName, subBeanField) -> {
            statements.addAll(buildStatements(sourceBeanDefinition, targetFieldName, subFieldName, subBeanField));
        });

        String setStatementStr = String.format(NON_PROPERTIES_JAVA_BEAN_SET_TEMPLATE, target.variableName, targetFieldName);
        statements.add(elementFactory.createStatementFromText(setStatementStr, psiFile.getContext()));
        return statements;
    }

    private List<PsiStatement> buildBeanExpandAndSetNullStatement(String beanClassStr, String variableName) {
        List<PsiStatement> statements = new ArrayList<>();
        PsiClass beanClass = javaPsiFacade.findClass(beanClassStr, GlobalSearchScope.allScope(project));
        if (beanClass == null) {
            return statements;
        }
        String newStatementStr = String.format(NON_PROPERTIES_JAVA_BEAN_TEMPLATE_V2, beanClassStr, variableName);
        statements.add(elementFactory.createStatementFromText(newStatementStr, psiFile.getContext()));

        BeanDefinition beanDefinition = new BeanDefinition(beanClass, variableName);
        beanDefinition.fields.forEach((fieldName, beanField) -> {
            String statementStr = String.format(NON_PROPERTIES_TEMPLATE, variableName, fieldName);
            statements.add(elementFactory.createStatementFromText(statementStr, psiFile.getContext()));
        });

        String setStatementStr = String.format(NON_PROPERTIES_JAVA_BEAN_SET_TEMPLATE, target.variableName, variableName);
        statements.add(elementFactory.createStatementFromText(setStatementStr, psiFile.getContext()));
        return statements;
    }
}
