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
import com.intellij.psi.*;
import com.intellij.psi.search.GlobalSearchScope;
import core.framework.plugin.properties.BeanDefinition;
import core.framework.plugin.utils.ClassUtils;
import core.framework.plugin.utils.PsiUtils;
import org.apache.commons.lang.RandomStringUtils;
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
                String statementStr = String.format(JAVA_BEAN_TEMPLATE, focusBeanDefinition.variableName, fieldName, focusBeanDefinition.getSimpleFieldType(fieldName).get());
                statements.add(elementFactory.createStatementFromText(statementStr, psiFile.getContext()));
                focusBeanDefinition.getFieldType(fieldName).ifPresent(beanClassStr -> {
                    String name = focusBeanDefinition.variableName + "." + fieldName;
                    expandJavaBean(project, javaPsiFacade, elementFactory, beanClassStr, name, method, statements, 0);
                });
            } else if (ClassUtils.isEnum(type)) {
                String mock = focusBeanDefinition.getSimpleFieldType(fieldName).get() + ".values()[0]";
                String statementStr = String.format(SET_PROPERTIES_TEMPLATE, focusBeanDefinition.variableName, fieldName, mock);
                statements.add(elementFactory.createStatementFromText(statementStr, psiFile.getContext()));
            } else {
                String statementStr = String.format(SET_PROPERTIES_TEMPLATE, focusBeanDefinition.variableName, fieldName, mockValue(type));
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

    private void expandJavaBean(Project project, JavaPsiFacade javaPsiFacade, PsiElementFactory elementFactory, String beanClassStr, String variableName, PsiElement methodBlock, List<PsiStatement> statements, int depth) {
        PsiClass beanClass = javaPsiFacade.findClass(beanClassStr, GlobalSearchScope.allScope(project));
        if (beanClass == null) {
            return;
        }
        BeanDefinition beanDefinition = new BeanDefinition(beanClass, variableName);
        beanDefinition.fields.forEach((fieldName, type) -> {
            if (ClassUtils.isJavaBean(type)) {
                String statementStr = String.format(JAVA_BEAN_TEMPLATE, variableName, fieldName, beanDefinition.getSimpleFieldType(fieldName).get());
                statements.add(elementFactory.createStatementFromText(statementStr, methodBlock.getContext()));
                if (depth >= 2) {
                    return;
                }
                beanDefinition.getFieldType(fieldName).ifPresent(_beanClassStr -> {
                    String name = variableName + "." + fieldName;
                    expandJavaBean(project, javaPsiFacade, elementFactory, _beanClassStr, name, methodBlock, statements, depth + 1);
                });
            } else {
                String statementStr = String.format(SET_PROPERTIES_TEMPLATE, variableName, fieldName, mockValue(type));
                statements.add(elementFactory.createStatementFromText(statementStr, methodBlock.getContext()));
            }
        });
    }

    private String mockValue(String type) {
        if (ClassUtils.isDouble(type)) {
            double mock = BigDecimal.valueOf(ThreadLocalRandom.current().nextDouble(0, 10)).setScale(2, RoundingMode.HALF_UP).doubleValue();
            return String.valueOf(mock);
        } else if (ClassUtils.isInteger(type)) {
            int mock = ThreadLocalRandom.current().nextInt(0, 10);
            return String.valueOf(mock);
        } else if (ClassUtils.isLong(type)) {
            long mock = ThreadLocalRandom.current().nextLong(0, 100);
            return mock + "L";
        } else if (ClassUtils.isBigDecimal(type)) {
            int mock = ThreadLocalRandom.current().nextInt(0, 10);
            return "new BigDecimal(" + mock + ")";
        } else if (ClassUtils.isString(type)) {
            return "\"" + RandomStringUtils.random(10, true, false) + "\"";
        } else if (ClassUtils.isZonedDateTime(type)) {
            return "ZonedDateTime.now()";
        } else if (ClassUtils.isLocalDateTime(type)) {
            return "LocalDateTime.now()";
        } else if (ClassUtils.isLocalDate(type)) {
            return "LocalDate.now()";
        } else if (ClassUtils.isList(type)) {
            return "List.of()";
        } else if (ClassUtils.isSet(type)) {
            return "Set.of()";
        } else if (ClassUtils.isBoolean(type)) {
            return ThreadLocalRandom.current().nextBoolean() ? "Boolean.TRUE" : "Boolean.FALSE";
        }
        return "null";
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
