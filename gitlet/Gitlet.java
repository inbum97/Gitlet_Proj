package gitlet;

import java.io.File;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Arrays;
import java.util.Objects;

/*Gitlet is a controller to every command*/
public class Gitlet implements Serializable {

    /*Instances*/
    private static final String gitlet_Directory = ".gitlet";
    private static final String sep = File.separator;
    //private static final String commit_Directory = ".gitlet/Commit";
    //private static final String stageA_Directory = ".gitlet/StageAdd";
    private String Head;
    private HashMap<String,String> branches;
    private HashMap<String, String> toStage;
    private ArrayList<String> toRemove;
    private ArrayList<String> removed;
    /*return head*/
    public String getHead(String headPtr) {
        return branches.get(headPtr);
    }
    /*return commitID of head*/
    public HashMap<String, String> getBranch() {
        return branches;
    }

    /***********************************Init.*************************
    ** Initialize gitlet to get started.*******************************/
    public void init() {
        File gitlet_dir = new File(gitlet_Directory);
        if (gitlet_dir.exists()) {
            Utils.message("A Gitlet version-control system already exists in the current directory.");
            return;
        }
        Head = "master";
        Commit firstCommit = new Commit();
        //File commit_dir = new File(commit_Directory);
        //File stage_dirA = new File(stageA_Directory);
        File commit_dir = Utils.join(gitlet_Directory,"Commit");
        File stage_dirA = Utils.join(gitlet_Directory,"StageAdd");
        gitlet_dir.mkdir();
        commit_dir.mkdir();
        stage_dirA.mkdir();

        String sha1ID = firstCommit.get_ID();
        branches = new HashMap<String, String>();
        branches.put(Head, sha1ID);
        File initialCommit = new File(gitlet_Directory + sep +  "Commit" + sep + sha1ID);
        //File initialCommit = new File(commit_Directory+ "/" + sha1ID);
        Utils.writeObject(initialCommit, firstCommit);
        toStage = new HashMap<String, String>();
        toRemove = new ArrayList<String>();
        removed = new ArrayList<String>();
    }

    /***********************************Add.**********************
    ** add a file into a staging area to be commited.************/
    @SuppressWarnings("unchecked")
    public void add(String fileName) {

        File fileRef= new File(fileName);
        if (!fileRef.exists()) {
            System.out.println("File does not exist.");
            return;
        }
        String incomingID = Utils.sha1(Utils.readContentsAsString(fileRef));
        String curHeadPtr = getHead(Head);
        Commit prevCommit = id2Commit(curHeadPtr);
        HashMap<String , String> prevCommitedFiles= prevCommit.getBlobs();
        if (isModified(prevCommitedFiles, incomingID,fileName)) {
            File incomingBlob = new File(gitlet_Directory + sep +  "StageAdd" + sep + incomingID);
            toStage.put(fileName, incomingID);
            String fileContent = Utils.readContentsAsString(fileRef);
            Utils.writeContents(incomingBlob, fileContent);
        }
        if (toRemove != null &&toRemove.contains(fileName)) {
            toStage.remove(fileName);
            toRemove.remove(fileName);
        }

    }

    /***********************Commit*****************************
     *
     * Take a snapshot of all the files added and save them
     */
    @SuppressWarnings("unchecked")
    public void commit(String commit_msg, String[] duparent) {

        if (commit_msg.equals("")) {
            //Utils.message("Please enter a commit message.");
            System.out.println("Please enter a commit message.");
            return;
        }
        if (toStage.isEmpty() && toRemove.isEmpty()) {
            Utils.message("No changes added to the commit.");
            return;
        }
        Commit latestCommit = id2Commit(getHead(Head));
        String latestID[] = new String[] {latestCommit.get_ID()};
        HashMap<String, String> toCommit = latestCommit.getBlobs();
        if (toCommit == null) {
            toCommit = new HashMap<String, String>();
        }
        if (!toStage.isEmpty()) {
            for(String key :toStage.keySet()) {
                toCommit.put(key,toStage.get(key));
            }
            if (toRemove!=null) {
                for (String file : toRemove) {
                    toCommit.remove(file);
                }
            }
        }
        Commit newC;
        if (duparent == null) {
            newC = new Commit(toCommit, commit_msg, latestID);
        }
        else {
            newC = new Commit(toCommit, commit_msg, duparent);
        }
        String newID = newC.get_ID();
        branches.put(Head, newID);
        File newFile = new File(gitlet_Directory + sep +  "Commit" + sep + newID);
        //File newFile = new File(commit_Directory+"/"+newID);
        Utils.writeObject(newFile, newC);
        toStage.clear();
        if (toRemove != null) {
            toRemove.clear();
        }
    }

