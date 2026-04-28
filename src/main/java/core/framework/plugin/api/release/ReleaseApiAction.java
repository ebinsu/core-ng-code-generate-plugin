package core.framework.plugin.api.release;

import com.intellij.execution.process.ProcessOutputTypes;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.command.WriteCommandAction;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.fileEditor.FileDocumentManager;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.Task;
import com.intellij.openapi.project.DumbAware;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.search.FilenameIndex;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.vcs.log.CommitId;
import com.intellij.vcs.log.VcsLogCommitSelection;
import com.intellij.vcs.log.VcsLogDataKeys;
import core.framework.plugin.api.TextAreaDialogWrapper;
import core.framework.plugin.api.release.diff.ModuleDiff;
import core.framework.plugin.utils.JSON;
import git4idea.commands.Git;
import git4idea.commands.GitCommand;
import git4idea.commands.GitCommandResult;
import git4idea.commands.GitLineHandler;
import git4idea.commands.GitLineHandlerListener;
import git4idea.repo.GitRepository;
import git4idea.repo.GitRepositoryManager;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * @author ebin
 */
public class ReleaseApiAction extends AnAction implements DumbAware {

    @Override
    public void actionPerformed(@NotNull AnActionEvent e) {
        Project project = e.getProject();
        if (project == null) return;

        VcsLogCommitSelection selection = e.getData(VcsLogDataKeys.VCS_LOG_COMMIT_SELECTION);
        if (selection == null || selection.getCommits().isEmpty()) return;

        Collection<VirtualFile> publishJSONFiles = FilenameIndex.getVirtualFilesByName(
            "publish.json",
            GlobalSearchScope.FilesScope.allScope(project)
        );
        if (publishJSONFiles.isEmpty()) return;

        FileDocumentManager documentManager = FileDocumentManager.getInstance();
        Document publishJSONDoc = documentManager.getDocument(publishJSONFiles.stream().findFirst().get());
        if (publishJSONDoc == null) return;
        String text = publishJSONDoc.getText();
        PublishJSON publishJSONDTO;
        try {
            publishJSONDTO = JSON.fromJSON(PublishJSON.class, text);
        } catch (Exception ex) {
            return;
        }

        CommitId commitId = selection.getCommits().getFirst();
        String commitHash = commitId.getHash().toShortString();

        new Task.Backgroundable(project, "Executing git diff...", false) {
            @Override
            public void run(@NotNull ProgressIndicator indicator) {
                GitRepository repository = GitRepositoryManager.getInstance(project).getRepositoryForRoot(commitId.getRoot());
                if (repository == null) return;

                List<ModuleDiff> moduleDiffList = new ArrayList<>();
                publishJSONDTO.modules.forEach(module -> {
                    // git diff ${commitHash} develop -- ${path}
                    String path = module.name;

                    if (path.startsWith(":")) {
                        path = path.substring(1);

                    }
                    path = path.replace(":", "/");


                    GitLineHandler handler = new GitLineHandler(project, repository.getRoot(), GitCommand.DIFF, List.of("pager.diff=false"));
                    handler.addParameters(commitHash, "develop", "--", path);

                    GitCommandResult result = Git.getInstance().runCommand(handler);

                    if (result.success()) {
                        ModuleDiff moduleDiff = GitDiffSemanticAnalyzer.analyze(result.getOutputAsJoinedString());
                        moduleDiff.module = module;
                        moduleDiffList.add(moduleDiff);
                    }
                });
                List<ModuleDiff.Result> diffResults = moduleDiffList.stream().map(ModuleDiff::analyze).toList();

                String changeResult = diffResults.stream().filter(f -> ModuleDiff.Level.NONE != f.level).map(ModuleDiff.Result::toString).collect(Collectors.joining("\n==============\n\n"));
                String noneChangeResult = diffResults.stream().filter(f -> ModuleDiff.Level.NONE == f.level).map(ModuleDiff.Result::toString).collect(Collectors.joining());

                PublishJSON publishJSON = new PublishJSON();
                publishJSON.modules = diffResults.stream().map(ModuleDiff.Result::toNewModule).toList();
                String publishJSONContext = StringUtil.convertLineSeparators(JSON.toPrettyJSON(publishJSON));
                int lineEndOffset = publishJSONDoc.getLineEndOffset(Math.max(publishJSONDoc.getLineCount() - 1, 0));

                String summary = diffResults.stream().map(ModuleDiff.Result::toSummary).collect(Collectors.joining("\n"));

                ApplicationManager.getApplication().invokeLater(() -> {
                    TextAreaDialogWrapper historyDialog = new TextAreaDialogWrapper("Preview Change", "");
                    historyDialog.setInputText(changeResult + "\n==============\n" + noneChangeResult);
                    historyDialog.show();
                    if (historyDialog.cancel) {
                        return;
                    }
                    WriteCommandAction.runWriteCommandAction(project, () -> {
                        //UPDATE publish.json
                        publishJSONDoc.replaceString(0, lineEndOffset, publishJSONContext);
                    });
                    TextAreaDialogWrapper resultDialog = new TextAreaDialogWrapper("API Change Please review", "");
                    resultDialog.setInputText(summary);
                    resultDialog.show();
                });
            }
        }.queue();
    }

    @Override
    public void update(@NotNull AnActionEvent e) {
        VcsLogCommitSelection selection = e.getData(VcsLogDataKeys.VCS_LOG_COMMIT_SELECTION);
        e.getPresentation().setEnabledAndVisible(selection != null && !selection.getCommits().isEmpty());
    }
}
