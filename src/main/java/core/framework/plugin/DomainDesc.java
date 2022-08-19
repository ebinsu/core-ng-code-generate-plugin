package core.framework.plugin;

/**
 * @author ebin
 */
public class DomainDesc {
    public String columnName;
    public String columnTypeDesc;
    public String columnNullableDesc;

    public DomainDesc(String columnName, String columnTypeDesc, String columnNullableDesc) {
        this.columnName = columnName;
        this.columnTypeDesc = columnTypeDesc;
        this.columnNullableDesc = columnNullableDesc;
    }
}
