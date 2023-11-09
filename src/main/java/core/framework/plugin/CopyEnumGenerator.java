package core.framework.plugin;

import com.intellij.ide.highlighter.JavaFileType;
import com.intellij.ide.util.TreeClassChooserFactory;
import com.intellij.ide.util.TreeFileChooser;
import com.intellij.lang.java.JavaLanguage;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.CommonDataKeys;
import com.intellij.openapi.actionSystem.PlatformDataKeys;
import com.intellij.openapi.command.WriteCommandAction;
import com.intellij.openapi.fileTypes.FileType;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.Messages;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.vfs.VirtualFileManager;
import com.intellij.psi.PsiAnnotation;
import com.intellij.psi.PsiAnnotationMemberValue;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiDirectory;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiElementFactory;
import com.intellij.psi.PsiField;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiFileFactory;
import com.intellij.psi.PsiJavaFile;
import com.intellij.psi.PsiManager;
import com.intellij.psi.PsiModifierList;
import com.intellij.psi.impl.file.PsiDirectoryFactory;
import com.intellij.psi.impl.file.PsiJavaDirectoryImpl;
import com.intellij.psi.impl.source.PsiJavaFileImpl;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.StringUtils;

import java.io.IOException;
import java.util.Arrays;
import java.util.stream.Collectors;

/**
 * @author ebin
 */
public class CopyEnumGenerator extends AnAction {
    public static final String JAVA_DIR = "/src/main/java";
    public static final String ENUM_TEMPLATE = "import core.framework.api.json.Property;\n" +
            "\n" +
            "public enum %1$s {\n";
    public static final String ENUM_FIELD_TEMPLATE = "    @Property(name = %2$s)\n" +
            "    %1$s";

    public static final String TEST_CLASS_TEMPLATE = "import core.framework.test.Assertions;\n" +
            "import org.junit.jupiter.api.Test;\n" +
            "\n" +
            "class EnumTest {\n" +
            "    @Test\n" +
            "    void test%1$s() {\n" +
            "        Assertions.assertEnumClass(%2$s.class).hasExactlyConstantsAs(%3$s.class);\n" +
            "    }\n" +
            "}";
    public static final String TEST_METHOD_TEMPLATE = "@Test void test%1$s() {Assertions.assertEnumClass(%2$s.class).hasExactlyConstantsAs(%3$s.class);}";

    @Override
    public void actionPerformed(AnActionEvent e) {
        Project project = e.getData(PlatformDataKeys.PROJECT);
        if (project == null) {
            return;
        }
        PsiElement psiElement = e.getData(CommonDataKeys.PSI_ELEMENT);
        PsiJavaDirectoryImpl javaDirectory;
        if (psiElement instanceof PsiJavaDirectoryImpl) {
            javaDirectory = (PsiJavaDirectoryImpl) psiElement;
        } else if (psiElement instanceof PsiClass) {
            PsiClass target = (PsiClass) psiElement;
            PsiElement parent = target.getParent();
            while (!(parent instanceof PsiJavaDirectoryImpl)) {
                parent = parent.getParent();
                if (parent == null) {
                    return;
                }
            }
            javaDirectory = (PsiJavaDirectoryImpl) parent;
        } else {
            return;
        }

        PsiDirectoryFactory psiDirectoryFactory = PsiDirectoryFactory.getInstance(project);
        if (!psiDirectoryFactory.isPackage(javaDirectory)) {
            return;
        }

        TreeClassChooserFactory instance = TreeClassChooserFactory.getInstance(project);
        TreeFileChooser chooser = instance.createFileChooser("Choose Domain To Generate Sql File.", null,
                JavaFileType.INSTANCE, file -> {
                    FileType fileType = file.getFileType();
                    if (fileType instanceof JavaFileType) {
                        return Arrays.stream(((PsiJavaFile) file).getClasses()).anyMatch(PsiClass::isEnum);
                    }
                    return true;
                }, true, false);
        chooser.showDialog();
        PsiFile selectPsiFile = chooser.getSelectedFile();
        if (selectPsiFile == null) {
            return;
        }
        String selectEnumPackage = ((PsiJavaFileImpl) selectPsiFile).getPackageName();
        PsiClass[] classes = ((PsiJavaFileImpl) selectPsiFile).getClasses();
        if (classes.length == 0) {
            return;
        }
        PsiClass selectEnumClass = classes[0];

        InputDialogWrapper dialog = new InputDialogWrapper(selectEnumClass.getName());
        dialog.show();
        String generateEnumName = dialog.inputText;
        if (generateEnumName == null) {
            return;
        }
        String generateDirUrl = javaDirectory.getVirtualFile().getUrl();
        String generatePackageName = generateDirUrl.substring(generateDirUrl.indexOf(JAVA_DIR) + JAVA_DIR.length() + 1).replace("/", ".");

        PsiField[] enumClassFields = selectEnumClass.getFields();
        PsiElementFactory psiElementFactory = PsiElementFactory.getInstance(project);

        StringBuilder sb = new StringBuilder(String.format(ENUM_TEMPLATE, generateEnumName));
        int i = 1;
        for (PsiField field : enumClassFields) {
            String annotationValue = getAnnotationValue(field);
            sb.append(String.format(ENUM_FIELD_TEMPLATE, field.getName(), annotationValue)).append(i == enumClassFields.length ? "" : ",").append("\n");
            i++;
        }
        sb.append("}");
        PsiFile fileFromText = PsiFileFactory.getInstance(project).createFileFromText(JavaLanguage.INSTANCE, sb.toString());
        fileFromText.setName(generateEnumName + ".java");

        VirtualFileManager virtualFileManager = VirtualFileManager.getInstance();

        WriteCommandAction.runWriteCommandAction(project, () -> {
            VirtualFile enumFile = javaDirectory.getVirtualFile().findChild(fileFromText.getName());
            if (enumFile != null) {
                Messages.showMessageDialog(generateEnumName + " existed.", "File Existed", Messages.getInformationIcon());
                return;
            }
            javaDirectory.add(fileFromText);
            String testDir = findTestDir(javaDirectory);
            String testBaseDir = findTestBaseDir(javaDirectory);
            VirtualFile testDirVF = null;
            if (testDir != null && testBaseDir != null) {
                testDirVF = virtualFileManager.findFileByUrl(testDir + testBaseDir);
                if (testDirVF == null) {
                    VirtualFile testDirFile = virtualFileManager.findFileByUrl(testDir);
                    if (testDirFile != null) {
                        try {
                            testDirVF = testDirFile.createChildDirectory(new Object(), testBaseDir);
                        } catch (IOException ex) {
                        }
                    }
                }
            }
            if (testDirVF != null) {
                VirtualFile enumTestVF = testDirVF.findChild("EnumTest.java");
                String fullGenerateEnumName = generatePackageName + "." + generateEnumName;
                String methodName = Arrays.stream(StringUtils.split(fullGenerateEnumName, ".")).map(StringUtils::capitalize).collect(Collectors.joining());
                String fullSelectEnumName = selectEnumPackage + "." + selectEnumClass.getName();
                if (enumTestVF == null) {
                    PsiFile testClassFile = PsiFileFactory.getInstance(project).createFileFromText(JavaLanguage.INSTANCE,
                            String.format(TEST_CLASS_TEMPLATE, methodName, fullGenerateEnumName, fullSelectEnumName));
                    testClassFile.setName("EnumTest.java");
                    PsiDirectory directory = psiDirectoryFactory.createDirectory(testDirVF);
                    directory.add(testClassFile);
                } else {
                    PsiFile file = PsiManager.getInstance(project).findFile(enumTestVF);
                    if (file != null) {
                        PsiClass testClass = ((PsiJavaFileImpl) file).getClasses()[0];
                        testClass.add(psiElementFactory.createMethodFromText(String.format(TEST_METHOD_TEMPLATE, methodName, fullGenerateEnumName, fullSelectEnumName), selectEnumClass.getContext()));
                    }
                }
            }
        });
    }

