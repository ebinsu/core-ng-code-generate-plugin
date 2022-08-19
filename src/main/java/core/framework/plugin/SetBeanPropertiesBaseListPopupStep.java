package core.framework.plugin;

import com.intellij.openapi.command.WriteCommandAction;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.popup.PopupStep;
import com.intellij.openapi.ui.popup.util.BaseListPopupStep;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiElementFactory;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiLocalVariable;
import com.intellij.psi.PsiStatement;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

/**
 * @author ebin
 */
public class SetBeanPropertiesBaseListPopupStep extends BaseListPopupStep<LocalVariableBean> {
    public static final String COPY_TEMPLATE = "%1$s.%2$s=%3$s.%2$s;";
    public static final String SET_ENUM_TEMPLATE = "%1$s.%2$s=%4$s.valueOf(%3$s.%2$s.name());";
    public static final String SET_LIST_TEMPLATE = "%1$s.%2$s=%3$s.%2$s.stream().map(this::%2$s).collect(Collectors.toList());";
    public static final String SET_SET_TEMPLATE = "%1$s.%2$s=%3$s.%2$s.stream().map(this::%2$s).collect(Collectors.toSet());";

    public static final String SET_DIFFERENT_BEAN_TEMPLATE = "%1$s.%2$s=this.%2$s(%3$s.%2$s);";

    private final Project project;
    private final PsiFile psiFile;
    private final PsiLocalVariable localVariable;
    private final PsiElement methodBlock;
    private final PsiElement statement;
    List<LocalVariableFiledBean> fields;

    public SetBeanPropertiesBaseListPopupStep(List<LocalVariableBean> listValues, Project project, PsiFile psiFile, PsiLocalVariable localVariable,
                                              PsiElement methodBlock, PsiElement statement, List<LocalVariableFiledBean> fields) {
        this.project = project;
        this.psiFile = psiFile;
        this.localVariable = localVariable;
        this.methodBlock = methodBlock;
        this.statement = statement;
        this.fields = fields;
        init("Select Local Variable To Set Properties :", listValues, null);
    }

    @Override
    public @NotNull String getTextFor(LocalVariableBean value) {
        return value.displayName;
    }

    @Override
    public @Nullable PopupStep<?> onChosen(LocalVariableBean selectedValue, boolean finalChoice) {
        generate(selectedValue);
        return super.onChosen(selectedValue, finalChoice);
    }

    public void generate(LocalVariableBean selectedValue) {
        String selectValueName = selectedValue.name;
        PsiElementFactory elementFactory = PsiElementFactory.getInstance(project);
        List<PsiStatement> statements = new ArrayList<>();
        for (LocalVariableFiledBean field : fields) {
            if (field.isEnum() && !selectedValue.isSameVariableType(field.field)) {
                String statement = String.format(SET_ENUM_TEMPLATE, localVariable.getName(), field.getName(), selectValueName, field.getTypeClass().getName());
                statements.add(elementFactory.createStatementFromText(statement, psiFile.getContext()));
            } else if (field.isList()) {
                String statement = String.format(SET_LIST_TEMPLATE, localVariable.getName(), field.getName(), selectValueName);
                statements.add(elementFactory.createStatementFromText(statement, psiFile.getContext()));
            } else if (field.isSet()) {
                String statement = String.format(SET_SET_TEMPLATE, localVariable.getName(), field.getName(), selectValueName);
                statements.add(elementFactory.createStatementFromText(statement, psiFile.getContext()));
            } else if (field.isJavaBean() && !selectedValue.isSameVariableType(field.field)) {
                String setDifferentBeanStatement = String.format(SET_DIFFERENT_BEAN_TEMPLATE, localVariable.getName(), field.getName(), selectValueName);
                statements.add(elementFactory.createStatementFromText(setDifferentBeanStatement, psiFile.getContext()));
            } else {
                statements.add(elementFactory.createStatementFromText(String.format(COPY_TEMPLATE, localVariable.getName(), field.getName(), selectValueName), psiFile.getContext()));
            }
        }

        WriteCommandAction.runWriteCommandAction(project, () -> {
            for (PsiStatement addStatement : statements) {
                methodBlock.addAfter(addStatement, statement);
            }
        });
    }
}
