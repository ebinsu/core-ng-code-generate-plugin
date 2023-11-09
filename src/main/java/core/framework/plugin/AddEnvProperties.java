package core.framework.plugin;

import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.PlatformDataKeys;
import com.intellij.openapi.command.WriteCommandAction;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.fileEditor.FileDocumentManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.Messages;
import com.intellij.openapi.util.text.Strings;
import com.intellij.openapi.vfs.VfsUtil;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiManager;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.io.IOException;
import java.io.StringReader;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Properties;

/**
 * @author ebin
 */
public class AddEnvProperties extends AnAction {
    private static String LOCAL_ENV_PATH = "/src/main/resources";

    @Override
    public void actionPerformed(@NotNull AnActionEvent e) {
        Project project = e.getData(PlatformDataKeys.PROJECT);
        VirtualFile virtualFile = e.getData(PlatformDataKeys.VIRTUAL_FILE);
        if (project == null || virtualFile == null) {
            return;
        }
        String filename = virtualFile.getName();
        if (!isPropertiesFile(filename)) {
            return;
        }
        String currentFilePath = virtualFile.getPath();
        if (!isEnvFile(currentFilePath)) {
            return;
        }
        FileDocumentManager documentManager = FileDocumentManager.getInstance();
        Document doc = documentManager.getDocument(virtualFile);
        if (doc == null) {
            return;
        }

        InputDialogWrapper dialog = new InputDialogWrapper("Input properties", "");
        dialog.show();
        String inputText = dialog.inputText;
        if (StringUtils.isEmpty(inputText)) {
            return;
        }
        if (!availableInput(inputText)) {
            Messages.showMessageDialog("Invalid properties", "Error", Messages.getInformationIcon());
        }
        String[] split = inputText.split("=");
        if (split.length < 2) {
            Messages.showMessageDialog("Invalid properties", "Error", Messages.getInformationIcon());
        }
        String key = split[0].trim();
        String value = split[1].trim();

        PsiFile file = PsiManager.getInstance(project).findFile(virtualFile);
        if (file == null) {
            return;
        }

        String localPath;
        String devPath;
        String uatPath;
        String prodPath;

        if (currentFilePath.contains("dev")) {
            String servicePath = currentFilePath.substring(0, currentFilePath.indexOf("/conf"));
            localPath = servicePath + LOCAL_ENV_PATH + "/" + filename;
            devPath = currentFilePath;
            uatPath = currentFilePath.replace("dev", "uat");
            prodPath = currentFilePath.replace("dev", "prod");
        } else if (currentFilePath.contains("uat")) {
            String servicePath = currentFilePath.substring(0, currentFilePath.indexOf("/conf"));
            localPath = servicePath + LOCAL_ENV_PATH + "/" + filename;
            devPath = currentFilePath.replace("uat", "dev");
            uatPath = currentFilePath;
            prodPath = currentFilePath.replace("uat", "prod");
        } else if (currentFilePath.contains("prod")) {
            String servicePath = currentFilePath.substring(0, currentFilePath.indexOf("/conf"));
            localPath = servicePath + LOCAL_ENV_PATH + "/" + filename;
            devPath = currentFilePath.replace("prod", "dev");
            uatPath = currentFilePath.replace("prod", "uat");
            prodPath = currentFilePath;
        } else {
            String servicePath = currentFilePath.substring(0, currentFilePath.indexOf(LOCAL_ENV_PATH));
            localPath = currentFilePath;
            devPath = servicePath + "/conf/dev/resources";
            uatPath = servicePath + "/conf/uat/resources";
            prodPath = servicePath + "/conf/prod/resources";
        }

        Document localDoc = getDoc(documentManager, localPath);
        Document devDoc = getDoc(documentManager, devPath);
        Document uatDoc = getDoc(documentManager, uatPath);
        Document prodDoc = getDoc(documentManager, prodPath);

        WriteCommandAction.runWriteCommandAction(project, () -> {
            if (localDoc != null) {
                String propertiesText = localDoc.getText();
                Properties properties = new Properties();
                try {
                    properties.load(new StringReader(propertiesText));
                } catch (IOException ignored) {
                }
                properties.put(key, value);
                Map<String, String> result = new LinkedHashMap<>();
                properties.keySet().stream().sorted().forEach(k -> result.put((String) k, (String) properties.get(k)));
                StringBuilder sb = new StringBuilder();
                result.forEach((k, v) -> sb.append(k).append('=').append(v).append('\n'));

                localDoc.replaceString(0, localDoc.getTextLength(), sb.toString());
            }
            if (devDoc != null) {
                String propertiesText = devDoc.getText();
                Properties properties = new Properties();
                try {
                    properties.load(new StringReader(propertiesText));
                } catch (IOException ignored) {
                }
                String nv = value;
                if (value.contains("uat") || value.contains("prod")) {
                    nv = value.replace("uat", "dev").replace("prod", "dev");
                }
                properties.put(key, nv);
                Map<String, String> result = new LinkedHashMap<>();
                properties.keySet().stream().sorted().forEach(k -> result.put((String) k, (String) properties.get(k)));
                StringBuilder sb = new StringBuilder();
                result.forEach((k, v) -> sb.append(k).append('=').append(v).append('\n'));

                devDoc.replaceString(0, devDoc.getTextLength(), sb.toString());
            }
            if (uatDoc != null) {
                String propertiesText = uatDoc.getText();
                Properties properties = new Properties();
                try {
                    properties.load(new StringReader(propertiesText));
                } catch (IOException ignored) {
                }
                String nv = value;
                if (value.contains("dev") || value.contains("prod")) {
                    nv = value.replace("dev", "uat").replace("prod", "uat");
                }
                properties.put(key, nv);
                Map<String, String> result = new LinkedHashMap<>();
                properties.keySet().stream().sorted().forEach(k -> result.put((String) k, (String) properties.get(k)));
                StringBuilder sb = new StringBuilder();
                result.forEach((k, v) -> sb.append(k).append('=').append(v).append('\n'));

                uatDoc.replaceString(0, uatDoc.getTextLength(), sb.toString());
            }
            if (prodDoc != null) {
                String propertiesText = prodDoc.getText();
                Properties properties = new Properties();
                try {
                    properties.load(new StringReader(propertiesText));
                } catch (IOException ignored) {
                }
                String nv = value;
                if (value.contains("dev") || value.contains("uat")) {
                    nv = value.replace("dev", "prod").replace("uat", "prod");
                }
                properties.put(key, nv);
                Map<String, String> result = new LinkedHashMap<>();
                properties.keySet().stream().sorted().forEach(k -> result.put((String) k, (String) properties.get(k)));
                StringBuilder sb = new StringBuilder();
                result.forEach((k, v) -> sb.append(k).append('=').append(v).append('\n'));

                prodDoc.replaceString(0, prodDoc.getTextLength(), sb.toString());
            }
        });
    }

    private Document getDoc(FileDocumentManager documentManager, String path) {
        Document doc = null;
        VirtualFile localFile = VfsUtil.findFileByIoFile(new File(path), true);
        if (localFile != null) {
            doc = documentManager.getDocument(localFile);
        }
        return doc;
    }

    private boolean availableInput(String inputText) {
        if (Strings.isEmpty(inputText)) {
            return false;
        } else {
            return inputText.contains("=");
        }
    }

    private boolean isEnvFile(String path) {
        if (path.contains(LOCAL_ENV_PATH)) {
            return true;
        } else
            return path.contains("/resources") && (path.contains("dev") || path.contains("uat") || path.contains("prod"));
    }

    private boolean isPropertiesFile(String name) {
        String extension = FilenameUtils.getExtension(name);
        return extension.equals("properties");
    }
}
