package core.framework.plugin.api.release;

import com.github.difflib.UnifiedDiffUtils;
import com.github.difflib.patch.Patch;
import core.framework.plugin.api.release.context.ClassContext;
import core.framework.plugin.api.release.context.FieldContext;
import core.framework.plugin.api.release.context.MethodContext;
import core.framework.plugin.api.release.diff.AnnotationDiff;
import core.framework.plugin.api.release.diff.ClassDiff;
import core.framework.plugin.api.release.diff.MethodDiff;
import core.framework.plugin.api.release.diff.ModuleDiff;
import core.framework.plugin.utils.FileUtils;
import org.apache.commons.io.FilenameUtils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * @author ebin
 */
public class GitDiffSemanticAnalyzer {
    private static final Pattern HUNK_CONTEXT_PATTERN = Pattern.compile("@@\\s-\\d+,\\d+\\s\\+\\d+,\\d+\\s@@\\s(?:.*\\s)?(class|enum|interface)\\s+(\\w+)");
    private static final Pattern TYPE_REG = Pattern.compile("^[+-]?(\\s*)(?:public|static|private|protected|final)*\\s*(class|enum|interface)\\s+(\\w+)");

    private static final Pattern FIELD_REG = Pattern.compile("(public|private|protected|final)\\s+[\\w<>,\\[\\]\\.]+\\s+(\\w+);");
    private static final Pattern METHOD_REG = Pattern.compile(
        "^[+-]?\\s*(?:(?:public|protected|private|static|final|native|synchronized|default)\\s+)*" + // 修饰符
            "([\\w<>,\\[\\]\\.]+)\\s+" +    // 1. 返回值类型 (支持泛型和数组)
            "(\\w+)\\s*" +                 // 2. 方法名
            "\\(([^)]*)\\)"                // 3. 入参列表 (括号内的所有内容)
    );
    private static final Pattern INTERFACE_CONST_REG = Pattern.compile("^[+-]?\\s*(?:(?:public|static|final)\\s+)*[\\w<>,\\[\\]\\.]+\\s+(\\w+)\\s*=[^;]+;");
    private static final Pattern ENUM_VALUE_REG = Pattern.compile("^[+-]?(\\s*)([A-Z][A-Z0-9_]+)(?:\\s*\\(.*\\))?\\s*[,;]?\\s*$");

    public static ModuleDiff analyze(String rawDiff) {
        ModuleDiff moduleDiff = new ModuleDiff();

        // 将大 Diff 按文件拆分 (Git 以 "diff --git" 作为新文件开头)
        String[] fileDiffs = rawDiff.split("(?=diff --git)");
        for (String fileDiff : fileDiffs) {
            if (fileDiff.trim().isEmpty()) continue;
            processFileDiff(moduleDiff, fileDiff);
        }
        return moduleDiff;
    }

    private static void processFileDiff(ModuleDiff moduleDiff, String fileDiff) {
        List<String> diffLines = Arrays.asList(fileDiff.split("\n"));

        String fileName;
        String oldPath = null;
        String newPath = null;
        boolean isDeleted = false;
        boolean isNew = false;
        ClassContext oldMainClass = null;
        ClassContext newMainClass = null;
        for (String line : diffLines) {
            if (line.startsWith("--- a/")) {
                oldPath = line.substring(6);
            } else if (line.startsWith("+++ b/")) {
                newPath = line.substring(6);
            } else if (line.startsWith("--- /dev/null")) {
                isNew = true;
            } else if (line.startsWith("+++ /dev/null")) {
                isDeleted = true;
            } else if (line.startsWith("deleted file mode")) {
                isDeleted = true; // 显式标记为删除
            }
            if (line.startsWith("@@")) {
                Matcher headerMatcher = HUNK_CONTEXT_PATTERN.matcher(line);
                if (headerMatcher.find()) {
                    String type = headerMatcher.group(1);
                    String className = headerMatcher.group(2);
                    oldMainClass = new ClassContext(className, type);
                    newMainClass = new ClassContext(className, type);
                }
                break;
            }
        }

        fileName = isNew ? newPath : oldPath;
        if (!FileUtils.isJavaFile(fileName)) return;
        fileName = FilenameUtils.getBaseName(fileName);
        if (isNew) {
            moduleDiff.addClasses.add(fileName);
            return;
        }
        if (isDeleted) {
            moduleDiff.removeClasses.add(fileName);
            return;
        }

        String type = "class";
        if (fileName.endsWith("WebService")) {
            type = "interface";
        }
        if (oldMainClass == null) {
            oldMainClass = new ClassContext(fileName, type);
        }
        if (newMainClass == null) {
            newMainClass = new ClassContext(fileName, type);
        }

        Patch<String> patch = UnifiedDiffUtils.parseUnifiedDiff(diffLines);
        List<String> sourceLines = patch.getDeltas().stream().flatMap(f -> f.getSource().getLines().stream()).toList();
        List<String> targetLines = patch.getDeltas().stream().flatMap(f -> f.getTarget().getLines().stream()).toList();

        oldMainClass = extract(oldMainClass, sourceLines);
        newMainClass = extract(newMainClass, targetLines);

        if (oldMainClass == null || newMainClass == null) {
            return;
        }

        moduleDiff.classDiffs.add(analyzeChanges(oldMainClass, newMainClass));
    }

