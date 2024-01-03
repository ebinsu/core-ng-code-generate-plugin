package core.framework.plugin.generator.properties;

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
    public static final String COPY_TEMPLATE = "%1$s.%2$s=%3$s.%4$s;";
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
        listValues.add(new NullBeanDefinition());
        listValues.add(new ExpandPropertiesNullBeanDefinition());
        init("Select Local Variable To Set Properties :", listValues, null);
    }

    @Override
    public @NotNull String getTextFor(BeanDefinition value) {
        return value.getDisplayName();
    }

    @Override
    public @Nullable PopupStep<?> onChosen(BeanDefinition selectedValue, boolean finalChoice) {
        if (selectedValue instanceof NullBeanDefinition) {
            generateNull();
        } else if (selectedValue instanceof ExpandPropertiesNullBeanDefinition) {
            generateExpandPropertiesSetNull();
        } else {
            generate(selectedValue);
        }
        return super.onChosen(selectedValue, finalChoice);
    }

    private void generateNull() {
        PsiElementFactory elementFactory = PsiElementFactory.getInstance(project);
        List<PsiStatement> statements = new ArrayList<>();
        target.fields.forEach((fieldName, type) -> {
            if (!alreadyAssignedFiledNames.contains(fieldName)) {
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

    private void generateExpandPropertiesSetNull() {
        PsiElementFactory elementFactory = PsiElementFactory.getInstance(project);
        List<PsiStatement> statements = new ArrayList<>();
        target.fields.forEach((fieldName, type) -> {
            if (alreadyAssignedFiledNames.contains(fieldName)) {
                return;
            }
            if (ClassUtils.isJavaBean(type.typeName)) {
                String statementStr = String.format(NON_PROPERTIES_JAVA_BEAN_TEMPLATE, target.variableName, fieldName, target.getSimpleFieldType(fieldName).get());
                statements.add(elementFactory.createStatementFromText(statementStr, psiFile.getContext()));
                target.getFieldType(fieldName).ifPresent(beanClassStr -> {
                    String name = target.variableName + "." + fieldName;
                    expandJavaBean(project, javaPsiFacade, elementFactory, beanClassStr, name, statement.getContext(), statements, 0);
                });
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

    public void generate(BeanDefinition selected) {
        String selectedVariableName = selected.variableName;
        PsiElementFactory elementFactory = PsiElementFactory.getInstance(project);
        List<PsiStatement> statements = new ArrayList<>();

        target.fields.forEach((fieldName, beanField) -> {
            if (alreadyAssignedFiledNames.contains(fieldName)) {
                return;
            }
            String statement = null;
            if (selected.hasField(fieldName, beanField.typeName)) {
                statement = String.format(COPY_TEMPLATE, target.variableName, fieldName, selectedVariableName, fieldName);
            } else {
                Optional<String> selectedSimilarityFieldOptional = selected.getSimilarityField(fieldName, beanField.typeName);
                if (selectedSimilarityFieldOptional.isPresent()) {
                    String selectedSimilarityField = selectedSimilarityFieldOptional.get();
                    Optional<String> fieldType = selected.getFieldType(selectedSimilarityField);
                    if (fieldType.isPresent()) {
                        if (fieldType.get().equals(beanField.typeName)) {
                            statement = String.format(COPY_TEMPLATE, target.variableName, fieldName, selectedVariableName, selectedSimilarityField);
                        } else {
                            String selectedSimilarityFieldType = fieldType.get();
                            Boolean nullable = selected.fields.get(selectedSimilarityField).nullable;
                            if (ClassUtils.isEnum(beanField.typeName)) {
                                statement = String.format(nullable ? SET_ENUM_NULLABLE_TEMPLATE : SET_ENUM_TEMPLATE,
                                    target.variableName,
                                    fieldName,
                                    target.getSimpleFieldType(fieldName).get(),
                                    selectedVariableName,
                                    selectedSimilarityField);
                            } else if (ClassUtils.isList(beanField.typeName)) {
                                statement = String.format(nullable ? SET_LIST_NULLABLE_TEMPLATE : SET_LIST_TEMPLATE,
                                    target.variableName,
                                    fieldName,
                                    selectedVariableName,
                                    selectedSimilarityField);
                            } else if (ClassUtils.isSet(beanField.typeName)) {
                                statement = String.format(nullable ? SET_SET_NULLABLE_TEMPLATE : SET_SET_TEMPLATE,
                                    target.variableName,
                                    fieldName, selectedVariableName,
                                    selectedSimilarityField);
                            } else if (ClassUtils.isDouble(selectedSimilarityFieldType) && ClassUtils.isBigDecimal(beanField.typeName)) {
                                statement = String.format(DOUBLE_TO_BIG_DECIMAL_TEMPLATE, target.variableName, fieldName, selectedVariableName, selectedSimilarityField);
                            } else if (ClassUtils.isBigDecimal(selectedSimilarityFieldType) && ClassUtils.isDouble(beanField.typeName)) {
                                statement = String.format(BIG_DECIMAL_TO_DOUBLE_TEMPLATE, target.variableName, fieldName, selectedVariableName, selectedSimilarityField);
                            } else if (ClassUtils.isJavaBean(fieldName)) {
                                statement = String.format(SET_DIFFERENT_BEAN_TEMPLATE, target.variableName, fieldName, selectedVariableName, selectedSimilarityField);
                            } else {
                                statement = String.format(NON_PROPERTIES_TEMPLATE, target.variableName, fieldName);
                            }
                        }
                    }
                }
                if (statement == null) {
                    statement = String.format(NON_PROPERTIES_TEMPLATE, target.variableName, fieldName);
                }
            }
            statements.add(elementFactory.createStatementFromText(statement, psiFile.getContext()));
        });
        WriteCommandAction.runWriteCommandAction(project, () -> {
            Collections.reverse(statements);
            for (PsiStatement addStatement : statements) {
                statement.getParent().addAfter(addStatement, statement);
            }
        });
    }

    private void expandJavaBean(Project project, JavaPsiFacade javaPsiFacade, PsiElementFactory elementFactory,
                                String beanClassStr, String variableName, PsiElement context,
                                List<PsiStatement> statements, int depth) {
        PsiClass beanClass = javaPsiFacade.findClass(beanClassStr, GlobalSearchScope.allScope(project));
        if (beanClass == null) {
            return;
        }
        BeanDefinition beanDefinition = new BeanDefinition(beanClass, variableName);
        beanDefinition.fields.forEach((fieldName, beanField) -> {
            if (ClassUtils.isJavaBean(beanField.typeName)) {
                String statementStr = String.format(NON_PROPERTIES_JAVA_BEAN_TEMPLATE, variableName, fieldName, beanDefinition.getSimpleFieldType(fieldName).get());
                statements.add(elementFactory.createStatementFromText(statementStr, context));
                if (depth >= 2) {
                    return;
                }
                beanDefinition.getFieldType(fieldName).ifPresent(_beanClassStr -> {
                    String name = variableName + "." + fieldName;
                    expandJavaBean(project, javaPsiFacade, elementFactory, _beanClassStr, name, context, statements, depth + 1);
                });
            } else {
                String statementStr = String.format(NON_PROPERTIES_TEMPLATE, variableName, fieldName);
                statements.add(elementFactory.createStatementFromText(statementStr, context));
            }
        });
    }
}
