package core.framework.plugin.properties;

import com.intellij.codeInsight.intention.IntentionAction;
import com.intellij.codeInsight.intention.PsiElementBaseIntentionAction;
import com.intellij.codeInspection.util.IntentionFamilyName;
import com.intellij.lang.ASTNode;
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
import com.intellij.psi.PsiLambdaExpression;
import com.intellij.psi.PsiLocalVariable;
import com.intellij.psi.PsiManager;
import com.intellij.psi.PsiMethod;
import com.intellij.psi.PsiParameter;
import com.intellij.psi.PsiParameterList;
import com.intellij.psi.PsiType;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.testFramework.LightVirtualFile;
import com.intellij.util.IncorrectOperationException;
import core.framework.plugin.utils.PsiUtils;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

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
        PsiLocalVariable focusLocalVariable = (PsiLocalVariable) element.getParent();
        PsiType focusLocalVariableType = focusLocalVariable.getType();
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
        List<PsiElement> methods = findMethods(statement);
        if (methods.isEmpty()) {
            return;
        }
        List<BeanDefinition> methodAllVariable = new ArrayList<>();
        List<String> alreadyAssignedFiledNames = new ArrayList<>();
        for (PsiElement method : methods) {
            findMethodParameterVariable(project, javaPsiFacade, method, methodAllVariable);
            findMethodBodyVariable(project, javaPsiFacade, method, focusLocalVariable, methodAllVariable, alreadyAssignedFiledNames);
        }
        PsiFile psiFile = PsiManager.getInstance(project).findFile(virtualFile);
        JBPopupFactory jbPopupFactory = JBPopupFactory.getInstance();
        ListPopup listPopup = jbPopupFactory.createListPopup(new SetBeanPropertiesBaseListPopupStep(methodAllVariable, project, javaPsiFacade, psiFile, statement, focusBeanDefinition, alreadyAssignedFiledNames));
        listPopup.showInBestPositionFor(editor);
    }

    @Override
    public boolean isAvailable(@NotNull Project project, Editor editor, @NotNull PsiElement element) {
        PsiElement maybeLocalVariable = element.getParent();
        if (!(maybeLocalVariable instanceof PsiLocalVariable focusLocalVariable)) {
            return false;
        }
        PsiType focusLocalVariableType = focusLocalVariable.getType();
        return PsiUtils.isJavaBean(focusLocalVariableType);
    }

    @Override
    public @NotNull @IntentionFamilyName String getFamilyName() {
        return "#Populate properties";
    }

    @NotNull
    public String getText() {
        return getFamilyName();
    }

    private List<PsiElement> findMethods(PsiElement statement) {
        List<PsiElement> methods = new ArrayList<>();
        PsiElement maybeMethod = statement;
        do {
            maybeMethod = maybeMethod.getParent();
            if (maybeMethod instanceof PsiMethod) {
                methods.add(maybeMethod);
                break;
            } else if (maybeMethod instanceof ASTNode) {
                String type = ((ASTNode) maybeMethod).getElementType().toString();
                if ("METHOD".equals(type)) {
                    methods.add(maybeMethod);
                    break;
                } else if ("EXPRESSION_LIST".equals(type)) {
                    Stream.of(maybeMethod.getChildren()).filter(f -> f instanceof PsiLambdaExpression).findFirst().ifPresent(methods::add);
                }
            }
        } while (maybeMethod.getParent() != null);
        return methods;
    }

    private void findMethodBodyVariable(Project project, JavaPsiFacade javaPsiFacade, PsiElement method, PsiLocalVariable focusLocalVariable, List<BeanDefinition> methodAllVariable, List<String> alreadyAssignedFiledNames) {
        method.accept(new JavaRecursiveElementVisitor() {
                          @Override
                          public void visitLocalVariable(PsiLocalVariable localVariable) {
                              if (!localVariable.getName().equals(focusLocalVariable.getName())) {
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
                              if (expression.getText().contains(focusLocalVariable.getName())) {
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
