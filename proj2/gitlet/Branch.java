package gitlet;

import java.io.File;

import static gitlet.Utils.*;

public class Branch {

    public static void createBranch(String branchName, String commitSha1) {
        File branchFile = join(Repository.HEAD_DIR, branchName);
        if (branchFile.exists()) {
            exitWithError("A branch with that name already exists.");
        }
        writeContents(branchFile, commitSha1);
    }

    public static File getCurrentBranch() {
        String ref = Utils.readContentsAsString(Repository.HEAD_FILE);
        String branchName = ref.substring("heads/".length());
        return join(Repository.HEAD_DIR, branchName);
    }
}