    private static ClassDiff analyzeChanges(ClassContext oldClassDef, ClassContext newClassDef) {
        ClassDiff classDiff = analyzeClassChanges(oldClassDef, newClassDef);
        for (ClassContext oldInnerClass : oldClassDef.innerClasses) {
            Optional<ClassContext> newInnerClassOpt = newClassDef.innerClasses.stream().filter(f -> oldInnerClass.className.equals(f.className)).findFirst();
            if (newInnerClassOpt.isEmpty()) continue;
            ClassContext newInnerClass = newInnerClassOpt.get();
            classDiff.innerClassDiffs.add(analyzeClassChanges(oldInnerClass, newInnerClass));
        }
        return classDiff;
    }

    private static ClassDiff analyzeClassChanges(ClassContext oldClassDef, ClassContext newClassDef) {
        ClassDiff classDiff = new ClassDiff(oldClassDef.className, oldClassDef.type.toString());
        Set<String> fieldsInOld = oldClassDef.fieldList.stream().map(m -> m.name).collect(Collectors.toSet());
        Set<String> fieldsInNew = newClassDef.fieldList.stream().map(m -> m.name).collect(Collectors.toSet());
        for (FieldContext field : oldClassDef.fieldList) {
            if (!fieldsInNew.contains(field.name)) {
                classDiff.deleteFields.put(field.name, field.annotations);
            } else {
                AnnotationDiff annotationDiff = new AnnotationDiff();
                List<String> oldAnno = field.annotations;
                List<String> newAnno = newClassDef.getFieldAnnotations(field.name);

                annotationDiff.deleteAnnotations.addAll(newAnno);
                annotationDiff.deleteAnnotations.removeAll(oldAnno);

                annotationDiff.addAnnotations.addAll(oldAnno);
                annotationDiff.addAnnotations.removeAll(newAnno);

                if (annotationDiff.hasDiff()) {
                    classDiff.fieldChanges.put(field.name, annotationDiff);
                }
            }
        }
        for (FieldContext field : newClassDef.fieldList) {
            if (!fieldsInOld.contains(field.name)) {
                classDiff.addFields.put(field.name, field.annotations);
            }
        }

        Set<String> methodsInOld = oldClassDef.methodList.stream().map(m -> m.name).collect(Collectors.toSet());
        Set<String> methodsInNew = newClassDef.methodList.stream().map(m -> m.name).collect(Collectors.toSet());
        for (MethodContext oldMethod : oldClassDef.methodList) {
            if (!methodsInNew.contains(oldMethod.name)) {
                classDiff.deleteMethods.put(oldMethod.name, oldMethod.annotations);
            } else {
                MethodContext newMethod = newClassDef.getMethod(oldMethod.name);
                if (newMethod != null) {
                    MethodDiff methodDiff = new MethodDiff();

                    AnnotationDiff annotationDiff = new AnnotationDiff();
                    List<String> oldAnno = oldMethod.annotations;
                    List<String> newAnno = newMethod.annotations;

                    annotationDiff.deleteAnnotations.addAll(newAnno);
                    annotationDiff.deleteAnnotations.removeAll(oldAnno);

                    annotationDiff.addAnnotations.addAll(oldAnno);
                    annotationDiff.addAnnotations.removeAll(newAnno);

                    methodDiff.annotationDiff = annotationDiff;

                    if (!Objects.equals(oldMethod.returnType, newMethod.returnType)) {
                        methodDiff.oldReturnType = null;
                        methodDiff.newReturnType = null;
                    }

                    if (!Objects.equals(oldMethod.params, newMethod.params)) {
                        methodDiff.oldParams = null;
                        methodDiff.newParams = null;
                    }

                    if (annotationDiff.hasDiff()) {
                        classDiff.methodChanges.put(oldMethod.name, methodDiff);
                    }
                }
            }
        }
        for (MethodContext method : newClassDef.methodList) {
            if (!methodsInOld.contains(method.name)) {
                classDiff.addMethods.put(method.name, method.annotations);
            }
        }
        return classDiff;
    }

