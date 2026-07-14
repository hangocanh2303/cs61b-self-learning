package gitlet;

import java.io.File;

public class Blob {
    static final File BLOB_FOLDER = Utils.join(Commit.COMMIT_FOLDER, "blobs");

    private String sha1Id;

    private byte[] contents;

    public Blob(String sha1Id, byte[] contents) {
        this.sha1Id = sha1Id;
        this.contents = contents;
    }

    public void save() {
        File blob = Utils.join(BLOB_FOLDER, sha1Id);
        Utils.writeContents(blob, (Object) contents);
    }

    public static byte[] load(String blobSha1) {
        File blob = Utils.join(BLOB_FOLDER, blobSha1);
        return Utils.readContents(blob);
    }

    public static String readContentAsString(String blobSha1) {
        File blob = Utils.join(BLOB_FOLDER, blobSha1);
        return Utils.readContentsAsString(blob);
    }
}
