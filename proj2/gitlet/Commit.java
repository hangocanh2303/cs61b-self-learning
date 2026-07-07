package gitlet;

// TODO: any imports you need here

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.*;

import static gitlet.Utils.join;
import static gitlet.Utils.readContentsAsString;

/** Represents a gitlet commit object.
 *  TODO: It's a good idea to give a description here of what else this Class
 *  does at a high level.
 *
 *  @author TODO
 */
public class Commit implements Dumpable {

    static final File COMMIT_FOLDER = Utils.join(Repository.GITLET_DIR, "objects");

//    SimpleDateFormat formatter = new SimpleDateFormat("EEE MMM d HH:mm:ss yyyy Z", Locale.ENGLISH);

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

    private TreeMap<String, String> trackedFiles;
    /* TODO: fill in the rest of this class. */

    public Commit(String message, Date timeStamp, String firstParentId, String secondParentId, TreeMap<String, String> trackedFiles) {
        this.message = message;
        this.timeStamp = timeStamp;
        this.firstParentId = firstParentId;
        this.secondParentId = secondParentId;
        this.trackedFiles = trackedFiles;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public void setTimeStamp(Date timeStamp) {
        this.timeStamp = timeStamp;
    }

    public void setFirstParentId(String firstParentId) {
        this.firstParentId = firstParentId;
    }

    public void setSecondParentId(String secondParentId) {
        this.secondParentId = secondParentId;
    }

    public void setTrackedFiles(TreeMap<String, String> trackedFiles) {
        this.trackedFiles = trackedFiles;
    }

    public TreeMap<String, String> getTrackedFiles() {
        return trackedFiles;
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

    public boolean containFile(String fileName) {
        return trackedFiles != null && trackedFiles.containsKey(fileName);
    }

    public String getBlobSha1(String fileName) {
        if (containFile(fileName)) {
            return trackedFiles.get(fileName);
        }
        return null;
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

    public static Commit getCommitWithId(String commitId) {
        String fullSha1Commit = findFullSha1CommitId(commitId);
        if (fullSha1Commit == null) {
            return null;
        }
        return Commit.fromFile(fullSha1Commit);
    }

    public void printCommitLog() {
        System.out.println("===");
        System.out.println("commit " + getCommitSha1());
        SimpleDateFormat formatter = new SimpleDateFormat("EEE MMM d HH:mm:ss yyyy Z", Locale.ENGLISH);
        System.out.println("Date: " + formatter.format(timeStamp));
        System.out.println(message);
        System.out.println();
    }

    private static String findFullSha1CommitId(String commitId) {
        if (commitId.length() == 40)  {
            return commitId;
        }
        List<String> allIds = Utils.plainFilenamesIn(COMMIT_FOLDER);
        if (allIds != null) {
            for (String fullCommitId: allIds) {
                if (fullCommitId.startsWith(commitId)) {
                    return fullCommitId;
                }
            }
        }
        return null;
    }

    public void updateTrackedFiles(StagingArea stagingArea) {
        // update add files
        TreeMap<String, String> addFiles = stagingArea.getAddFiles();
        TreeSet<String> removeFiles = stagingArea.getRemoveFiles();
        for(String fileName: addFiles.keySet()) {
            String sha1OfItemAddFile = addFiles.get(fileName);
            trackedFiles.put(fileName, sha1OfItemAddFile);
        }
        // update remove files
        for (String fileName: removeFiles) {
            trackedFiles.remove(fileName);
        }
    }

    @Override
    public String toString() {
        return "Commit{" +
                "message='" + message + '\'' +
                ", timeStamp=" + timeStamp +
                ", firstParentId='" + firstParentId + '\'' +
                ", secondParentId='" + secondParentId + '\'' +
                ", trackedFiles=" + trackedFiles +
                '}';
    }

    public String getFirstParentId() {
        return firstParentId;
    }

    public String getSecondParentId() {
        return secondParentId;
    }

    public String getMessage() {
        return message;
    }

    @Override
    public void dump() {
        System.out.println(this);
    }
}
