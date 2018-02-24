//import org.apache.commons.io.FileUtils;
//import org.eclipse.jgit.api.*;
//import org.eclipse.jgit.api.errors.GitAPIException;
//import org.eclipse.jgit.lib.ObjectId;
//import org.eclipse.jgit.lib.Ref;
//import org.eclipse.jgit.lib.Repository;
//import org.eclipse.jgit.revwalk.RevCommit;
//import org.eclipse.jgit.storage.file.FileRepositoryBuilder;
//import org.eclipse.jgit.transport.PushResult;
//import org.eclipse.jgit.transport.UsernamePasswordCredentialsProvider;
//
//import java.io.File;
//import java.io.IOException;
//import java.util.Arrays;
//import java.util.Map;
//
///**
// * Created by alexanderm on 21/02/2018.
// */
//public class MyGit {
////    public static void main(String[] args) throws IOException, GitAPIException {
////
////        Repository repository = Helper.createNewRepository();
////        Git git = new Git(repository);
////        // create some commit on master
////        createCommit(repository, git, "masterFile", "content12");
////
////        // create branch "changes"
////        Ref changes = git.branchCreate().setName("changes").call();
////        System.out.println("Result of creating the branch: " + changes);
////
////        // now start a change on master
////        createCommit(repository, git, "sharedFile", "content12");
////
////        // check out branch "changes"
////        Ref checkout = git.checkout().setName("changes").call();
////        System.out.println("Result of checking out the branch: " + checkout);
////
////        // create some commit on branch "changes", one of them conflicting with the change on master
////        createCommit(repository, git, "branchFile", "content98");
////        createCommit(repository, git, "sharedFile", "content98");
////
////        // check out "master"
////        checkout = git.checkout().setName("master").call();
////        System.out.println("Result of checking out master: " + checkout);
////
////        // retrieve the objectId of the latest commit on branch
////        ObjectId mergeBase = repository.resolve("changes");
////
////
////
////
////        // perform the actual merge, here we disable FastForward to see the
////        // actual merge-commit even though the merge is trivial
////        MergeResult merge = git.merge().
////                include(mergeBase).
////                setCommit(true).
////                setFastForward(MergeCommand.FastForwardMode.NO_FF).
////                //setSquash(false).
////                        setMessage("Merged changes").
////                        call();
////        System.out.println("Merge-Results for id: " + mergeBase + ": " + merge);
//////        for (Map.Entry<String, int[][]> entry : merge.getConflicts().entrySet()) {
//////            System.out.println("Key: " + entry.getKey());
//////            for (int[] arr : entry.getValue()) {
//////                System.out.println("value: " + Arrays.toString(arr));
//////            }
//////        }
////        Map<String, int[][]> allConflicts = merge.getConflicts();
////        for (String path : allConflicts.keySet()) {
////            int[][] c = allConflicts.get(path);
////            System.out.println("Conflicts in file " + path);
////            for (int i = 0; i < c.length; ++i) {
////                System.out.println("  Conflict #" + i);
////                for (int j = 0; j < (c[i].length) - 1; ++j) {
////                    if (c[i][j] >= 0)
////                        System.out.println("    Chunk for "
////                                + merge.getMergedCommits()[j] + " starts on line #"
////                                + c[i][j]);
////                }
////            }
////        }
////        System.out.println(merge.getMergeStatus());
////        System.out.println(repository.getRepositoryState());
////        git.add().addFilepattern("sharedFile").call();
////        git.commit()
////                .setMessage("Added " + "sharedFile")
////                .call();
////        merge = git.merge().
////                include(mergeBase).
////                setCommit(true).
////                setFastForward(MergeCommand.FastForwardMode.NO_FF).
////                //setSquash(false).
////                        setMessage("Merged changes").
////                        call();
////        System.out.println(merge.getMergeStatus());
////
////        System.out.println(repository.getRepositoryState());
////
////        merge = git.merge().
////                include(mergeBase).
////                setCommit(true).
////                setFastForward(MergeCommand.FastForwardMode.NO_FF).
////                //setSquash(false).
////                        setMessage("Merged changes").
////                        call();
////        System.out.println(merge.getMergeStatus());
////
////        System.out.println(repository.getRepositoryState());
////
////        merge = git.merge().
////                include(mergeBase).
////                setCommit(true).
////                setFastForward(MergeCommand.FastForwardMode.NO_FF).
////                //setSquash(false).
////                        setMessage("Merged changes").
////                        call();
////        System.out.println(merge.getMergeStatus());
////
////        System.out.println(repository.getRepositoryState());
////    }
//
//    //    private static void createCommit(Repository repository, Git git, String fileName, String content) throws IOException, GitAPIException {
////        // create the file
////        File myFile = new File(repository.getDirectory().getParent(), fileName);
////        FileUtils.writeStringToFile(myFile, content, "UTF-8");
////
////        // run the add
////        git.add()
////                .addFilepattern(fileName)
////                .call();
////
////        // and then commit the changes
////        RevCommit revCommit = git.commit()
////                .setMessage("Added " + fileName)
////                .call();
////
////        System.out.println("Committed file " + myFile + " as " + revCommit + " to repository at " + repository.getDirectory());
////    }
//    public static void main(String[] args) throws IOException, GitAPIException {
//        // first create a test-repository, the return is including the .get directory here!
//        File repoDir = new File("C:\\Users\\alexanderm\\IdeaProjects\\myGit\\spectraGit\\.git");
//
//        // now open the resulting repository with a FileRepositoryBuilder
//        FileRepositoryBuilder builder = new FileRepositoryBuilder();
//        Repository repository = builder.setGitDir(repoDir)
//                .readEnvironment() // scan environment GIT_* variables
//                .findGitDir() // scan up the file system tree
//                .build();
//        System.out.println("Having repository: " + repository.getDirectory());
//
//        // the Ref holds an ObjectId for any type of object (tree, commit, blob, tree)
////            Ref head = repository.exactRef("/refs/remotes/origin/master");
////            System.out.println("Ref of refs/heads/master: " + head);
//
////            Git git = new Git(repository);
////
////            Status status = git.status().call();
////            System.out.println("Added: " + status.getAdded());
////            System.out.println("Changed: " + status.getChanged());
////            System.out.println("Conflicting: " + status.getConflicting());
////            System.out.println("ConflictingStageState: " + status.getConflictingStageState());
////            System.out.println("IgnoredNotInIndex: " + status.getIgnoredNotInIndex());
////            System.out.println("Missing: " + status.getMissing());
////            System.out.println("Modified: " + status.getModified());
////            System.out.println("Removed: " + status.getRemoved());
////            System.out.println("Untracked: " + status.getUntracked());
////            System.out.println("UntrackedFolders: " + status.getUntrackedFolders());
//
//        Git git = new Git(repository);
//
//        // add remote repo:
////        RemoteAddCommand remoteAddCommand = git.remoteAdd();
////        remoteAddCommand.setName("origin");
////        remoteAddCommand.setUri(new URIish(httpUrl));
////        // you can add more settings here if needed
////        remoteAddCommand.call();
//
//        // push to remote:
////        PullCommand pull = git.pull();
////        PullResult call = pull.call();
////        PushCommand pushCommand = git.push();
////        pushCommand.setCredentialsProvider(new UsernamePasswordCredentialsProvider("alexmalev", "Bubikim0804"));
////        // you can add more settings here if needed
////        Iterable<PushResult> pushResults = pushCommand.call();
//        Status status = git.status().call();
//        System.out.println("is clean: " + status.isClean());
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
//
//    }
//}
