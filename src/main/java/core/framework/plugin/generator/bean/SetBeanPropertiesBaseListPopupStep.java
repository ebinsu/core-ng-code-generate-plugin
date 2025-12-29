package core.framework.plugin.generator.bean;

import com.google.common.base.CaseFormat;
import com.intellij.openapi.command.WriteCommandAction;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.popup.PopupStep;
import com.intellij.openapi.ui.popup.util.BaseListPopupStep;
import com.intellij.psi.JavaPsiFacade;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiElementFactory;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiMethod;
import com.intellij.psi.PsiModifier;
import com.intellij.psi.PsiStatement;
import com.intellij.psi.search.GlobalSearchScope;
import core.framework.plugin.utils.ClassUtils;
import core.framework.plugin.utils.PsiUtils;
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
    public static final String NON_PROPERTIES_TEMPLATE_V2 = "%1$s.%2$s=;";
    public static final String NON_PROPERTIES_JAVA_BEAN_TEMPLATE = "%1$s.%2$s=new %3$s();";
    public static final String NON_PROPERTIES_JAVA_BEAN_TEMPLATE_V2 = "%1$s %2$s=new %1$s();";
    public static final String NON_PROPERTIES_JAVA_BEAN_SET_TEMPLATE = "%1$s.%2$s=%2$s;";
    public static final String COPY_TEMPLATE = "%1$s.%2$s=%3$s.%4$s;";
    public static final String COPY_TEMPLATE_V2 = "%1$s.%2$s=%3$s.%2$s;";
    public static final String SET_ENUM_TEMPLATE = "%1$s.%2$s=%3$s.valueOf(%4$s.%5$s.name());";
    public static final String SET_ENUM_NULLABLE_TEMPLATE = "%1$s.%2$s=Optional.ofNullable(%4$s.%5$s).map(e -> %3$s.valueOf(e.name())).orElse(null);";
    public static final String SET_LIST_TEMPLATE = "%1$s.%2$s=%3$s.%4$s.stream().map(this::%5$s).toList();";
    public static final String SET_STATIC_LIST_TEMPLATE = "%1$s.%2$s=%3$s.%4$s.stream().map(%5$s::%6$s).toList();";
    public static final String SET_LIST_NULLABLE_TEMPLATE = "%1$s.%2$s=Optional.ofNullable(%3$s.%4$s).map(list-> list.stream().map(this::%5$s).toList()).orElse(null);";
    public static final String SET_STATIC_LIST_NULLABLE_TEMPLATE = "%1$s.%2$s=Optional.ofNullable(%3$s.%4$s).map(list-> list.stream().map(%5$s::%6$s).toList()).orElse(null);";
    public static final String SET_SET_TEMPLATE = "%1$s.%2$s=%3$s.%4$s.stream().map(this::%5$s).collect(Collectors.toSet());";
    public static final String SET_STATIC_SET_TEMPLATE = "%1$s.%2$s=%3$s.%4$s.stream().map(%5$s::%6$s).collect(Collectors.toSet());";
    public static final String SET_SET_NULLABLE_TEMPLATE = "%1$s.%2$s=Optional.ofNullable(%3$s.%4$s).map(set-> set.stream().map(this::%5$s).collect(Collectors.toSet())).orElse(null);";
    public static final String SET_STATIC_SET_NULLABLE_TEMPLATE = "%1$s.%2$s=Optional.ofNullable(%3$s.%4$s).map(set-> set.stream().map(%5$s::%6$s).collect(Collectors.toSet())).orElse(null);";
    public static final String SET_DIFFERENT_BEAN_TEMPLATE = "%1$s.%2$s=%2$s(%3$s.%4$s);";

    public static final String DOUBLE_TO_BIG_DECIMAL_TEMPLATE = "%1$s.%2$s=Maths.toBigDecimal(%3$s.%4$s);";
    public static final String BIG_DECIMAL_TO_DOUBLE_TEMPLATE = "%1$s.%2$s=Maths.toDouble(%3$s.%4$s);";

    public static final String MAP_BEAN_METHOD_TEMPLATE = "private %1$s %2$s(%3$s source) {}";
    public static final String MAP_BEAN_STATIC_METHOD_TEMPLATE = "private static %1$s %2$s(%3$s source) {}";
    public static final String MAP_BEAN_METHOD_NEW_VAR_TEMPLATE = "%1$s target = new %1$s();";
    public static final String MAP_BEAN_METHOD_BODY_NULLABLE_TEMPLATE = "if(source == null) return null;";

    private final Project project;
    private final PsiFile psiFile;
    private final PsiElement statement;
    private final PsiElementFactory elementFactory;
    private final BeanDefinition target;
    private final JavaPsiFacade javaPsiFacade;
    private final List<String> alreadyAssignedFiledNames;
    private final PsiClass parentClass;
    private final PsiElement parentMethod;
    private final boolean isParentMethodStatic;

    public SetBeanPropertiesBaseListPopupStep(List<BeanDefinition> listValues, Project project, JavaPsiFacade javaPsiFacade, PsiFile psiFile,
                                              PsiElement statement, BeanDefinition target, List<String> alreadyAssignedFiledNames,
                                              PsiClass parentClass, PsiElement parentMethod) {
        this.parentClass = parentClass;
        this.parentMethod = parentMethod;
        this.isParentMethodStatic = ((PsiMethod) parentMethod).hasModifierProperty(PsiModifier.STATIC);
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
                statements.addAll(buildBeanExpandAndSetNullStatement(type));
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
        List<PsiMethod> addMethods = new ArrayList<>();
        target.fields.forEach((targetFieldName, targetBeanField) -> {
            if (alreadyAssignedFiledNames.contains(targetFieldName)) {
                return;
            }
            StatementHolder statementHolder = buildStatements(source, target.variableName, targetFieldName, targetBeanField);
            statements.addAll(statementHolder.statements);
            addMethods.addAll(statementHolder.addMethods);
        });
        WriteCommandAction.runWriteCommandAction(project, () -> {
            Collections.reverse(statements);
            Collections.reverse(addMethods);
            for (PsiStatement addStatement : statements) {
                statement.getParent().addAfter(addStatement, statement);
            }
            addMethods.forEach(parentClass::add);
        });
    }

    private StatementHolder buildStatements(BeanDefinition source, String targetVariableName, String targetFieldName, BeanField targetBeanField) {
        String sourceVariableName = source.variableName;
        List<PsiStatement> statements = new ArrayList<>();
        List<PsiMethod> addMethods = new ArrayList<>();
        if (source.hasField(targetFieldName, targetBeanField.typeName)) {
            String statement = String.format(COPY_TEMPLATE_V2, targetVariableName, targetFieldName, sourceVariableName);
            statements.add(elementFactory.createStatementFromText(statement, psiFile.getContext()));
            return new StatementHolder(statements, addMethods);
        } else {
            Optional<String> similarityFieldNameOptional = source.getSimilarityFieldName(targetFieldName, targetBeanField.simpleTypeName);
            if (similarityFieldNameOptional.isPresent()) {
                String sourceFieldName = similarityFieldNameOptional.get();
                BeanField sourceBeanField = source.getBeanField(sourceFieldName).get();
                String sourceFieldType = sourceBeanField.typeName;
                Boolean nullable = sourceBeanField.nullable;

                if (ClassUtils.isEnum(targetBeanField.typeName)) {
                    String statement = String.format(nullable ? SET_ENUM_NULLABLE_TEMPLATE : SET_ENUM_TEMPLATE,
                        targetVariableName,
                        targetFieldName,
                        targetBeanField.simpleTypeName,
                        sourceVariableName,
                        sourceFieldName);
                    statements.add(elementFactory.createStatementFromText(statement, psiFile.getContext()));
                } else if (ClassUtils.isList(targetBeanField.typeName) || ClassUtils.isSet(targetBeanField.typeName)) {
                    PsiClass sourceBeanFieldClass = javaPsiFacade.findClass(sourceBeanField.getGenericType(), GlobalSearchScope.allScope(project));
                    PsiClass targetBeanFieldClass = javaPsiFacade.findClass(targetBeanField.getGenericType(), GlobalSearchScope.allScope(project));
                    if (sourceBeanFieldClass != null && targetBeanFieldClass != null && targetBeanFieldClass.getName() != null) {
                        boolean list = ClassUtils.isList(targetBeanField.typeName);
                        String mapMethodName = CaseFormat.UPPER_CAMEL.to(CaseFormat.LOWER_CAMEL, targetBeanFieldClass.getName());
                        String statement;
                        if (isParentMethodStatic) {
                            statement = String.format(nullable ?
                                    list ? SET_STATIC_LIST_NULLABLE_TEMPLATE : SET_STATIC_SET_NULLABLE_TEMPLATE
                                    : list ? SET_STATIC_LIST_TEMPLATE : SET_STATIC_SET_TEMPLATE,
                                targetVariableName,
                                targetFieldName,
                                sourceVariableName,
                                sourceFieldName,
                                parentClass.getQualifiedName(),
                                mapMethodName);
                        } else {
                            statement = String.format(nullable ?
                                    list ? SET_LIST_NULLABLE_TEMPLATE : SET_SET_NULLABLE_TEMPLATE
                                    : list ? SET_LIST_TEMPLATE : SET_SET_TEMPLATE,
                                targetVariableName,
                                targetFieldName,
                                sourceVariableName,
                                sourceFieldName,
                                mapMethodName);
                        }
                        statements.add(elementFactory.createStatementFromText(statement, psiFile.getContext()));
                        genMapMethod(sourceBeanFieldClass, targetBeanFieldClass, mapMethodName, addMethods, nullable);
                    } else {
                        String statement = String.format(NON_PROPERTIES_TEMPLATE_V2, targetVariableName, targetFieldName);
                        statements.add(elementFactory.createStatementFromText(statement, psiFile.getContext()));
                    }
                } else if (ClassUtils.isDouble(sourceFieldType) && ClassUtils.isBigDecimal(targetBeanField.typeName)) {
                    String statement = String.format(DOUBLE_TO_BIG_DECIMAL_TEMPLATE, targetVariableName, targetFieldName, sourceVariableName, sourceFieldName);
                    statements.add(elementFactory.createStatementFromText(statement, psiFile.getContext()));
                } else if (ClassUtils.isBigDecimal(sourceFieldType) && ClassUtils.isDouble(targetBeanField.typeName)) {
                    String statement = String.format(BIG_DECIMAL_TO_DOUBLE_TEMPLATE, targetVariableName, targetFieldName, sourceVariableName, sourceFieldName);
                    statements.add(elementFactory.createStatementFromText(statement, psiFile.getContext()));
                } else if (ClassUtils.isJavaBean(targetBeanField.typeName)) {
                    PsiClass sourceBeanClass = javaPsiFacade.findClass(sourceFieldType, GlobalSearchScope.allScope(project));
                    PsiClass targetBeanClass = javaPsiFacade.findClass(targetBeanField.typeName, GlobalSearchScope.allScope(project));
                    if (sourceBeanClass == null || targetBeanClass == null) {
                        statements.addAll(buildBeanExpandAndSetNullStatement(targetBeanField));
                    } else {
                        StatementHolder statementHolder = fillJavaBeanProperties(targetVariableName, targetFieldName, targetBeanField, targetBeanClass, sourceBeanClass, source.variableName, nullable);
                        statements.addAll(statementHolder.statements);
                        addMethods.addAll(statementHolder.addMethods);
                    }
                } else {
                    String statement = String.format(NON_PROPERTIES_TEMPLATE_V2, targetVariableName, targetFieldName);
                    statements.add(elementFactory.createStatementFromText(statement, psiFile.getContext()));
                }
            } else {
                if (ClassUtils.isJavaBean(targetBeanField.typeName)) {
                    statements.addAll(buildBeanExpandAndSetNullStatement(targetBeanField));
                } else {
                    String statement = String.format(NON_PROPERTIES_TEMPLATE_V2, targetVariableName, targetFieldName);
                    statements.add(elementFactory.createStatementFromText(statement, psiFile.getContext()));
                }
            }
            return new StatementHolder(statements, addMethods);
        }
    }

    private void genMapMethod(PsiClass sourceBeanFieldClass, PsiClass targetBeanFieldClass, String mapMethodName, List<PsiMethod> genMethodCollect, Boolean nullable) {
        String sourceBeanFieldClassName = PsiUtils.getSourceStyleClassName(sourceBeanFieldClass);
        String targetBeanFieldClassName = PsiUtils.getSourceStyleClassName(targetBeanFieldClass);
        if (sourceBeanFieldClassName.equals(targetBeanFieldClassName)) {
            sourceBeanFieldClassName = sourceBeanFieldClass.getQualifiedName();
        }
        BeanDefinition sourceBeanFieldBeanDefinition = new BeanDefinition(sourceBeanFieldClass, "source");
        BeanDefinition targetBeanFieldBeanDefinition = new BeanDefinition(targetBeanFieldClass, "target");
        PsiMethod method = elementFactory.createMethodFromText(String.format(isParentMethodStatic ? MAP_BEAN_STATIC_METHOD_TEMPLATE : MAP_BEAN_METHOD_TEMPLATE, targetBeanFieldClassName, mapMethodName, sourceBeanFieldClassName), psiFile.getContext());

        if (nullable) {
            method.getBody().add(elementFactory.createStatementFromText(String.format(MAP_BEAN_METHOD_BODY_NULLABLE_TEMPLATE), psiFile.getContext()));
        }
        method.getBody().add(elementFactory.createStatementFromText(String.format(MAP_BEAN_METHOD_NEW_VAR_TEMPLATE, targetBeanFieldClassName), psiFile.getContext()));

        targetBeanFieldBeanDefinition.fields.forEach((subTargetFieldName, subTargetBeanField) -> {
            StatementHolder statementHolder = buildStatements(sourceBeanFieldBeanDefinition, "target", subTargetFieldName, subTargetBeanField);
            statementHolder.statements.forEach(s -> method.getBody().add(s));
            genMethodCollect.addAll(statementHolder.addMethods);
        });

        method.getBody().add(elementFactory.createStatementFromText("return target;", psiFile.getContext()));
        genMethodCollect.add(method);
    }

    private StatementHolder fillJavaBeanProperties(String targetVariableName, String targetFieldName, BeanField targetFiledBeanField, PsiClass targetBeanClass, PsiClass sourceBeanClass, String sourceVariableName, Boolean nullable) {
        List<PsiStatement> statements = new ArrayList<>();
        List<PsiMethod> addMethods = new ArrayList<>();
        String statementStr = String.format(SET_DIFFERENT_BEAN_TEMPLATE, targetVariableName, targetFieldName, sourceVariableName, targetFieldName);
        statements.add(elementFactory.createStatementFromText(statementStr, psiFile.getContext()));

        genMapMethod(sourceBeanClass, targetBeanClass, targetFieldName, addMethods, nullable);
        return new StatementHolder(statements, addMethods);
    }

    private List<PsiStatement> buildBeanExpandAndSetNullStatement(BeanField targetBeanField) {
        List<PsiStatement> statements = new ArrayList<>();
        PsiClass beanClass = javaPsiFacade.findClass(targetBeanField.typeName, GlobalSearchScope.allScope(project));
        if (beanClass == null) {
            return statements;
        }
        String newStatementStr = String.format(NON_PROPERTIES_JAVA_BEAN_TEMPLATE_V2, targetBeanField.simpleTypeName, targetBeanField.name);
        statements.add(elementFactory.createStatementFromText(newStatementStr, psiFile.getContext()));

        BeanDefinition beanDefinition = new BeanDefinition(beanClass, targetBeanField.name);
        beanDefinition.fields.forEach((fieldName, beanField) -> {
            String statementStr = String.format(NON_PROPERTIES_TEMPLATE, targetBeanField.name, fieldName);
            statements.add(elementFactory.createStatementFromText(statementStr, psiFile.getContext()));
        });

        String setStatementStr = String.format(NON_PROPERTIES_JAVA_BEAN_SET_TEMPLATE, target.variableName, targetBeanField.name);
        statements.add(elementFactory.createStatementFromText(setStatementStr, psiFile.getContext()));
        return statements;
    }
}
