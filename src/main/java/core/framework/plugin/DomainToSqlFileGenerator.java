package core.framework.plugin;

import com.intellij.ide.highlighter.JavaFileType;
import com.intellij.ide.util.TreeClassChooserFactory;
import com.intellij.ide.util.TreeFileChooser;
import com.intellij.lang.jvm.annotation.JvmAnnotationAttribute;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.PlatformDataKeys;
import com.intellij.openapi.command.WriteCommandAction;
import com.intellij.openapi.fileTypes.PlainTextFileType;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.JavaPsiFacade;
import com.intellij.psi.PsiAnnotation;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiDirectory;
import com.intellij.psi.PsiField;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiFileFactory;
import com.intellij.psi.PsiPrimitiveType;
import com.intellij.psi.PsiType;
import com.intellij.psi.impl.file.PsiDirectoryFactory;
import com.intellij.psi.impl.source.PsiJavaFileImpl;
import com.intellij.psi.impl.source.tree.java.PsiNameValuePairImpl;
import com.intellij.psi.search.GlobalSearchScope;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * @author ebin
 */
public class DomainToSqlFileGenerator extends AnAction {
    public static final String LINE_START = "   ";
    public static final String EMPTY_BLOCK = "      ";
    public static final String SINGLE_EMPTY_BLOCK = " ";
    public static final String COLUMN_NAME_FLAG = "`";

    @Override
    public void actionPerformed(AnActionEvent e) {
        Project project = e.getData(PlatformDataKeys.PROJECT);
        VirtualFile virtualFile = e.getData(PlatformDataKeys.VIRTUAL_FILE);
        if (project == null || virtualFile == null) {
            return;
        }
        while (!virtualFile.isDirectory()) {
            virtualFile = virtualFile.getParent();
        }
        TreeClassChooserFactory instance = TreeClassChooserFactory.getInstance(project);
        TreeFileChooser chooser = instance.createFileChooser("Choose Domain To Generate Sql File.", null, JavaFileType.INSTANCE, null, true, false);
        chooser.showDialog();
        PsiFile psiFile = chooser.getSelectedFile();
        if (psiFile == null) {
            return;
        }
        PsiClass[] classes = ((PsiJavaFileImpl) psiFile).getClasses();
        if (classes.length == 0) {
            return;
        }
        PsiClass mainClass = classes[0];
        PsiField[] fields = mainClass.getFields();
        if (fields.length == 0) {
            return;
        }

        String tableName = findTableName(mainClass);
        if (tableName == null) {
            return;
        }
        StringBuilder sql = new StringBuilder("CREATE TABLE IF NOT EXISTS `");
        sql.append(tableName).append("`\n").append("(\n");
        JavaPsiFacade psiFacade = JavaPsiFacade.getInstance(project);
        List<String> pks = new ArrayList<>();
        List<DomainDesc> domainDescs = new ArrayList<>();
        for (PsiField field : fields) {
            String columnName = getColumnName(field);
            if (columnName == null) {
                continue;
            }
            String columnType = getColumnType(field, columnName, project, psiFacade);
            boolean notNull = isNotNull(field);
            String columnNullableDesc = notNull ? "NOT NULL" : "NULL";
            domainDescs.add(new DomainDesc(columnName, columnType, columnNullableDesc));
            if (isPrimaryKey(field)) {
                pks.add(COLUMN_NAME_FLAG + field.getName() + COLUMN_NAME_FLAG);
            }
        }

        int maxColumnNameLength = 0;
        int maxColumnTypeDescLength = 0;
        for (DomainDesc domainDesc : domainDescs) {
            maxColumnNameLength = Math.max(domainDesc.columnName.length(), maxColumnNameLength);
            maxColumnTypeDescLength = Math.max(domainDesc.columnTypeDesc.length(), maxColumnTypeDescLength);
        }

        for (DomainDesc domainDesc : domainDescs) {
            StringBuilder columnSql = new StringBuilder(LINE_START);
            columnSql.append(COLUMN_NAME_FLAG).append(domainDesc.columnName).append(COLUMN_NAME_FLAG);
            if (domainDesc.columnName.length() < maxColumnNameLength) {
                int i = maxColumnNameLength - domainDesc.columnName.length();
                columnSql.append(SINGLE_EMPTY_BLOCK.repeat(i));
            }
            columnSql.append(EMPTY_BLOCK).append(domainDesc.columnTypeDesc);
            if (domainDesc.columnTypeDesc.length() < maxColumnTypeDescLength) {
                int i = maxColumnTypeDescLength - domainDesc.columnTypeDesc.length();
                columnSql.append(SINGLE_EMPTY_BLOCK.repeat(i));
            }
            columnSql.append(EMPTY_BLOCK).append(domainDesc.columnNullableDesc).append(",\n");
            sql.append(columnSql);
        }

        if (!pks.isEmpty()) {
            sql.append(LINE_START).append("PRIMARY KEY (").append(String.join(", ", pks)).append(")\n");
        }
        sql.append(") ENGINE = InnoDB;\n");
        String filename = "R__" + tableName + ".sql";
        PsiFile sqlFile = PsiFileFactory.getInstance(project).createFileFromText(filename, PlainTextFileType.INSTANCE, sql.toString());
        PsiDirectory directory = PsiDirectoryFactory.getInstance(project).createDirectory(virtualFile);

        WriteCommandAction.runWriteCommandAction(project, () -> {
            PsiFile existsFile = directory.findFile(filename);
            if (existsFile != null) {
                try {
                    existsFile.getVirtualFile().delete(new Object());
                } catch (IOException ex) {
                    ex.printStackTrace();
                }
            }
            directory.add(sqlFile);
        });
    }

