package core.framework.plugin.api.link;

import com.intellij.codeInsight.hints.FactoryInlayHintsCollector;
import com.intellij.codeInsight.hints.InlayHintsSink;
import com.intellij.codeInsight.hints.presentation.InlayPresentation;
import com.intellij.codeInsight.hints.presentation.MouseButton;
import com.intellij.codeInsight.hints.presentation.PresentationFactory;
import com.intellij.ide.BrowserUtil;
import com.intellij.lang.jvm.annotation.JvmAnnotationAttribute;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.ui.popup.JBPopupFactory;
import com.intellij.openapi.ui.popup.ListPopup;
import com.intellij.psi.PsiAnnotation;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiJavaFile;
import com.intellij.psi.PsiMethod;
import com.intellij.psi.impl.source.tree.java.PsiNameValuePairImpl;
import org.jetbrains.annotations.NotNull;
import org.jspecify.annotations.NonNull;

import java.awt.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @author ebin
 */
@SuppressWarnings("UnstableApiUsage")
public class ActionLinkCollector extends FactoryInlayHintsCollector {
    private final static Pattern PATTERN = Pattern.compile("([^/\\\\]+)(?=-interface)");
    private final static String DATADOG_URL_TPL = "https://us3.datadoghq.com/apm/traces?query=env:prod service:%1$s resource_name:\"api:%2$s:%3$s\"";
    private final static String KIBANA_TPL = "api:%2$s:%3$s";

    public ActionLinkCollector(@NotNull Editor editor) {
        super(editor);
    }

    @Override
    public boolean collect(@NotNull PsiElement element, @NotNull Editor editor, @NotNull InlayHintsSink sink) {
        if (element instanceof PsiMethod method) {
            PsiElement mabePsiClass = method.getParent();
            if (mabePsiClass instanceof PsiClass psiClass) {
                String psiClassName = psiClass.getName();
                if (psiClassName == null || !psiClassName.contains("WebService")) {
                    return false;
                }
            } else {
                return false;
            }

            String serviceName = null;
            String pathValue = null;
            PsiElement mabePsiJavaFile = mabePsiClass.getParent();
            if (mabePsiJavaFile instanceof PsiJavaFile psiJavaFile) {
                String path = psiJavaFile.getVirtualFile().getPath();
                Matcher matcher = PATTERN.matcher(path);

                if (matcher.find()) {
                    serviceName = matcher.group(1);
                }
            }

            if (serviceName == null) {
                return true;
            }

            PsiAnnotation[] annotations = method.getAnnotations();
            PsiAnnotation pathAnnotation = null;
            String httpMethodType = null;
            for (PsiAnnotation annotation : annotations) {
                String qualifiedName = annotation.getQualifiedName();
                if (qualifiedName == null) {
                    continue;
                }

                if (qualifiedName.contains("core.framework.api.web.service.Path")) {
                    pathAnnotation = annotation;
                } else if (qualifiedName.contains("core.framework.api.web.service.DELETE")) {
                    httpMethodType = "delete";
                } else if (qualifiedName.contains("core.framework.api.web.service.GET")) {
                    httpMethodType = "get";
                } else if (qualifiedName.contains("core.framework.api.web.service.PATCH")) {
                    httpMethodType = "patch";
                } else if (qualifiedName.contains("core.framework.api.web.service.POST")) {
                    httpMethodType = "post";
                } else if (qualifiedName.contains("core.framework.api.web.service.PUT")) {
                    httpMethodType = "put";
                }
            }

            if (pathAnnotation == null || httpMethodType == null) {
                return true;
            }

            JvmAnnotationAttribute attribute = pathAnnotation.getAttributes().getFirst();
            if (attribute instanceof PsiNameValuePairImpl pair) {
                pathValue = pair.getLiteralValue();
            }

            if (pathValue == null) {
                return true;
            }

            var factory = getFactory();

            String datadogUrl = String.format(DATADOG_URL_TPL, serviceName, httpMethodType, pathValue);
            InlayPresentation datadogInline = datadogInlay(factory, datadogUrl);

            String kibanaAction = String.format(KIBANA_TPL, serviceName, httpMethodType, pathValue);
            InlayPresentation kibanaInline = kibanaInline(editor, factory, serviceName, kibanaAction);

            sink.addInlineElement(pathAnnotation.getTextRange().getEndOffset(), true, datadogInline, false);
            sink.addInlineElement(pathAnnotation.getTextRange().getEndOffset(), true, kibanaInline, false);
        }
        return true;
    }

    private @NonNull InlayPresentation kibanaInline(@NonNull Editor editor, PresentationFactory factory, String serviceName, String kibanaAction) {
        InlayPresentation datadog = factory.inset(factory.seq(
            factory.smallText("Kibana")
        ), 15, 0, 5, 0);

        InlayPresentation clickablePresentation = factory.onClick(datadog, MouseButton.Left, (event, point) -> {
            JBPopupFactory jbPopupFactory = JBPopupFactory.getInstance();
            ListPopup listPopup = jbPopupFactory.createListPopup(new KibanaPopupStep(serviceName, kibanaAction));
            listPopup.showInBestPositionFor(editor);
            return null;
        });

        return factory.withCursorOnHover(
            clickablePresentation,
            Cursor.getPredefinedCursor(Cursor.HAND_CURSOR)
        );
    }

    private InlayPresentation datadogInlay(PresentationFactory factory, String url) {
        InlayPresentation datadog = factory.inset(factory.seq(
            factory.smallText("Datadog")
        ), 10, 0, 5, 0);


        InlayPresentation clickablePresentation = factory.onClick(datadog, MouseButton.Left, (event, point) -> {
            BrowserUtil.browse(url);
            return null;
        });

        return factory.withCursorOnHover(
            clickablePresentation,
            Cursor.getPredefinedCursor(Cursor.HAND_CURSOR)
        );
    }


}
