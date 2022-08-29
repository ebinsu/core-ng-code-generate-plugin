package core.framework.plugin.properties;

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
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiLocalVariable;
import com.intellij.psi.PsiParameter;
import com.intellij.psi.PsiParameterList;
import com.intellij.psi.PsiType;
import com.intellij.psi.search.GlobalSearchScope;
import core.framework.plugin.utils.PsiUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * @author ebin
 */
public class SetBeanPropertiesGenerator extends AnAction {

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
        PsiLocalVariable focusLocalVariable = (PsiLocalVariable) maybeLocalVariable;
        PsiType focusLocalVariableType = focusLocalVariable.getType();
        if (!PsiUtils.isJavaBean(focusLocalVariableType)) {
            return;
        }
        JavaPsiFacade javaPsiFacade = JavaPsiFacade.getInstance(project);
        PsiClass selectLocalVariableTypeClass = javaPsiFacade.findClass(
                focusLocalVariableType.getCanonicalText(),
                GlobalSearchScope.allScope(project)
        );
        if (selectLocalVariableTypeClass == null) {
            return;
        }
        BeanDefinition focusBeanDefinition = new BeanDefinition(selectLocalVariableTypeClass, focusLocalVariable.getName());
        if (focusBeanDefinition.fields.isEmpty()) {
            return;
        }

        PsiElement statement = focusLocalVariable.getParent();
        PsiElement methodBlock = statement.getParent();
        PsiElement method = methodBlock.getParent();

        List<BeanDefinition> methodAllVariable = new ArrayList<>();
        findMethodParameterVariable(project, javaPsiFacade, method, methodAllVariable);
        findMethodBodyVariable(project, javaPsiFacade, method, focusLocalVariable, methodAllVariable);

        JBPopupFactory jbPopupFactory = JBPopupFactory.getInstance();
        ListPopup listPopup = jbPopupFactory.createListPopup(new SetBeanPropertiesBaseListPopupStep(methodAllVariable, project, javaPsiFacade, psiFile, methodBlock, statement, focusBeanDefinition));
        listPopup.showInBestPositionFor(e.getDataContext());
    }

    private void findMethodBodyVariable(Project project, JavaPsiFacade javaPsiFacade, PsiElement method, PsiLocalVariable focusLocalVariable, List<BeanDefinition> methodAllVariable) {
        AtomicBoolean add = new AtomicBoolean(true);
        method.accept(new JavaRecursiveElementVisitor() {
                          @Override
                          public void visitLocalVariable(PsiLocalVariable localVariable) {
                              if (!add.get()) {
                                  return;
                              }
                              if (!localVariable.getName().equals(focusLocalVariable.getName())) {
                                  PsiType type = localVariable.getTypeElement().getType();
                                  if (PsiUtils.isJavaBean(type)) {
                                      PsiClass typeClass = javaPsiFacade.findClass(type.getCanonicalText(), GlobalSearchScope.allScope(project));
                                      if (typeClass != null) {
                                          methodAllVariable.add(new BeanDefinition(typeClass, localVariable.getName()));
                                      }
                                  }
                              } else {
                                  add.getAndSet(false);
                              }
                              super.visitLocalVariable(localVariable);
                          }
                      }
        );
    }

    private void findMethodParameterVariable(Project project, JavaPsiFacade javaPsiFacade, PsiElement method, List<BeanDefinition> methodAllVariable) {
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
                if (PsiUtils.isJavaBean(type)) {
                    PsiClass typeClass = javaPsiFacade.findClass(type.getCanonicalText(), GlobalSearchScope.allScope(project));
                    if (typeClass != null) {
                        methodAllVariable.add(new BeanDefinition(typeClass, parameter.getName()));
                    }
                }
            }
        }
    }
}
