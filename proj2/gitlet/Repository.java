package gitlet;

import java.io.File;
import java.util.Date;
import java.util.List;
import java.util.TreeMap;
import java.util.TreeSet;

import static gitlet.Utils.*;

// TODO: any imports you need here

/** Represents a gitlet repository.
 *  TODO: It's a good idea to give a description here of what else this Class
 *  does at a high level.
 *
 *  @author TODO
 */
public class Repository {
    /**
     * TODO: add instance variables here.
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

    /* TODO: fill in the rest of this class. */

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
        String headCommitSha1 = headCommit.getCommitSha1();

        if (sha1File.equals(headCommitSha1)) {
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
        }
        else {
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

    public static void logCommand() {
        Commit commit = Commit.getHeadCommit();
        while(commit != null) {
            commit.printCommitLog();
            String parentSha1 = commit.getFirstParentId();
            commit = parentSha1 != null ? Commit.getCommitWithId(parentSha1) : null;
        }
    }

    private static void mkdirGitletFolder() {
        if (GITLET_DIR.exists()) {
            Utils.exitWithError("A Gitlet version-control system already exists in the current directory.");
        }else {
            GITLET_DIR.mkdir();
            Commit.COMMIT_FOLDER.mkdir();
            HEAD_DIR.mkdir();
            Blob.BLOB_FOLDER.mkdir();
        }
    }
}
