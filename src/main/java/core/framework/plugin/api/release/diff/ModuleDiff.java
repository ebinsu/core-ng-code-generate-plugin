package core.framework.plugin.api.release.diff;

import core.framework.plugin.api.release.Module;
import core.framework.plugin.utils.StringUtils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * @author ebin
 */
public class ModuleDiff {
    public Module module;

    public List<String> addClasses = new ArrayList<>();
    public List<String> removeClasses = new ArrayList<>();
    public List<ClassDiff> classDiffs = new ArrayList<>();

    public Result analyze() {
        Result result = new Result(module);

        removeClasses.forEach(clazz -> result.breakingReason.add("delete class: " + clazz));
        addClasses.forEach(clazz -> result.minorReason.add("add new class: " + clazz));
        classDiffs.forEach(clazz -> {
            calculateChange(clazz, result);
        });

        result.calculateVersion();
        return result;
    }

    private static void calculateChange(ClassDiff clazz, Result result) {
        if (ClassDiff.Type.ENUM == clazz.type) {
            result.breakingReason.add(clazz.className + " added enum values: " + StringUtils.truncate(String.join(",", clazz.addFields.keySet()), 40));
        } else {
            clazz.addFields.forEach((fieldName, annotations) -> {
                if (annotations.stream().anyMatch(a -> a.contains("@Property"))) {
                    if (annotations.stream().anyMatch(a -> a.contains("@NotNull"))) {
                        result.breakingReason.add(clazz.className + " added @NotNull field: " + fieldName);
                    } else if (annotations.stream().anyMatch(a -> a.contains("@NotBlank"))) {
                        result.minorReason.add(clazz.className + " added @NotBlank field: " + fieldName);
                    } else if (annotations.stream().anyMatch(a -> a.contains("@Size"))) {
                        result.minorReason.add(clazz.className + " added @Size field: " + fieldName);
                    } else if (annotations.stream().anyMatch(a -> a.contains("@Min"))) {
                        result.minorReason.add(clazz.className + " added @Min field: " + fieldName);
                    } else if (annotations.stream().anyMatch(a -> a.contains("@Max"))) {
                        result.minorReason.add(clazz.className + " added @Max field: " + fieldName);
                    }
                }
            });
        }

        clazz.deleteFields.forEach((fieldName, annotations) -> {
            if (annotations.stream().anyMatch(a -> a.contains("@NotNull"))) {
                result.breakingReason.add(clazz.className + " deleted @NotNull field: " + fieldName);
            } else {
                result.minorReason.add(clazz.className + " deleted nullable field: " + fieldName);
            }
        });

        clazz.fieldChanges.forEach((fieldName, annotations) -> {
            boolean hasAddProperty = annotations.addAnnotations.stream().anyMatch(a -> a.contains("@Property"));
            boolean hasDeleteProperty = annotations.deleteAnnotations.stream().anyMatch(a -> a.contains("@Property"));
            if (hasAddProperty && hasDeleteProperty) {
                result.breakingReason.add(clazz.className + "." + fieldName + " change @Property name");
            } else if (annotations.addAnnotations.stream().anyMatch(a -> a.contains("@NotNull"))) {
                result.breakingReason.add(clazz.className + "." + fieldName + " add @NotNull");
            } else if (annotations.deleteAnnotations.stream().anyMatch(a -> a.contains("@NotNull"))) {
                result.minorReason.add(clazz.className + "." + fieldName + " delete @NotNull");
            }
        });

        clazz.addMethods.forEach((methodName, annotations) -> {
            result.minorReason.add(clazz.className + " add method: " + methodName);
        });

        clazz.deleteMethods.forEach((methodName, annotations) -> {
            result.breakingReason.add(clazz.className + " deleted method: " + methodName);
        });

        clazz.methodChanges.forEach((methodName, method) -> {
            // annotation
            boolean hasAddPath = method.annotationDiff.addAnnotations.stream().anyMatch(a -> a.contains("@Path"));
            boolean hasDeletePath = method.annotationDiff.addAnnotations.stream().anyMatch(a -> a.contains("@Path"));
            if (hasAddPath && hasDeletePath) {
                result.breakingReason.add(clazz.className + "." + methodName + "() change @Path");
            } else {
                // params
                if (method.changeParams()) {
                    result.breakingReason.add(clazz.className + "." + methodName + "() change params: " + method.oldParams + " =>  " + method.newParams);
                } else if (method.changeReturnType()) {
                    result.breakingReason.add(clazz.className + "." + methodName + "() change return type: " + method.oldReturnType + " =>  " + method.newReturnType);
                }
            }
        });

        clazz.innerClassDiffs.forEach(innerClass -> calculateChange(innerClass, result));
    }

    public static class Result {
        private final Module module;
        public String name;
        public String version;
        public Level level = Level.NONE;
        public List<String> breakingReason = new ArrayList<>();
        public List<String> minorReason = new ArrayList<>();

        public Result(Module module) {
            this.module = module;
            this.name = module.name;
            this.version = module.version;
        }

        public void calculateVersion() {
            int[] versionNumbers = Arrays.stream(version.split("\\.")).mapToInt(Integer::valueOf).toArray();
            if (!breakingReason.isEmpty()) {
                level = Level.BREAKING;
                versionNumbers[0] = versionNumbers[0] + 1;
                versionNumbers[1] = 0;
                versionNumbers[2] = 0;
            } else if (!minorReason.isEmpty()) {
                level = Level.MINOR;
                versionNumbers[1] = versionNumbers[1] + 1;
                versionNumbers[2] = 0;
            }
            this.version = Arrays.stream(versionNumbers).mapToObj(String::valueOf).collect(Collectors.joining("."));
        }

        public Module toNewModule() {
            Module newModule = new Module();
            newModule.name = module.name;
            newModule.artifactId = module.artifactId;
            newModule.version = this.version;
            return newModule;
        }

        public String toSummary() {
            String serviceName;
            String[] split = name.split(":");
            if (split.length > 0) {
                serviceName = split[split.length - 1];
            } else {
                serviceName = name;
            }
            return serviceName + " " + level + " " + version;
        }

        @Override
        public String toString() {
            String serviceName;
            String[] split = name.split(":");
            if (split.length > 0) {
                serviceName = split[split.length - 1];
            } else {
                serviceName = name;
            }
            String str = serviceName + " " + level + " " + version + "\n";
            if (!breakingReason.isEmpty()) {
                str += breakingReason.stream().collect(Collectors.joining("\n", "breaking : \n", "\n"));
            }
            if (!minorReason.isEmpty()) {
                str += minorReason.stream().collect(Collectors.joining("\n", "minor : \n", "\n"));
            }
            return str;
        }
    }

    public enum Level {
        BREAKING,
        MINOR,
        NONE
    }
}
