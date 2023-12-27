package core.framework.plugin;

import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.PlatformDataKeys;
import com.intellij.openapi.command.WriteCommandAction;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.fileEditor.FileDocumentManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.Messages;
import com.intellij.openapi.util.TextRange;
import com.intellij.openapi.util.text.Strings;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.search.FilenameIndex;
import com.intellij.psi.search.GlobalSearchScope;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * @author ebin
 */
public class UpdateDependenceVersion extends AnAction {
    private static final Pattern PATTERN = Pattern.compile("\\$\\{.*}");
    private static final Pattern INTERFACE_PATTERN = Pattern.compile("[^\\s:]+-[^\\s:]+");
    private static final Pattern VERSON_PATTERN = Pattern.compile("\\d.\\d.\\d");
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
        Map<String, String> dependenceVersion = new LinkedHashMap<>();
        String[] inputLines = inputText.split("\n");
        for (String line : inputLines) {
            // kitchen-cooking-service-v2-interface : 2.0.0
            // com.wonder:identity-library: 2.0.0
            if (Strings.isEmpty(line)) {
                continue;
            }
            Matcher dependenceNameMatcher = INTERFACE_PATTERN.matcher(line);
            Matcher versionMatcher = VERSON_PATTERN.matcher(line);
            if (dependenceNameMatcher.find() && versionMatcher.find()) {
                String dependenceName = dependenceNameMatcher.group();
                String version = versionMatcher.group();
                dependenceVersion.put(dependenceName.trim(), version.trim());
            }
        }

        if (dependenceVersion.isEmpty()) {
            return;
        }

        Messages.showMessageDialog(inputVersion(dependenceVersion), "Input Version", Messages.getInformationIcon());

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
            Map<String, List<Pair<String, String>>> changeHistories = new LinkedHashMap<>();
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
                            String newDef = String.format(DEF_TPL, varName, version);
                            doc.replaceString(textRange.getStartOffset(), textRange.getEndOffset(), newDef);
                            String fileName = doc.toString().replace("DocumentImpl[file://", "").replace("]", "");
                            changeHistories.computeIfAbsent(fileName, k -> new ArrayList<>()).add(Pair.of(text, newDef));
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
                            String newDef = String.format(GRADLE_TPL, varName, version);
                            doc.replaceString(textRange.getStartOffset(), textRange.getEndOffset(), String.format(GRADLE_TPL, varName, version));
                            String fileName = doc.toString().replace("DocumentImpl[file://", "").replace("]", "");
                            changeHistories.computeIfAbsent(fileName, k -> new ArrayList<>()).add(Pair.of(text, newDef));
                        }
                    }
                }
            }

            Messages.showMessageDialog(changeHistory(changeHistories), "Change Histories", Messages.getInformationIcon());

        });

    }

    private String inputVersion(Map<String, String> dependenceVersion) {
        return dependenceVersion.entrySet().stream().map(m -> m.getKey() + " : " + m.getValue()).collect(Collectors.joining("\n"));
    }

    private String changeHistory(Map<String, List<Pair<String, String>>> changeHistories) {
        if (changeHistories.isEmpty()) {
            return "No changed!";
        }
        return changeHistories.entrySet().stream().map(m -> m.getKey() + " : \n" +
                m.getValue().stream().map(x -> "--- " + x.getKey() + "\n" + "+++ " + x.getValue()).collect(Collectors.joining("\n"))
        ).collect(Collectors.joining("\n"));
    }
}
