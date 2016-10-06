/**
 * 
 */
package weiboCraw.util;

/**
 * @author wong3
 *
 */
public enum ErrorCode {
    UNSOLVE_ERROR(-1,"�޷�����Ĵ���"),
    MISSING_SOURCE(10006,"ȱ��source����(appkey), �ܿ�����appKeyʧЧ"),
    IP_OUT_LIMIT(10022,"IP���󳬹�����"),
    USER_OUT_LIMIT(10023,"�û����󳬹�����"),
    POI_DOES_NOT_EXIST(23805,"Ŀ��POI������");
    
    int code;
    String desc;

    ErrorCode(int code, String desc){
	this.code = code;
	this.desc = desc;
    }

    public int getCode() {
        return code;
    }

    public void setCode(int code) {
        this.code = code;
    }

    public String getDesc() {
        return desc;
    }

    public void setDesc(String desc) {
        this.desc = desc;
    }
    
    public static String getDescByCode(int code){
	for(ErrorCode c : ErrorCode.values()){
	    if(c.getCode() == code)
		return c.getDesc();
	}
	
	return null;
    }
    
    public static ErrorCode getErrorCodeByCode(int code){
	for(ErrorCode c : ErrorCode.values()){
	    if(c.getCode() == code)
		return c;
	}
	
	return UNSOLVE_ERROR;
    }
    
    @Override
    public String toString(){
	return "�����룺"+this.code+",�������飺"+this.desc;
    }
}
