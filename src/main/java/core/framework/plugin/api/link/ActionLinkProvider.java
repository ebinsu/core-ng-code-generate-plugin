package core.framework.plugin.api.link;

import com.intellij.codeInsight.hints.ImmediateConfigurable;
import com.intellij.codeInsight.hints.InlayHintsCollector;
import com.intellij.codeInsight.hints.InlayHintsProvider;
import com.intellij.codeInsight.hints.InlayHintsSink;
import com.intellij.codeInsight.hints.NoSettings;
import com.intellij.codeInsight.hints.SettingsKey;
import com.intellij.openapi.editor.Editor;
import com.intellij.psi.PsiFile;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;

@SuppressWarnings("UnstableApiUsage")
public class ActionLinkProvider implements InlayHintsProvider<NoSettings> {
    private static final SettingsKey<NoSettings> KEY = new SettingsKey<>("datadog.link.hints");

    @Nullable
    @Override
    public InlayHintsCollector getCollectorFor(@NotNull PsiFile file,
                                               @NotNull Editor editor,
                                               @NotNull NoSettings settings,
                                               @NotNull InlayHintsSink sink) {
        return new ActionLinkCollector(editor);
    }

    @NotNull
    @Override
    public NoSettings createSettings() {
        return new NoSettings();
    }

    @NotNull
    @Override
    public String getName() {
        return "Datadog";
    }

    @NotNull
    @Override
    public SettingsKey<NoSettings> getKey() {
        return KEY;
    }

    @Nullable
    @Override
    public String getPreviewText() {
        return null;
    }

    @NotNull
    @Override
    public ImmediateConfigurable createConfigurable(@NotNull NoSettings settings) {
        return listener -> new JPanel();
    }
}