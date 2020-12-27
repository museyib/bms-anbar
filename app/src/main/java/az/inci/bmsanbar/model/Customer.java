package az.inci.bmsanbar.model;

import androidx.annotation.NonNull;

public class Customer
{
    private String bpCode;
    private String bpName;
    private String sbeCode;
    private String sbeName;

    public String getBpCode()
    {
        return bpCode;
    }

    public void setBpCode(String bpCode)
    {
        this.bpCode = bpCode;
    }

    public String getBpName()
    {
        return bpName;
    }

    public void setBpName(String bpName)
    {
        this.bpName = bpName;
    }

    public String getSbeCode()
    {
        return sbeCode;
    }

    public void setSbeCode(String sbeCode)
    {
        this.sbeCode = sbeCode;
    }

    public String getSbeName()
    {
        return sbeName;
    }

    public void setSbeName(String sbeName)
    {
        this.sbeName = sbeName;
    }

    @Override
    @NonNull
    public String toString()
    {
        if ((bpCode != null && bpName != null) && (!bpCode.isEmpty() && !bpName.isEmpty()))
            return bpCode + " - " + bpName;
        else
            return "";
    }
}
