package core.framework.plugin;

import com.intellij.ide.highlighter.JavaFileType;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.CommonDataKeys;
import com.intellij.openapi.actionSystem.PlatformDataKeys;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.fileTypes.FileType;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.popup.JBPopupFactory;
import com.intellij.openapi.ui.popup.ListPopup;
import com.intellij.psi.JavaPsiFacade;
import com.intellij.psi.JavaRecursiveElementVisitor;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiField;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiLocalVariable;
import com.intellij.psi.PsiParameter;
import com.intellij.psi.PsiParameterList;
import com.intellij.psi.PsiPrimitiveType;
import com.intellij.psi.PsiType;
import com.intellij.psi.search.GlobalSearchScope;

import java.util.ArrayList;
import java.util.List;

/**
 * @author ebin
 */
public class SetBeanPropertiesGenerator extends AnAction {
    public static final String JAVA_PACKAGE = "java.";
    public static final String DISPLAY_NAME_SPLIT = " ";

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
        Editor editor = e.getData(PlatformDataKeys.EDITOR);
        if (editor == null) {
            return;
        }
        PsiElement element = psiFile.findElementAt(editor.getCaretModel().getOffset());
        if (element == null) {
            return;
        }
        PsiElement maybeLocalVariable = element.getParent();
        if (!(maybeLocalVariable instanceof PsiLocalVariable)) {
            return;
        }
        PsiLocalVariable mainLocalVariable = (PsiLocalVariable) maybeLocalVariable;
        PsiClass localVariableType = JavaPsiFacade.getInstance(project)
                .findClass(mainLocalVariable.getTypeElement().getType().getCanonicalText(), GlobalSearchScope.allScope(project));
        if (localVariableType == null) {
            return;
        }
        PsiElement statement = mainLocalVariable.getParent();
        PsiElement methodBlock = statement.getParent();
        PsiElement method = methodBlock.getParent();

        List<String> methodAllVariable = new ArrayList<>();
        PsiElement[] children = method.getChildren();
        PsiParameterList methodParamList = null;
        for (PsiElement psiElement : children) {
            if (psiElement instanceof PsiParameterList) {
                methodParamList = (PsiParameterList) psiElement;
                break;
            }
        }
        if (methodParamList != null) {
            PsiParameter[] parameters = methodParamList.getParameters();
            for (PsiParameter parameter : parameters) {
                PsiType type = parameter.getType();
                if (!(type instanceof PsiPrimitiveType) && !type.getCanonicalText().startsWith(JAVA_PACKAGE)) {
                    methodAllVariable.add(parameter.getType().getPresentableText() + DISPLAY_NAME_SPLIT + parameter.getName());
                }
            }
        }
        method.accept(new JavaRecursiveElementVisitor() {
                          @Override
                          public void visitLocalVariable(PsiLocalVariable localVariable) {
                              if (!localVariable.getName().equals(mainLocalVariable.getName())) {
                                  PsiType type = localVariable.getTypeElement().getType();
                                  if (!(type instanceof PsiPrimitiveType) && !type.getCanonicalText().startsWith(JAVA_PACKAGE)) {
                                      methodAllVariable.add(localVariable.getTypeElement().getType().getPresentableText() + DISPLAY_NAME_SPLIT + localVariable.getName());
                                  }
                              }
                              super.visitLocalVariable(localVariable);
                          }
                      }
        );
        PsiField[] fields = localVariableType.getFields();
        JBPopupFactory jbPopupFactory = JBPopupFactory.getInstance();
        ListPopup listPopup = jbPopupFactory.createListPopup(new SetBeanPropertiesBaseListPopupStep(methodAllVariable, project, psiFile, mainLocalVariable, methodBlock, statement, fields));
        listPopup.showInBestPositionFor(e.getDataContext());
    }
}
