package core.framework.plugin.sql;

import com.intellij.codeInsight.daemon.RelatedItemLineMarkerInfo;
import com.intellij.codeInsight.daemon.RelatedItemLineMarkerProvider;
import com.intellij.codeInsight.navigation.NavigationGutterIconBuilder;
import com.intellij.icons.AllIcons;
import com.intellij.lang.jvm.annotation.JvmAnnotationAttribute;
import com.intellij.openapi.editor.markup.GutterIconRenderer;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VfsUtil;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.vfs.newvfs.impl.VirtualDirectoryImpl;
import com.intellij.psi.JavaPsiFacade;
import com.intellij.psi.PsiAnnotation;
import com.intellij.psi.PsiAnnotationMemberValue;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiField;
import com.intellij.psi.PsiLiteralExpression;
import com.intellij.psi.PsiPackage;
import com.intellij.psi.PsiReferenceExpression;
import com.intellij.psi.impl.JavaPsiFacadeImpl;
import com.intellij.psi.impl.file.PsiJavaDirectoryImpl;
import com.intellij.psi.impl.source.PsiPlainTextFileImpl;
import com.intellij.psi.impl.source.tree.java.PsiNameValuePairImpl;
import com.intellij.psi.impl.source.tree.java.PsiReferenceExpressionImpl;
import com.intellij.psi.search.GlobalSearchScope;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @author ebin
 */
public class SqlFileToDomainLineMarkerProvider extends RelatedItemLineMarkerProvider {
    private static final String MIGRATE_EXT = "db-migration";
    private static final Pattern PATTERN = Pattern.compile("^CREATE TABLE.*`.*`");

    @Override
    protected void collectNavigationMarkers(@NotNull PsiElement element, @NotNull Collection<? super RelatedItemLineMarkerInfo<?>> result) {
        if (element.getParent() != null) {
            PsiElement parent = element.getParent();
            if (parent instanceof PsiPlainTextFileImpl file) {
                if (!"sql".equals(file.getVirtualFile().getExtension())) {
                    return;
                }
            }
        }
        String fullText = element.getText();
        if (fullText.startsWith("CREATE TABLE") && element.getParent() != null) {
            Matcher matcher = PATTERN.matcher(fullText);
            if (matcher.find()) {
                Project project = element.getProject();
                JavaPsiFacade javaPsiFacade = JavaPsiFacade.getInstance(project);

                String group = matcher.group();
                String tableName = group.substring(group.indexOf("`")).replace("`", "");

                String path = getPath(element);
                //eg: C:/workspace/shipping-project/backend/shipping-service-db-migration/src/main/resources/db/migration
                if (path == null) {
                    return;
                }
                int index = path.indexOf(MIGRATE_EXT);
                if (index == -1) {
                    return;
                }
                path = path.substring(0, index + MIGRATE_EXT.length());
                path = path.replace("-db-migration", "");
                //eg: C:/workspace/shipping-project/backend/shipping-service
                path += "/src/main/java/app";

                VirtualFile appPath = VfsUtil.findFileByIoFile(new File(path), true);
                if (appPath == null) {
                    return;
                }
                List<VirtualFile> virtualFiles = VfsUtil.collectChildrenRecursively(appPath);
                String finalPath = path;
                List<String> domainPackageNames = virtualFiles.stream()
                    .filter(f -> f instanceof VirtualDirectoryImpl && f.getPath().contains("domain"))
                    .map(m -> ("app" + m.getPath().replace(finalPath, "")).replace("/", "."))
                    .toList();
                GlobalSearchScope globalSearchScope = GlobalSearchScope.allScope(element.getProject());
                PsiClass targetPsiClass = null;
                for (String domainPackageName : domainPackageNames) {
                    PsiPackage aPackage = javaPsiFacade.findPackage(domainPackageName);
                    if (aPackage == null) {
                        continue;
                    }
                    Set<String> classNames = ((JavaPsiFacadeImpl) javaPsiFacade).getClassNames(aPackage, globalSearchScope);
                    for (String className : classNames) {
                        PsiClass psiClass = javaPsiFacade.findClass(domainPackageName + "." + className, globalSearchScope);
                        if (psiClass == null) {
                            continue;
                        }
                        PsiAnnotation[] annotations = psiClass.getAnnotations();
                        String annTableName = findTableName(javaPsiFacade, annotations, globalSearchScope);
                        if (tableName.equals(annTableName)) {
                            targetPsiClass = psiClass;
                            break;
                        }
                    }
                    if (targetPsiClass != null) {
                        break;
                    }
                }
                if (targetPsiClass != null) {
                    NavigationGutterIconBuilder<PsiElement> builder = NavigationGutterIconBuilder.create(AllIcons.FileTypes.Java)
                        .setTarget(targetPsiClass)
                        .setTooltipText("Navigate to " + targetPsiClass.getQualifiedName())
                        .setAlignment(GutterIconRenderer.Alignment.CENTER);
                    result.add(builder.createLineMarkerInfo(element.getParent()));
                }
            }
        }
    }

    private static String getPath(PsiElement element) {
        if (element instanceof PsiJavaDirectoryImpl directory) {
            return directory.getVirtualFile().getPath();
        }
        PsiElement parent = element;
        do {
            parent = parent.getParent();
            if (parent instanceof PsiJavaDirectoryImpl directory) {
                return directory.getVirtualFile().getPath();
            }
        } while (parent.getParent() != null);
        return null;
    }

    private String findTableName(JavaPsiFacade javaPsiFacade, PsiAnnotation[] annotations, GlobalSearchScope globalSearchScope) {
        String annTableName = null;
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
                                    globalSearchScope
                                );
                                if (refClass != null) {
                                    PsiField fieldByName = refClass.findFieldByName(referenceExpression.getReferenceName(), true);
                                    if (fieldByName != null) {
                                        annTableName = Arrays.stream(fieldByName.getChildren()).filter(f -> f instanceof PsiLiteralExpression).findFirst().map(m -> {
                                            String text = m.getText();
                                            if (text.contains("\"")) {
                                                text = text.replace("\"", "");
                                            }
                                            return text;
                                        }).orElse(null);
                                    }
                                }
                            } else {
                                annTableName = pair.getLiteralValue();
                            }
                        }
                    }
                }
            }
        }
        return annTableName;
    }
}
