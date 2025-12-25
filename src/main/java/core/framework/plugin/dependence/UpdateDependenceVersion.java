package core.framework.plugin.dependence;

import com.intellij.ide.actions.EditSourceAction;
import com.intellij.openapi.actionSystem.ActionUpdateThread;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.CommonDataKeys;
import com.intellij.openapi.actionSystem.PlatformCoreDataKeys;
import com.intellij.openapi.actionSystem.PlatformDataKeys;
import com.intellij.openapi.command.WriteCommandAction;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.fileEditor.FileDocumentManager;
import com.intellij.openapi.module.Module;
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

import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * @author ebin
 */
public class UpdateDependenceVersion extends EditSourceAction {
    private static final Pattern PATTERN = Pattern.compile("(implementation|runtimeOnly|testRuntimeOnly)\\s*\\(\".*:(?<dependenceName>.*):\\$\\{(?<variable>.*)}");
    private static final Pattern SETTINGS_GRADLE_PATTERN = Pattern.compile("library\\s*.*,\\s*\"(?<dependence>.*)\"");
    private static final Pattern INTERFACE_PATTERN = Pattern.compile("[^\\s:]+-[^\\s:]+");
    private static final Pattern VERSON_PATTERN = Pattern.compile("\\d+.\\d+.\\d+");
    private static final String DEF_TPL = "val %1$s = \"%2$s\"";
    private static final String GRADLE_TPL = "%1$s=%2$s";
    private static final String FORMAT = "\n************************************************\n";

    @Override
    public void update(@NotNull AnActionEvent e) {
        e.getPresentation().setVisible(isClickRootModule(e));
    }

    public @NotNull ActionUpdateThread getActionUpdateThread() {
        return ActionUpdateThread.BGT;
    }

    private boolean isClickRootModule(AnActionEvent e) {
        Project project = getEventProject(e);
        Module module = e.getData(PlatformCoreDataKeys.MODULE);
        if (project != null && module != null) {
            VirtualFile moduleFolder = e.getData(CommonDataKeys.VIRTUAL_FILE);
            if (moduleFolder != null) {
                return project.getName().equals(moduleFolder.getName());
            }
        }
        return false;
    }

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
        List<DocHolder> buildDoc = buildFiles.stream().map(v -> new DocHolder(documentManager.getDocument(v), v)).filter(f -> f.document() != null).toList();
        List<DocHolder> gradleDoc = gradleFiles.stream().map(v -> new DocHolder(documentManager.getDocument(v), v)).filter(f -> f.document() != null).toList();
        List<DocHolder> settingsGradleDoc = settingsGradleFiles.stream().map(v -> new DocHolder(documentManager.getDocument(v), v)).filter(f -> f.document() != null).toList();

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

        Messages.showMessageDialog(inputVersion(dependenceVersion), "Input Version (  " + dependenceVersion.size() + " )", Messages.getInformationIcon());

        //FROM buildFiles
        Map<String, String> variableDependence = new HashMap<>();
        for (DocHolder docHolder : buildDoc) {
            Document doc = docHolder.document();
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

        Map<String, List<Pair<String, String>>> changeHistories = previewChange(variableDependence, dependenceVersion, buildDoc, gradleDoc, settingsGradleDoc);
        TextAreaDialogWrapper historyDialog = new TextAreaDialogWrapper("Preview Change Histories", "");
        historyDialog.setInputText(changeHistory(changeHistories));
        historyDialog.show();

        if (historyDialog.cancel) {
            return;
        }
        WriteDocHandler writeDocHandler = new WriteDocHandler();
        WriteCommandAction.runWriteCommandAction(project, () -> {
            //UPDATE build.gradle def
            handleBuildDoc(variableDependence, dependenceVersion, buildDoc, writeDocHandler);
            //UPDATE gradle.properties
            handleGradleDoc(variableDependence, dependenceVersion, gradleDoc, writeDocHandler);
            //UPDATE settings.gradle.kts
            handleSettingsDoc(dependenceVersion, settingsGradleDoc, writeDocHandler);
        });
    }

    private Map<String, List<Pair<String, String>>> previewChange(Map<String, String> variableDependence,
                                                                  Map<String, String> dependenceVersion,
                                                                  List<DocHolder> buildDoc,
                                                                  List<DocHolder> gradleDoc,
                                                                  List<DocHolder> settingsGradleDoc) {
        Map<String, List<Pair<String, String>>> changeHistories = new LinkedHashMap<>();
        PreviewDocHandler previewDocHandler = new PreviewDocHandler(changeHistories);
        //UPDATE build.gradle def
        handleBuildDoc(variableDependence, dependenceVersion, buildDoc, previewDocHandler);
        //UPDATE gradle.properties
        handleGradleDoc(variableDependence, dependenceVersion, gradleDoc, previewDocHandler);
        //UPDATE settings.gradle.kts
        handleSettingsDoc(dependenceVersion, settingsGradleDoc, previewDocHandler);
        return changeHistories;
    }

    private void handleSettingsDoc(Map<String, String> dependenceVersion, List<DocHolder> settingsGradleDoc,
                                   IDocHandler docHandler) {
        for (DocHolder docHolder : settingsGradleDoc) {
            Document doc = docHolder.document();
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
                                    docHandler.handle(docHolder, textRange, text, newText);
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    private void handleGradleDoc(Map<String, String> variableDependence, Map<String, String> dependenceVersion,
                                 List<DocHolder> gradleDoc, IDocHandler docHandler) {
        for (DocHolder docHolder : gradleDoc) {
            Document doc = docHolder.document();
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
                        docHandler.handle(docHolder, textRange, text, newDef);
                    }
                }
            }
        }
    }

    private void handleBuildDoc(Map<String, String> variableDependence, Map<String, String> dependenceVersion,
                                List<DocHolder> buildDoc, IDocHandler docHandler) {
        for (DocHolder docHolder : buildDoc) {
            Document doc = docHolder.document();
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
                        docHandler.handle(docHolder, textRange, text, newDef);
                    }
                }
            }
        }
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