    /*****************************Log*************************
     * Display metadata of each commit
     */
    @SuppressWarnings("unchecked")
    public void log(){

        String currentCommit = getHead(Head);
        while (currentCommit!= null) {
            Commit toPrint = id2Commit(currentCommit);
            toPrint.displayLog();
            currentCommit = toPrint.getParent();
        }
    }

    /**********************************Remove************************
     *
     * remove file if it's in staging area, or marked to be removed
     * for the next commit
     */
    @SuppressWarnings("unchecked")
    public void rm(String fileName) {

        String cur = getHead(Head);
        Commit curCommit = id2Commit(cur);
        HashMap<String, String> trackingBlobs = curCommit.getBlobs();
        boolean isStaged = toStage.containsKey(fileName);
        boolean isTracked = false;
        if (trackingBlobs != null && trackingBlobs.containsKey(fileName)) {
            isTracked = true;
        }
        if (!isStaged && !isTracked) {
            Utils.message("No reason to remove the file.");
            return;
        }
        if (isStaged) {
            toStage.remove(fileName);
        }
        if (isTracked) {
            //String toRemoveID = trackingBlobs.get(fileName);
            toRemove.add(fileName);
            removed.add(fileName);
            Utils.restrictedDelete(fileName);
        }

    }

    /***********************Global Log*****************
     * Display metadata of every commit
     */
    @SuppressWarnings("unchecked")
    public void globalLog() {
        File allCommit = Utils.join(gitlet_Directory, "Commit");
        //File allCommit = new File(commit_Directory);
        for (File eachCommit : allCommit.listFiles()) {
            Commit toPrint = id2Commit(eachCommit.getName());
            toPrint.displayLog();
        }
    }

    /**************************Checkout**********************
     *
     * replace files from previous history by commit ID,
     * or fileName, or name of a branch.
     */
    @SuppressWarnings("unchecked")
    public void checkoutByFileName(String fileName) {

        String cur = getHead(Head);
        Commit currentCommit = id2Commit(cur);
        HashMap<String,String> tracked = currentCommit.getBlobs();
        if (tracked!=null && tracked.containsKey(fileName)) {
                File toChange = new File(fileName);
                String restoreBlob = gitlet_Directory + sep + "StageAdd" + sep + tracked.get(fileName);
                File overwrite = new File(restoreBlob);
                String content = Utils.readContentsAsString(overwrite);
                Utils.writeContents(toChange, content);
        }
        else {
            Utils.message("File does not exist in that commit.");
            return;
        }
    }
    @SuppressWarnings("unchecked")
    public void checkoutByCommit(String ID, String fileName) {

        ID = abbrevID2Full(ID);
        Commit specificCommit = id2Commit(ID);
        HashMap<String,String> tracked= specificCommit.getBlobs();
        if (tracked!=null && tracked.containsKey(fileName)) {
                File toChange = new File(fileName);
                String restoreBlob = gitlet_Directory + sep + "StageAdd" + sep + tracked.get(fileName);
                File overwrite = new File(restoreBlob);
                String content = Utils.readContentsAsString(overwrite);
                Utils.writeContents(toChange, content);
        }
        else {
            Utils.message("File does not exist in that commit.");
            return;
        }
    }
    @SuppressWarnings("unchecked")
    public void checkoutByBranch(String branchN) {

        if (!branches.containsKey(branchN)) {
            Utils.message("No such branch exists.");
            return;
        }
        if (Head.equals(branchN)) {
            Utils.message("No need to checkout the current branch.");
            return;
        }
        String currentHead = getHead(Head);
        String branchHead = getHead(branchN);
        Commit currentCommit = id2Commit(currentHead);
        Commit branchCommit = id2Commit(branchHead);
        HashMap<String, String> tracked = branchCommit.getBlobs();
        HashMap<String, String> curTracked = currentCommit.getBlobs();
        isUntracked(curTracked);
        if (tracked != null) {
            for (String fileName : tracked.keySet()) {
                File f = new File(gitlet_Directory + sep + "StageAdd" + sep + tracked.get(fileName));
                String content = Utils.readContentsAsString(f);
                File proj3 = Utils.join(System.getProperty("user.dir"), fileName);
                Utils.writeContents(proj3, content);
            }
            if (curTracked != null) {
                for (String fileN : curTracked.keySet()) {
                    if (!tracked.containsKey(fileN)) {
                        File tbDel = Utils.join(System.getProperty("user.dir"), fileN);
                        removed.add(fileN);
                        Utils.restrictedDelete(tbDel);
                    }
                }
            }

        }
        else if(curTracked != null) {
            for (String fileN : curTracked.keySet()) {
                File tbDel = Utils.join(System.getProperty("user.dir"), fileN);
                removed.add(fileN);
                Utils.restrictedDelete(tbDel);
            }
        }
            branches.put(branchN, branchCommit.get_ID());
            Head = branchN;
            toStage.clear();
        }

