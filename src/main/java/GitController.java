import org.eclipse.jgit.api.*;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.lib.BranchTrackingStatus;
import org.eclipse.jgit.lib.Ref;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.storage.file.FileRepositoryBuilder;
import org.eclipse.jgit.transport.PushResult;
import org.eclipse.jgit.transport.RemoteRefUpdate;
import org.eclipse.jgit.transport.UsernamePasswordCredentialsProvider;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.*;

/**
 * Created by alexanderm on 24/02/2018.
 */
public class GitController {
    private Properties properties;
    private RepoStatus repoStatus;
    private FileStatus fileStatus;
    private Repository repository;
    private boolean canPush;
    private RelativeToRemote relativeToRemote;
    private boolean haveUncommitedChanges;
    private boolean haveUnstagedChanges;
    private boolean merging;
    private Git git;

    public GitController() {
        this.properties = getGitProperties();
        this.repoStatus = RepoStatus.Invalid;
        this.fileStatus = FileStatus.UptoDate;
        this.haveUncommitedChanges = false;
        this.canPush = false;
        this.relativeToRemote = RelativeToRemote.UptoDate;
        this.merging = false;
    }

    public void setRepoStatus(RepoStatus repoStatus) {
        this.repoStatus = repoStatus;
    }

    public RepoStatus getRepoStatus() {
        return repoStatus;
    }

    public FileStatus getFileStatus() {
        return fileStatus;
    }

    public boolean getPushStatus() {
        return canPush;
    }

    public boolean isMerging() {
        return merging;
    }


    public boolean isHaveUncommitedChanges() {
        return haveUncommitedChanges;
    }

    public boolean isHaveUnstagedChanges() {
        return haveUnstagedChanges;
    }
    public RelativeToRemote getRelativeToRemote() {
        return relativeToRemote;
    }


    public void setRepository(Repository repository) {
        this.repository = repository;
    }

    public void cloneRepo(File directory) throws GitAPIException {
        git = Git.cloneRepository()
                .setURI(properties.getProperty("remoteurl"))
                .setDirectory(directory)
                .call();
        repository = git.getRepository();
        if (repository.getAllRefs().entrySet().size() > 0) {
            repoStatus = RepoStatus.Open;
        }
    }

    private Properties getGitProperties() {
        Properties prop = new Properties();
        InputStream input = null;
        try {
            input = new FileInputStream("config.properties");
            prop.load(input);

        } catch (java.io.IOException ex) {
            ex.printStackTrace();
        } finally {
            if (input != null) {
                try {
                    input.close();
                } catch (java.io.IOException e) {
                    e.printStackTrace();
                }
            }
        }
        return prop;
    }

    public void openExistingRepo(File repoDirectory) {
        FileRepositoryBuilder builder = new FileRepositoryBuilder();
        File gitDirectory = new File(repoDirectory.getAbsolutePath() + "\\.git");
        try {
            repository = builder.setGitDir(gitDirectory)
                    .readEnvironment() // scan environment GIT_* variables
                    .findGitDir() // scan up the file system tree
                    .build();
        } catch (IOException e) {
            e.printStackTrace();
        }
        if (repository.getAllRefs().entrySet().size() > 0) {
            repoStatus = RepoStatus.Open;
        }
        git = new Git(repository);
    }

    public void add(String relativePath) {
        try {
            git.add()
                    .addFilepattern(relativePath)
                    .call();
        } catch (GitAPIException e) {
            e.printStackTrace();
        }
    }

    private void setUncommitedChangesStatus() {
        Status status = null;
        try {
            status = git.status().call();
        } catch (GitAPIException e) {
            e.printStackTrace();
        }
        Set<String> changed = status.getChanged();
        Set<String> added = status.getAdded();
        haveUncommitedChanges = changed.size() > 0 || added.size() > 0;
    }

    private void setUnstagedChangesStatus() {
        Status status = null;
        try {
            status = git.status().call();
        } catch (GitAPIException e) {
            e.printStackTrace();
        }
        Set<String> untracked = status.getUntracked();
        Set<String> modified = status.getModified();
        haveUnstagedChanges = untracked.size() > 0 || modified.size() > 0;
    }

