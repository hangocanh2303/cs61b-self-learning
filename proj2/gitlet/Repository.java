package gitlet;

import java.io.File;
import java.util.*;

import static gitlet.Utils.*;

// any imports you need here

/** Represents a gitlet repository.
 *  It's a good idea to give a description here of what else this Class
 *  does at a high level.
 *
 *  @author anhhn
 */
public class Repository {
    /**
     * add instance variables here.
     *
     * List all instance variables of the Repository class here with a useful
     * comment above them describing what that variable represents and how that
     * variable is used. We've provided two examples for you.
     */

    /** The current working directory. */
    public static final File CWD = new File(System.getProperty("user.dir"));

    /** The .gitlet directory. */
    public static final File GITLET_DIR = join(CWD, ".gitlet");

    public static final File HEAD_FILE = join(GITLET_DIR, "HEAD");

    public static final File HEAD_DIR = join(GITLET_DIR, "heads");

    public static final String MASTER_BRANCH = "master";

    /* fill in the rest of this class. */

    public static void initCommand() {
        mkdirGitletFolder();
        Commit commitZero = new Commit("initial commit", new Date(0), null, null, new TreeMap<>());
        commitZero.saveCommit();
        Head.updateHead(MASTER_BRANCH);
        Branch.createBranch(MASTER_BRANCH, commitZero.getCommitSha1());
    }

    public static void addCommand(String fileName) {
        List<String> fileNames = plainFilenamesIn(CWD);
        if (fileNames != null && !fileNames.contains(fileName)) {
            exitWithError("File does not exist.");
        }
        String sha1File = sha1FileCWD(fileName);
        StagingArea stagingArea = StagingArea.loadStagingArea();

        Commit headCommit = Commit.getHeadCommit();
        String headCommitBlobSha1 = headCommit.getBlobSha1(fileName);

        if (sha1File.equals(headCommitBlobSha1)) {
            stagingArea.removeAddFile(fileName);
        } else {
            // save blob
            File addFile = join(CWD, fileName);
            Blob blob = new Blob(sha1File, Utils.readContents(addFile));
            blob.save();
            stagingArea.updateAddFiles(fileName, sha1File);
        }
        stagingArea.removeItemInRemoveFiles(fileName);
        stagingArea.saveStagingArea();
    }

    public static void commitCommand(String message) {
        if (message == null || message.isBlank()) {
            Utils.exitWithError("Please enter a commit message.");
        }
        StagingArea stagingArea = StagingArea.loadStagingArea();
        if (stagingArea.isEmpty()) {
            Utils.exitWithError("No changes added to the commit.");
        }
        Commit headCommit = Commit.getHeadCommit();

        String oldSha1HeadCommit = headCommit.getCommitSha1();

        headCommit.updateTrackedFiles(stagingArea);
        headCommit.setMessage(message);
        headCommit.setTimeStamp(new Date());
        headCommit.setFirstParentId(oldSha1HeadCommit);
        headCommit.saveCommit();

        String newSha1HeadCommit = headCommit.getCommitSha1();
        // update head commit
        File branchFile = Branch.getCurrentBranch();
        Utils.writeContents(branchFile, newSha1HeadCommit);
        // clear and save staging area
        stagingArea.clear();
        stagingArea.saveStagingArea();
    }

    public static void checkoutFileFromCommit(String commitId, String fileName) {
        Commit targetCommit = Commit.getCommitWithId(commitId);
        if (targetCommit == null) {
            Utils.exitWithError("No commit with that id exists.");
        } else {
            if (!targetCommit.containFile(fileName)) {
                Utils.exitWithError("File does not exist in that commit.");
            }
            byte[] blob = Blob.load(targetCommit.getBlobSha1(fileName));
            File targetFileCWD = Utils.join(CWD, fileName);
            Utils.writeContents(targetFileCWD, (Object) blob);
        }
    }

    public static void checkoutFileFromHead(String fileName) {
        Commit targetCommit = Commit.getHeadCommit();
        checkoutFileFromCommit(targetCommit.getCommitSha1(), fileName);
    }

