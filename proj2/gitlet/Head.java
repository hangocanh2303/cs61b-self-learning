package gitlet;

import static gitlet.Utils.writeContents;

public class Head {

    public static void updateHead(String branchName) {
        String ref = "heads/" + branchName;
        writeContents(Repository.HEAD_FILE, ref);
    }
}
