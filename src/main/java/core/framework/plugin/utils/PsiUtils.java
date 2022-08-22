package core.framework.plugin.utils;

import com.intellij.psi.PsiAnnotation;
import com.intellij.psi.PsiField;
import com.intellij.psi.PsiPrimitiveType;
import com.intellij.psi.PsiType;

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
}
