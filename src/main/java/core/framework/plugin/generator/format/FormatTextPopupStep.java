package core.framework.plugin.generator.format;

import com.google.common.base.CaseFormat;
import com.intellij.openapi.command.WriteCommandAction;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.popup.PopupStep;
import com.intellij.openapi.ui.popup.util.BaseListPopupStep;
import core.framework.plugin.generator.format.selection.FormatSelection;
import core.framework.plugin.generator.format.selection.FormatToCamel;
import core.framework.plugin.generator.format.selection.FormatToMiddleLine;
import core.framework.plugin.generator.format.selection.FormatToSentence;
import core.framework.plugin.generator.format.selection.FormatToUnderLine;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.Function;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * @author ebin
 */
public class FormatTextPopupStep extends BaseListPopupStep<FormatSelection> {
    private static final Pattern LOWER_CAMEL = Pattern.compile("^[a-z]+[A-Za-z0-9]*$");
    private final Editor editor;
    private final String selectedText;
    private final Project project;

    public FormatTextPopupStep(Project project, Editor editor, String selectedText) {
        this.project = project;
        this.editor = editor;
        this.selectedText = selectedText;
        List<FormatSelection> selections = new ArrayList<>();
        selections.add(new FormatToCamel());
        selections.add(new FormatToUnderLine());
        selections.add(new FormatToMiddleLine());
        selections.add(new FormatToSentence());
        init("Select Format Style :", selections, null);
    }

    @Override
    public @NotNull String getTextFor(FormatSelection value) {
        return value.displayName();
    }

    public @Nullable PopupStep<?> onChosen(FormatSelection selectedValue, boolean finalChoice) {
        Document document = editor.getDocument();

        Function<String, String> formatFunction;
        if (selectedValue instanceof FormatToCamel) {
            formatFunction = selectedText -> {
                String t = selectedText.replace(" ", "_");
                t = CaseFormat.LOWER_UNDERSCORE.to(CaseFormat.LOWER_CAMEL, t);
                t = CaseFormat.LOWER_HYPHEN.to(CaseFormat.LOWER_CAMEL, t);
                return t;
            };
        } else if (selectedValue instanceof FormatToMiddleLine) {
            formatFunction = selectedText -> {
                String t = selectedText.replace(" ", "-").replace("_", "-");
                return Arrays.stream(t.split("-")).map(s -> {
                    if (LOWER_CAMEL.matcher(s).matches()) {
                        return CaseFormat.LOWER_CAMEL.to(CaseFormat.LOWER_HYPHEN, s);
                    }
                    return s;
                }).collect(Collectors.joining("-"));
            };
        } else if (selectedValue instanceof FormatToUnderLine) {
            formatFunction = selectedText -> {
                String t = selectedText.replace(" ", "_").replace("-", "_");
                return Arrays.stream(t.split("_")).map(s -> {
                    if (LOWER_CAMEL.matcher(s).matches()) {
                        return CaseFormat.LOWER_CAMEL.to(CaseFormat.LOWER_UNDERSCORE, s);
                    }
                    return s;
                }).collect(Collectors.joining("_"));
            };
        } else if (selectedValue instanceof FormatToSentence) {
            formatFunction = selectedText -> selectedText
                .replaceAll("([a-z])([A-Z])", "$1 $2")
                .replaceAll("([A-Z]+)([A-Z][a-z])", "$1 $2")
                .replace('_', ' ')
                .replace('-', ' ')
                .replaceAll("\\s+", " ")
                .toLowerCase()
                .trim();
        } else {
            formatFunction = null;
        }
        if (formatFunction != null) {
            WriteCommandAction.runWriteCommandAction(project, () -> {
                document.replaceString(
                    editor.getSelectionModel().getSelectionStart(),
                    editor.getSelectionModel().getSelectionEnd(),
                    formatFunction.apply(selectedText)
                );
            });
        }
        return super.onChosen(selectedValue, finalChoice);
    }
}
