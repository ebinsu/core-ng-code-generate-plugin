package core.framework.plugin.generator.collection;

import com.intellij.codeInsight.intention.IntentionAction;
import com.intellij.codeInsight.intention.PsiElementBaseIntentionAction;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.popup.ListPopup;
import com.intellij.psi.JavaPsiFacade;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiExpression;
import com.intellij.psi.PsiIdentifier;
import com.intellij.psi.PsiMethodCallExpression;
import com.intellij.psi.PsiReferenceExpression;
import com.intellij.psi.PsiType;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.util.IncorrectOperationException;
import core.framework.plugin.generator.bean.BeanDefinition;
import core.framework.plugin.utils.ClassUtils;
import org.jetbrains.annotations.NotNull;

import java.util.Arrays;
import java.util.Optional;
import java.util.stream.Stream;

/**
 * @author ebin
 */
public abstract class AbstractMapIntentionAction extends PsiElementBaseIntentionAction implements IntentionAction {
    @Override
    public void invoke(@NotNull Project project, Editor editor, @NotNull PsiElement element) throws IncorrectOperationException {
        PsiType type = getPsiType(element);
        if (type == null) {
            return;
        }
        String name = getName(element);
        if (name == null) {
            return;
        }
        String canonicalText = type.getCanonicalText();
        int start = canonicalText.indexOf("<");
        int end = canonicalText.indexOf(">");
        if (start != -1 && end != -1) {
            canonicalText = canonicalText.substring(start + 1, end);
        }
        if (!ClassUtils.isJavaBean(canonicalText)) {
            return;
        }
        JavaPsiFacade javaPsiFacade = JavaPsiFacade.getInstance(project);
        PsiClass selectClass = javaPsiFacade.findClass(
            canonicalText,
            GlobalSearchScope.allScope(project)
        );
        if (selectClass == null) {
            return;
        }
        BeanDefinition beanDefinition = new BeanDefinition(selectClass, name);
        if (beanDefinition.fields.isEmpty()) {
            return;
        }

        ListPopup listPopup = popup(project, editor, beanDefinition, name, type);
        listPopup.showInBestPositionFor(editor);
    }

    protected abstract ListPopup popup(@NotNull Project project, Editor editor, BeanDefinition beanDefinition, String name, PsiType type);

    private PsiType getPsiType(@NotNull PsiElement element) {
        if (element instanceof PsiIdentifier psiIdentifier
            && psiIdentifier.getParent() instanceof PsiReferenceExpression expression
            && expression.getType() != null) {
            return expression.getType();
        }
        PsiElement prevSibling = element.getPrevSibling();
        if (prevSibling != null) {
            @NotNull PsiElement[] children = prevSibling.getChildren();
            Optional<@NotNull PsiElement> optional = Stream.of(children).filter(f -> f instanceof PsiReferenceExpression).findFirst();
            if (optional.isPresent()) {
                PsiElement psiElement = optional.get();
                if (psiElement instanceof PsiExpression expression && expression.getType() != null) {
                    return expression.getType();
                }
            } else {
                Optional<@NotNull PsiElement> methodCallOptional = Arrays.stream(children).filter(f -> f instanceof PsiMethodCallExpression).findFirst();
                if (methodCallOptional.isPresent()) {
                    PsiElement psiElement = methodCallOptional.get();
                    Optional<@NotNull PsiElement> expressionOptional = Arrays.stream(psiElement.getChildren()).filter(f -> f instanceof PsiExpression).findFirst();
                    if (expressionOptional.isPresent()) {
                        PsiElement e = expressionOptional.get();
                        if (e instanceof PsiExpression expression && expression.getType() != null) {
                            return expression.getType();
                        }
                    }
                }
            }
        }
        return null;
    }

    private String getName(@NotNull PsiElement element) {
        if (element instanceof PsiIdentifier) {
            return element.getText();
        }
        PsiElement prevSibling = element.getPrevSibling();
        if (prevSibling != null) {
            @NotNull PsiElement[] children = prevSibling.getChildren();
            Optional<@NotNull PsiElement> optional = Stream.of(children).filter(f -> f instanceof PsiReferenceExpression).findFirst();
            if (optional.isPresent()) {
                PsiElement psiElement = optional.get();
                if (psiElement instanceof PsiExpression expression && expression.getType() != null) {
                    return Arrays.stream(expression.getChildren()).filter(f -> f instanceof PsiIdentifier).findFirst().map(PsiElement::getText).orElse(null);
                }
            } else {
                Optional<@NotNull PsiElement> methodCallOptional = Arrays.stream(children).filter(f -> f instanceof PsiMethodCallExpression).findFirst();
                if (methodCallOptional.isPresent()) {
                    PsiElement psiElement = methodCallOptional.get();
                    Optional<@NotNull PsiElement> expressionOptional = Arrays.stream(psiElement.getChildren()).filter(f -> f instanceof PsiExpression).findFirst();
                    if (expressionOptional.isPresent()) {
                        PsiElement e = expressionOptional.get();
                        if (e instanceof PsiExpression expression && expression.getType() != null) {
                            return expression.getText();
                        }
                    }
                }
            }
        }
        return null;
    }

    @Override
    public boolean isAvailable(@NotNull Project project, Editor editor, @NotNull PsiElement element) {
        if (element instanceof PsiIdentifier psiIdentifier
            && psiIdentifier.getParent() instanceof PsiReferenceExpression expression
            && expression.getType() != null) {
            return isAvailableType(expression);
        }
        PsiElement prevSibling = element.getPrevSibling();
        if (prevSibling != null) {
            @NotNull PsiElement[] children = prevSibling.getChildren();
            Optional<@NotNull PsiElement> optional = Arrays.stream(children).filter(f -> f instanceof PsiReferenceExpression).findFirst();
            if (optional.isEmpty()) {
                Optional<@NotNull PsiElement> methodCallOptional = Arrays.stream(children).filter(f -> f instanceof PsiMethodCallExpression).findFirst();
                if (methodCallOptional.isEmpty()) {
                    return false;
                } else {
                    PsiElement psiElement = methodCallOptional.get();
                    Optional<@NotNull PsiElement> expressionOptional = Arrays.stream(psiElement.getChildren()).filter(f -> f instanceof PsiExpression).findFirst();
                    if (expressionOptional.isEmpty()) {
                        return false;
                    } else {
                        PsiElement e = expressionOptional.get();
                        return isAvailableType(e);
                    }
                }
            } else {
                PsiElement psiElement = optional.get();
                return isAvailableType(psiElement);
            }
        }
        return false;
    }

    @NotNull
    public String getText() {
        return getFamilyName();
    }

    @Override
    public boolean startInWriteAction() {
        return false;
    }

    private boolean isAvailableType(PsiElement psiElement) {
        if (psiElement instanceof PsiExpression expression && expression.getType() != null) {
            String type = expression.getType().getCanonicalText();
            return ClassUtils.isList(type) || ClassUtils.isSet(type) || ClassUtils.isStream(type);
        }
        return false;
    }
}