    /***********************Branch*************************
     *
     * Add a new branch
     */
    @SuppressWarnings("unchecked")
    public void branch(String branchN) {

        if (branches.containsKey(branchN)) {
            Utils.message("A branch with that name already exists.");
            return;
        }
        else
            branches.put(branchN, getHead(Head));

    }

    /*******************************Remove Branch**********************
     *
     * Remove branch by deferencing currently pointing commit
     */
    @SuppressWarnings("unchecked")
    public void rmBranch(String branchName) {

        if (!branches.containsKey(branchName)) {
            Utils.message("A branch with that name does not exist.");
            return;
        }
        if (Head.equals(branchName)) {
            Utils.message("Cannot remove the current branch.");
            return;
        }
        branches.remove(branchName);

    }

    /*****************************Find***********************
     *
     * Print ID of commit that has a given commit message
     */
    public void find(String msg) {
        File allCommit = Utils.join(gitlet_Directory, "Commit");
        boolean isExist = false;
        for(File eachCommit : Objects.requireNonNull(allCommit.listFiles())) {
            Commit toPrint = id2Commit(eachCommit.getName());
            if (toPrint.getMsg().equals(msg)) {
                System.out.println(toPrint.get_ID());
                isExist = true;
            }
        }
        if (!isExist) {
            Utils.message("Found no commit with that message.");
            return;
        }
    }

    /*************************Reset**********************
     *
     * reset it
     */
    @SuppressWarnings("unchecked")
    public void reset(String ID) {

        ID= abbrevID2Full(ID);
        Commit specificCommit = id2Commit(ID);
        Commit cur = id2Commit(getHead(Head));
        HashMap<String, String> blobs = specificCommit.getBlobs();
        isUntracked(cur.getBlobs());
        File trackedFiles= new File(System.getProperty("user.dir"));
        for (File file : trackedFiles.listFiles()) {
            if (file.getName().endsWith("txt") && file.getName() != ".gitignore") {
                if (!blobs.containsKey(file.getName())) {
                    removed.add(file.getName());
                    Utils.restrictedDelete(file);
                }
            }

        }
        branches.put(Head,specificCommit.get_ID());
        toStage.clear();

    }

    /************************Status*******************************
     * Display status of current gitlet system
     */
    @SuppressWarnings("unchecked")
    public void status() {

        System.out.println("=== Branches ===");
        Object[] lstBranch = branches.keySet().toArray();
        Arrays.sort(lstBranch);
        for (Object branch : lstBranch) {
            if (branch.equals(Head)) {
                System.out.print("*");
            }
            System.out.println(branch);
        }
        System.out.print("\n");
        System.out.println("=== Staged Files ===");
        Object[] lstStaged = toStage.keySet().toArray();
        Arrays.sort(lstStaged);
        for (Object stage : lstStaged) {
            System.out.println(stage);
        }
        System.out.print("\n");
        System.out.println("=== Removed Files ===");
        if (removed!=null) {
            Object[] removedFiles = toRemove.toArray();
            Arrays.sort(removedFiles);
            for (Object file : removedFiles) {
                System.out.println(file);
            }
        }
        System.out.println();
        System.out.println("=== Modifications Not Staged For Commit ===");
        System.out.println();
        System.out.println("=== Untracked Files ===");
        System.out.println();

    }

