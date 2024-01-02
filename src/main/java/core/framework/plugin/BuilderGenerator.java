package core.framework.plugin;

import com.intellij.ide.highlighter.JavaFileType;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.CommonDataKeys;
import com.intellij.openapi.actionSystem.PlatformDataKeys;
import com.intellij.openapi.command.WriteCommandAction;
import com.intellij.openapi.fileTypes.FileType;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiCodeBlock;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiElementFactory;
import com.intellij.psi.PsiField;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiMethod;
import com.intellij.psi.impl.source.PsiJavaFileImpl;

/**
 * @author ebin
 */
@Deprecated
public class BuilderGenerator extends AnAction {
    public static final String MAIN_CLASS_CONSTRUCTOR_TEMPLATE = "private %1$s(Builder builder) {}";
    public static final String MAIN_CLASS_CONSTRUCTOR_BODY_TEMPLATE = "%1$s = builder.%1$s;";
    public static final String MAIN_CLASS_BUILDER_METHOD_TEMPLATE = "public static %1$s.Builder builder() { return new %1$s.Builder(); }";
    public static final String BUILDER_CLASS_FIELD_TEMPLATE = "private %1$s %2$s;";
    public static final String BUILDER_CLASS_SET_METHOD_TEMPLATE = "public Builder %1$s(%2$s %1$s) {this.%1$s=%1$s; return this;}";
    public static final String BUILDER_CLASS_BUILD_METHOD_TEMPLATE = "public %1$s build() {return new %1$s(this);}";
    public static final String BUILDER_CLASS_NAME = "Builder";

    @Override
    public void actionPerformed(AnActionEvent e) {
        Project project = e.getData(PlatformDataKeys.PROJECT);
        PsiFile psiFile = e.getData(CommonDataKeys.PSI_FILE);
        if (project == null || psiFile == null) {
            return;
        }
        FileType fileType = psiFile.getFileType();
        if (!(fileType instanceof JavaFileType)) {
            return;
        }
        PsiClass[] classes = ((PsiJavaFileImpl) psiFile).getClasses();
        if (classes.length == 0) {
            return;
        }
        PsiClass mainClass = classes[0];
        PsiClass[] innerClasses = mainClass.getInnerClasses();
        if (innerClasses.length > 0) {
            boolean hasBuilderClass = false;
            for (PsiClass innerClass : innerClasses) {
                if (BUILDER_CLASS_NAME.equals(innerClass.getName())) {
                    hasBuilderClass = true;
                    break;
                }
            }
            if (hasBuilderClass) {
                return;
            }
        }
        PsiField[] allFields = mainClass.getFields();
        if (allFields.length == 0) {
            return;
        }
        PsiElement context = psiFile.getContext();
        PsiElementFactory elementFactory = PsiElementFactory.getInstance(project);

        PsiClass builderClass = elementFactory.createClass(BUILDER_CLASS_NAME);
        builderClass.getModifierList().add(elementFactory.createKeyword("static"));
        builderClass.add(elementFactory.createMethodFromText(String.format(BUILDER_CLASS_BUILD_METHOD_TEMPLATE, mainClass.getName()), context));

        PsiMethod mainClassBuilderMethod = elementFactory.createMethodFromText(String.format(MAIN_CLASS_BUILDER_METHOD_TEMPLATE, mainClass.getName()), context);
        PsiMethod mainClassConstructor = elementFactory.createMethodFromText(String.format(MAIN_CLASS_CONSTRUCTOR_TEMPLATE, mainClass.getName()), context);
        PsiCodeBlock body = mainClassConstructor.getBody();
        for (PsiField field : allFields) {
            String fieldName = field.getName();
            builderClass.add(elementFactory.createFieldFromText(String.format(BUILDER_CLASS_FIELD_TEMPLATE, field.getType().getPresentableText(), fieldName), context));
            builderClass.add(elementFactory.createMethodFromText(String.format(BUILDER_CLASS_SET_METHOD_TEMPLATE, fieldName, field.getType().getPresentableText()), context));
            if (body != null)
                body.add(elementFactory.createStatementFromText(String.format(MAIN_CLASS_CONSTRUCTOR_BODY_TEMPLATE, fieldName), context));
        }

        WriteCommandAction.runWriteCommandAction(project, () -> {
            mainClass.add(mainClassConstructor);
            mainClass.add(mainClassBuilderMethod);
            mainClass.add(builderClass);
        });
    }
}
