package core.framework.plugin.generator.bean;

import com.intellij.codeInsight.intention.IntentionAction;
import com.intellij.codeInsight.intention.PsiElementBaseIntentionAction;
import com.intellij.codeInspection.util.IntentionFamilyName;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.fileEditor.FileDocumentManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.popup.JBPopupFactory;
import com.intellij.openapi.ui.popup.ListPopup;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.JavaPsiFacade;
import com.intellij.psi.JavaRecursiveElementVisitor;
import com.intellij.psi.PsiAssignmentExpression;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiLocalVariable;
import com.intellij.psi.PsiManager;
import com.intellij.psi.PsiMethod;
import com.intellij.psi.PsiParameter;
import com.intellij.psi.PsiParameterList;
import com.intellij.psi.PsiReferenceExpression;
import com.intellij.psi.PsiType;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.testFramework.LightVirtualFile;
import com.intellij.util.IncorrectOperationException;
import core.framework.plugin.utils.PsiUtils;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * @author ebin
 */
public class SetBeanPropertiesIntentionAction extends PsiElementBaseIntentionAction implements IntentionAction {
    @Override
    public void invoke(@NotNull Project project, Editor editor, @NotNull PsiElement element) throws IncorrectOperationException {
        FileDocumentManager documentManager = FileDocumentManager.getInstance();
        VirtualFile virtualFile = getVirtualFile(editor, documentManager);
        if (virtualFile == null) {
            return;
        }
        PsiElement selectBlock = element.getParent();
        JavaPsiFacade javaPsiFacade = JavaPsiFacade.getInstance(project);
        Optional<PsiElement> methodOptional = PsiUtils.findMethod(element);
        PsiClass aClass = PsiUtils.findClass(element);
        if (methodOptional.isEmpty() || aClass == null) {
            return;
        }
        PsiElement method = methodOptional.get();
        BeanDefinition focusBeanDefinition;
        PsiElement codeLine;

        if (element.getParent() instanceof PsiReferenceExpression) {
            List<BeanDefinition> methodAllVariable = new ArrayList<>();

            findMethodParameterVariable(project, javaPsiFacade, method, methodAllVariable);
            findMethodBodyVariable(project, javaPsiFacade, method, null, methodAllVariable, new ArrayList<>());
            String variableName = element.getParent().getText();
            focusBeanDefinition = methodAllVariable.stream().filter(b -> b.variableName.equals(variableName)).findFirst().orElse(null);
            Optional<PsiElement> optional = PsiUtils.findExpressionStatement(element);
            if (optional.isEmpty()) {
                return;
            }
            codeLine = optional.get();
        } else if (selectBlock instanceof PsiLocalVariable focusLocalVariable) {
            PsiType focusLocalVariableType = focusLocalVariable.getType();
            PsiClass selectLocalVariableTypeClass = javaPsiFacade.findClass(
                focusLocalVariableType.getCanonicalText(),
                GlobalSearchScope.allScope(project)
            );
            if (selectLocalVariableTypeClass == null) {
                return;
            }
            focusBeanDefinition = new BeanDefinition(selectLocalVariableTypeClass, focusLocalVariable.getName());
            codeLine = focusLocalVariable.getParent();
        } else {
            return;
        }

        if (codeLine == null || focusBeanDefinition == null || focusBeanDefinition.fields.isEmpty()) {
            return;
        }

        List<BeanDefinition> methodAllVariable = new ArrayList<>();
        List<String> alreadyAssignedFiledNames = new ArrayList<>();
        findMethodParameterVariable(project, javaPsiFacade, method, methodAllVariable);
        findMethodBodyVariable(project, javaPsiFacade, method, focusBeanDefinition, methodAllVariable, alreadyAssignedFiledNames);

        PsiFile psiFile = PsiManager.getInstance(project).findFile(virtualFile);
        JBPopupFactory jbPopupFactory = JBPopupFactory.getInstance();

        ListPopup listPopup = jbPopupFactory.createListPopup(new SetBeanPropertiesBaseListPopupStep(methodAllVariable, project, javaPsiFacade, psiFile,
            codeLine, focusBeanDefinition, alreadyAssignedFiledNames, aClass, method));
        listPopup.showInBestPositionFor(editor);
    }

    @Override
    public boolean isAvailable(@NotNull Project project, Editor editor, @NotNull PsiElement element) {
        PsiElement maybeLocalVariable = element.getParent();
        if (element.getParent() instanceof PsiReferenceExpression) {
            JavaPsiFacade javaPsiFacade = JavaPsiFacade.getInstance(project);
            List<PsiElement> methods = PsiUtils.findMethods(element.getParent());
            List<BeanDefinition> methodAllVariable = new ArrayList<>();

            for (PsiElement method : methods) {
                findMethodParameterVariable(project, javaPsiFacade, method, methodAllVariable);
                findMethodBodyVariable(project, javaPsiFacade, method, null, methodAllVariable, new ArrayList<>());
            }
            String variableName = element.getParent().getText();
            return methodAllVariable.stream().anyMatch(b -> b.variableName.equals(variableName));
        } else if ((maybeLocalVariable instanceof PsiLocalVariable focusLocalVariable)) {
            PsiType focusLocalVariableType = focusLocalVariable.getType();
            return PsiUtils.isJavaBean(focusLocalVariableType);
        } else {
            return false;
        }
    }

    @Override
    public @NotNull @IntentionFamilyName String getFamilyName() {
        return "# Populate properties";
    }

    @NotNull
    public String getText() {
        return getFamilyName();
    }

    private void findMethodBodyVariable(Project project, JavaPsiFacade javaPsiFacade, PsiElement method, BeanDefinition focusLocalVariable, List<BeanDefinition> methodAllVariable, List<String> alreadyAssignedFiledNames) {
        method.accept(new JavaRecursiveElementVisitor() {
                          @Override
                          public void visitLocalVariable(PsiLocalVariable localVariable) {
                              if (focusLocalVariable == null || !localVariable.getName().equals(focusLocalVariable.variableName)) {
                                  PsiType type = localVariable.getTypeElement().getType();
                                  if (PsiUtils.isJavaBean(type)) {
                                      PsiClass typeClass = javaPsiFacade.findClass(type.getCanonicalText(), GlobalSearchScope.allScope(project));
                                      if (typeClass != null) {
                                          methodAllVariable.add(new BeanDefinition(typeClass, localVariable.getName()));
                                      }
                                  }
                              }
                              super.visitLocalVariable(localVariable);
                          }

                          @Override
                          public void visitAssignmentExpression(@NotNull PsiAssignmentExpression expression) {
                              if (focusLocalVariable == null || expression.getText().contains(focusLocalVariable.variableName)) {
                                  alreadyAssignedFiledNames.add(expression.getFirstChild().getLastChild().getText());
                              }
                              super.visitAssignmentExpression(expression);
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

    private VirtualFile getVirtualFile(Editor editor, FileDocumentManager documentManager) {
        VirtualFile virtualFile = documentManager.getFile(editor.getDocument());
        if (virtualFile instanceof LightVirtualFile lightVirtualFile) {
            return lightVirtualFile.getOriginalFile();
        } else {
            return virtualFile;
        }
    }

    @Override
    public boolean startInWriteAction() {
        return false;
    }
}
