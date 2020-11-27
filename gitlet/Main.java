package gitlet;
import java.io.File;
/** Driver class for Gitlet, the tiny stupid version-control system.
 *  @author Daivd Bang
 */
public class Main {

    /** Usage: java gitlet.Main ARGS, where ARGS contains
     *  <COMMAND> <OPERAND> .... */
    private static Gitlet controller;
    private static final String controlPath = ".gitlet";
    private static final String sep = File.separator;
    /**Main menu of Gitlet system, receive input.
     * @return**/
    public static void main(String... args) {
         try {
             if (isInitialized()) {
                 controller = retrieveGit();
             }
             else {
                 if (args[0].equals("init")) {
                     controller = new Gitlet();
                 } else {
                     Utils.message("Not in an initialized Gitlet directory.");
                     System.exit(0);
                 }
             }
             String str1 = "";
             String str2 = "";
             String str3 = "";
             if (args.length == 0) {
                 Utils.message("Please enter a command");
                 throw new GitletException();
             }
             if (args.length > 1) {
                 str1 = args[1];
             }
             if (args.length > 2) {
                 str2 = args[2];
             }
             if (args.length > 3) {
                 str3 = args[3];
             }
             String command = args[0];
             switch (command) {
                 case "init":
                     controller.init();
                     break;
                 case "add":
                     controller.add(str1);
                     break;
                 case "commit":
                     controller.commit(str1, null);
                     break;
                 case "log":
                     controller.log();
                     break;
                 case "global-log":
                     controller.globalLog();
                     break;
                 case "rm":
                     controller.rm(str1);
                     break;
                 case "checkout":
                     if (args.length == 2) {
                         controller.checkoutByBranch(str1);
                     } else if (args.length == 3) {
                         controller.checkoutByFileName(str2);
                     } else if (args.length == 4) {
                         if (!str2.equals("--")) {
                             System.out.println("Incorrect operands");
                             throw new GitletException();
                         }
                         controller.checkoutByCommit(str1, str3);
                     }
                     break;
                 case "branch":
                     controller.branch(str1);
                     break;
                 case "rm-branch":
                     controller.rmBranch(str1);
                     break;
                 case "reset":
                     controller.reset(str1);
                     break;
                 case "status":
                     controller.status();
                     break;
                 case "merge":
                     controller.merge(str1);
                     break;
                 case "find":
                     controller.find(str1);
                     break;
                 default:
                     Utils.message("Incorrect Operand.");
                     throw new GitletException();
             }
             File savingGit = Utils.join(controlPath, "CMD");
             Utils.writeObject(savingGit, controller);
         }
         catch (GitletException exception) {
             File savingGit = Utils.join(controlPath, "CMD");
             Utils.writeObject(savingGit, controller);
         }


    }
    public static boolean isInitialized() {
        File dir = new File(System.getProperty("user.dir") + sep + ".gitlet");
        if (dir.exists()) {
            return true;
        }
        return false;
    }
    public static Gitlet retrieveGit() {
         return Utils.readObject(Utils.join(controlPath, "CMD"), Gitlet.class);
    }


}
