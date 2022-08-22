package core.framework.plugin.properties;

import com.intellij.openapi.command.WriteCommandAction;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.popup.PopupStep;
import com.intellij.openapi.ui.popup.util.BaseListPopupStep;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiElementFactory;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiStatement;
import core.framework.plugin.ClassUtils;
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
    public static final String COPY_TEMPLATE = "%1$s.%2$s=%3$s.%4$s;";
    public static final String SET_ENUM_TEMPLATE = "%1$s.%2$s=%3$s.valueOf(%4$s.%5$s.name());";
    public static final String SET_LIST_TEMPLATE = "%1$s.%2$s=%3$s.%4$s.stream().map(this::%2$s).collect(Collectors.toList());";
    public static final String SET_SET_TEMPLATE = "%1$s.%2$s=%3$s.%4$s.stream().map(this::%2$s).collect(Collectors.toSet());";
    public static final String SET_DIFFERENT_BEAN_TEMPLATE = "%1$s.%2$s=this.%2$s(%3$s.%4$s);";

    private final Project project;
    private final PsiFile psiFile;
    private final PsiElement methodBlock;
    private final PsiElement statement;
    private final BeanDefinition target;

    public SetBeanPropertiesBaseListPopupStep(List<BeanDefinition> listValues, Project project, PsiFile psiFile,
                                              PsiElement methodBlock, PsiElement statement, BeanDefinition target) {
        this.project = project;
        this.psiFile = psiFile;
        this.methodBlock = methodBlock;
        this.statement = statement;
        this.target = target;
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
            generateSetNull();
        } else {
            generate(selectedValue);
        }
        return super.onChosen(selectedValue, finalChoice);
    }

    private void generateSetNull() {
        List<PsiStatement> statements = new ArrayList<>();
        PsiElementFactory elementFactory = PsiElementFactory.getInstance(project);
        target.fields.forEach((fieldName, type) -> {
            String statement = String.format(NON_PROPERTIES_TEMPLATE, target.variableName, fieldName);
            statements.add(elementFactory.createStatementFromText(statement, psiFile.getContext()));
        });
        WriteCommandAction.runWriteCommandAction(project, () -> {
            Collections.reverse(statements);
            for (PsiStatement addStatement : statements) {
                methodBlock.addAfter(addStatement, statement);
            }
        });
    }

    public void generate(BeanDefinition selected) {
        String selectedVariableName = selected.variableName;
        PsiElementFactory elementFactory = PsiElementFactory.getInstance(project);
        List<PsiStatement> statements = new ArrayList<>();

        target.fields.forEach((fieldName, type) -> {
            String statement;
            if (selected.hasField(fieldName, type)) {
                statement = String.format(COPY_TEMPLATE, target.variableName, fieldName, selectedVariableName, fieldName);
            } else {
                Optional<String> selectedSimilarityFieldOptional = selected.getSimilarityField(fieldName, type);
                if (selectedSimilarityFieldOptional.isPresent()) {
                    String selectedSimilarityField = selectedSimilarityFieldOptional.get();
                    Optional<String> fieldType = selected.getFieldType(selectedSimilarityField);
                    if (fieldType.isPresent() && fieldType.get().equals(type)) {
                        statement = String.format(COPY_TEMPLATE, target.variableName, fieldName, selectedVariableName, selectedSimilarityField);
                    } else {
                        if (ClassUtils.isEnum(type)) {
                            statement = String.format(SET_ENUM_TEMPLATE, target.variableName, fieldName,
                                    target.getSimpleFieldType(fieldName).get(),
                                    selectedVariableName, selectedSimilarityField);
                        } else if (ClassUtils.isList(type)) {
                            statement = String.format(SET_LIST_TEMPLATE, target.variableName, fieldName, selectedVariableName, selectedSimilarityField);
                        } else if (ClassUtils.isSet(type)) {
                            statement = String.format(SET_SET_TEMPLATE, target.variableName, fieldName, selectedVariableName, selectedSimilarityField);
                        } else if (ClassUtils.isJavaBean(fieldName)) {
                            statement = String.format(SET_DIFFERENT_BEAN_TEMPLATE, target.variableName, fieldName, selectedVariableName, selectedSimilarityField);
                        } else {
                            statement = String.format(NON_PROPERTIES_TEMPLATE, target.variableName, fieldName);
                        }
                    }
                } else {
                    statement = String.format(NON_PROPERTIES_TEMPLATE, target.variableName, fieldName);
                }
            }
            statements.add(elementFactory.createStatementFromText(statement, psiFile.getContext()));
        });
        WriteCommandAction.runWriteCommandAction(project, () -> {
            Collections.reverse(statements);
            for (PsiStatement addStatement : statements) {
                methodBlock.addAfter(addStatement, statement);
            }
        });
    }
}