    public static void checkoutBranch(String branchName) {
        File branchFile = join(Repository.HEAD_DIR, branchName);
        if (!branchFile.exists()) {
            exitWithError("No such branch exists.");
        } else {
            String currentBranch = Branch.getCurrentBranchName();
            if (currentBranch.equals(branchName)) {
                exitWithError("No need to checkout the current branch.");
            } else {
                Commit currentHeadCommit = Commit.getHeadCommit();
                Commit targetCommit = Commit.getCommitWithId(readContentsAsString(branchFile));
                List<String> untrackedFiles = getUntrackedFiles(false);
                for (String untrackedFile: untrackedFiles) {
                    if (targetCommit != null && targetCommit.containFile(untrackedFile)) {
                        exitWithError("There is an untracked file in the way; " +
                                "delete it, or add and commit it first.");
                        return;
                    }
                }

                // write content from target commit to cwd
                TreeMap<String, String> targetCommitTrackedFiles = targetCommit != null ? targetCommit.getTrackedFiles() : new TreeMap<>();
                if (targetCommit != null) {
                    for (String fileName: targetCommitTrackedFiles.keySet()) {
                        byte[] blob = Blob.load(targetCommitTrackedFiles.get(fileName));
                        writeContents(join(CWD, fileName), (Object) blob);
                    }
                }

                // update head file to branch name
                Head.updateHead(branchName);

                // delete tracked files head but not in target (cwd)
                TreeMap<String, String> currentHeadCommitTrackedFiles = currentHeadCommit != null ? currentHeadCommit.getTrackedFiles() : new TreeMap<>();
                if (targetCommit != null) {
                    for (String fileName: currentHeadCommitTrackedFiles.keySet()) {
                        if (!targetCommit.containFile(fileName)) {
                            restrictedDelete(fileName);
                        }
                    }
                }

                // update staging area
                StagingArea stagingArea = StagingArea.loadStagingArea();
                stagingArea.clear();
                stagingArea.saveStagingArea();

            }
        }
    }

    public static void logCommand() {
        Commit commit = Commit.getHeadCommit();
        while (commit != null) {
            commit.printCommitLog();
            String parentSha1 = commit.getFirstParentId();
            commit = parentSha1 != null ? Commit.getCommitWithId(parentSha1) : null;
        }
    }

    public static void globalLogCommand() {
        List<String> commits = Utils.plainFilenamesIn(Commit.COMMIT_FOLDER);
        if (commits != null) {
            for (String sha1Commit: commits) {
                Commit commit = Commit.fromFile(sha1Commit);
                commit.printCommitLog();
            }
        }
    }

    public static void rmCommand(String fileName) {
        StagingArea stagingArea = StagingArea.loadStagingArea();
        boolean rmFileNotInAddFiles = true;
        if (stagingArea.getAddFiles().containsKey(fileName)) {
            rmFileNotInAddFiles = false;
            stagingArea.getAddFiles().remove(fileName);
            stagingArea.saveStagingArea();
        }

        Commit headCommit = Commit.getHeadCommit();

        boolean rmFileNotInTrackedFiles = true;
        if (headCommit.containFile(fileName)) {
            stagingArea.getRemoveFiles().add(fileName);
            Utils.restrictedDelete(fileName);
            rmFileNotInTrackedFiles = false;
            stagingArea.saveStagingArea();
        }
        if (rmFileNotInAddFiles && rmFileNotInTrackedFiles) {
            Utils.exitWithError("No reason to remove the file.");
        }
    }

    public static void findCommand(String message) {
        List<String> commits = Utils.plainFilenamesIn(Commit.COMMIT_FOLDER);
        boolean found = false;
        if (commits != null) {
            for (String sha1Commit: commits) {
                Commit commit = Commit.fromFile(sha1Commit);
                if (commit.getMessage().equals(message)) {
                    System.out.println(sha1Commit);
                    found = true;
                }
            }
        }
        if (!found) {
            Utils.exitWithError("Found no commit with that message.");
        }
    }

    public static void statusCommand() {
        Branch.printAll();
        System.out.println();

        StagingArea stagingArea = StagingArea.loadStagingArea();
        stagingArea.printAddFiles();
        System.out.println();

        stagingArea.printRemoveFiles();
        System.out.println();

        modifyNotStagedForCommit();
        System.out.println();

        untrackedFiles();
    }

    public static void branchCommand(String fileName) {
        Commit headCommit = Commit.getHeadCommit();
        Branch.createBranch(fileName, headCommit.getCommitSha1());
    }

