package az.inci.bmsanbar.model;

import androidx.annotation.NonNull;

import java.util.Objects;

public class ExpCenter {
    private String expCenterCode;
    private String expCenterName;

    public String getExpCenterCode() {
        return expCenterCode;
    }

    public void setExpCenterCode(String expCenterCode) {
        this.expCenterCode = expCenterCode;
    }

    public String getExpCenterName() {
        return expCenterName;
    }

    public void setExpCenterName(String expCenterName) {
        this.expCenterName = expCenterName;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ExpCenter expCenter = (ExpCenter) o;
        return expCenterCode.equals(expCenter.expCenterCode);
    }

    @Override
    public int hashCode() {
        return Objects.hash(expCenterCode);
    }

    @Override
    @NonNull
    public String toString() {

        if (expCenterCode != null
                && (!expCenterCode.isEmpty()
                && !expCenterName.isEmpty()))
            return expCenterCode + " - " + expCenterName;
        else
            return "";
    }
}
