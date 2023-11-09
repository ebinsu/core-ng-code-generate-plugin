package core.framework.plugin;

import com.intellij.codeInsight.daemon.RelatedItemLineMarkerInfo;
import com.intellij.codeInsight.daemon.RelatedItemLineMarkerProvider;
import com.intellij.codeInsight.navigation.NavigationGutterIconBuilder;
import com.intellij.icons.AllIcons;
import com.intellij.lang.jvm.annotation.JvmAnnotationAttribute;
import com.intellij.openapi.editor.markup.GutterIconRenderer;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.JavaPsiFacade;
import com.intellij.psi.PsiAnnotation;
import com.intellij.psi.PsiAnnotationMemberValue;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiField;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiLiteralExpression;
import com.intellij.psi.PsiManager;
import com.intellij.psi.PsiNameIdentifierOwner;
import com.intellij.psi.PsiReferenceExpression;
import com.intellij.psi.impl.source.PsiClassImpl;
import com.intellij.psi.impl.source.PsiJavaFileImpl;
import com.intellij.psi.impl.source.tree.java.PsiNameValuePairImpl;
import com.intellij.psi.impl.source.tree.java.PsiReferenceExpressionImpl;
import com.intellij.psi.search.FilenameIndex;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.util.PlatformIcons;
import org.jetbrains.annotations.NotNull;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;

/**
 * @author ebin
 */
public class DomainToSqlLineMarkerProvider extends RelatedItemLineMarkerProvider {

    @Override
    protected void collectNavigationMarkers(@NotNull PsiElement element, @NotNull Collection<? super RelatedItemLineMarkerInfo<?>> result) {
        String tableName = null;
        if (element.getText().contains("@Table") && element instanceof PsiClassImpl psiClass) {
            Project project = element.getProject();
            JavaPsiFacade javaPsiFacade = JavaPsiFacade.getInstance(project);
            PsiAnnotation[] annotations = psiClass.getAnnotations();
            for (PsiAnnotation annotation : annotations) {
                String qualifiedName = annotation.getQualifiedName();
                if (qualifiedName != null && annotation.getQualifiedName().contains("Table")) {
                    List<JvmAnnotationAttribute> attributes = annotation.getAttributes();
                    for (JvmAnnotationAttribute attribute : attributes) {
                        if ("name".equals(attribute.getAttributeName())) {
                            if (attribute instanceof PsiNameValuePairImpl pair) {
                                PsiAnnotationMemberValue value = pair.getValue();
                                if (value instanceof PsiReferenceExpression referenceExpression) {
                                    PsiClass refClass = javaPsiFacade.findClass(
                                        ((PsiReferenceExpressionImpl) referenceExpression.getFirstChild()).getCanonicalText(),
                                        GlobalSearchScope.allScope(element.getProject())
                                    );
                                    if (refClass != null) {
                                        PsiField fieldByName = refClass.findFieldByName(referenceExpression.getReferenceName(), true);
                                        if (fieldByName != null) {
                                            tableName = Arrays.stream(fieldByName.getChildren()).filter(f -> f instanceof PsiLiteralExpression).findFirst().map(m -> {
                                                String text = m.getText();
                                                if (text.contains("\"")) {
                                                    text = text.replace("\"", "");
                                                }
                                                return text;
                                            }).orElse(null);
                                        }
                                    }
                                } else {
                                    tableName = pair.getLiteralValue();
                                }
                            }
                        }
                    }
                }
            }
        }
        if (tableName != null) {
            PsiNameIdentifierOwner owner = (PsiNameIdentifierOwner) element;
            Collection<VirtualFile> files = FilenameIndex.getVirtualFilesByName("R__" + tableName + ".sql", GlobalSearchScope.FilesScope.allScope(element.getProject()));
            if (files.isEmpty()) {
                return;
            }
            String path = ((PsiJavaFileImpl) element.getParent()).getVirtualFile().getPath();
            int srcIndex = path.indexOf("/src");
            path = path.substring(0, srcIndex);
            int lasted = path.lastIndexOf("/");
            path = path.substring(lasted + 1);

            String finalPath = path;
            VirtualFile virtualFile = files.stream().filter(f -> f.getPath().contains(finalPath)).findFirst().orElse(null);
            if (virtualFile == null) {
                return;
            }
            PsiFile file = PsiManager.getInstance(element.getProject()).findFile(virtualFile);
            NavigationGutterIconBuilder<PsiElement> builder = NavigationGutterIconBuilder.create(AllIcons.Providers.Mysql)
                .setAlignment(GutterIconRenderer.Alignment.CENTER)
                .setTargets(file)
                .setTooltipText("Navigation to db migration sql file");
            PsiElement nameIdentifier = owner.getNameIdentifier();
            if (nameIdentifier != null) {
                result.add(builder.createLineMarkerInfo(nameIdentifier));
            }
        }
    }
}
