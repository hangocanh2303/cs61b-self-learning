package gitlet;

import java.io.File;
import java.util.List;
import static gitlet.Utils.*;

public class Branch {

    public static void createBranch(String branchName, String commitSha1) {
        File branchFile = join(Repository.HEAD_DIR, branchName);
        if (branchFile.exists()) {
            exitWithError("A branch with that name already exists.");
        }
        writeContents(branchFile, commitSha1);
    }

    public static void removeBranch(String branchName) {
        File branchFile = join(Repository.HEAD_DIR, branchName);
        if (!branchFile.exists()) {
            exitWithError("A branch with that name does not exist.");
        } else {
          String currentBranch = getCurrentBranchName();
          if (currentBranch.equals(branchName)) {
              exitWithError("Cannot remove the current branch.");
          } else {
              branchFile.delete();
          }
        }
    }

    public static File getCurrentBranch() {
        String ref = Utils.readContentsAsString(Repository.HEAD_FILE);
        String branchName = ref.substring("heads/".length());
        return join(Repository.HEAD_DIR, branchName);
    }

    public static String getCurrentBranchName() {
        String ref = Utils.readContentsAsString(Repository.HEAD_FILE);
        return ref.substring("heads/".length());
    }

    public static List<String> getAllBranch() {
        return Utils.plainFilenamesIn(Repository.HEAD_DIR);
    }

    /**
     * === Branches ===
     * *master
     * other-branch
     */
    public static void printAll() {
        System.out.println("=== Branches ===");
        String currentBranch = getCurrentBranchName();
        List<String> branches = getAllBranch();
        branches.sort(null);
        for (int i = 0; i < branches.size(); i += 1) {
            if (!branches.get(i).equals(currentBranch)) {
                System.out.println(branches.get(i));
            } else {
                branches.set(i, "*" + currentBranch);
                System.out.println(branches.get(i));
            }
        }
    }
}
