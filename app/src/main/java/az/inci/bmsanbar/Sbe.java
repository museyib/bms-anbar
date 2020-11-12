package az.inci.bmsanbar;

import androidx.annotation.NonNull;

public class Sbe
{
    private String sbeCode;
    private String sbeName;

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
        return sbeCode + " - " + sbeName;
    }
}
