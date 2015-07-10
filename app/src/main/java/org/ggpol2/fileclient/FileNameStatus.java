package org.ggpol2.fileclient;

/**
 * Created by End-User on 2015-06-29.
 */
public class FileNameStatus {

    private String strFilePathName;
    private Integer nFilePercent;
    private Long lProgress;
    private Boolean isStop;
    private Boolean isComplete;

    public String getStrFilePathName() {
        return strFilePathName;
    }

    public void setStrFilePathName(String strFilePathName) {
        this.strFilePathName = strFilePathName;
    }


    public Integer getnFilePercent() {
        return nFilePercent;
    }

    public void setnFilePercent(Integer nFilePercent) {
        this.nFilePercent = nFilePercent;
    }

    public Long getlProgress() {
        return lProgress;
    }

    public void setlProgress(Long lProgress) {
        this.lProgress = lProgress;
    }

    public Boolean getIsStop() {
        return isStop;
    }

    public void setIsStop(Boolean isStop) {
        this.isStop = isStop;
    }

    public Boolean getIsComplete() {
        return isComplete;
    }

    public void setIsComplete(Boolean isComplete) {
        this.isComplete = isComplete;
    }


}
