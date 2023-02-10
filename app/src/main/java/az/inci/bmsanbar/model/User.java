package az.inci.bmsanbar.model;

public class User
{

    private String id;
    private String password;
    private String name;
    private String whsCode;
    private String pickGroup;
    private boolean collectFlag;
    private boolean pickFlag;
    private boolean checkFlag;
    private boolean countFlag;
    private boolean attributeFlag;
    private boolean locationFlag;
    private boolean packFlag;
    private boolean docFlag;
    private boolean loadingFlag;
    private boolean approveFlag;
    private boolean approvePrdFlag;

    public String getId()
    {
        return id;
    }

    public void setId(String id)
    {
        this.id = id;
    }

    public String getPassword()
    {
        return password;
    }

    public void setPassword(String password)
    {
        this.password = password;
    }

    public String getWhsCode()
    {
        return whsCode;
    }

    public void setWhsCode(String whsCode)
    {
        this.whsCode = whsCode;
    }

    public boolean isCollect()
    {
        return collectFlag;
    }

    public void setCollectFlag(boolean collectFlag)
    {
        this.collectFlag = collectFlag;
    }

    public boolean isPick()
    {
        return pickFlag;
    }

    public void setPickFlag(boolean pickFlag)
    {
        this.pickFlag = pickFlag;
    }

    public boolean isCheck()
    {
        return checkFlag;
    }

    public void setCheckFlag(boolean checkFlag)
    {
        this.checkFlag = checkFlag;
    }

    public boolean isCount()
    {
        return countFlag;
    }

    public void setCountFlag(boolean countFlag)
    {
        this.countFlag = countFlag;
    }

    public boolean isLocation()
    {
        return locationFlag;
    }

    public void setLocationFlag(boolean locationFlag)
    {
        this.locationFlag = locationFlag;
    }

    public boolean isPack()
    {
        return packFlag;
    }

    public void setPackFlag(boolean packFlag)
    {
        this.packFlag = packFlag;
    }

    public boolean isDoc()
    {
        return docFlag;
    }

    public void setDocFlag(boolean docFlag)
    {
        this.docFlag = docFlag;
    }

    public String getPickGroup()
    {
        return pickGroup;
    }

    public void setPickGroup(String pickGroup)
    {
        this.pickGroup = pickGroup;
    }

    public String getName()
    {
        return name;
    }

    public void setName(String name)
    {
        this.name = name;
    }

    public boolean isLoading()
    {
        return loadingFlag;
    }

    public void setLoadingFlag(boolean loadingFlag)
    {
        this.loadingFlag = loadingFlag;
    }

    public boolean isApprove()
    {
        return approveFlag;
    }

    public void setApproveFlag(boolean approveFlag)
    {
        this.approveFlag = approveFlag;
    }

    @Override
    public String toString()
    {
        return "User{" +
               "id='" + id + '\'' +
               ", password='" + password + '\'' +
               ", name='" + name + '\'' +
               ", whsCode='" + whsCode + '\'' +
               ", pickGroup='" + pickGroup + '\'' +
               ", collectFlag=" + collectFlag +
               ", pickFlag=" + pickFlag +
               ", checkFlag=" + checkFlag +
               ", countFlag=" + countFlag +
               ", attributeFlag=" + attributeFlag +
               ", locationFlag=" + locationFlag +
               ", packFlag=" + packFlag +
               ", docFlag=" + docFlag +
               ", loadingFlag=" + loadingFlag +
               ", approveFlag=" + approveFlag +
               ", approvePrdFlag=" + approvePrdFlag +
               '}';
    }

    public boolean isApprovePrd()
    {
        return approvePrdFlag;
    }

    public void setApprovePrdFlag(boolean approvePrdFlag)
    {
        this.approvePrdFlag = approvePrdFlag;
    }

    public boolean isAttribute()
    {
        return attributeFlag;
    }

    public void setAttributeFlag(boolean attributeFlag)
    {
        this.attributeFlag = attributeFlag;
    }
}
