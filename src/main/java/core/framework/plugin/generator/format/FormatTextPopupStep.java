package core.framework.plugin.generator.format;

import com.intellij.openapi.command.WriteCommandAction;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.popup.PopupStep;
import com.intellij.openapi.ui.popup.util.BaseListPopupStep;
import core.framework.plugin.generator.format.selection.FormatSelection;
import core.framework.plugin.generator.format.selection.FormatToCamel;
import core.framework.plugin.generator.format.selection.FormatToMiddleLine;
import core.framework.plugin.generator.format.selection.FormatToLowerSpace;
import core.framework.plugin.generator.format.selection.FormatToUnderLine;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;
import java.util.regex.Pattern;

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
        selections.add(new FormatToLowerSpace());
        init("Select Format Style :", selections, null);
    }

    @Override
    public @NotNull String getTextFor(FormatSelection value) {
        return value.displayName();
    }

    public @Nullable PopupStep<?> onChosen(FormatSelection selectedValue, boolean finalChoice) {
        Document document = editor.getDocument();

        Function<String, String> formatFunction = getStringStringFunction(selectedValue);
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

    private static @Nullable Function<String, String> getStringStringFunction(FormatSelection selectedValue) {
        Function<String, String> formatFunction;
        if (selectedValue instanceof FormatToCamel) {
            formatFunction = StringConverterUtils::toLowerCamel;
        } else if (selectedValue instanceof FormatToMiddleLine) {
            formatFunction = StringConverterUtils::toMiddleLine;
        } else if (selectedValue instanceof FormatToUnderLine) {
            formatFunction = StringConverterUtils::toUnderLine;
        } else if (selectedValue instanceof FormatToLowerSpace) {
            formatFunction = StringConverterUtils::toLowerSpace;
        } else {
            formatFunction = null;
        }
        return formatFunction;
    }
}