    /***************************Merge********************
     *
     * Merge commit from current branch to given branch
     */
    @SuppressWarnings("unchecked")
    public void merge(String branchN) {

        if (!toStage.isEmpty() || !toRemove.isEmpty()) {
            Utils.message("You have uncommitted changes.");
            return;
        }
        if (!branches.containsKey(branchN)) {
            Utils.message("A branch with that name does not exist.");
            return;
        }
        if (branchN == Head) {
            Utils.message("Cannot merge a branch with itself.");
            return;
        }
        Commit curCommit = id2Commit(getHead(Head));
        HashMap<String, String> curTracked = curCommit.getBlobs();
        isUntracked(curTracked);
        //Finding splitPoint commit
        String splitID = "";
        ArrayList<String> currBranchCommits = new ArrayList<String>();
        ArrayList<String> givenBranchCommits = new ArrayList<String>();
        String curID = getHead(Head);
        String branchID = getHead(branchN);
        while (curID != null) {
            currBranchCommits.add(curID);
            Commit curCom = id2Commit(curID);
            curID = curCom.getParent();
        }
        while (branchID != null) {
            givenBranchCommits.add(branchID);
            Commit branchCom = id2Commit(branchID);
            branchID = branchCom.getParent();
        }
        for (String id : currBranchCommits) {
            if (givenBranchCommits.contains(id)) {
                splitID = id;
                break;
            }
        }
        //Found the split Commit
        Commit splitCommit = id2Commit(splitID);
        HashMap<String, String> splitBlobs = splitCommit.getBlobs();
        if (splitID.equals(getHead(branchN))) {
            Utils.message("Given branch is an ancestor of the current branch.");
            return;
        }
        if (splitID.equals(getHead(Head))) {
            branches.put(Head, getHead(branchN));
            Utils.message("Current branch fast-forwarded.");
            return;
        }
        //Merge method
        String bID = getHead(branchN);
        Commit bCom = id2Commit(bID);
        HashMap<String, String> bBlob = bCom.getBlobs();
        ArrayList<String> addi = new ArrayList<String>();
        boolean isCurAdded = false;
        boolean isGvnAdded = false;
        boolean isConflict = false;
        String[] duparent = new String[2];
        for (String FN : splitBlobs.keySet()) {
            boolean isCurModified = false;
            boolean isGvnModified = false;
            isCurModified = hasChanged(FN, splitBlobs, curTracked);
            isGvnModified = hasChanged(FN, splitBlobs, bBlob);
            isCurAdded = isAdded(splitBlobs, curTracked);
            isGvnAdded = isAdded(splitBlobs, bBlob);
            //First case
            if (!isCurModified && isGvnModified) {
                System.out.println("first checkoutbycommit");
                checkoutByCommit(getHead(branchN), FN);
                add(FN);
            }
            //Second case
            if (isCurModified && isGvnModified) {
                if (!hasChanged(FN, curTracked, bBlob)) {
                    if (!curTracked.containsKey(FN) && !bBlob.containsKey(FN)) {
                        Utils.restrictedDelete(FN);
                    }
                }
            }
            //Fourth case
            if (!isCurModified && !bBlob.containsKey(FN)) {
                Utils.restrictedDelete(FN);
                //rm(FN);
                //Do I need restrictDel?
            }
            //Merge Conflict case
            if (isCurModified && isGvnModified) {
                if (hasChanged(FN, curTracked, bBlob)) {
                    conflictHandle(splitBlobs, curTracked, bBlob, FN);
                    isConflict = true;
                    duparent = new String[]{getHead(Head), getHead(branchN)};
                }
            }
        }
        for (String fileN : bBlob.keySet()) {
            if (!splitBlobs.containsKey(fileN)) {
                isGvnAdded = true;
                addi.add(fileN);
            }
        }
        for (String fileN : curTracked.keySet()) {
            if (!splitBlobs.containsKey(fileN)) {
                isCurAdded = true;
            }
        }
        //Third case
        if (!isCurAdded && isGvnAdded) {
            //checkoutByBranch(branchN);
            System.out.println("checkoutbycommit");
            for (String fn : addi) {
                checkoutByCommit(getHead(branchN), fn);
                add(fn);
            }
        }
        if (isConflict) {
            System.out.println("commiting");
            commit("Merged " + branchN + " into " + Head + ".", duparent);
        }



    }