    private String findTestBaseDir(PsiJavaDirectoryImpl javaDirectory) {
        String url = javaDirectory.getVirtualFile().getUrl();
        if (!url.contains(JAVA_DIR)) {
            return null;
        }
        int index = url.indexOf(JAVA_DIR);
        String baseDir = Arrays.stream(url.substring(index + JAVA_DIR.length()).split("/")).filter(StringUtils::isNoneBlank).findFirst().orElse(null);
        if (baseDir == null) {
            baseDir = Arrays.stream(javaDirectory.getVirtualFile().getChildren()).filter(VirtualFile::isDirectory).findFirst().map(f -> FilenameUtils.getBaseName(f.getName())).orElse(null);
        }
        return baseDir;
    }

    private String findTestDir(PsiJavaDirectoryImpl javaDirectory) {
        String url = javaDirectory.getVirtualFile().getUrl();
        if (!url.contains(JAVA_DIR)) {
            return null;
        }
        int index = url.indexOf(JAVA_DIR);
        String projectDir = url.substring(0, index);
        if (projectDir.contains("-interface")) {
            projectDir = projectDir.replace("-interface", "");
        }
        return projectDir + "/src/test/java/";
    }

    private String getAnnotationValue(PsiField field) {
        String annValue;
        PsiModifierList modifierList = field.getModifierList();
        if (modifierList != null) {
            PsiElement[] children = field.getModifierList().getChildren();
            annValue = Arrays.stream(children).filter(c -> {
                if (c instanceof PsiAnnotation) {
                    String qualifiedName = ((PsiAnnotation) c).getQualifiedName();
                    if (qualifiedName == null) {
                        return false;
                    }
                    return qualifiedName.contains("DBEnumValue") || qualifiedName.contains("MongoEnumValue");
                }
                return false;
            }).findFirst().map(m -> {
                PsiAnnotationMemberValue value = ((PsiAnnotation) m).findAttributeValue("value");
                if (value != null) {
                    return value.getText();
                } else {
                    return null;
                }
            }).orElse(null);
            if (annValue == null) {
                annValue = Arrays.stream(children).filter(c -> {
                    if (c instanceof PsiAnnotation) {
                        String qualifiedName = ((PsiAnnotation) c).getQualifiedName();
                        if (qualifiedName == null) {
                            return false;
                        }
                        return qualifiedName.contains("Property");
                    }
                    return false;
                }).findFirst().map(m -> {
                    PsiAnnotationMemberValue value = ((PsiAnnotation) m).findAttributeValue("name");
                    if (value != null) {
                        return value.getText();
                    } else {
                        return null;
                    }
                }).orElse(null);
            }
        } else {
            annValue = "\"" + field.getName().toUpperCase() + "\"";
        }
        return annValue;
    }
}
