package core.framework.plugin;

import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.PlatformDataKeys;
import com.intellij.openapi.command.WriteCommandAction;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.fileEditor.FileDocumentManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.TextRange;
import com.intellij.openapi.util.text.Strings;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.search.FilenameIndex;
import com.intellij.psi.search.GlobalSearchScope;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @author ebin
 */
public class UpdateDependenceVersion extends AnAction {
    private static final Pattern PATTERN = Pattern.compile("\\$\\{.*}");
    private static final String DEF_TPL = "def %1$s = '%2$s'";
    private static final String GRADLE_TPL = "%1$s=%2$s";

    @Override
    public void actionPerformed(@NotNull AnActionEvent e) {
        Project project = e.getData(PlatformDataKeys.PROJECT);
        if (project == null) {
            return;
        }
        Collection<VirtualFile> buildFiles = FilenameIndex.getVirtualFilesByName(
            "build.gradle",
            GlobalSearchScope.FilesScope.allScope(project)
        );
        Collection<VirtualFile> gradleFiles = FilenameIndex.getVirtualFilesByName(
            "gradle.properties",
            GlobalSearchScope.FilesScope.allScope(project)
        );
        FileDocumentManager documentManager = FileDocumentManager.getInstance();
        List<Document> buildDoc = buildFiles.stream().map(documentManager::getDocument).filter(Objects::nonNull).toList();
        List<Document> gradleDoc = gradleFiles.stream().map(documentManager::getDocument).filter(Objects::nonNull).toList();

        //INPUT
        TextAreaDialogWrapper dialog = new TextAreaDialogWrapper("Input Dependence:Version", "");
        dialog.show();
        String inputText = dialog.inputText;
        if (StringUtils.isEmpty(inputText)) {
            return;
        }
        Map<String, String> dependenceVersion = new HashMap<>();
        String[] inputLines = inputText.split("\n");
        for (String line : inputLines) {
            // kitchen-cooking-service-v2-interface : 2.0.0
            // com.wonder:identity-library: 2.0.0
            if (Strings.isEmpty(line)) {
                continue;
            }
            if (line.contains("breaking")) {
                String[] split = line.split("breaking");
                String dependenceName = split[0].trim();
                if (dependenceName.contains(":")) {
                    dependenceName = dependenceName.split(":")[1];
                }
                dependenceVersion.put(dependenceName, split[1].trim());
            } else if (line.contains("minor")) {
                String[] split = line.split("minor");
                String dependenceName = split[0].trim();
                if (dependenceName.contains(":")) {
                    dependenceName = dependenceName.split(":")[1];
                }
                dependenceVersion.put(dependenceName, split[1].trim());
            } else if (line.contains("none")) {
                String[] split = line.split("none");
                String dependenceName = split[0].trim();
                if (dependenceName.contains(":")) {
                    dependenceName = dependenceName.split(":")[1];
                }
                dependenceVersion.put(dependenceName, split[1].trim());
            } else if (line.contains(":")) {
                int indexOf = line.indexOf(":");
                int lastIndexOf = line.lastIndexOf(":");
                String[] split = line.split(":");
                if (indexOf == lastIndexOf) {
                    // only one :
                    dependenceVersion.put(split[0].trim(), split[1].trim());
                } else {
                    dependenceVersion.put(split[split.length - 2].trim(), split[split.length - 1].trim());
                }
            }
        }

        if (dependenceVersion.isEmpty()) {
            return;
        }

        //FROM buildFiles
        Map<String, String> variableDependence = new HashMap<>();
        for (Document doc : buildDoc) {
            int lineCount = doc.getLineCount();
            for (int i = 0; i < lineCount; i++) {
                TextRange textRange = new TextRange(doc.getLineStartOffset(i), doc.getLineEndOffset(i));
                String text = doc.getText(textRange);
                if (text.contains("implementation") || text.contains("runtimeOnly") || text.contains("testRuntimeOnly")) {
                    Matcher matcher = PATTERN.matcher(text);
                    if (matcher.find()) {
                        String group = matcher.group();
                        String variable = group.replace("${", "").replace("}", "");
                        String dependenceName = text.replace("implementation", "")
                            .replace("runtimeOnly", "")
                            .replace("testRuntimeOnly", "")
                            .replace(group, "")
                            .replace("\"", "")
                            .trim();
                        if (dependenceName.endsWith(":")) {
                            dependenceName = dependenceName.substring(0, dependenceName.length() - 1);
                        }
                        if (dependenceName.contains(":")) {
                            dependenceName = dependenceName.split(":")[1];
                        }
                        variableDependence.put(variable, dependenceName);
                    }
                }
            }
        }

        WriteCommandAction.runWriteCommandAction(project, () -> {
            //UPDATE build.gradle def
            for (Document doc : buildDoc) {
                int lineCount = doc.getLineCount();
                for (int i = 0; i < lineCount; i++) {
                    TextRange textRange = new TextRange(doc.getLineStartOffset(i), doc.getLineEndOffset(i));
                    String text = doc.getText(textRange);
                    if (text.startsWith("def") && text.contains("=")) {
                        String leftTxt = text.split("=")[0];
                        String varName = leftTxt.replace("def", "").trim();
                        String dependenceName = variableDependence.get(varName);
                        String version = dependenceVersion.get(dependenceName);
                        if (version != null) {
                            doc.replaceString(textRange.getStartOffset(), textRange.getEndOffset(), String.format(DEF_TPL, varName, version));
                        }
                    }
                }
            }
            //UPDATE gradle.properties
            for (Document doc : gradleDoc) {
                int lineCount = doc.getLineCount();
                for (int i = 0; i < lineCount; i++) {
                    TextRange textRange = new TextRange(doc.getLineStartOffset(i), doc.getLineEndOffset(i));
                    String text = doc.getText(textRange);
                    if (!text.startsWith("#") && text.contains("=")) {
                        String varName = text.split("=")[0].trim();
                        String dependenceName = variableDependence.get(varName);
                        String version = dependenceVersion.get(dependenceName);
                        if (version != null) {
                            doc.replaceString(textRange.getStartOffset(), textRange.getEndOffset(), String.format(GRADLE_TPL, varName, version));
                        }
                    }
                }
            }
        });

    }
}
