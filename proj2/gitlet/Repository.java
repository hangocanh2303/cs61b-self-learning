package gitlet;

import java.io.File;
import java.util.Date;
import java.util.List;

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
        Commit commitZero = new Commit("initial commit", new Date(0), null, null);
        commitZero.saveCommit();
        updateHead(MASTER_BRANCH);
        createBranch(MASTER_BRANCH, commitZero.getCommitSha1());
    }

    public static void addCommand(String fileName) {
        List<String> fileNames = plainFilenamesIn(CWD);
        if (fileNames != null && !fileNames.contains(fileName)) {
            exitWithError("File does not exist.");
        }
        File addFile = join(CWD, fileName);
        byte[] contents = readContents(addFile);
        String sha1File = sha1((Object) contents);
    }

    private static void mkdirGitletFolder() {
        if (GITLET_DIR.exists()) {
            Utils.exitWithError("A Gitlet version-control system already exists in the current directory.");
        }else {
            GITLET_DIR.mkdir();
            Commit.COMMIT_FOLDER.mkdir();
            HEAD_DIR.mkdir();
        }
    }

    private static void updateHead(String branchName) {
        String ref = "heads/" + branchName;
        writeContents(HEAD_FILE, ref);
    }

    private static void createBranch(String branchName, String commitSha1) {
        File branchFile = join(HEAD_DIR, branchName);
        if (branchFile.exists()) {
            exitWithError("A branch with that name already exists.");
        }
        writeContents(branchFile, commitSha1);
    }
}
