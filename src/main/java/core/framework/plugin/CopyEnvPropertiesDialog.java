package core.framework.plugin;

import com.intellij.openapi.command.WriteCommandAction;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.fileEditor.FileDocumentManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VfsUtil;
import com.intellij.openapi.vfs.VirtualFile;

import java.io.File;
import java.io.IOException;
import java.io.StringReader;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

/**
 * @author ebin
 */
public class CopyEnvPropertiesDialog extends CheckBoxDialogWrapper {
    private static final String LOCAL_ENV_PATH = "/src/main/resources";
    public String currentFilePath;
    public Project project;

    public CopyEnvPropertiesDialog(List<String> selections, String currentFilePath, Project project) {
        super("Select Properties", selections);
        this.currentFilePath = currentFilePath;
        this.project = project;
    }

    @Override
    protected void doOKAction() {
        FileDocumentManager documentManager = FileDocumentManager.getInstance();
        VirtualFile virtualFile = getVirtualFile(documentManager, currentFilePath);
        String filename = virtualFile.getName();
        String currentFilePath = virtualFile.getPath();
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

        Properties toBeCopy = new Properties(selects.size());
        selects.forEach(index -> {
            String o = selections.get(index);
            String[] split = o.split("=");
            String key = split[0].trim();
            String value = split[1].trim();
            toBeCopy.put(key, value);
        });
        if (toBeCopy.isEmpty()) {
            return;
        }

        WriteCommandAction.runWriteCommandAction(project, () -> {
            if (localDoc != null) {
                String propertiesText = localDoc.getText();
                Properties properties = new Properties();
                try {
                    properties.load(new StringReader(propertiesText));
                } catch (IOException ignored) {
                }
                properties.putAll(toBeCopy);
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
                toBeCopy.forEach((k, v) -> {
                    String nv = v.toString();
                    if (nv.contains("uat") || nv.contains("prod")) {
                        nv = nv.replace("uat", "dev").replace("prod", "dev");
                    }
                    properties.put(k, nv);
                });
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
                toBeCopy.forEach((k, v) -> {
                    String nv = v.toString();
                    if (nv.contains("dev") || nv.contains("prod")) {
                        nv = nv.replace("dev", "uat").replace("prod", "uat");
                    }
                    properties.put(k, nv);
                });
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
                toBeCopy.forEach((k, v) -> {
                    String nv = v.toString();
                    if (nv.contains("dev") || nv.contains("uat")) {
                        nv = nv.replace("dev", "prod").replace("uat", "prod");
                    }
                    properties.put(k, nv);
                });
                Map<String, String> result = new LinkedHashMap<>();
                properties.keySet().stream().sorted().forEach(k -> result.put((String) k, (String) properties.get(k)));
                StringBuilder sb = new StringBuilder();
                result.forEach((k, v) -> sb.append(k).append('=').append(v).append('\n'));

                prodDoc.replaceString(0, prodDoc.getTextLength(), sb.toString());
            }
        });
        super.doOKAction();
    }

    private VirtualFile getVirtualFile(FileDocumentManager documentManager, String path) {
        return VfsUtil.findFileByIoFile(new File(path), true);
    }

    private Document getDoc(FileDocumentManager documentManager, String path) {
        Document doc = null;
        VirtualFile localFile = VfsUtil.findFileByIoFile(new File(path), true);
        if (localFile != null) {
            doc = documentManager.getDocument(localFile);
        }
        return doc;
    }
}
