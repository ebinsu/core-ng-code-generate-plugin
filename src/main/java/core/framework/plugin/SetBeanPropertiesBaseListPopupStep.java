package core.framework.plugin;

import com.intellij.openapi.command.WriteCommandAction;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.popup.PopupStep;
import com.intellij.openapi.ui.popup.util.BaseListPopupStep;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiElementFactory;
import com.intellij.psi.PsiField;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiLocalVariable;
import org.jetbrains.annotations.Nullable;

import java.util.List;

import static core.framework.plugin.SetBeanPropertiesGenerator.DISPLAY_NAME_SPLIT;

/**
 * @author ebin
 */
public class SetBeanPropertiesBaseListPopupStep extends BaseListPopupStep<String> {
    public static final String COPY_TEMPLATE = "%1$s.%2$s=%3$s.%2$s;";

    private final Project project;
    private final PsiFile psiFile;
    private final PsiLocalVariable localVariable;
    private final PsiElement methodBlock;
    private final PsiElement statement;
    private final PsiField[] fields;

    public SetBeanPropertiesBaseListPopupStep(List<String> listValues, Project project, PsiFile psiFile, PsiLocalVariable localVariable,
                                              PsiElement methodBlock, PsiElement statement, PsiField[] fields) {
        this.project = project;
        this.psiFile = psiFile;
        this.localVariable = localVariable;
        this.methodBlock = methodBlock;
        this.statement = statement;
        this.fields = fields;
        init("Select Local Variable To Set Properties :", listValues, null);
    }

    @Override
    public @Nullable PopupStep<?> onChosen(String selectedValue, boolean finalChoice) {
        String value = selectedValue.split(DISPLAY_NAME_SPLIT)[1];
        generate(value);
        return super.onChosen(selectedValue, finalChoice);
    }

    public void generate(String value) {
        PsiElementFactory elementFactory = PsiElementFactory.getInstance(project);
        WriteCommandAction.runWriteCommandAction(project, () -> {
            for (PsiField field : fields) {
                methodBlock.addAfter(elementFactory.createStatementFromText(String.format(COPY_TEMPLATE, localVariable.getName(), field.getName(), value), psiFile.getContext()), statement);
            }
        });
    }
}
