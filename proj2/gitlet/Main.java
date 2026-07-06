package gitlet;

/** Driver class for Gitlet, a subset of the Git version-control system.
 *  @author TODO
 */
public class Main {

    /** Usage: java gitlet.Main ARGS, where ARGS contains
     *  <COMMAND> <OPERAND1> <OPERAND2> ... 
     */
    public static void main(String[] args) {
        // TODO: what if args is empty?
        if (args.length == 0) {
            Utils.exitWithError("Please enter a command.");
        }
        String firstArg = args[0];
        switch(firstArg) {
            case "init":
                // TODO: handle the `init` command
                validateNumArgs("init", args, 1);
                Repository.initCommand();
                break;
            case "add":
                // TODO: handle the `add [filename]` command
                validateNumArgs("add", args, 2);
                Repository.addCommand(args[1]);
                break;
            case "commit":
                validateNumArgs("commit", args, 2);
                Repository.commitCommand(args[1]);
                break;
            case "checkout":
                checkout(args);
                break;
            case "log":
                validateNumArgs("log", args, 1);
                Repository.logCommand();
                break;
            case "global-log":
                validateNumArgs("global-log", args, 1);
                Repository.globalLogCommand();
                break;
            case "rm":
                validateNumArgs("rm", args, 2);
                Repository.rmCommand(args[1]);
                break;
            // TODO: FILL THE REST IN
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
        }
        else {
            Utils.exitWithError("Incorrect operands.");
        }
    }
}