    public void setFileStatus(String fileName) {
        Status status = null;
        try {
            status = git.status().call();
        } catch (GitAPIException e) {
            e.printStackTrace();
        }
        Set<String> modified = status.getModified();
        Set<String> untracked = status.getUntracked();
        Set<String> changed = status.getChanged();
        Set<String> conflicting = status.getConflicting();
        Set<String> added = status.getAdded();
        if (modified.contains(fileName)) {
            fileStatus = FileStatus.Modified;
            return;
        }
        if (untracked.contains(fileName)) {
            fileStatus = FileStatus.Untracked;
            return;
        }
        if (changed.contains(fileName)) {
            fileStatus = FileStatus.Changed;
            return;
        }
        if (conflicting.contains(fileName)) {
            fileStatus = FileStatus.Conflicting;
            return;
        }
        if (added.contains(fileName)) {
            fileStatus = FileStatus.Added;
            return;
        }
        fileStatus = FileStatus.UptoDate;
    }

    public void commit() {
        DateFormat df = new SimpleDateFormat("MM/dd/yyyy HH:mm:ss");
        Date now = new Date();
        String commitTime = df.format(now);
        try {
            git.commit()
                    .setMessage("Commit by spectra on " + commitTime)
                    .call();
        } catch (GitAPIException e) {
            e.printStackTrace();
        }
    }

    public void fetch() {
        try {
            git.fetch().setCheckFetchedObjects(true).call();
        } catch (GitAPIException e) {
            e.printStackTrace();
        }
    }

    public void push() {
        PushCommand pushCommand = git.push();
        pushCommand.setCredentialsProvider(new UsernamePasswordCredentialsProvider(properties.getProperty("username"),
                properties.getProperty("password")));
        try {
            Iterable<PushResult> pushResults = pushCommand.call();
            for (PushResult result : pushResults) {
                RemoteRefUpdate update = result.getRemoteUpdate("refs/heads/master");
                if (!update.getStatus().equals(RemoteRefUpdate.Status.OK)) {
                    canPush = false;
                    return;
                }
            }
            canPush = true;
        } catch (GitAPIException e) {
            e.printStackTrace();
        }

    }

    private void setMergingStatus() {
        File mergeFile = new File(repository.getDirectory() + "/MERGE_HEAD");
        boolean updatedMerging = mergeFile.isFile();
        if (merging & !updatedMerging)
            canPush = true;
        merging = mergeFile.isFile();
    }

    private void setStatusRelativeToRemote() {
        String masterBranch = "refs/heads/master";
        BranchTrackingStatus trackingStatus = null;
        try {
            trackingStatus = BranchTrackingStatus.of(repository, masterBranch);
        } catch (IOException e) {
            e.printStackTrace();
        }
        if (trackingStatus != null) {
            int aheadCount = trackingStatus.getAheadCount();
            int behindCount = trackingStatus.getBehindCount();
            if (aheadCount > 0 && behindCount > 0)
                relativeToRemote = RelativeToRemote.Diverged;
            else if (aheadCount > 0)
                relativeToRemote = RelativeToRemote.Ahead;
            else if (behindCount > 0)
                relativeToRemote = RelativeToRemote.Behind;
            else
                relativeToRemote = RelativeToRemote.UptoDate;
        }
    }

    public void refreshGitStatus() {
        setUncommitedChangesStatus();
        setUnstagedChangesStatus();
        setMergingStatus();
        setStatusRelativeToRemote();
    }

    public void merge() {
        Ref fetchHead = null;
        try {
            fetchHead = git.getRepository().exactRef("FETCH_HEAD");
        } catch (IOException e) {
            e.printStackTrace();
        }
        try {
            git.merge().include(fetchHead).setFastForward(MergeCommand.FastForwardMode.NO_FF).call();
        } catch (GitAPIException e) {
            e.printStackTrace();
        }
    }
}
