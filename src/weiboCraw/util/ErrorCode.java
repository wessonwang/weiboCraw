/**
 * 
 */
package weiboCraw.util;

/**
 * @author wong3
 *
 */
public enum ErrorCode {
    UNSOLVE_ERROR(-1,"无法处理的错误"),
    MISSING_SOURCE(10006,"缺少source参数(appkey), 很可能是appKey失效"),
    IP_OUT_LIMIT(10022,"IP请求超过上限"),
    USER_OUT_LIMIT(10023,"用户请求超过上限"),
    POI_DOES_NOT_EXIST(23805,"目标POI不存在");
    
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
	return "错误码："+this.code+",错误详情："+this.desc;
    }
}
