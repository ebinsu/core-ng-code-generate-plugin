package core.framework.plugin;

import com.intellij.psi.PsiPrimitiveType;
import com.intellij.psi.PsiType;
import info.debatty.java.stringsimilarity.Levenshtein;

/**
 * @author ebin
 */
public final class PsiUtils {
    public static final String JAVA_PACKAGE = "java.";
    public static final Levenshtein levenshtein = new Levenshtein();

    private PsiUtils() {
    }

    public static boolean isJavaBean(PsiType type) {
        return !(type instanceof PsiPrimitiveType) && !type.getCanonicalText().startsWith(JAVA_PACKAGE);
    }

    public static double filedSimilarity(final String s1, final String s2) {
        return levenshtein.distance(s1, s2);
    }
}
