package core.framework.plugin.utils;

import com.intellij.lang.ASTNode;
import com.intellij.psi.PsiAnnotation;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiExpressionStatement;
import com.intellij.psi.PsiField;
import com.intellij.psi.PsiLambdaExpression;
import com.intellij.psi.PsiMethod;
import com.intellij.psi.PsiModifier;
import com.intellij.psi.PsiPrimitiveType;
import com.intellij.psi.PsiType;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Deque;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

/**
 * @author ebin
 */
public final class PsiUtils {
    public static final String JAVA_PACKAGE = "java.";

    private PsiUtils() {
    }

    public static boolean isJavaBean(PsiType type) {
        return !(type instanceof PsiPrimitiveType) && !type.getCanonicalText().startsWith(JAVA_PACKAGE);
    }

    public static boolean hasAnnotation(PsiField field, String annotationSimpleName) {
        PsiAnnotation[] filedAnnotations = field.getAnnotations();
        for (PsiAnnotation filedAnnotation : filedAnnotations) {
            String qualifiedName = filedAnnotation.getQualifiedName();
            if (qualifiedName != null && qualifiedName.contains(annotationSimpleName)) {
                return true;
            }
        }
        return false;
    }

    public static PsiClass findClass(PsiElement statement) {
        PsiElement maybeClass = statement;
        do {
            maybeClass = maybeClass.getParent();
            if (maybeClass instanceof PsiClass) {
                return (PsiClass) maybeClass;
            }
        } while (maybeClass.getParent() != null);
        return null;
    }

    @NotNull
    public static List<PsiElement> findMethods(PsiElement statement) {
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

    @NotNull
    public static Optional<PsiElement> findMethod(PsiElement statement) {
        PsiElement maybeMethod = statement;
        do {
            maybeMethod = maybeMethod.getParent();
            if (maybeMethod instanceof PsiMethod) {
                return Optional.of(maybeMethod);
            } else if (maybeMethod instanceof ASTNode) {
                String type = ((ASTNode) maybeMethod).getElementType().toString();
                if ("METHOD".equals(type)) {
                    return Optional.of(maybeMethod);
                }
            }
        } while (maybeMethod.getParent() != null);
        return Optional.empty();
    }

    public static Optional<PsiElement> findExpressionStatement(PsiElement psiElement) {
        if (psiElement instanceof PsiExpressionStatement) {
            return Optional.of(psiElement);
        }
        PsiElement maybeStatement = psiElement;
        do {
            maybeStatement = maybeStatement.getParent();
            if (maybeStatement instanceof PsiExpressionStatement) {
                return Optional.of(maybeStatement);
            }
        } while (maybeStatement.getParent() != null);
        return Optional.empty();
    }

    @NotNull
    public static List<PsiField> findPublicFields(PsiClass typeClass) {
        PsiField[] classFields = typeClass.getFields();
        return Arrays.stream(classFields).filter(filed -> filed.getModifierList() != null)
            .filter(field -> !field.getModifierList().hasModifierProperty(PsiModifier.PRIVATE)
                && !field.getModifierList().hasModifierProperty(PsiModifier.STATIC)
                && !field.getModifierList().hasModifierProperty(PsiModifier.FINAL)
                && !field.getModifierList().hasModifierProperty(PsiModifier.PROTECTED)
                && !field.getModifierList().hasModifierProperty(PsiModifier.VOLATILE)
            ).toList();
    }

    public static String getSourceStyleClassName(PsiClass psiClass) {
        Deque<String> names = new ArrayDeque<>();
        PsiClass current = psiClass;

        while (current != null) {
            names.push(current.getName());
            current = current.getContainingClass();
        }

        return String.join(".", names);
    }
}
