import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.PushCommand;
import org.eclipse.jgit.api.Status;
import org.eclipse.jgit.api.errors.GitAPIException;
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
import java.util.Date;
import java.util.Properties;
import java.util.Set;

/**
 * Created by alexanderm on 24/02/2018.
 */
public class GitController {
    private Properties properties;
    private RepoStatus repoStatus;
    private FileStatus fileStatus;
    private Repository repository;
    private PushStatus pushStatus;
    private boolean haveChanges;
    private Git git;

    public GitController() {
        this.properties = getGitProperties();
        this.repoStatus = RepoStatus.Invalid;
        this.fileStatus = FileStatus.UptoDate;
        this.haveChanges = false;
        this.pushStatus = PushStatus.Ok;
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

    public PushStatus getPushStatus() {
        return pushStatus;
    }

    public boolean isHaveChanges() {
        return haveChanges;
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
        System.out.println("Having repository: " + git.getRepository().getDirectory());
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

    public void setChangesStatus() {
        Status status = null;
        try {
            status = git.status().call();
        } catch (GitAPIException e) {
            e.printStackTrace();
        }
        Set<String> changed = status.getChanged();
        Set<String> added = status.getAdded();
        haveChanges = changed.size() > 0 || added.size() > 0;
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

    public void push() {
        PushCommand pushCommand = git.push();
        pushCommand.setCredentialsProvider(new UsernamePasswordCredentialsProvider(properties.getProperty("username"),
                properties.getProperty("password")));
        try {
            Iterable<PushResult> pushResults = pushCommand.call();
            for (PushResult result : pushResults) {
                RemoteRefUpdate update = result.getRemoteUpdate("refs/heads/master");
                if (!update.getStatus().equals(RemoteRefUpdate.Status.OK)){
                    pushStatus = PushStatus.Rejected;
                    return;
                }
            }
            pushStatus = PushStatus.Ok;
            System.out.println("pushed");
        } catch (GitAPIException e) {
            e.printStackTrace();
        }

    }
}