    private boolean isPrimaryKey(PsiField field) {
        PsiAnnotation[] filedAnnotations = field.getAnnotations();
        for (PsiAnnotation filedAnnotation : filedAnnotations) {
            String qualifiedName = filedAnnotation.getQualifiedName();
            if (qualifiedName != null && qualifiedName.contains("PrimaryKey")) {
                return true;
            }
        }
        return false;
    }

    private String findTableName(PsiClass mainClass) {
        PsiAnnotation[] annotations = mainClass.getAnnotations();
        for (PsiAnnotation annotation : annotations) {
            String qualifiedName = annotation.getQualifiedName();
            if (qualifiedName != null && annotation.getQualifiedName().contains("Table")) {
                List<JvmAnnotationAttribute> attributes = annotation.getAttributes();
                for (JvmAnnotationAttribute attribute : attributes) {
                    if ("name".equals(attribute.getAttributeName())) {
                        if (attribute instanceof PsiNameValuePairImpl) {
                            return ((PsiNameValuePairImpl) attribute).getLiteralValue();
                        }
                    }
                }
            }
        }
        return null;
    }

    private boolean isNotNull(PsiField field) {
        PsiAnnotation[] filedAnnotations = field.getAnnotations();
        for (PsiAnnotation filedAnnotation : filedAnnotations) {
            String qualifiedName = filedAnnotation.getQualifiedName();
            if (qualifiedName != null && filedAnnotation.getQualifiedName().contains("NotNull")) {
                return true;
            }
        }
        return false;
    }

    private String getColumnName(PsiField field) {
        PsiAnnotation[] filedAnnotations = field.getAnnotations();
        for (PsiAnnotation filedAnnotation : filedAnnotations) {
            String qualifiedName = filedAnnotation.getQualifiedName();
            if (qualifiedName != null && filedAnnotation.getQualifiedName().contains("Column")) {
                List<JvmAnnotationAttribute> attributes = filedAnnotation.getAttributes();
                for (JvmAnnotationAttribute attribute : attributes) {
                    if ("name".equals(attribute.getAttributeName())) {
                        if (attribute instanceof PsiNameValuePairImpl) {
                            return ((PsiNameValuePairImpl) attribute).getLiteralValue();
                        }
                    }
                }
            }
        }
        return null;
    }

    private String getColumnType(PsiField field, String columnName, Project project, JavaPsiFacade psiFacade) {
        PsiType type = field.getType();
        if (type instanceof PsiPrimitiveType) {
            return "UNKNOWN";
        }
        String canonicalText = type.getCanonicalText();
        switch (canonicalText) {
            case "java.lang.String":
                int size = 100;
                if (columnName.contains("ids")) {
                    size = 500;
                } else if (columnName.contains("note") || columnName.contains("reason")) {
                    size = 255;
                } else if (columnName.contains("id") || columnName.equals("created_by") || columnName.equals("updated_by")) {
                    size = 50;
                }
                return "VARCHAR(" + size + ")";
            case "java.time.ZonedDateTime":
            case "java.time.LocalDateTime":
                return "TIMESTAMP(6)";
            case "java.time.LocalDate":
                return "DATE";
            case "java.lang.Boolean":
                return "BIT(1)";
            case "java.lang.Integer":
                return "INT";
            case "java.lang.Double":
                return "DECIMAL(10, 2)";
            case "java.lang.Long":
                return "BIGINT";
            default:
                PsiClass localVariableType = psiFacade.findClass(type.getCanonicalText(), GlobalSearchScope.allScope(project));
                if (localVariableType != null && localVariableType.isEnum()) {
                    return "VARCHAR(100)";
                }
                break;
        }
        return "UNKNOWN";
    }
}
