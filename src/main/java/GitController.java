import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.Status;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.storage.file.FileRepositoryBuilder;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
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
    private Git git;

    public void setRepoStatus(RepoStatus repoStatus) {
        this.repoStatus = repoStatus;
    }

    public RepoStatus getRepoStatus() {
        return repoStatus;
    }
    public FileStatus getFileStatus() {
        return fileStatus;
    }


    public void setRepository(Repository repository) {
        this.repository = repository;
    }



    public GitController() {
        this.properties = getGitProperties();
        this.repoStatus = RepoStatus.Invalid;
        this.fileStatus = FileStatus.UptoDate;
    }

    public void cloneRepo(File directory) throws GitAPIException {
        git = Git.cloneRepository()
                .setURI(properties.getProperty("remoteurl"))
                .setDirectory(directory)
                .call();
        repository = git.getRepository();
        System.out.println("Having repository: " + git.getRepository().getDirectory());
        if (repository.getAllRefs().entrySet().size() > 0){
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
        if (repository.getAllRefs().entrySet().size() > 0){
            repoStatus = RepoStatus.Open;
        }
        git = new Git(repository);
    }

    public void add(File fileToAdd) {
        try {
            git.add()
                    .addFilepattern(fileToAdd.getName())
                    .call();
        } catch (GitAPIException e) {
            e.printStackTrace();
        }
    }

    public void setFileStatus(String fileName){
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
        if (modified.contains(fileName)){
            fileStatus = FileStatus.Modified;
            return;
        }
        if (untracked.contains(fileName)){
            fileStatus = FileStatus.Untracked;
            return;
        }
        if (changed.contains(fileName)){
            fileStatus = FileStatus.Changed;
            return;
        }
        if (conflicting.contains(fileName)){
            fileStatus = FileStatus.Conflicting;
            return;
        }
        if (added.contains(fileName)){
            fileStatus = FileStatus.Added;
            return;
        }
        fileStatus = FileStatus.UptoDate;

//        System.out.println("Added: " + status.getAdded());
//        System.out.println("Changed: " + status.getChanged());
//        System.out.println("Conflicting: " + status.getConflicting());
//        System.out.println("ConflictingStageState: " + status.getConflictingStageState());
//        System.out.println("IgnoredNotInIndex: " + status.getIgnoredNotInIndex());
//        System.out.println("Missing: " + status.getMissing());
//        System.out.println("Modified: " + status.getModified());
//        System.out.println("Removed: " + status.getRemoved());
//        System.out.println("Untracked: " + status.getUntracked());
//        System.out.println("UntrackedFolders: " + status.getUntrackedFolders());
    }
}
