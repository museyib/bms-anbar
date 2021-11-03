package az.inci.bmsanbar.model;

import androidx.annotation.NonNull;

import java.util.Objects;

public class Whs
{
    private String whsCode;
    private String whsName;

    public Whs()
    {

    }
    public Whs(String whsCode)
    {
        this.whsCode = whsCode;
    }

    public String getWhsCode()
    {
        return whsCode;
    }

    public void setWhsCode(String whsCode)
    {
        this.whsCode = whsCode;
    }

    public String getWhsName()
    {
        return whsName;
    }

    public void setWhsName(String whsName)
    {
        this.whsName = whsName;
    }

    @Override
    @NonNull
    public String toString()
    {
        if (whsCode != null && (!whsCode.isEmpty() && !whsName.isEmpty()))
            return whsCode + " - " + whsName;
        else
            return "";
    }

    @Override
    public boolean equals(Object o)
    {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Whs whs = (Whs) o;
        return Objects.equals(whsCode, whs.whsCode);
    }

    @Override
    public int hashCode()
    {
        return Objects.hash(whsCode);
    }
}
