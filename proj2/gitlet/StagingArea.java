package gitlet;

import java.io.File;
import java.io.Serializable;
import java.util.TreeMap;
import java.util.TreeSet;

public class StagingArea implements Dumpable {

    static final File STAGING = Utils.join(Repository.GITLET_DIR, "staging");

    private TreeMap<String, String> addFiles;
    private TreeSet<String> removeFiles;

    public StagingArea(TreeMap<String, String> addFiles, TreeSet<String> removeFiles) {
        this.addFiles = addFiles;
        this.removeFiles = removeFiles;
    }

    public TreeMap<String, String> getAddFiles() {
        return addFiles;
    }

    public TreeSet<String> getRemoveFiles() {
        return removeFiles;
    }

    public void setAddFiles(TreeMap<String, String> addFiles) {
        this.addFiles = addFiles;
    }

    public void setRemoveFiles(TreeSet<String> removeFiles) {
        this.removeFiles = removeFiles;
    }

    public void updateAddFiles(String fileName, String sha1File) {
        this.addFiles.put(fileName, sha1File);
    }

    public void updateRemoveFiles(String fileName) {
        this.removeFiles.add(fileName);
    }

    public void removeAddFile(String fileName) {
        this.addFiles.remove(fileName);
    }

    public void removeItemInRemoveFiles(String fileName) {
        this.removeFiles.remove(fileName);
    }

    public void saveStagingArea() {
        Utils.writeObject(STAGING, this);
    }

    public static StagingArea fromFile() {
        return Utils.readObject(STAGING, StagingArea.class);
    }

    public String getStagingAreaSha1() {
        byte[] commitByteArray = Utils.serialize(this);
        return Utils.sha1((Object) commitByteArray);
    }

    public static StagingArea loadStagingArea() {
        if (StagingArea.STAGING.exists()) {
            return StagingArea.fromFile();
        }
        return new StagingArea(new TreeMap<>(), new TreeSet<>());
    }

    @Override
    public void dump() {
        System.out.println("addFiles: " + addFiles);
        System.out.println("removeFiles: " + removeFiles);
    }
}
