package core.framework.plugin.mock;

import com.intellij.ide.highlighter.JavaFileType;
import com.intellij.lang.ASTNode;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.CommonDataKeys;
import com.intellij.openapi.actionSystem.PlatformDataKeys;
import com.intellij.openapi.command.WriteCommandAction;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.fileTypes.FileType;
import com.intellij.openapi.project.Project;
import com.intellij.psi.JavaPsiFacade;
import com.intellij.psi.JavaRecursiveElementVisitor;
import com.intellij.psi.PsiAssignmentExpression;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiElementFactory;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiLocalVariable;
import com.intellij.psi.PsiParameter;
import com.intellij.psi.PsiParameterList;
import com.intellij.psi.PsiStatement;
import com.intellij.psi.PsiType;
import com.intellij.psi.search.GlobalSearchScope;
import core.framework.plugin.properties.BeanDefinition;
import core.framework.plugin.utils.ClassUtils;
import core.framework.plugin.utils.PsiUtils;
import org.jetbrains.annotations.NotNull;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

/**
 * @author ebin
 */
public class MockBeanGenerator extends AnAction {
    public static final String JAVA_BEAN_TEMPLATE = "%1$s.%2$s=new %3$s();";
    public static final String SET_PROPERTIES_TEMPLATE = "%1$s.%2$s=%3$s;";

    @Override
    public void actionPerformed(@NotNull AnActionEvent e) {
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
        if (!(maybeLocalVariable instanceof PsiLocalVariable focusLocalVariable)) {
            return;
        }
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
        PsiElement method = findMethod(statement);
        if (method == null) {
            return;
        }
        List<BeanDefinition> methodAllVariable = new ArrayList<>();
        List<String> alreadyAssignedFiledNames = new ArrayList<>();
        findMethodParameterVariable(project, javaPsiFacade, method, methodAllVariable);
        findMethodBodyVariable(project, javaPsiFacade, method, focusLocalVariable, methodAllVariable, alreadyAssignedFiledNames);

        PsiElementFactory elementFactory = PsiElementFactory.getInstance(project);
        List<PsiStatement> statements = new ArrayList<>();
        focusBeanDefinition.fields.forEach((fieldName, type) -> {
            if (alreadyAssignedFiledNames.contains(fieldName)) {
                return;
            }
            if (ClassUtils.isJavaBean(type)) {
//                String statementStr = String.format(NON_PROPERTIES_JAVA_BEAN_TEMPLATE, target.variableName, fieldName, target.getSimpleFieldType(fieldName).get());
//                statements.add(elementFactory.createStatementFromText(statementStr, psiFile.getContext()));
//                target.getFieldType(fieldName).ifPresent(beanClassStr -> {
//                    String name = target.variableName + "." + fieldName;
//                    expandJavaBean(project, javaPsiFacade, elementFactory, beanClassStr, name, methodBlock, statements);
//                });
            } else {
                String statementStr = String.format(SET_PROPERTIES_TEMPLATE, focusBeanDefinition.variableName, fieldName, type);
                statements.add(elementFactory.createStatementFromText(statementStr, psiFile.getContext()));
            }
        });
        WriteCommandAction.runWriteCommandAction(project, () -> {
            Collections.reverse(statements);
            for (PsiStatement addStatement : statements) {
                method.addAfter(addStatement, statement);
            }
        });
    }

    public static String mockValue(String type) {
        if (ClassUtils.isDouble(type)) {
            double mock = BigDecimal.valueOf(ThreadLocalRandom.current().nextDouble(0, 10)).setScale(2, RoundingMode.HALF_UP).doubleValue();
            return String.valueOf(mock);
        } else if (ClassUtils.isInteger(type)) {
            int mock = ThreadLocalRandom.current().nextInt(0, 10);
            return String.valueOf(mock);
        } else if (ClassUtils.isLong(type)) {
            long mock = ThreadLocalRandom.current().nextLong(0, 100);
            return String.valueOf(mock);
        } else if (ClassUtils.isBigDecimal(type)) {
            int mock = ThreadLocalRandom.current().nextInt(0, 10);
            return "new BigDecimal(" + mock + ");";
        } else if (ClassUtils.isString(type)) {
            return "Mock";
        }
        return "null";
    }

    public static void main(String[] args) {
        System.out.println(mockValue(Double.class.getName()));
    }

    private PsiElement findMethod(PsiElement statement) {
        PsiElement maybeMethod = statement;
        boolean isMethod;
        do {
            maybeMethod = maybeMethod.getParent();
            if (maybeMethod instanceof ASTNode) {
                isMethod = "METHOD".equals(((ASTNode) maybeMethod).getElementType().toString());
            } else {
                isMethod = true;
            }
        } while (!isMethod);
        return maybeMethod;
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
}
