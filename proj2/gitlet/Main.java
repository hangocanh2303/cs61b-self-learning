package gitlet;

/** Driver class for Gitlet, a subset of the Git version-control system.
 *  @author anhhn
 */
public class Main {

    /** Usage: java gitlet.Main ARGS, where ARGS contains
     *  <COMMAND> <OPERAND1> <OPERAND2> ... 
     */
    public static void main(String[] args) {
        // what if args is empty?
        if (args.length == 0) {
            Utils.exitWithError("Please enter a command.");
        }
        String firstArg = args[0];
        switch (firstArg) {
            case "init":
                // handle the `init` command
                validateNumArgs("init", args, 1);
                Repository.initCommand();
                break;
            case "add":
                // handle the `add [filename]` command
                validateNumArgs("add", args, 2);
                Repository.validateGitletFolder();
                Repository.addCommand(args[1]);
                break;
            case "commit":
                validateNumArgs("commit", args, 2);
                Repository.validateGitletFolder();
                Repository.commitCommand(args[1], null);
                break;
            case "checkout":
                Repository.validateGitletFolder();
                checkout(args);
                break;
            case "log":
                validateNumArgs("log", args, 1);
                Repository.validateGitletFolder();
                Repository.logCommand();
                break;
            case "global-log":
                validateNumArgs("global-log", args, 1);
                Repository.validateGitletFolder();
                Repository.globalLogCommand();
                break;
            case "rm":
                validateNumArgs("rm", args, 2);
                Repository.validateGitletFolder();
                Repository.rmCommand(args[1]);
                break;
            case "find":
                validateNumArgs("find", args, 2);
                Repository.validateGitletFolder();
                Repository.findCommand(args[1]);
                break;
            case "status":
                validateNumArgs("status", args, 1);
                Repository.validateGitletFolder();
                Repository.statusCommand();
                break;
            case "branch":
                validateNumArgs("branch", args, 2);
                Repository.validateGitletFolder();
                Repository.branchCommand(args[1]);
                break;
            case "rm-branch":
                validateNumArgs("rm-branch", args, 2);
                Repository.validateGitletFolder();
                Repository.removeBranchCommand(args[1]);
                break;
            case "reset":
                validateNumArgs("reset", args, 2);
                Repository.validateGitletFolder();
                Repository.resetCommand(args[1]);
                break;
            case "merge":
                validateNumArgs("merge", args, 2);
                Repository.validateGitletFolder();
                Repository.mergeCommand(args[1]);
                break;
            // FILL THE REST IN
            default:
                Utils.exitWithError("No command with that name exists.");
        }
    }

    /**
     * Checks the number of arguments versus the expected number,
     * throws a RuntimeException if they do not match.
     *
     * @param cmd Name of command you are validating
     * @param args Argument array from command line
     * @param n Number of expected arguments
     */
    public static void validateNumArgs(String cmd, String[] args, int n) {
        if (args.length != n) {
            throw new RuntimeException(
                    String.format("Invalid number of arguments for: %s.", cmd));
        }
    }

    public static void checkout(String[] args) {
        if (args.length == 4 && args[2].equals("--")) {
            Repository.checkoutFileFromCommit(args[1], args[3]);
        } else if (args.length == 3 && args[1].equals("--")) {
            Repository.checkoutFileFromHead(args[2]);
        } else if (args.length == 2) {
            Repository.checkoutBranch(args[1]);
        } else {
            Utils.exitWithError("Incorrect operands.");
        }
    }
}
