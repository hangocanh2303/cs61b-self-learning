package gitlet;

// TODO: any imports you need here

import java.io.File;
import java.io.Serializable;
import java.util.Date; // TODO: You'll likely use this in this class

import static gitlet.Utils.join;
import static gitlet.Utils.readContentsAsString;

/** Represents a gitlet commit object.
 *  TODO: It's a good idea to give a description here of what else this Class
 *  does at a high level.
 *
 *  @author TODO
 */
public class Commit implements Serializable {

    static final File COMMIT_FOLDER = Utils.join(Repository.GITLET_DIR, "objects");

    /**
     * TODO: add instance variables here.
     *
     * List all instance variables of the Commit class here with a useful
     * comment above them describing what that variable represents and how that
     * variable is used. We've provided one example for `message`.
     */

    /** The message of this Commit. */
    private String message;

    private Date timeStamp;

    private String firstParentId;

    private String secondParentId;

    /* TODO: fill in the rest of this class. */

    public Commit(String message, Date timeStamp, String firstParentId, String secondParentId) {
        this.message = message;
        this.timeStamp = timeStamp;
        this.firstParentId = firstParentId;
        this.secondParentId = secondParentId;
    }

    /**
     * Save a commit to a file
     */
    public void saveCommit() {
        File commit = Utils.join(COMMIT_FOLDER, getCommitSha1());
        Utils.writeObject(commit, this);
    }

    public static Commit fromFile(String commitSha1Id) {
        File commit = Utils.join(COMMIT_FOLDER, commitSha1Id);
        return Utils.readObject(commit, Commit.class);
    }

    public String getCommitSha1() {
        byte[] commitByteArray = Utils.serialize(this);
        return Utils.sha1((Object) commitByteArray);
    }

    public static Commit getHeadCommit() {
        String ref = Utils.readContentsAsString(Repository.HEAD_FILE);
        String branchName = ref.substring("heads/".length());
        File branchFile = join(Repository.HEAD_DIR, branchName);
        String commitSha1InBranchFile = readContentsAsString(branchFile);
        return Commit.fromFile(commitSha1InBranchFile);
    }
}