    public static void removeBranchCommand(String branchName) {
        Branch.removeBranch(branchName);
    }

    public static void resetCommand(String commitSha1) {
        Commit targetCommit = Commit.getCommitWithId(commitSha1);
        if (targetCommit == null) {
            exitWithError("No commit with that id exists.");
        } else {
            Commit headCommit = Commit.getHeadCommit();
            for (String fileName: headCommit.getTrackedFiles().keySet()) {
                if (!targetCommit.containFile(fileName)) {
                    restrictedDelete(fileName);
                }
            }

            List<String> untrackedFiles = getUntrackedFiles(false);
            for (String untrackedFile: untrackedFiles) {
                if (targetCommit.containFile(untrackedFile)) {
                    exitWithError("There is an untracked file in the way; " +
                            "delete it, or add and commit it first.");
                    return;
                }
            }

            for (String fileName: targetCommit.getTrackedFiles().keySet()) {
                checkoutFileFromCommit(commitSha1, fileName);
            }

            // Move pointer
            Branch.updateCurrentBranch(Commit.findFullSha1CommitId(commitSha1));

            // Staging area
            StagingArea stagingArea = StagingArea.loadStagingArea();
            stagingArea.clear();
            stagingArea.saveStagingArea();

        }
    }

    private static void mkdirGitletFolder() {
        if (GITLET_DIR.exists()) {
            Utils.exitWithError("A Gitlet version-control system already " +
                    "exists in the current directory.");
        } else {
            GITLET_DIR.mkdir();
            Commit.COMMIT_FOLDER.mkdir();
            HEAD_DIR.mkdir();
            Blob.BLOB_FOLDER.mkdir();
        }
    }

    private static void modifyNotStagedForCommit() {
        System.out.println("=== Modifications Not Staged For Commit ===");

        Commit headCommit = Commit.getHeadCommit();
        StagingArea stagingArea = StagingArea.loadStagingArea();
        TreeSet<String> allFiles = new TreeSet<>();
        allFiles.addAll(Utils.plainFilenamesIn(CWD));
        allFiles.addAll(stagingArea.getAddFiles().keySet());
        allFiles.addAll(stagingArea.getRemoveFiles());
        allFiles.addAll(headCommit.getTrackedFiles().keySet());

        for (String fileName: allFiles) {
            boolean inTrackedFile = headCommit.containFile(fileName);
            if (Utils.isDeletedFromCWD(fileName)) {
                // condition 3
                if (stagingArea.isStagingAdd(fileName)) {
                    System.out.println(fileName + " (deleted)");
                } else if (!stagingArea.isStagingRemove(fileName) && inTrackedFile) { // condition 4
                    System.out.println(fileName + " (deleted)");
                }
            } else {

                String cwdSha1 = Utils.sha1FileCWD(fileName);
                // condition 1
                if (inTrackedFile) {
                    if (!cwdSha1.equals(headCommit.getTrackedFiles().get(fileName))
                            && !stagingArea.isStagingAdd(fileName)
                            && !stagingArea.isStagingRemove(fileName)) {
                        System.out.println(fileName + " (modified)");
                    }
                }

                // condition 2
                if (stagingArea.isStagingAdd(fileName)
                        && !cwdSha1.equals(stagingArea.getAddFiles().get(fileName))) {
                    System.out.println(fileName + " (modified)");
                }
            }
        }

    }

    private static void untrackedFiles() {
        System.out.println("=== Untracked Files ===");
        getUntrackedFiles(true);
    }

    private static List<String> getUntrackedFiles(boolean print) {
        StagingArea stagingArea = StagingArea.loadStagingArea();
        Commit headCommit = Commit.getHeadCommit();
        List<String> cwdFiles = Utils.plainFilenamesIn(CWD);
        List<String> untrackedFiles = new ArrayList<>();

        if (cwdFiles != null) {
            for (String fileName: cwdFiles) {
                if ((!stagingArea.isStagingAdd(fileName) && !headCommit.containFile(fileName))
                        || stagingArea.isStagingRemove(fileName)) {
                    if (print) {
                        System.out.println(fileName);
                    }
                    untrackedFiles.add(fileName);
                }
            }
        }
        return untrackedFiles;
    }
}