    //Other Functions
    public void conflictHandle(HashMap<String, String> spBlob,
                HashMap<String, String> cBlob, HashMap<String, String> gBlob,
                               String fileName) {

        String currContent;
        File currModiBlob = new File(gitlet_Directory + sep + "StageAdd" + sep + cBlob.get(fileName));
        if (cBlob.containsKey(fileName)) {
            currContent = Utils.readContentsAsString(currModiBlob);
        }
        else {
            currModiBlob = null;
            currContent = "";
        }

        String brnchContent;
        File gvnModiBlob = new File(gitlet_Directory + sep + "StageAdd" + sep + gBlob.get(fileName));
        if (gBlob.containsKey(fileName)) {
            brnchContent = Utils.readContentsAsString(gvnModiBlob);
        }
        else {
            gvnModiBlob = null;
            brnchContent = "";
        }
        String display = "<<<<<<< HEAD\n"+currContent+
                "\n=======\n"+brnchContent+"\n>>>>>>>";

        Utils.writeContents(new File(fileName), display);
        add(fileName);
        }

    public boolean isAdded(HashMap<String, String> split, HashMap<String, String> one) {
        boolean TF = false;
        for(String fileN: one.keySet()) {
            if (!split.containsKey(fileN)) {
                return true;
            }
        }
        return false;
    }
    public boolean hasChanged(String fileN, HashMap<String, String> split, HashMap<String, String> other) {
        if (split.containsKey(fileN) && other.containsKey(fileN)) {
            String oneHash = split.get(fileN);
            String otherHash = other.get(fileN);
            if(!oneHash.equals(otherHash))
                return true;
        }
       /*if (!other.containsKey(fileN) || !split.containsKey(fileN)) {
            return true;
        }*/
       return false;
    }
    public Commit id2Commit(String shaID) {
        File f = new File(gitlet_Directory + sep + "Commit" + sep + shaID);
        if (f.exists()) {
            return Utils.readObject(f, Commit.class);
        }
        else {
            Utils.message("No commit with that id exists.");
            throw new GitletException();
        }
    }

    private String abbrevID2Full(String ID) {
        if (ID.length() == Utils.UID_LENGTH) {
            return ID;
        }
        File allCommits = new File(gitlet_Directory + sep + "StageAdd");

        for (File eachCommit : allCommits.listFiles()) {
            if (eachCommit.getName().contains(ID)) {
                return eachCommit.getName();
            }
        }
        Utils.message("No commit with that id exists.");
        throw new GitletException();
    }

    public boolean isModified(HashMap<String, String> prevFiles, String incomingID,String fileName) {
        if (prevFiles == null) {
            return true;
        }
        if (prevFiles.containsKey(fileName)) {
            String ID = prevFiles.get(fileName);
            if (ID.equals(incomingID)) {
                return false;
            }
            else {
                return true;
            }
        }
        return true;
    }

    /*Check if there is any untracked file*/
    public void isUntracked(HashMap<String, String> curTracked) {
        File files= new File(System.getProperty("user.dir"));
        for (File file : files.listFiles()) {
            if (curTracked == null) {
                if (files.listFiles().length > 1) {
                    Utils.message("There is untracked file in the way; delete it or add it first.");
                    throw new GitletException();
                }
            } else {
                boolean b = !curTracked.containsKey(file.getName());
                boolean c = !toStage.containsKey(file.getName());
                if (b && !file.getName().equals(".gitlet") && c) {
                    Utils.message("err");

                    throw new GitletException();
                }
            }
        }
        /*int numUntracked = 0;
        if (curTracked == null) {
            for (File file : Objects.requireNonNull(files.listFiles())) {
                if (file.getName().endsWith("txt")) {
                    numUntracked += 1;
                }
            }
            if (numUntracked > 1) {
                Utils.message("There is an untracked file in the way; delete it or add it first.");
                return;
            }
        }

        else{
            for (File file: Objects.requireNonNull(files.listFiles())) {
                String n = file.getName();
                if (n.endsWith("txt") && !n.equals(".gitignore")) {
                    if (!curTracked.containsKey(file.getName()) && !toStage.containsKey(file.getName())) {
                            Utils.message("There is an untracked file in the way; "
                                    + "delete it or add it first.");
                            return;
                    }
                    if (curTracked.containsKey(file.getName())) {
                        String restoreBlob = gitlet_Directory + sep + "StageAdd" + sep + curTracked.get(file.getName());
                        File overwrite = new File(restoreBlob);
                        String content1 = Utils.readContentsAsString(overwrite);
                        String content2 = Utils.readContentsAsString(new File(file.getName()));
                        System.out.println(content1 + sep + content2);
                        System.out.println("should be overwritten error");
                        if (!content1.equals(content2)) {
                            Utils.message("There is an untracked file in the way; "
                                    + "delete it or add it first.");
                            return;
                        }
                    }


                }
            }
        }*/
    }




}
