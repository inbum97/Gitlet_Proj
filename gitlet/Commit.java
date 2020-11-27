package gitlet;
import java.io.Serializable;
import java.util.HashMap;
import java.util.Date;
import java.text.SimpleDateFormat;

/* Commit class contains metadata of each commit
 *@author David Bang*/
public class Commit implements Serializable {
    private String _ID;
    private String _commit_msg;
    private String _commit_time;
    private String[] _parentCommit;
    private final String pattern = "EEE MMM d HH:mm:ss yyyy";
    private SimpleDateFormat myFormat = new SimpleDateFormat(pattern);
    private HashMap<String, String> blobs;

    /*Default Constructor Initial Commit*/
    public Commit() {
        _commit_msg = "initial commit";
        _commit_time = myFormat.format(new Date(0));
        _ID = Utils.sha1(_commit_msg);
        _parentCommit = null;
        blobs = null;
    }

    /*Commit Constructor*/
    public Commit(HashMap<String, String> blobb, String cmt_msg,
                  String[] prevCommit) {
        _commit_msg = cmt_msg;
        _commit_time = myFormat.format(new Date());
        this.blobs = blobb;
        _parentCommit = prevCommit;
        _ID = Utils.sha1(_commit_msg , _commit_time , _parentCommit[0]);
    }

    public void displayLog() {
        String logData = "===" + "\n" + "commit "
                + this._ID + "\n" + "Date: " + this._commit_time +
                " -0800"
                + "\n" + this._commit_msg
                + "\n";
        System.out.println(logData);
        if (_parentCommit != null && _parentCommit.length>1) {
            System.out.println(_commit_msg);
        }
    }


    /*getters for blob*/
    public HashMap<String, String> getBlobs() {
        return blobs;
    }
    /*Return ID*/
    public String get_ID() {
        return _ID;
    }
    /*return commit msg*/
    public String getMsg() {
        return _commit_msg;
    }
    /*return time*/
    public String getTime() {
        return _commit_time;
    }
    /*return parentID*/
    public String getParent() {
        if (_parentCommit != null) {
            return _parentCommit[0];
        }
        else {
            return null;
        }
    }



}

