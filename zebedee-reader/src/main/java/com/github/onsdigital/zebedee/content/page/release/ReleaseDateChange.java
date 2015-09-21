package com.github.onsdigital.zebedee.content.page.release;

import java.util.Date;

/**
 * Created by bren on 11/08/15.
 */
public class ReleaseDateChange {
    private Date previousDate;
    private String changeNotice;

    public Date getPreviousDate() {
        return previousDate;
    }

    public void setPreviousDate(Date previousDate) {
        this.previousDate = previousDate;
    }

    public String getChangeNotice() {
        return changeNotice;
    }

    public void setChangeNotice(String changeNotice) {
        this.changeNotice = changeNotice;
    }
}