    private static ClassContext extract(ClassContext mainClass, List<String> lines) {
        ClassContext innerClass = null;

        for (int i = 0; i < lines.size(); i++) {
            String line = lines.get(i);
            Matcher matcher = TYPE_REG.matcher(line);

            if (matcher.find()) {
                String type = matcher.group(2);
                String name = matcher.group(3);

                if (mainClass == null) {
                    mainClass = new ClassContext(name, type);
                } else {
                    if (name.equals(mainClass.className)) {
                        continue;
                    }
                    innerClass = new ClassContext(mainClass.className + "." + name, type);
                    mainClass.innerClasses.add(innerClass);
                }
            } else {
                Matcher methodMatcher = METHOD_REG.matcher(line.trim());
                if (methodMatcher.find()) {
                    String returnType = methodMatcher.group(1);
                    String methodName = methodMatcher.group(2);
                    String params = methodMatcher.group(3);


                    MethodContext methodContext = new MethodContext(methodName, extractFieldAnnotations(i, lines), params, returnType);
                    if (innerClass != null) {
                        innerClass.methodList.add(methodContext);
                    } else if (mainClass != null) {
                        mainClass.methodList.add(methodContext);
                    }
                    continue;
                }
                Matcher fieldMatcher = FIELD_REG.matcher(line.trim());
                if (fieldMatcher.find()) {
                    String fieldName = fieldMatcher.group(2);
                    FieldContext filedContext = new FieldContext(fieldName, extractFieldAnnotations(i, lines));
                    if (innerClass != null) {
                        innerClass.fieldList.add(filedContext);
                    } else if (mainClass != null) {
                        mainClass.fieldList.add(filedContext);
                    }
                    continue;
                }
                ClassContext.Type type = innerClass != null ? innerClass.type : mainClass != null ? mainClass.type : null;
                if (ClassContext.Type.ENUM == type) {
                    Matcher enumMatcher = ENUM_VALUE_REG.matcher(line.trim());
                    if (enumMatcher.find()) {
                        String enumValueName = enumMatcher.group(2);
                        FieldContext filedContext = new FieldContext(enumValueName, extractFieldAnnotations(i, lines));
                        if (innerClass != null) {
                            innerClass.fieldList.add(filedContext);
                        } else {
                            mainClass.fieldList.add(filedContext);
                        }
                        continue;
                    }
                }

                Matcher interfaceConstMatcher = INTERFACE_CONST_REG.matcher(line.trim());
                if (interfaceConstMatcher.find()) {
                    String fieldName = interfaceConstMatcher.group(1);
                    FieldContext filedContext = new FieldContext(fieldName, extractFieldAnnotations(i, lines));
                    if (innerClass != null) {
                        innerClass.fieldList.add(filedContext);
                    } else if (mainClass != null) {
                        mainClass.fieldList.add(filedContext);
                    }
                }
            }
        }
        return mainClass;
    }

    private static List<String> extractFieldAnnotations(int fileIndex, List<String> lines) {
        List<String> annotations = new ArrayList<>();
        for (int i = fileIndex - 1; i >= 0; i--) {
            String mabeAnno = lines.get(i);
            if (isAnnotation(mabeAnno)) {
                annotations.add(mabeAnno);
            } else {
                Matcher m = FIELD_REG.matcher(lines.get(i).trim());
                if (m.find()) {
                    break;
                }
                Matcher enumMatcher = ENUM_VALUE_REG.matcher(lines.get(i).trim());
                if (enumMatcher.find()) {
                    break;
                }
                Matcher methodMatcher = METHOD_REG.matcher(lines.get(i).trim());
                if (methodMatcher.find()) {
                    break;
                }
                Matcher interfaceConstMatcher = INTERFACE_CONST_REG.matcher(lines.get(i).trim());
                if (interfaceConstMatcher.find()) {
                    break;
                }
            }
        }
        return annotations;
    }

    public static boolean isAnnotation(String line) {
        if (line == null) return false;
        String cleanLine = line.replaceFirst("^[+-]", "").trim();

        return cleanLine.matches("@\\w+.*") && !cleanLine.contains(";");
    }
}