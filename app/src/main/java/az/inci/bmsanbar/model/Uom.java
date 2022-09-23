package az.inci.bmsanbar.model;

import androidx.annotation.NonNull;

import java.util.Objects;

public class Uom
{
    private String uomCode;
    private String uomName;

    public Uom(String uomCode)
    {
        this.uomCode = uomCode;
    }

    public String getUomCode()
    {
        return uomCode;
    }

    public void setUomCode(String uomCode)
    {
        this.uomCode = uomCode;
    }

    public String getUomName()
    {
        return uomName;
    }

    public void setUomName(String uomName)
    {
        this.uomName = uomName;
    }

    @NonNull
    @Override
    public String toString() {
        return  uomCode;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Uom uom = (Uom) o;
        return Objects.equals(uomCode, uom.uomCode);
    }

    @Override
    public int hashCode() {
        return Objects.hash(uomCode);
    }
}
