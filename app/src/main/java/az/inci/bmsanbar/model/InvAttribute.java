package az.inci.bmsanbar.model;

public class InvAttribute
{
    private String invCode;
    private String attributeId;
    private String attributeType;
    private String attributeName;
    private String attributeValue;
    private String whsCode;
    private boolean defined;

    public String getInvCode()
    {
        return invCode;
    }

    public void setInvCode(String invCode)
    {
        this.invCode = invCode;
    }

    public String getAttributeType()
    {
        return attributeType;
    }

    public void setAttributeType(String attributeType)
    {
        this.attributeType = attributeType;
    }

    public String getAttributeName()
    {
        return attributeName;
    }

    public void setAttributeName(String attributeName)
    {
        this.attributeName = attributeName;
    }

    public String getAttributeValue()
    {
        return attributeValue;
    }

    public void setAttributeValue(String attributeValue)
    {
        this.attributeValue = attributeValue;
    }

    public boolean isDefined()
    {
        return defined;
    }

    public void setDefined(boolean defined)
    {
        this.defined = defined;
    }

    public String getAttributeId()
    {
        return attributeId;
    }

    public void setAttributeId(String attributeId)
    {
        this.attributeId = attributeId;
    }

    public String getWhsCode() {
        return whsCode;
    }

    public void setWhsCode(String whsCode) {
        this.whsCode = whsCode;
    }
}
