package core.framework.plugin;

import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.PlatformDataKeys;
import com.intellij.openapi.command.WriteCommandAction;
import com.intellij.openapi.fileTypes.PlainTextFileType;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.Messages;
import com.intellij.openapi.vfs.VfsUtil;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiDirectory;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiFileFactory;
import com.intellij.psi.impl.file.PsiDirectoryFactory;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.util.Optional;

/**
 * @author ebin
 */
public class NewEnvPropertiesFile extends AnAction {
    private static String LOCAL_ENV_PATH = "/src/main/resources";

    @Override
    public void actionPerformed(@NotNull AnActionEvent e) {
        Project project = e.getData(PlatformDataKeys.PROJECT);
        VirtualFile virtualFile = e.getData(PlatformDataKeys.VIRTUAL_FILE);
        if (project == null || virtualFile == null) {
            return;
        }
        while (!virtualFile.isDirectory()) {
            virtualFile = virtualFile.getParent();
        }
        String path = virtualFile.getPath();
        String localPath = null;
        String devPath = null;
        String uatPath = null;
        String prodPath = null;
        Env env;
        if (path.endsWith(LOCAL_ENV_PATH)) {
            env = Env.LOCAL;
            localPath = path;
            String servicePath = path.substring(0, path.indexOf(LOCAL_ENV_PATH));
            devPath = servicePath + "/conf/dev/resources";
            uatPath = servicePath + "/conf/uat/resources";
            prodPath = servicePath + "/conf/prod/resources";
        } else if (path.endsWith("/resources")) {
            String servicePath = path.substring(0, path.indexOf("/conf"));
            localPath = servicePath + LOCAL_ENV_PATH;
            if (path.contains("dev")) {
                env = Env.DEV;
                devPath = path;
                uatPath = path.replace("dev", "uat");
                prodPath = path.replace("dev", "prod");
            } else if (path.contains("uat")) {
                env = Env.UAT;
                uatPath = path;
                devPath = path.replace("uat", "dev");
                prodPath = path.replace("uat", "prod");
            } else if (path.contains("prod")) {
                env = Env.PROD;
                prodPath = path;
                devPath = path.replace("prod", "dev");
                uatPath = path.replace("prod", "uat");
            } else {
                env = null;
            }
        } else {
            env = null;
        }
        InputDialogWrapper dialog = new InputDialogWrapper("");
        dialog.show();
        String filename = dialog.inputText;
        if (StringUtils.isEmpty(filename)) {
            return;
        }
        filename = formatWithPropertiesExtension(filename);
        PsiDirectory currentDir = PsiDirectoryFactory.getInstance(project).createDirectory(virtualFile);
        PsiDirectory localDirectory = null;
        PsiDirectory devDirectory = null;
        PsiDirectory uatDirectory = null;
        PsiDirectory prodDirectory = null;
        if (localPath != null) {
            localDirectory = PsiDirectoryFactory.getInstance(project).createDirectory(
                VfsUtil.findFileByIoFile(new File(localPath), true)
            );
        }
        if (devPath != null) {
            devDirectory = PsiDirectoryFactory.getInstance(project).createDirectory(
                VfsUtil.findFileByIoFile(new File(devPath), true)
            );
        }
        if (uatPath != null) {
            uatDirectory = PsiDirectoryFactory.getInstance(project).createDirectory(
                VfsUtil.findFileByIoFile(new File(uatPath), true)
            );
        }
        if (prodPath != null) {
            prodDirectory = PsiDirectoryFactory.getInstance(project).createDirectory(
                VfsUtil.findFileByIoFile(new File(prodPath), true)
            );
        }

        PsiFile existsFile = currentDir.findFile(filename);
        if (existsFile == null) {
            PsiFile file = PsiFileFactory.getInstance(project).createFileFromText(filename, PlainTextFileType.INSTANCE, "");
            PsiDirectory finalLocalDirectory = localDirectory;
            PsiDirectory finalDevDirectory = devDirectory;
            PsiDirectory finalUatDirectory = uatDirectory;
            PsiDirectory finalProdDirectory = prodDirectory;
            String finalFilename = filename;
            WriteCommandAction.runWriteCommandAction(project, () -> {
                currentDir.add(file);
                if (Env.LOCAL != env && finalLocalDirectory != null) {
                    Optional<PsiFile> optional = Optional.ofNullable(finalLocalDirectory.findFile(finalFilename));
                    if (optional.isEmpty()) {
                        finalLocalDirectory.add(file);
                    }
                }
                if (Env.DEV != env && finalDevDirectory != null) {
                    Optional<PsiFile> optional = Optional.ofNullable(finalDevDirectory.findFile(finalFilename));
                    if (optional.isEmpty()) {
                        finalDevDirectory.add(file);
                    }
                }
                if (Env.UAT != env && finalUatDirectory != null) {
                    Optional<PsiFile> optional = Optional.ofNullable(finalUatDirectory.findFile(finalFilename));
                    if (optional.isEmpty()) {
                        finalUatDirectory.add(file);
                    }
                }
                if (Env.PROD != env && finalProdDirectory != null) {
                    Optional<PsiFile> optional = Optional.ofNullable(finalProdDirectory.findFile(finalFilename));
                    if (optional.isEmpty()) {
                        finalProdDirectory.add(file);
                    }
                }
            });
        } else {
            Messages.showMessageDialog(filename + " existed.", "File Existed", Messages.getInformationIcon());
        }
    }

    private String formatWithPropertiesExtension(String filename) {
        String result = filename;
        String extension = FilenameUtils.getExtension(filename);
        if (StringUtils.isEmpty(extension)) {
            result = filename + FilenameUtils.EXTENSION_SEPARATOR + "properties";
        } else {
            if (!extension.equals("properties")) {
                result = FilenameUtils.getBaseName(filename) + FilenameUtils.EXTENSION_SEPARATOR + "properties";
            }
        }
        return result;
    }
}
