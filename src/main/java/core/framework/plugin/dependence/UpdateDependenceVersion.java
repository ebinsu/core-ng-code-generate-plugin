package core.framework.plugin.dependence;

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
    private static final Pattern PATTERN = Pattern.compile("(implementation|runtimeOnly|testRuntimeOnly)\\s*\\(\".*:(?<dependenceName>.*):\\$\\{(?<variable>.*)}");
    private static final Pattern SETTINGS_GRADLE_PATTERN = Pattern.compile("library\\s*.*,\\s*\"(?<dependence>.*)\"");
    private static final Pattern INTERFACE_PATTERN = Pattern.compile("[^\\s:]+-[^\\s:]+");
    private static final Pattern VERSON_PATTERN = Pattern.compile("\\d+.\\d+.\\d+");
    private static final String DEF_TPL = "val %1$s = \"%2$s\"";
    private static final String GRADLE_TPL = "%1$s=%2$s";
    private static final String FORMAT = "\n************************************************\n";

    @Override
    public void actionPerformed(@NotNull AnActionEvent e) {
        Project project = e.getData(PlatformDataKeys.PROJECT);
        if (project == null) {
            return;
        }
        Collection<VirtualFile> buildFiles = FilenameIndex.getVirtualFilesByName(
            "build.gradle.kts",
            GlobalSearchScope.FilesScope.allScope(project)
        );
        Collection<VirtualFile> gradleFiles = FilenameIndex.getVirtualFilesByName(
            "gradle.properties",
            GlobalSearchScope.FilesScope.allScope(project)
        );
        Collection<VirtualFile> settingsGradleFiles = FilenameIndex.getVirtualFilesByName(
            "settings.gradle.kts",
            GlobalSearchScope.FilesScope.allScope(project)
        );
        FileDocumentManager documentManager = FileDocumentManager.getInstance();
        List<Document> buildDoc = buildFiles.stream().map(documentManager::getDocument).filter(Objects::nonNull).toList();
        List<Document> gradleDoc = gradleFiles.stream().map(documentManager::getDocument).filter(Objects::nonNull).toList();
        List<Document> settingsGradleDoc = settingsGradleFiles.stream().map(documentManager::getDocument).filter(Objects::nonNull).toList();

        //INPUT
        TextAreaDialogWrapper dialog = new TextAreaDialogWrapper("Input Dependence:Version", "");
        dialog.show();
        String inputText = dialog.inputText;
        if (dialog.cancel || StringUtils.isEmpty(inputText)) {
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
                //  implementation("com.zendesk:sunshine-conversations-client:${sunshineConversationsVersion}")
                if (text.contains("implementation") || text.contains("runtimeOnly") || text.contains("testRuntimeOnly")) {
                    Matcher matcher = PATTERN.matcher(text);
                    if (matcher.find()) {
                        String dependenceName = matcher.group("dependenceName");
                        String variable = matcher.group("variable");
                        if (StringUtils.isNotEmpty(dependenceName) && StringUtils.isNotEmpty(variable)) {
                            variableDependence.put(variable, dependenceName);
                        }
                    }
                }
            }
        }

        Map<String, List<Pair<String, String>>> changeHistories = previewChange(buildDoc, variableDependence, dependenceVersion, gradleDoc, settingsGradleDoc);
        TextAreaDialogWrapper historyDialog = new TextAreaDialogWrapper("Preview Change Histories", "");
        historyDialog.setInputText(changeHistory(changeHistories));
        historyDialog.show();

        if (historyDialog.cancel) {
            return;
        }

        WriteCommandAction.runWriteCommandAction(project, () -> {
            //UPDATE build.gradle def
            for (Document doc : buildDoc) {
                int lineCount = doc.getLineCount();
                for (int i = 0; i < lineCount; i++) {
                    TextRange textRange = new TextRange(doc.getLineStartOffset(i), doc.getLineEndOffset(i));
                    String text = doc.getText(textRange);
                    if (text.startsWith("val") && text.contains("=")) {
                        String leftTxt = text.split("=")[0];
                        String varName = leftTxt.replace("val", "").trim();
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
            //UPDATE settings.gradle.kts
            for (Document doc : settingsGradleDoc) {
                int lineCount = doc.getLineCount();
                for (int i = 0; i < lineCount; i++) {
                    TextRange textRange = new TextRange(doc.getLineStartOffset(i), doc.getLineEndOffset(i));
                    String text = doc.getText(textRange);
                    if (text.trim().startsWith("library")) {
                        Matcher matcher = SETTINGS_GRADLE_PATTERN.matcher(text);
                        if (matcher.find()) {
                            String dependenceStr = matcher.group("dependence");
                            if (StringUtils.isNotEmpty(dependenceStr)) {
                                Matcher dependenceNameMatcher = INTERFACE_PATTERN.matcher(dependenceStr);
                                Matcher versionMatcher = VERSON_PATTERN.matcher(dependenceStr);
                                if (dependenceNameMatcher.find() && versionMatcher.find()) {
                                    String dependenceName = dependenceNameMatcher.group();
                                    String oldVersion = versionMatcher.group();
                                    String version = dependenceVersion.get(dependenceName);
                                    if (version != null && !version.equals(oldVersion)) {
                                        String newText = text.replace(oldVersion, version);
                                        doc.replaceString(textRange.getStartOffset(), textRange.getEndOffset(), newText);
                                    }
                                }
                            }
                        }
                    }
                }
            }
        });
    }

    private Map<String, List<Pair<String, String>>> previewChange(List<Document> buildDoc,
                                                                  Map<String, String> variableDependence,
                                                                  Map<String, String> dependenceVersion,
                                                                  List<Document> gradleDoc,
                                                                  List<Document> settingsGradleDoc) {
        Map<String, List<Pair<String, String>>> changeHistories = new LinkedHashMap<>();
        //UPDATE build.gradle def
        for (Document doc : buildDoc) {
            int lineCount = doc.getLineCount();
            for (int i = 0; i < lineCount; i++) {
                TextRange textRange = new TextRange(doc.getLineStartOffset(i), doc.getLineEndOffset(i));
                String text = doc.getText(textRange);
                if (text.startsWith("val") && text.contains("=")) {
                    String leftTxt = text.split("=")[0];
                    String varName = leftTxt.replace("val", "").trim();
                    String dependenceName = variableDependence.get(varName);
                    String version = dependenceVersion.get(dependenceName);
                    if (version != null) {
                        String newDef = String.format(DEF_TPL, varName, version);
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
                        String fileName = doc.toString().replace("DocumentImpl[file://", "").replace("]", "");
                        changeHistories.computeIfAbsent(fileName, k -> new ArrayList<>()).add(Pair.of(text, newDef));
                    }
                }
            }
        }

        for (Document doc : settingsGradleDoc) {
            int lineCount = doc.getLineCount();
            for (int i = 0; i < lineCount; i++) {
                TextRange textRange = new TextRange(doc.getLineStartOffset(i), doc.getLineEndOffset(i));
                String text = doc.getText(textRange);
                if (text.trim().startsWith("library")) {
                    Matcher matcher = SETTINGS_GRADLE_PATTERN.matcher(text);
                    if (matcher.find()) {
                        String dependenceStr = matcher.group("dependence");
                        if (StringUtils.isNotEmpty(dependenceStr)) {
                            Matcher dependenceNameMatcher = INTERFACE_PATTERN.matcher(dependenceStr);
                            Matcher versionMatcher = VERSON_PATTERN.matcher(dependenceStr);
                            if (dependenceNameMatcher.find() && versionMatcher.find()) {
                                String dependenceName = dependenceNameMatcher.group();
                                String oldVersion = versionMatcher.group();
                                String version = dependenceVersion.get(dependenceName);
                                if (version != null && !version.equals(oldVersion)) {
                                    String newText = text.replace(oldVersion, version);
                                    String fileName = doc.toString().replace("DocumentImpl[file://", "").replace("]", "");
                                    changeHistories.computeIfAbsent(fileName, k -> new ArrayList<>()).add(Pair.of(text, newText));
                                }
                            }
                        }
                    }
                }
            }
        }

        return changeHistories;
    }

    private String inputVersion(Map<String, String> dependenceVersion) {
        return dependenceVersion.entrySet().stream().map(m -> m.getKey() + " : " + m.getValue()).collect(Collectors.joining("\n"));
    }

    private String changeHistory(Map<String, List<Pair<String, String>>> changeHistories) {
        if (changeHistories.isEmpty()) {
            return "No changed!";
        }
        return changeHistories.entrySet().stream().map(m -> m.getKey() + " : \n" +
            m.getValue().stream().map(x -> "--- " + x.getKey() + "\n" + "+++ " + x.getValue()).collect(Collectors.joining("\n\n", FORMAT, FORMAT))
        ).collect(Collectors.joining("\n\n"));
    }
}
