package xlsrv.com.action;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.Serializable;
import java.io.UnsupportedEncodingException;
import java.io.Writer;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.security.cert.X509Certificate;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.sql.Types;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.Date;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Timer;
import java.util.TimerTask;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSession;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import javax.servlet.ServletContext;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpression;
import javax.xml.xpath.XPathFactory;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.filefilter.DirectoryFileFilter;
import org.apache.commons.io.filefilter.FileFilterUtils;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONException;
import com.alibaba.fastjson.JSONObject;
import com.alibaba.fastjson.JSONPath;

import xlsrv.com.action.DataTransformUtil;

import java.util.concurrent.Callable;
import java.util.concurrent.TimeUnit;

import java.net.*;

public class SxService implements Runnable{ //ly schedule
	
	private PreparedStatement[] pStemt = null;
	private int ParseDeep;
	private Boolean is_debug=false; 
	private int for_debug_count=0;
	private String clearStr=null;
	private Map<String, Object> paramMapAdd=null;
	private Map<String, Object> paramMap=null;
	public String errStr="";
	public String webPath=null;
	public HttpServletRequest request=null; //ly session
	public HttpServletResponse response=null;
	private Node parseNode=null;
	
	private Boolean isRunning=false;
	private Boolean isBreak=false;
	private String CallError="";
	private static String SvcFile="Service.xml";
	private static Node dbNode=null;	
	private static Map<String,Connection> dbConns=null;
	private static Map<String,Integer> ScheduleCycles=null;
	private static Map<String,Integer> ScheduleTimes=null;
	private Integer HxRunTimes=0;
	private Integer ScheduleType=0;
	private String ScheduleNode=null;
	private ScheduleManager ScheduleObj=null;
//	public static final int SCHEDULE_TYPE_LONG=8;
//	public static final int SCHEDULE_TYPE_NORMAL=9;
//	public static final int SCHEDULE_TYPE_DAILY=10;
//    public static final int SCHEDULE_TYPE_WEEKLY=11;
//    public static final int SCHEDULE_TYPE_MONTHLY=12;
    private Document ConfigFile=null;
    private String ConfigFileName="";
    private XPath Xpath;
	
	public SxService() {
		super();
		this.ScheduleNode=null;
		this.Init();
	}
	
	public SxService(Integer taskType,ScheduleManager schedule_obj) { //ly schedule
		super();
		//设定DWM对应节点
		this.ScheduleType=taskType;
		this.ScheduleObj=schedule_obj;
		this.ScheduleNode=ScheduleManager.transScheduleNode(this.ScheduleType);

		//初始化
		this.Init();
	}
	
	public void Init() {
		//取得参数
		
		this.errStr="";
		this.parseNode=null;
		
		if (ScheduleCycles==null);
			ScheduleCycles=new HashMap<String,Integer>();
		//取得基础数据库配置,启动链接
		if (dbConns==null) {
			Node base_node=getWebServiceCfg(SvcFile,"_DateBase_");
			if (base_node==null) {
				errStr="[\"error\","+"\"Action Cfg: (/_DateBase_/*)  not defined!\""+"]";
				return;
			} 
			//打开数据库集this->dbConns;
			this.CloseAllDBConn();
			dbNode=base_node;
			String db_r_str=GetDBConn();
			if (db_r_str.length()!=0) {
				System.out.println("DBConnect Error:"+db_r_str);
				errStr="[\"error\","+"\"Action Cfg: db connecting error!\""+"]";
				return;
			}
		}
			
		//设置全局参数
		this.ParseDeep=0;
		return;
	}
	
	public void RunPubParams() {
		this.paramMap.put("@_@RunNowTime", GetNowTime());
		this.paramMap.put("@_@RunNowDate", GetNowTime("Date"));
		this.paramMap.put("@_@RunNowString", GetNowTime("String"));
	}
	
	public String WebRun(Map<String, Object> add_paras) {
		//取得配置信息
		this.paramMap = new HashMap<String, Object>();
		this.paramMap.putAll(add_paras); //深拷贝初始化
		String act=paramMap.containsKey("@@_act") ? ""+paramMap.get("@@_act") : null;
		/*@SuppressWarnings("unchecked")
		Map<String, Object> url_map=paramMap.containsKey("@_@URL") ? (Map<String, Object>)(paramMap.get("@_@URL")) : null;
		String act=url_map.containsKey("act") ? ""+url_map.get("act") : null;*/
		
		this.parseNode=getWebServiceCfg("Service.xml",act);
		if (parseNode==null) {
			errStr= "[\"error\","+"\"Action Cfg: act("+act+")not found!\""+"]";
			return errStr;
		} 
		paramMap.put("@_@THIS_CFG",(Object)this.ConfigFile); //ly xpath
		RunPubParams();
		
		//运行WebService宏代码
		this.ParseDeep=0;
		System.out.println(">>> WebService("+act+")");
		String result="";
		try {
			result=ParseAction(this.parseNode,paramMap);
		}catch(Exception e) {
			System.out.println(e);
		}
		if (result.length()!=0){
			result = "[\"error\","+"\""+result+"\"]";
			return result;
		}else {
			if (paramMap.containsKey("@@_ParseResult"))
				result=""+paramMap.get("@@_ParseResult");
			else if (paramMap.containsKey("@@_HttpResult")) {
				result=""+paramMap.get("@@_HttpResult");
				String type= paramMap.containsKey("@@_HttpType") ? ""+paramMap.get("@@_HttpType") : ""; //ly_new2
				add_paras.put("@_@HTTP_Type", type);
				return result;
			}
			else
				result="";
		}
		//返回成功信息
		return "[\"success\",\""+result+"\"]";
	}
	

	public Object __RunWeb(Map<String,String> paras, Map<String,Object> paras_map) {
		Map<String, Object> param_map = new HashMap<String, Object>();
		Map<String,Object> url_map=new HashMap<String, Object>();
		//循环url参数
		String act="";
		for(String key : paras.keySet()){
			String value=paras.containsKey(key) ? ""+paras.get(key) : "";
			url_map.put(key, value);
			if (key=="act")
				act=value;
		}
		//
		param_map.put("@@_act",act);
		param_map.put("@_@URL", url_map);
		//增补普通参数
		
		//运行WebService宏代码
		SxService hx=new SxService();
		String result="";
		try {
			result=hx.WebRun(param_map);
			//System.out.println(result);
		}catch(Exception e) {
			System.out.println(e);
		}
		
		return result;
	}
	
	public void GetAddParams(Map<String, Object> paras_map,String filter_str) {
		//查找节点
		Node base_node=getWebServiceCfg(SvcFile,"_GlobalParams_");
		if (base_node==null) 
			return;
		
		//过滤设置
		String filter="";
		if (filter_str!=null && !filter_str.isEmpty()) 
			filter=(String)";"+filter_str;
		
		//查找AddParam节点
		NodeList nodes = base_node.getChildNodes();
		if(nodes!=null){
			for (int i = 0; i < nodes.getLength(); i++){
				Node node = nodes.item(i);
				if(node.getNodeType()==Node.ELEMENT_NODE) { 
					//换算取得value
					String value=Xml_getNodeValue(node);
					String key=node.getNodeName();
					if (!filter.isEmpty() && filter.indexOf((String)";"+key)< 0) {
						continue;//过滤
					}
					//插入属性
					paras_map.put((String)"@_@"+key,value);
				}
			}
		}
	}
	
	public void run() {	
		//执行互斥
		System.out.println("Task Start at:"+GetNowTime()+"=>"+this.ScheduleNode);
		if (isRunning)
			return;
		//jishu
		HxRunTimes++;
		//取得基础数据库配置,启动链接
		Node schedule_node=getWebServiceCfg("Service.xml",this.ScheduleNode);
		if (schedule_node==null) {
			return;
		}
		String apps_str=Xml_getNodeValue(schedule_node);
		if (apps_str==null || apps_str.length()==0) 
			return;
		String[] apps=apps_str.split(";");
		
		//循环每个任务
		this.isRunning=true;
		for(int i=0; i<apps.length; i++) {		
			//取得配置信息
			String act=apps[i];
			this.parseNode=getWebServiceCfg("Service.xml",act);
			if (parseNode==null) {
				errStr= "[\"error\","+"\"Action Cfg: act("+act+")not found!\""+"]";
				System.out.println("任务差错： "+errStr);
				break;
			} 
			
			//判断是否已到循环周期
			if (ScheduleType==ScheduleManager.SCHEDULE_TYPE_LONG || ScheduleType==ScheduleManager.SCHEDULE_TYPE_NORMAL  ) {//针对毫秒的精确周期控制 ly schedule
				if (!ScheduleCycles.containsKey(apps[i])) {//插入跟踪队列
					Map<String,String> parent_attrs=getParseNodeAttr(this.parseNode);
					String time_node=apps[i];
					String time_cycle=""+parent_attrs.get("TimeCycle");
					if(time_cycle!=null && isNumeric(time_cycle)) {
						ScheduleCycles.put(apps[i], Integer.parseInt(time_cycle));
					}else {
						ScheduleCycles.put(apps[i], 0);
					}
				}
				Integer num=ScheduleCycles.get(apps[i]);
				if (num!=null && num!=0) {//累计减处理
					Integer tmp=HxRunTimes % num;
					if (tmp!=0)
						continue; //通过余数循环，并且HxRunTimes从1为起点
				}else if (num==0 && HxRunTimes!=1) {
					continue;//只在启动色时候启动一次，如果TimeCycle设置为0
				}
				System.out.println("###"+apps[i]+"("+(num==0 ? 0 : HxRunTimes/num)+"):");
			}else {//Day\Week\Month\Hour周期控制。
				System.out.println("###"+apps[i]+"("+HxRunTimes+"):");
			}
			
			//运行parseNode
			this.ParseDeep=0;
			this.paramMap = new HashMap<String, Object>();
			RunPubParams();
			String result=ParseAction(this.parseNode,paramMap);
			if (result.length()!=0){
				result = "[\"error\",\""+result+"\"]";
			}else {
				Node parse_node=getWebServiceCfg("Service.xml",apps[i]);
				if (paramMap.containsKey("@@_ParseResult")) {
					result=""+paramMap.get("@@_ParseResult");
					result="[\"success\",\""+result+"\"]";
				}else
					result="";
			}
			//返回
			PRINT(result);
			if (ScheduleType!=ScheduleManager.SCHEDULE_TYPE_LONG && ScheduleType!=ScheduleManager.SCHEDULE_TYPE_NORMAL && this.ScheduleObj!=null ) 
				this.ScheduleObj.ReloadDWHTask(this.ScheduleType,this); //运行后再次启动。//Day;Week:Month周期控制。ly schedule
		}
		
		this.isRunning=false;
		return;
	}
	public String GetNowTime() {
		return GetNowTime("");
	}
	public String GetNowTime(String param) {
		if (param.length()==0) {
			SimpleDateFormat sim = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
			return sim.format(new Date());
		}else if (param.indexOf("Date")>=0){
			SimpleDateFormat sim = new SimpleDateFormat("yyyy-MM-dd");
			return sim.format(new Date());
		}else if (param.indexOf("String")>=0){
			SimpleDateFormat sim = new SimpleDateFormat("yyyyMMdd_HHmmss");
			return sim.format(new Date());
		}else {
			System.out.println((String)"Error: GetNowTime(param), param"+param+" is not defined!");
			return "";
		}    
	}
	
	public String ParseAction(Node parseNode,Map<String, Object> paras_map){
		
		
		String err_str="";
		//测试是否跟踪
		Map<String,String> parent_attrs=getParseNodeAttr(parseNode);
		String debug_string=parent_attrs.get("Debug");
		this.is_debug=false;
		if (debug_string!=null) {
			if (debug_string.indexOf("Yes")>=0) 
				is_debug=true;
			else if (debug_string.indexOf(".")>=0) 
				System.out.println((String)">>>"+(parseNode.getNodeName())+":");
		}
		
		this.ParseDeep++; //用于记录解析层次深度.tool = tool.next_sibling()	
		//测试是否情况"/n;/t"
		String[] clear;
		this.clearStr=parent_attrs.get("Clear");
		if (this.clearStr!=null)
			clear=this.clearStr.split(";");
		else
			clear=null;
		
		//判断是否有临时增补属性
		if (parent_attrs.containsKey("AddParams")) {
			this.GetAddParams(paras_map, parent_attrs.get("AddParams"));
		}
		
		NodeList nodes = parseNode.getChildNodes();
		if(nodes!=null){
			for (int i = 0; i < nodes.getLength(); i++){
				Node node = nodes.item(i);
				if(node.getNodeType()==Node.ELEMENT_NODE) { 
					//换算取得value
					String value=Xml_getNodeValue(node);
					if (value!=null && value.length()!=0)
						value=ParseValue(value,paras_map,null,clear);
					//取得属性
					Map<String,String> attrs=getParseNodeAttr(node);
					//测试是否执行
					if (DoTestValue(attrs,paras_map,null)) {
						//是否跟踪
						if (is_debug)
							this.DebugY(node,value,attrs);
						//解析内容
						switch (node.getNodeName()) {
						case "SET":
							err_str=SET(node,value,attrs,paras_map);
							break;
						case "DO_SQL":
							err_str=DO_SQL(node,value,attrs,paras_map);
							break;
						case "CALL":
							err_str=CALL(node,attrs,paras_map,null,clear);
							break;
						case "FOR":
							err_str=FOR(node,attrs,paras_map);
							break;
						case "XSLT":
							err_str=XSLT(node,value,attrs,paras_map);
							break;
						case "PRINT":
							err_str=PRINT(value,attrs,paras_map);
							break;
						case "TRACK":
							err_str=TRACK(value,attrs,paras_map);
							break;
						case "JPATH":
							err_str=JPATH(node,value,attrs,paras_map,null,clear);
							break;
						case "XPATH":
							err_str=XPATH(node,value,attrs,paras_map,null,clear);
							break;
						case "ACTIONS": //ly session
							err_str=ParseAction(node,paras_map);
							break;
						case "RETURN":
							err_str = RETURN(node,value,attrs,paras_map);
							return "";//返回
						default:
							err_str = (String)"ParseError: node ("+node.getNodeName()+") not defined!";
						}
						//跟踪结果
						if (is_debug && attrs.containsKey("Debug") && (attrs.get("Debug").equals(".")||attrs.get("Debug").equals("..."))) {
							DebugV(attrs,paras_map,"To");
						}
					}
				}
				if (err_str.length()!=0) {
					System.out.println("Error: "+err_str);
					break;
				}
			}
		}
		
		this.ParseDeep--; //用于记录解析层次深度.tool = tool.next_sibling()
		return err_str;
	}
	
	public Map<String,String> getParseNodeAttr(Node parseNode){
		Map<String,String> map=new HashMap<String,String>();
		NamedNodeMap attrs = parseNode.getAttributes();
		if (attrs==null)
			return null;
		
		for (int j = 0; j < attrs.getLength(); j++){
            Node attr = attrs.item(j);
            map.put(attr.getNodeName(), Xml_getNodeValue(attr));
        }
		return map;
	}
	
	public Boolean DoTestValue_Old(Map<String,String> attrs, Map<String, Object> paras_map, Map<String, Object>row_paras) {
		//取得value值
		String value=null;
		if (attrs==null)
			return true;
		String test=attrs.get("Test");
		//test也需要做解析
		if (test==null)
			return true;
		value=ParseValue(test,paras_map,row_paras,null);
		
		//如果为定义，则直接执行
		if (value==null)
			return true;
		//如果定义则判断。
		if (value.indexOf("==")==-1 && value.indexOf("!=")==-1) {
			//为空则错
			if (value.indexOf("!")==0){
				if (value.length()==1)
					return true;
				else
					return false;
			}else {
				if (value.length()==0)
					return false;
				else
					return true;
			}
		}else{
			//等于、不等于判断
			if (value.indexOf("==")>=0) {
				String[] param=value.split("==");
				//普通字串判断
				if ((value.equals("==")) || (param.length>1 && param[0].equals(param[1])))
					return true;
				else
					return false;
				//map判断
			}else if (value.indexOf("!=")>=0) {
				String[] param=value.split("!=");
				if (!((value.equals("!=")) || (param.length>1 && param[0].equals(param[1]))))
					return true;
				else
					return false;
			}
		}
		return true;
	}

	public Boolean DoTestValue(Map<String,String> attrs, Map<String, Object> paras_map, Map<String, Object>row_paras) {
		//取得value值
		String value=null;
		if (attrs==null)
			return true;
		String test=attrs.get("Test");
		//test也需要做解析
		if (test==null)
			return true;
		value=ParseValue(test,paras_map,row_paras,null);
		
		//如果为定义，则直接执行
		if (value==null)
			return true;
		
		//TestCondition
		String[] and_arr;
		String[] or_arr= value.split("\\|\\|");
        Boolean r_or = false;
        
        for(int i=0; i<or_arr.length; i++){
            and_arr=or_arr[i].split("&&");
            Boolean r_and = true;
            
            for(int j=0; j<and_arr.length; j++){
                r_and = TestConditionStr(and_arr[j],paras_map,row_paras);
                if (r_and == false)
                    break;
            }
            if(r_and==true){
                r_or=true;
                break;
            }
        }
        //返回赋值
        return r_or;
	}
	
	public Boolean TestConditionStr(String value_str, Map<String, Object> paras_map, Map<String, Object>row_paras) {
		String value=value_str.trim();
		//如果为定义，则直接执行
		if (value==null)
			return true;
		//如果定义则判断。
		if (value.indexOf("==")==-1 && value.indexOf("!=")==-1) {
			//为空则错
			if (value.indexOf("!")==0){
				if (value.length()==1)
					return true;
				else
					return false;
			}else {
				if (value.length()==0)
					return false;
				else
					return true;
			}
		}else{
			//等于、不等于判断
			if (value.indexOf("==")>=0) {
				String[] param=value.split("==");
				//普通字串判断
				if ((value.equals("==")) || (param.length>1 && param[0].equals(param[1])))
					return true;
				else
					return false;
				//map判断
			}else if (value.indexOf("!=")>=0) {
				String[] param=value.split("!=");
				if (!((value.equals("!=")) || (param.length>1 && param[0].equals(param[1]))))
					return true;
				else
					return false;
			}
		}
		return true;
	}

	public void DebugY(Node node,String value,Map<String,String> attrs) {
		String debug_str="";
		Boolean debug_output=false;
		//递进
		String deep_str="";
		for (int i = 0; i<this.ParseDeep; i++) {
	        deep_str += "  ";
	    }
		//取得节点和属性
		if(node.getNodeType()==Node.ELEMENT_NODE) {
			debug_str+="<"+ node.getNodeName();
			if (attrs!=null)
				for (Map.Entry<String, String> entry : attrs.entrySet()) { 
					debug_str+=" "+entry.getKey() + "=\"" + entry.getValue()+"\""; 
					if (entry.getKey().equals("Debug")) {
						if (entry.getValue().indexOf("..")>=0)
							debug_output=true;	
					}
				}	
			debug_str+=">";
		}
		System.out.println(deep_str+debug_str);
		if (debug_output)
			System.out.println(deep_str+" "+value);
		return;
	}
	public void DebugV(Map<String,String> attrs,Map<String, Object> paras_map,String to) {
		//递进
		String deep_str="";
		for (int i = 0; i<this.ParseDeep; i++) {
	        deep_str += "  ";
	    }
		//取得节点和属性
		if (attrs.containsKey(to)) {
			String para=attrs.get(to);
			System.out.println(deep_str+" "+paras_map.get(para));
		}
		return;
	}
	
	//RETURN 动作
	public String RETURN (Node parseNode, String value, Map<String,String> attrs, Map<String, Object> paras_map){ 
		String type= attrs.containsKey("Type") ? attrs.get("Type") : "";
		//ly session 注：今后外移
		String session_str=attrs.containsKey("Session") ? attrs.get("Session") : "";
		if (session_str.length()!=0) {
			String[] session=session_str.split(":");
			String session_time=session.length>1 ? session[1] : "1800";
			Integer t;
			try {
			    t = Integer.parseInt(session_time);
			} catch (NumberFormatException e) {
			    t = 600;
			}
			Cookie cookie = new Cookie("JSESSIONID", session[0]);
			cookie.setMaxAge(t);// 设置有效期，时间为秒
			cookie.setPath("/YICTWEB");// 设置path
			this.response.addCookie(cookie);// 加入到response对象中
		}
		//
		if (type.equals("html") || type.equals("xml") || type.equals("json")) {
			paras_map.put("@@_HttpResult",value);
			paras_map.put("@@_HttpType",type);
		}else {
			paras_map.put("@@_ParseResult",value);
		}
		return "";
	}
	
	//RETURN 动作
	public String PRINT (String value){ 
		//递进
		String deep_str="";
		for (int i = 0; i<this.ParseDeep; i++) {
	        deep_str += "    ";
	    }
		System.out.println(deep_str+value);
		return "";
	}
	
	public String TRACK (String value, Map<String,String> attrs, Map<String, Object> paras_map){ 
		//递进
		String deep_str="";
		for (int i = 0; i<this.ParseDeep; i++) {
	        deep_str += "    ";
	    }
		System.out.println(deep_str+value);
		return "";
	}
	
	public String PRINT (String value, Map<String,String> attrs, Map<String, Object> paras_map){ 
		String to= attrs.containsKey("To") ? attrs.get("To") : "";
		String type= attrs.containsKey("Type") ? attrs.get("Type") : "";
		if (to.length()!=0) {
			try {
				String path=WebService.class.getClassLoader().getResource("").getPath();
				System.out.println(path);
				File file = new File(path+"/"+to);
				// 下面这也会抛出异常，这次我们为了代码结构清晰起见，直接throw给main吧
				if (type=="append") {
					Writer writer = new FileWriter(file,true);
					writer.write(value);
					writer.close();// 在这一定要记得关闭流
				}else {
					Writer writer = new FileWriter(file);
					writer.write(value);
					writer.close();// 在这一定要记得关闭流
				}		
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
				System.out.println(e);
				return "";
			}
		}else {
			//递进
			String deep_str="";
			for (int i = 0; i<this.ParseDeep; i++) {
		        deep_str += "    ";
		    }
			System.out.println(deep_str+value);
		}
		
		return "";
	}
	
	public String __GetExec (String cmd_str){ 
		String path=WebService.class.getClassLoader().getResource("").getPath();
		Exec obj = new Exec();
		String output = obj.executeCommand(cmd_str, new File(path));
		System.out.println(output);
		return output;
	}

	public String __FileRead (String file_name){ 
		String r="";
		try {
			String path=WebService.class.getClassLoader().getResource("").getPath();
			File file = new File(path+"/"+file_name);
			// 下面这也会抛出异常，这次我们为了代码结构清晰起见，直接throw给main吧
			FileReader reader = new FileReader(file);
			BufferedReader br = new BufferedReader(reader);
			String line = null;
			while ((line = br.readLine()) != null) {
				r+=line;
			}
			br.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			System.out.println(e);
			return r;
		}
		return r;
	}
	
	public String __CalculateDouble (String expression){ 
		double result = Calculator.conversion(expression);
		return ""+result;
	}
	public String __CalculateInt (String expression){ 
		double result = Calculator.conversion(expression);
		return ""+(int)result;
	}
	
	public String CALL (Node parseNode, Map<String,String> attrs, Map<String, Object> paras_map, Map<String, Object> row_map,String[] clear){ 
		//取得属性
		if (attrs==null)
			return "<CALL> Method=\"*\" should be set to your call function";
		String to= attrs.containsKey("To") ? attrs.get("To") : "";
		String obj_str= attrs.containsKey("Method") ? attrs.get("Method") : "";
		String debug=attrs.containsKey("Debug") ? attrs.get("Debug") : "";
		String params=attrs.containsKey("Params") ? attrs.get("Debug") : "";
//		if (to.length()==0)
//			return "<CALL> To=\"*\" should be set!";
		if (obj_str.length()==0)
			return "<CALL> Method=\"*\" should be set your call function!";
		
		//解析对象.方法
		String[] obj_method=obj_str.split("\\.");
		String obj=obj_method[0];
		String method=obj_method.length >1 ? obj_method[1] : "";
		Map<String, String> func_params = new HashMap<String, String>();
		if (method.length()!=0) 
			func_params.put("__method",method);
		if (params.length()!=0) 
			func_params.put("__params",params);
				
		//取得参数
		NodeList nodes = parseNode.getChildNodes();
		if(nodes!=null){
			for (int i = 0; i < nodes.getLength(); i++){
				Node node = nodes.item(i);
				if(node.getNodeType()==Node.ELEMENT_NODE) {
					String param_value=Xml_getNodeValue(node);
					param_value=param_value==null ? "" : param_value;//子节点为空处理
					param_value=this.ParseValue(param_value, paras_map, row_map, clear);
					DebugY(node,node.getNodeName()+":"+param_value,attrs);
					func_params.put(node.getNodeName(),param_value);
				}
			}
		}else {
			//func_params.put("__value",param_value);
		}
		//操作函数
		String err="";
		Object r_s=null;
		this.CallError="";
		try {
    			Method return_func=this.getClass().getMethod("__"+obj, Map.class, Map.class);
	    		r_s=return_func.invoke(this, func_params, paras_map);
		} catch (IllegalAccessException e) {
			e.printStackTrace();
			err=e.toString();
		} catch (IllegalArgumentException e) {
			e.printStackTrace();
			err=e.toString();
		} catch (InvocationTargetException e) {
			e.printStackTrace();
			err=e.toString();
		} catch (NoSuchMethodException e) {
			e.printStackTrace();
			err=e.toString();
		} catch (SecurityException e) {
			e.printStackTrace();
			err=e.toString();
		}
		if (this.CallError.length()!=0)
			return this.CallError;
		if (err==null || err.length()==0) {
			if (to.length()!=0)
				paras_map.put(to,r_s);
		}else {
			return err;
		}
		
		return "";
	}
	
	public Object __WxUtil(Map<String,String> paras, Map<String,Object> paras_map) {
		String method= paras.containsKey("__method") ? paras.get("__method") : "";
		if (method.equals("getTokenFromWx")==true) {
			//调用函数
			if (!paras.containsKey("Url"))
				return null;
			String url=paras.containsKey("Url")? paras.get("Url"):null;
			return WxUtil.getTokenFromWx(url);
		}else if (method.equals("createPostMsg")==true) {
			if (!paras.containsKey("Url") || !paras.containsKey("Content"))
				return null;
			//调用函数
			JSONObject tmp=WxUtil.createPostMsg(paras.get("Url"),paras.get("Content"));
			return tmp;
		}else if (method.equals("createGetMsg")==true) {
			if (!paras.containsKey("Url"))
				return null;
			//调用函数
			JSONObject tmp=WxUtil.createGetMsg(paras.get("Url"));
			return tmp;
		}else
			return null;
	}
	
	public Object __Session(Map<String,String> paras, Map<String,Object> paras_map) {//ly session
		String method= paras.containsKey("__method") ? paras.get("__method") : "";
		//工作目录
		String param=paras.containsKey("__params")  ? paras.get("__params") : "" ;
		//String src=paras.get("Src");
		
		if (method.equals("Get")==true) {
			boolean b=param.indexOf("false")>=0 ? false : true;
			HttpSession session = this.request.getSession(b);
			if (session == null ) {
				return null;
	        }else {
	        		Map<String,Object> session_obj = new HashMap<String, Object>();
	        		Enumeration<?> enumeration = session.getAttributeNames();
	        		// 遍历enumeration中的
	        		while (enumeration.hasMoreElements()) {
	        			// 获取session键值
	        			String name = enumeration.nextElement().toString();
        				// 根据键值取session中的值
        				Object value = session.getAttribute(name);
        				//写入session_obj
        				session_obj.put(name, value.toString());
        			}
	        		session_obj.put("__id", session.getId());
	        		return session_obj;	
	        }
		}else if (method.equals("Set")==true) {
			//调用函数	
			HttpSession session = this.request.getSession(false);
			if (session==null) {
				this.CallError="CALL->Session.Set()-> session not exited!"; 
				return "";
			}else {
				for(Map.Entry<String, Object> entry : paras_map.entrySet()){
				    String mapKey = entry.getKey();
				    String mapValue = entry.getValue().toString();
				    session.setAttribute(mapKey, mapValue);
				}
				return "";
			}
		}
		return "";
	}
	
	public Object __FileUtils(Map<String,String> paras, Map<String,Object> paras_map) {
		String method= paras.containsKey("__method") ? paras.get("__method") : "";
		//工作目录
		String param=paras.containsKey("Params")  ? paras.get("Params") : "" ;
		String work_path=this.webPath!=null ? this.webPath : WebService.class.getClassLoader().getResource("").getPath();;
		if (!paras.containsKey("Src") ) {
			this.CallError="CALL->FileUtils()->"+method+"->(Src) paras set error!"; 
			return "";
		}
		//src defined
		String src=paras.get("Src");
		if (src.length()==0) {
			this.CallError="file or dir("+src+") not exited!"; 
			return "";
		}
		if (src.indexOf("./")==0)
			src=work_path+src.substring(2);
		else if (src.indexOf("/")!=0)
			src=work_path+src;
		
		//取得原文件或目录upload
		File src_file=new File(src);
		if (!src_file.exists()) {
			if (param.indexOf("err_return")>=0)
				this.CallError="file or dir("+src+") not exited!"; 
			return "";
		}
		System.out.println(src);
		//dest defined 
		String dest=paras.containsKey("Dest") ? paras.get("Dest") : "";
		if (dest.length()!=0) {
			if (dest.length()!=0 && (dest.indexOf("./")==0))
				dest=work_path+dest.substring(2);
			else if (dest.indexOf("/")!=0)
				dest=work_path+dest;
		}
		//
		if (method.equals("moveFileToDirectory")==true) {
			//调用函数
			if (dest.length()==0) {
				this.CallError="CALL->FileUtils()->moveFileToDirectory->(Src,Dest,is_creatDir) paras set error!"; 
				return "";
			}
			String ext=paras.containsKey("Ext") ? paras.get("Ext") : "";
			//移动操作
			try {
				if (src_file.isDirectory()) {//多文件
					Collection<File> list_files =FileUtils.listFiles(src_file,FileFilterUtils.suffixFileFilter(ext),null);	
					for(File file : list_files){
						FileUtils.moveFileToDirectory(file,new File(dest),param.indexOf("create_dir")>=0);
					}
					if ((param.indexOf("del_empty_src")>=0) && (ext.length()==0)) {
						FileUtils.deleteQuietly(new File(src));
					};
				}else 
					FileUtils.moveFileToDirectory(src_file,new File(dest),param.indexOf("create_dir")>=0);	
			} catch (Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
				this.CallError=""+e;
			}
			return "";	
		}else if (method.equals("Delete")==true) {
			//调用函数
			if (src.indexOf("FileUpload")>=0)
				FileUtils.deleteQuietly(src_file);
			else {
				this.CallError="CALL->FileUtils()->del error delete too more and not saft!"; 
				return "";
			}	
			if (param.indexOf("return_dir")>=0) {
				Integer n=0;
				if ((n=src.lastIndexOf("\\"))>=0 || (n=src.lastIndexOf("/"))>=0)
					src=src.substring(0,n);		
				return this.FileUtils_ListFile(src,paras,paras_map);
			}else
				return "";
		}else if (method.equals("listFiles")==true) {
			//调用函数
			return this.FileUtils_ListFile(src,paras,paras_map);
		}else
			return "";
	}
	
	public Object FileUtils_ListFile(String src, Map<String,String> paras, Map<String,Object> paras_map) {
		String ext=paras.containsKey("Ext") ? paras.get("Ext") : "";
		//Boolean param=paras.containsKey("Params") && paras.get("Params").equals("deep") ? true :false ;
		String param=paras.containsKey("Params") ? paras.get("Params") : "" ;
		String trans_str=paras.containsKey("Trans") ? paras.get("Trans") : "";
		//具体操作
		Collection<File> list_files;
		try {
			list_files =FileUtils.listFiles(new File(src),FileFilterUtils.suffixFileFilter(ext),param.indexOf("deep")>=0 ? DirectoryFileFilter.INSTANCE : null);	
			if (trans_str.equals("list")) {
				List<Map<String, Object>> list=new ArrayList<Map<String, Object>>() ;
				Integer i=0;
				for (File file : list_files) {
					Map<String, Object> item=new HashMap<String, Object>();
					item.put(""+i,file.getName());
					list.add(item);
					i++;
		        }
				return list;
			}else {
				String dest_str="";
				for (File file : list_files) {
					if (param.indexOf("add_parent_path")>=0) {
						String path_tmp=file.getPath();
						String path=path_tmp.replaceAll("\\\\", "/");
						String file_name=path.substring(path.lastIndexOf("/")+1);
						String path2=path.substring(0,path.lastIndexOf("/"));
						String parent=path2.substring(path2.lastIndexOf("/")+1);
						dest_str+=",\""+parent+"/"+file_name+"\"";
					}else
						dest_str+=",\""+file.getName()+"\"";
		        }
				return dest_str.length()!=0 ? dest_str.substring(1) : "";
			}
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			this.CallError=""+e;
			return "";
		}
	}
	
	public Object __GetherGridXml(Map<String,String> paras, Map<String,Object> paras_map) {
		//取得参数List
		String res=null;
		if (!paras.containsKey("Grid")) {
			this.CallError="Call->GetherGridXml->Grid should be set!"; 
			return null;
		}
		String grid=paras.get("Grid");
		
		if (!paras_map.containsKey(grid)) {
			this.CallError="Call->GetherGridXml->Paras("+grid+") not found!"; 
			return null;
		}
		@SuppressWarnings("unchecked")
		Object list_map_obj=paras_map.get(grid);
		if (!(list_map_obj instanceof List )) {
			this.CallError="Call->GetherGridXml->Paras("+grid+") is not list_map!"; 
			return null;
		}
		if (list_map_obj==null) {
			this.CallError="Call->GetherGridXml->Paras("+grid+") list_map is null!"; 
			return null;
		}
		@SuppressWarnings("unchecked")
		List<Map<String, Object>> list_map = (List<Map<String, Object>>)list_map_obj;
		
		if (!paras.containsKey("GroupColumns")) {
			this.CallError="CALL->GetherGridXml()->GroupColumns should be set!"; 
			return null;
		}
		String group_columns=paras.get("GroupColumns");
		String add_columns=paras.containsKey("AddColumns") ? paras.get("AddColumns") : null;
		String add_columns_attr=paras.containsKey("AddColumnsAttr") ? paras.get("AddColumnsAttr") : null;
		if (!paras.containsKey("CntColumn")) {
			this.CallError="CALL->GetherGridXml()->CntColumn should be set!"; 
			return null;
		}
		String cnt_col=paras.get("CntColumn");
		if (!paras.containsKey("CntMethod")) {
			this.CallError="CALL->GetherGridXml()->CntMethod should be set!"; 
			return null;
		}
		String cnt_method=paras.get("CntMethod");
		
		//聚合处理
		GetherGrid gether=new GetherGrid(list_map, group_columns,add_columns,add_columns_attr, cnt_col,cnt_method);
		gether.TransType=paras.containsKey("Trans") && paras.get("Trans").equals("Json") ? 1 : 0;
		String title=paras.containsKey("FieldsTitle") ? paras.get("FieldsTitle") : "";
		if (!title.isEmpty())
			gether.title=title.split(";");
		res=gether.GetherGridXml(0,0,list_map.size());
		if (!gether.Error.isEmpty()) {
			this.CallError=gether.Error;
			return "";
		}
		if (res==null) {
			this.CallError="CALL->GetherGridXml() return null!";
			return "";
		}
		return res;
	}
	
	
	/*
	String[] _GetherGrid_Columns=null;
	String  _GetherGrid_XmlStr="";
	String  _GetherGrid_Method="";
	Integer _GetherGrid_Rec=0;
	String _GetherGrid_CntCol="";
	String _GetherGrid_Error="";
	public double GetherGridXml(List<Map<String, Object>> list_map, Integer group_col) {
		if (group_col >=this._GetherGrid_Columns.length)
			return 0; //递归结束。
		
		double sum=0; //累计数
		String col_val_old=""; //列内容跟踪；
		for(; this._GetherGrid_Rec<list_map.size(); this._GetherGrid_Rec++) {
			//取得列值
			Map<String, Object> col_map=(Map<String, Object>)list_map.get(this._GetherGrid_Rec);
			String col_name=this._GetherGrid_Columns[group_col];
			String col_val=(String)col_map.get(col_name);
			if (col_val==null)
				return 0;
			//列值变化
			Boolean is_chg=col_val_old.equals(col_val);
			//创建节点
			if (is_chg)
				this._GetherGrid_XmlStr="<"+col_val;
			//计算操作
			if (is_chg) {
				double method_col_val=Double.parseDouble((String)col_map.get(this._GetherGrid_CntCol));
				if (this._GetherGrid_Method.equals("Sum"))
					sum+=method_col_val;
			}
			//递归下层
			if (!is_chg)
				sum=this.GetherGridXml(list_map,group_col+1);
			//完善节点
			if (is_chg)
				this._GetherGrid_XmlStr=" "+_GetherGrid_Method+"="+sum+">";
			col_val_old=col_val;
		}
		return 0;
	}	*/
	
	public Object __zTreeToEasyTree(Map<String,String> paras, Map<String,Object> paras_map) {
		String[] trans=null;
		String method= paras.containsKey("__method") ? paras.get("__method") : "";
		if (method.equals("DoTrans")==true) {
			//调用函数
			if (!paras.containsKey("Src"))
				return null;
			String url=paras.containsKey("Src")? paras.get("Src"):null;
			String trans_param=paras.containsKey("Param")? paras.get("Param"):null;
			if (trans_param!=null) {
				trans=trans_param.split(",");
				if (trans.length<5) {
					this.CallError="Call->__zTreeToEasyTree->Paras('id_field|text_field|pId_field|pid_value+field|start_field') should be set!"; 
					return null;
				}
			}
			
			return DataTransformUtil.DoTrans(url,trans);
		}
		return null;
	}
	
	//SET 动作
	public String SET(Node parseNode, String value, Map<String,String> attrs, Map<String, Object> paras_map){ 
		//取得属性
		if (attrs==null)
			return "<SET> should set attribute(To)";
		String to= attrs.containsKey("To") ? attrs.get("To") : "";
		String type= attrs.containsKey("Type") ? attrs.get("Type") : "";
		String method= attrs.containsKey("Method") ? attrs.get("Method") : "";
		if (to.length()==0)
			return "<SET> should set attribute(To)!";
		
		//写入值;
		if (method.length()!=0) {
			//通过函数计算
			String err="",r_s="";	
			try {
	    			Method return_func=this.getClass().getMethod((String)"__"+method, String.class);
		    		r_s=(String)return_func.invoke(this, value);
			} catch (IllegalAccessException e) {
				e.printStackTrace();
				err=e.toString();
			} catch (IllegalArgumentException e) {
				e.printStackTrace();
				err=e.toString();
			} catch (InvocationTargetException e) {
				e.printStackTrace();
				err=e.toString();
			} catch (NoSuchMethodException e) {
				e.printStackTrace();
				err=e.toString();
			} catch (SecurityException e) {
				e.printStackTrace();
				err=e.toString();
			}
			if (err==null || err.isEmpty()) {
				paras_map.put(to,r_s);
			}else {
				return err;
			}
		}else if (type.length()==0) {//普通写入
			paras_map.put(to,value==null ? "" : value);
		}else {//累加操作
			String value_old="";
			if (paras_map.containsKey(to))
				value_old=""+paras_map.get(to);
			paras_map.put(to,value_old+(value==null ? "" : value));
		}
		return "";
	}
		
	//XSLT 动作
	public String XSLT(Node parseNode, String value, Map<String,String> attrs, Map<String, Object> paras_map){ 
		//取得属性
		if (attrs==null)
			return "<XSLT>To=\"*\" should be set";
		String to= attrs.containsKey("To") ? attrs.get("To") : "";
		String path_str= attrs.containsKey("Path") ? attrs.get("Path") : "";
		String path=ParseValue(path_str,paras_map,null,null);
		String act= attrs.containsKey("Act") ? attrs.get("Act") : "";
		String from= attrs.containsKey("From") ? attrs.get("From") : "";
		if (to.length()==0)
			return "<XSLT> attribute(To) should be set!";
		if (path.length()==0)
			return "<XSLT> attribute(Path) should be set!";
		if (from.length()!=0) {
			value=getStreamStr(from);
		}
		if (value==null || value.length()==0)
			return "<XSLT>*</XSLT> value should be set!";
		String xslt_file=path;
		
		//写入值;
		if (xslt_file!=null && xslt_file.equals("")==false) {//xslt转换后回传
		    	//取得xslt文件
		    	String xslt=getStreamStr(xslt_file);
		    	if (xslt==null) {
					return "xslt file(\""+xslt_file+"\") not found!\"";
			}
		    	//过滤命名空间
		    	if (act.indexOf("clear_xmlns")>=0) {
		    		String value_tmp=value;
		    		value=value_tmp.replaceAll("xmlns","xmlns_bak"); //取消xmlns作用
		    	}
		    	
			//转换操作
		    	//String err="";
		    	StringBuilder err_obj = new StringBuilder();
		    	String destXml=transXmlByXslt(value,xslt,err_obj,true);
		    if (destXml.length()==0 && err_obj.length()!=0) 
		    		return "xslt transform error("+err_obj.toString()+")"; 
		    else {
		    		paras_map.put(to,destXml);
		    }
		    return "";
		}else
			return "attribute path must be set!";
	}
	
	public String addXsltAct(String in_str) {
		String out_tmp=in_str.replaceAll("demo","调用主表");
		String out_str=out_tmp.replaceAll("Schedule History","历史计划");
		return out_str;
	}
	
	public static String transXmlByXslt(String srcXml, String xslt,StringBuilder err,Boolean del_head) {
		//输入子串转换
		ByteArrayInputStream xslt_stream = null;
	    try {
	        xslt_stream = new ByteArrayInputStream(xslt.getBytes("UTF-8"));
	    } catch (UnsupportedEncodingException e) {
	        e.printStackTrace();
	        err.append(e.toString());
			return "";
	    }
	    
	    ByteArrayInputStream xml_stream = null;
	    try {
	        xml_stream = new ByteArrayInputStream(srcXml.getBytes("UTF-8"));
	    } catch (UnsupportedEncodingException e) {
	        e.printStackTrace();
	        err.append(e.toString());
			return "";
	    }
	    ByteArrayOutputStream xml_dest = new ByteArrayOutputStream();
	    
		// 获取转换器工厂
		TransformerFactory tf = TransformerFactory.newInstance();
		try {
			// 获取转换器对象实例
			Transformer transformer = tf.newTransformer(new StreamSource(xslt_stream));  //请在，在此处提速，应该统一由json定义
			// 进行转换
			transformer.transform(new StreamSource(xml_stream),
					new StreamResult(xml_dest));
			if (del_head) {
				return xml_dest.toString().replace("<?xml version=\"1.0\" encoding=\"UTF-8\"?>", ""); //ly_new2
			}else
				return xml_dest.toString();
		} catch (TransformerConfigurationException e) {
			e.printStackTrace();
			err.append(e.toString());
			return "";
		} catch (TransformerException e) {
			e.printStackTrace();
			err.append(e.toString());
			return "";
		}
	};
	
	private String getStreamStr(String cfg_name){
		InputStream inputStreamObject = WebService.class.getClassLoader().getResourceAsStream(cfg_name);
		if (inputStreamObject==null){
			return null;
		}
		try {
		       BufferedReader streamReader = new BufferedReader(new InputStreamReader(inputStreamObject, "UTF-8"));
		       StringBuilder responseStrBuilder = new StringBuilder();

		       String inputStr;
		       while ((inputStr = streamReader.readLine()) != null)
		           responseStrBuilder.append(inputStr);
		       return responseStrBuilder.toString();
		   } catch (IOException e) {
		       e.printStackTrace();
		   } catch (JSONException e) {
		       e.printStackTrace();
		   }
		    //if something went wrong, return null
		    return null;
		}
	
	@SuppressWarnings("unchecked")
	public String JPATH(Node parseNode, String value, Map<String,String> attrs, Map<String, Object> paras_map, Map<String, Object> row_map, String[] clear){ 
		//取得属性
		if (attrs==null)
			return "<JPATH> attribute(To) should be set!";
		String err=null;
		String to= attrs.containsKey("To") ? attrs.get("To") : "";
		String from= attrs.containsKey("From") ? attrs.get("From") : "";
		String path= attrs.containsKey("Path") ? attrs.get("Path") : "";
		String type= attrs.containsKey("Trans") ? attrs.get("Trans") : "string";
		if (to.length()==0)
			return "<JPATH> attribute(To)  should be set!";
		if (path.length()==0)
			return "<JPATH>attribute(Path)  should be set!";
		//补充替换
		path=ParseValue(path,paras_map,row_map,clear);
		type=ParseValue(type,paras_map,row_map,clear);
		
		Object obj=null;
		if (from.length()==0) {
			if (value.length()==0)
				return "<JPATH> json src not exited!\"";
			obj=JSON.parseObject(value);
		}else {
			if (!paras_map.containsKey(from))
				return "<JPATH> From='"+from+"' value not exited!";
			from=ParseValue(from,paras_map,row_map,clear);
			Object from_obj=paras_map.get(from);
			if (from_obj instanceof JSONObject || from_obj instanceof JSONArray) {
				obj=from_obj;
			}else if (from_obj  instanceof String)
				obj = JSON.parseObject(from);
			else
				return ("JPATH: from ("+from+") value should be JSONObject or String!");
		}
		//
		if (obj==null)
			return ("JPATH: src(from or value) is not JSON!");
		//Path测试操作
		/*String jsonStr = "{ \"store\": {\"book\": [{ \"category\": \"reference\","+
				"\"author\": \"Nigel Rees\",\"title\": \"Sayings of the Century\","+
				"\"price\": 8.95},{ \"category\": \"fiction\",\"author\": \"Evelyn Waugh\","+
				"\"title\": \"Sword of Honour\",\"price\": 12.99,\"isbn\": \"0-553-21311-3\""+
				"}],\"bicycle\": {\"color\": \"red\",\"price\": 19.95}}}";

        // 获取json中store下book下的所有title值
		JSONObject jsonObject = JSON.parseObject(jsonStr);
		System.out.println("\n Book数目：" + JSONPath.eval(jsonObject, "$.store.book.size()"));
		System.out.println("第一本书title：" + JSONPath.eval(jsonObject, "$.store.book[0].title"));
		System.out.println("price大于10元的book：" + JSONPath.eval(jsonObject, "$.store.book[price > 10]"));
		System.out.println("price大于10元的title：" + JSONPath.eval(jsonObject, "$.store.book[price > 10][0].title"));
		System.out.println("category(类别)为fiction(小说)的book：" + JSONPath.eval(jsonObject, "$.store.book[category = 'fiction']"));
		System.out.println("bicycle的所有属性值" + JSONPath.eval(jsonObject, "$.store.bicycle.*"));
		System.out.println("bicycle的color和price属性值" + JSONPath.eval(jsonObject, "$.store.bicycle['color','price']"));
		*/
		//json path操作
		
		
		String out="";
		while(true) {
			if (type.indexOf("list")==0 || type.indexOf("map")==0) {
				//取得json数组
				List<Object> list_r=null;
				try {
					Object in_obj=JSONPath.eval(obj, path);
					if (in_obj instanceof Map) {//单记录
						List<Object> list_tmp = new ArrayList<Object>(); 
						list_tmp.add(in_obj);
						list_r=list_tmp;
					}else {//多记录
						list_r = (List<Object>)in_obj; // 返回enties的所有名称
					}	
				}catch(Exception e){
					e.printStackTrace();
					err="Eval jsonPath("+path+") error!";
					break;
				}
				if (list_r==null) {//是否返回空就可以？？？
					err="Eval jsonPath("+path+") error!";
					break;
				}
				//处理
				if (type.indexOf("list")==0) {
					//生成目标list_map;
					String[] map_para=type.split(":");
					List<Map<String, Object>> list_out = new ArrayList<Map<String, Object>>(); 
					if (map_para.length==1) {
						//生成目标list_map;
						for(int i=0; i<list_r.size(); i++) {
							Object cell=list_r.get(i);
							Map<String, Object> map = new HashMap<String, Object>();
							if (cell instanceof Map) {
								Map<String, Object> cell_map=(Map<String, Object>)cell;
								for (Map.Entry<String, Object> entry :cell_map.entrySet())
									map.put(entry.getKey(),entry.getValue()); 
							}else 
								map.put(".", list_r.get(i)); 
				            list_out.add(map);
						}
					}else {
						//list_map:sql_fiels_values:key;
						for(int i=0; i<list_r.size(); i++) {//ly new2
							Object cell=list_r.get(i);
							Map<String, Object> map = new HashMap<String, Object>();
							if (cell instanceof Map) {
								if (map_para.length==3) {//getkey
									@SuppressWarnings("unchecked")
									Object key_value=((Map<String,Object>)cell).get(map_para[2]);
									map.put("key", key_value==null ? "" : ""+key_value);
								}
								Map<String, Object> cell_map=(Map<String, Object>)cell;
								if (map_para[1].indexOf("into")>0) {
									String values="",fields="";Integer j=0;
									for (Map.Entry<String, Object> entry : ((Map<String,Object>)cell).entrySet()) { 
										if ((""+entry.getValue()).indexOf("++")>=0)
											values= values+ (j!=0 ? ",'"+entry.getValue().toString().substring(2)+"'" : "'"+entry.getValue().toString().substring(2)+"'");//累加处理
										else
											values= values+ (j!=0 ? ",'"+entry.getValue()+"'" : "'"+entry.getValue()+"'");//普通赋值
										fields= fields+ (j!=0 ? ","+entry.getKey() : entry.getKey());
										j++;
									}
									map.put("fields", fields);
									map.put("values", values);
								}
								if (map_para[1].indexOf("set")>0) {
									String set_str="";Integer j=0;
									for (Map.Entry<String, Object> entry : ((Map<String,Object>)cell).entrySet()) {
										if ((""+entry.getValue()).indexOf("++")>=0)
											set_str= set_str+(j==0 ? "": ",")+entry.getKey()+"="+entry.getKey()+"+"+entry.getValue().toString().substring(2)+"";//累加情况
										else
											set_str= set_str+(j==0 ? "": ",")+entry.getKey()+"='"+entry.getValue()+"'";//普通赋值
										j++;
									}
									map.put("sets", set_str);
								}
							}else 
								return "<JPATH> Trans=\"*\" 'list:sql_fields_value:[key]' obj is not map!";
				            list_out.add(map);
						}
					}
					paras_map.put(to,list_out);
				}else {//ly_sync
					//取得key_valu;map:key
					String[] map_para=type.split(":");
					if (map_para==null || map_para.length<2)
						return "<JPATH> Trans=\"*\" should be 'map:key'";
//					if (list_r.size()>0 && !(list_r.get(0) instanceof Map))
//						return "<JPATH> Trans=\"*\" map's value should be map obj!";
					String map_key = map_para[1];
					Map<String, Object> map_out = new HashMap<String, Object>(); 
					Integer i;
					for(i=0; i<list_r.size(); i++) {
						Object cell=list_r.get(i);
						if (cell==null || !(cell instanceof Map)) {
							return "<JPATH> Trans=\"*\" 'map:key' obj is not map!";
						}
						if (map_key.equals("#")) {
							map_out.put(""+i, list_r.get(i));
						}else {
							@SuppressWarnings("unchecked")
							Object key_value=((Map<String,Object>)cell).get(map_key);
							if (key_value==null)
								return "<JPATH> Trans=\"map:"+map_key+"\" invalid key("+map_key+")!";
							map_out.put(key_value.toString(), list_r.get(i));
						}
					}
//					for(i=0; i<list_out.size(); i++) {
//						Object cell=list_out.get(i);
//						if (cell==null || !(cell instanceof Map)) {
//							return "<JPATH> Trans=\"*\" 'map:key' obj is not map!";
//						}
//						@SuppressWarnings("unchecked")
//						Object key_value=((Map<String,Object>)cell).get(map_key);
//						if (key_value==null)
//							return "<JPATH> Trans=\"map:"+map_key+"\" invalid key("+map_key+")!";
//						map_out.put(key_value.toString(), list_out.get(i));
//					}
					if (map_out.size() < (i-1))
						System.out.println("WARN: !!! <JPATH>: trans=\"map\" may has duplicate key!");
					//建立key_value的map数组
					paras_map.put(to,map_out);
				}
			}else {
				try {
					out=""+JSONPath.eval(obj, path);
					paras_map.put(to,out);
				}catch(Exception e){
					e.printStackTrace();
					err="Eval jsonPath("+path+") error!";
					paras_map.put(to,null);
				}
			}
			break;
		}
		
		//写入值;
		if (err!=null)
			return err;
		else
			return "";
	}
	
	@SuppressWarnings("unchecked")
	public String XPATH(Node parseNode, String value, Map<String,String> attrs, Map<String, Object> paras_map, Map<String, Object> row_map, String[] clear){ 
		//取得属性 ly xpath
		if (attrs==null)
			return "<XPATH> attribute(To) should be set!";
		String err=null;
		String to= attrs.containsKey("To") ? attrs.get("To") : "";
		String from= attrs.containsKey("From") ? attrs.get("From") : "";
		String path= attrs.containsKey("Path") ? attrs.get("Path") : "";
		String type= attrs.containsKey("Trans") ? attrs.get("Trans") : "string";
		String parse= attrs.containsKey("Parse") ? attrs.get("Parse") : "off";
		if (to.length()==0)
			return "<XPATH> attribute(To)  should be set!";
		if (path.length()==0)
			return "<XPATH>attribute(Path)  should be set!";
		if (path.length()==0)
			return "<XPATH>attribute(From)  should be set!";
		if (!paras_map.containsKey(from))
			return "<XPATH> From='"+from+"' value not exited!";
		//补充替换
		from=ParseValue(from,paras_map,row_map,clear);
		path=ParseValue(path,paras_map,row_map,clear);
	
		//json path操作
		Object obj=null;
		Object from_obj=paras_map.get(from);
		if (from_obj instanceof Document) {
			obj=from_obj;
		}else if (from_obj  instanceof String) {
			obj = this.Xml_getDocument(from);
		}else {
			return ("XPATH: from ("+from+") value should be String!");
		}	
		
		String out="";
		while(true) {
			if (type.indexOf("list")==0 || type.indexOf("map")==0) {
				//取得json数组
				NodeList list_r;
				list_r=this.Xml_getXpathNodes((Document)obj, path);
				if (list_r==null) {
					err="Eval XPath("+path+") error!";
					break;
				}
				//处理
				if (type.indexOf("list")==0) {
					//生成目标list_map;
					List<Map<String, Object>> list_out = new ArrayList<Map<String, Object>>(); 
					//生成目标list_map;
					for (int i = 0; i < list_r.getLength(); i++) {
						Map<String, Object> map = new HashMap<String, Object>();
						if(list_r.item(i).getNodeType()==Node.ELEMENT_NODE) { 
							map.put(".",this.Xml_getNodeValue(list_r.item(i)));
							NodeList list_r_tmp=list_r.item(i).getChildNodes();
							for (int j = 0; j < list_r_tmp.getLength(); j++) {
								if(list_r_tmp.item(j).getNodeType()==Node.ELEMENT_NODE)
									map.put(list_r_tmp.item(j).getNodeName(),this.Xml_getNodeValue(list_r_tmp.item(j)));		
							}	
						}
						list_out.add(map);
			        }
					paras_map.put(to,list_out);
				}else {//ly_sync
					//取得key_valu;map:key
					String[] map_para=type.split(":");
					if (map_para==null || map_para.length<2)
						return "<XPATH> Trans=\"*\" should be 'map:key'";
					String map_key = map_para[1];
					Map<String, Object> map_out = new HashMap<String, Object>(); 
					String key_value=null;
					for (int i = 0; i < list_r.getLength(); i++) {
						Map<String, Object> map = new HashMap<String, Object>();
						if (list_r.item(i).getNodeType() == Node.TEXT_NODE)
							return "<XPATH> Trans=\"map:"+map_key+"\" invalid key("+map_key+")!";
						else {
							
							NodeList list_r_tmp=list_r.item(i).getChildNodes();
							for (int j = 0; j < list_r_tmp.getLength(); j++) {
								map.put(list_r_tmp.item(i).getNodeName(),list_r_tmp.item(i).getNodeValue()); 
								if (list_r_tmp.item(i).getNodeName().equals(map_key)) {
									key_value=list_r_tmp.item(i).getNodeValue();
								}	
							}
							if (key_value==null)
								return "<XPATH> Trans=\"map:"+map_key+"\" invalid key("+map_key+")!";
							map_out.put(key_value, map);
						}
						map_out.put(key_value, map);
			        }
					//建立key_value的map数组
					paras_map.put(to,map_out);
				}
			}else {
				try {
					String s1=this.Xml_getXpathValue((Document)obj, path);
					if (s1!=null && parse.equals("on")) {//ly session
						paras_map.put(to,ParseValue(s1,paras_map,row_map,clear));
					}else
						paras_map.put(to,s1);
				}catch(Exception e){
					e.printStackTrace();
					err="Eval jsonPath("+path+") error!";
					paras_map.put(to,null);
				}
			}
			break;
		}
		
		//写入值;
		if (err!=null)
			return err;
		else
			return "";
	}
	
	public Boolean IsUrlField(String value,Map<String, Object> param_map) {
		if (value.indexOf("@_@")>=0)
			return true;
		else
			return false;
	}
		
	//FOR 动作
	public String FOR(Node parseNode, Map<String,String> attrs, Map<String, Object> paras_map){ 
		this.ParseDeep++;
		String err_str="";
		this.isBreak =false;
		int j=0;

		//取得属性
		if (attrs==null)
			err_str= "<JPATH> attribute(In) as incoming list!";
		String to= attrs.containsKey("To") ? attrs.get("To") : "";
		String from= attrs.containsKey("In") ? attrs.get("In") : "";
		String num_str= attrs.containsKey("Num") ? attrs.get("Num") : "";
		Boolean is_url_field=IsUrlField(from,paras_map);
		if (from.length()>0 && !paras_map.containsKey(from) && !is_url_field)
			err_str= "<FOR> In=\"*\" should be set as incoming list!";
		if (num_str.length()>0 && num_str.indexOf("-")<=0)
			err_str= "<FOR> Num=\"*\" should be set [num1]-[num2], example: 0-200!";
		if (from.length()==0 && num_str.length()==0)
			err_str= "<FOR> In=\"*\" or In=\"*\" should be set as incoming list!";
		String[] clear=this.clearStr==null ? null : this.clearStr.split(";");
		
		//循环动作
		paras_map.put("@_#for_end","");
		paras_map.put("@_#for_num","");
		if (err_str.length()==0) {
			if (from.length()!=0) {
				Object obj;
				if (is_url_field)
					obj=(Object)ParseValue("{"+from+"}",paras_map,null,null);
				else
					obj=paras_map.get(from);
				if (obj==null)
					err_str="<FOR> In=\"*\" value is null!";  //如果list为空则不循环
				else if (err_str.length()==0) {
					if (obj instanceof List) {
						@SuppressWarnings("unchecked")
						List<Map<String, Object>> list = (List<Map<String, Object>>)obj;
						//循环查询数据库取得Map字段作为row_map传参
						for (j = 0; j < list.size(); j++) {
							paras_map.put("@_#for_num",""+j);
							if (j==(list.size()-1))
								paras_map.put("@_#for_end","true");
							Map<String, Object> row_paras=list.get(j);	
							err_str=_FOR_Node(parseNode, attrs, paras_map,row_paras,clear);
							if (err_str.length()!=0 || this.isBreak)
								break;
						}
					}else if (obj instanceof Map){//ly_sync
						@SuppressWarnings("unchecked")
						Map<String, Object> map_obj = (Map<String, Object>)obj;
						//循环查询数据库取得Map字段作为row_map传参
						Integer i=0;
						paras_map.put("@_#key","");
						for (Map.Entry<String, Object> entry : map_obj.entrySet()) { 
							paras_map.put("@_#for_num",""+j);
							paras_map.put("@_#key",""+entry.getKey());
							if (j==(map_obj.size()-1))
								paras_map.put("@_#for_end","true");
							Object row_obj=entry.getValue();
							if (!(row_obj instanceof Map)) {
								err_str="<FOR> Trans=\"map->son_obj\" also should be map object!";
								break;
							}
							@SuppressWarnings("unchecked")
							Map<String, Object> row_paras=(Map<String, Object>)entry.getValue();	
							err_str=_FOR_Node(parseNode, attrs, paras_map,row_paras,clear);
							if (err_str.length()!=0 || this.isBreak)
								break;
							i++;
						}
						paras_map.remove("@_#key");
					}else if (obj instanceof String) {
						String from_str=(String) obj;
						String[] from_arr=from_str.split(",");
						for(int i=0; i<from_arr.length; i++) {
							paras_map.put("@_#for_num",""+j);
							if (i==(from_arr.length-1))
								paras_map.put("@_#for_end","true");
							Map<String, Object> row_paras=new HashMap<String, Object>();	
							row_paras.put(".", from_arr[i]);
							err_str=_FOR_Node(parseNode, attrs, paras_map,row_paras,clear);
							if (err_str.length()!=0 || this.isBreak)
								break;
						}
					}
					else {
						err_str= "<FOR> In=\""+from+"\" value("+from+") should be list|map|***,***!";	
					}
				}		
			}else if (num_str.length()>0) {//from to
				String[] num= num_str.split("-");
				if (!isNumeric(num[0]) || !isNumeric(num[1]))
					err_str="<FOR> Num=\""+num[0]+"-"+num[1]+"\" should be number!";
				Integer start=Integer.parseInt(num[0]);
				Integer end=Integer.parseInt(num[1]);
				if (start>=end)
					err_str="<FOR> Num=\""+num[0]+"-"+num[1]+"\" "+num[0]+"should be small then "+num[1]+"\"";
				for(int k=start; k<=end; k++) {
					paras_map.put("@_#for_num",""+k);
					if (k==end)
						paras_map.put("@_#for_end","true");
					err_str=_FOR_Node(parseNode, attrs, paras_map,null,clear);
					if (err_str.length()!=0 || this.isBreak)
						break;
				}
			}	   
		}
		
		//返回值
		if (to.length()!=0) 
			paras_map.put(to,""+j);
		
		//清理
		paras_map.remove("@_#for_num");
		paras_map.remove("@_#for_end");
		this.ParseDeep--;
		return err_str;
	}
	
	private boolean isNumeric(String str){
        Pattern pattern = Pattern.compile("[0-9]*");
        Matcher isNum = pattern.matcher(str);
        if( !isNum.matches() ){
            return false;
        }
        return true;
	}
	
	private String _FOR_Node(Node parseNode, Map<String,String> attrs, Map<String, Object> paras_map,Map<String, Object> row_paras,String[] clear){
		String err_str="";
		//取得子节点
		NodeList nodes = parseNode.getChildNodes();
		if(nodes!=null){
			for_err_1:
			for (int i = 0; i < nodes.getLength(); i++){
				Node node = nodes.item(i);
				if(node.getNodeType()==Node.ELEMENT_NODE) { 
					//取得value
					String value=Xml_getNodeValue(node);
					if (value!=null && value.length()!=0)
						value=ParseValue(value,paras_map,row_paras,clear);
					//取得属性
					Map<String,String> attrs2=getParseNodeAttr(node);
					if (attrs2.containsKey("ParseValue2") && attrs2.get("ParseValue2").equals("true")) {//再来一次ParseValue，往往对于计算值还有{***}的情况
						value=ParseValue(value,paras_map,row_paras,clear);
					}
					//测试是否执行如果{***}为空或不为空的处理
					if (DoTestValue(attrs2,paras_map,row_paras)) {
						//跟踪语句
						if (is_debug)
							this.DebugY(node,value,attrs2);
						//解析内容
						switch (node.getNodeName()) {
						case "SET":
							err_str=SET(node,value,attrs2,paras_map);
							break;
						case "CALL":
							err_str=CALL(node,attrs2,paras_map,row_paras,clear);
							break;
						case "DO_SQL":
							err_str=DO_SQL(node,value,attrs2,paras_map);
							break;
						case "PRINT":
							err_str=PRINT(value,attrs2,paras_map);
							break;
						case "CONTINUE":
							break for_err_1;
						case "BREAK":
							if (attrs2.containsKey("Error") && attrs2.get("Error").equals("true"))
								err_str=value;//返回差错
							this.isBreak=true;	//普通返回
							break;
						case "JPATH":
							err_str=JPATH(node,value,attrs2,paras_map,row_paras,clear);
							break;
						case "XPATH":
							err_str=XPATH(node,value,attrs2,paras_map,row_paras,clear);
							break;
						default:
							err_str=(String)"ParseError: node <"+node.getNodeName()+"> not support!";
						}
						//跟踪结果
						if (is_debug && attrs2.containsKey("Debug") && (attrs2.get("Debug").equals(".")||attrs2.get("Debug").equals("..."))) {
							DebugV(attrs2,paras_map,"To");
						}
						if (err_str.length()!=0 || isBreak) {
							break for_err_1;
						}
					}
				}
			}
		}
		if (is_debug && attrs.containsKey("Debug") && attrs.get("Debug").equals("....")) {
			System.out.print(".");
			for_debug_count++;
			if (for_debug_count<100)
				System.out.print(".");
			else {
				System.out.println(".");
				for_debug_count=0;
			}
		}
		return err_str;
	}
	
	public String DO_SQL(Node parseNode, String value, Map<String,String> attrs, Map<String, Object> paras_map){ 
		if (this.dbConns==null)
			return "<DO_SQL>***</DO_SQL> dbConns is error! ";
		String err="";
		
		//取得属性
		if (attrs==null)
			return "<DO_SQL> attribute(To)  should be set!";
		String to= attrs.containsKey("To") ? attrs.get("To") : "";
		String total= attrs.containsKey("Total") ? attrs.get("Total") : "";
		Integer itotal;
		String type= attrs.containsKey("Type") ? attrs.get("Type") : "";
		String trans= attrs.containsKey("Trans") ? attrs.get("Trans") : "string";
		String db= attrs.containsKey("Db") ? attrs.get("Db") : "";
		String params= attrs.containsKey("Params") ? attrs.get("Params") : "";
		if (value.indexOf("[")==0) {
			Integer end=value.indexOf("]:");
			db=value.substring(1,end);
			value=value.substring(db.length()+3);
		}
		//if (to.length()==0)
			//return "<DO_SQL> To=\"*\" should be set";
		if (type.length()==0)
			return "<DO_SQL> Type=\"*\" should be 'query|update'";
		if (trans.length()==0)
			return "<DO_SQL> Trans=\"*\" should be 'xml|list|map'";
		if (value==null || value.length()==0)
			return "<DO_SQL>***</DO_SQL> should be set as SQL !";
		
		//打开数据库
		Connection db_conn=null;
		if (db=="") {//取得缺省db
			for (Map.Entry<String, Connection> entry : this.dbConns.entrySet()) { 
				  //System.out.println("Key = " + entry.getKey() + ", Value = " + entry.getValue()); 
				db_conn=entry.getValue();
				break;
			}
		}else 
			db_conn=this.dbConns.get(db);
		
		//check db connection
		/*try {
			if (db_conn.isClosed()) {
				this.GetDBConn();//重新加载数据库
				return "<DO_SQL> connection closed reload now!!!";
			}
		} catch (SQLException e1) {
			this.GetDBConn();//重新加载数据库
			e1.printStackTrace();
			return "<DO_SQL> connection closed reload now!!!";
		}*/
		
		if (db_conn==null)
			return "<DO_SQL> db not opened or error!";
		
		//动作处理
		if (type.equals("query")==true){
			try {
				Statement stat=db_conn.createStatement();
				outer:
				for(int dd=0; dd<1; dd++) {
					ResultSet rs=stat.executeQuery(value);
					List<Map<String, Object>> list=getListFromRs_Object(rs);//取得结果集row_map
					if (total.length()!=0 && list!=null) {
						itotal=list.size();
						paras_map.put(total,itotal.toString());//ly new2
					}
					if (trans.equals("xml")==true)
						paras_map.put(to,ListMapToXmlStr(list,null,null)); //返回row_map到指定变量,暂时没有变量传输
					else if (trans.equals("json")==true)
						paras_map.put(to,ListMapToJsonStr(list,null,null)); //返回row_map到指定变量,暂时没有变量传输
					else if (trans.equals("group")) {
						return "<DO_SQL> db not opened or error! not support";
					}else if (trans.equals("array")) {
						String split=",",dest_str="";
						Integer i;
						for(i=0; i<list.size(); i++) {
							Map<String, Object> cell=list.get(i);
							if (cell==null || !(cell instanceof Map)) {
								err="<DO_SQL> Trans=\"*\" 'array:,' obj is not map!";
								break outer;
							}
							dest_str+="[";
							Iterator<Map.Entry<String, Object>> it = cell.entrySet().iterator();
						    while (it.hasNext()) {
						    		Map.Entry<String, Object> entry = it.next();
						    		if (entry!=null && (entry.getValue())!=null)
						    		 	dest_str+="\""+entry.getValue()+"\"";
						    		else
						    			dest_str+="\"\"";
						    		dest_str+=it.hasNext()? split: "";
						    }
						    dest_str+="]";
						    if (i<(list.size()-1))
						    		dest_str+=",";
						}
						paras_map.put(to,dest_str); //返回row_map到指定变量
					}else if (trans.equals("list")==true)
						paras_map.put(to,list); //返回row_map到指定变量
					else if (trans.indexOf("map")==0) {//创建key类数组 //ly_sync
						//取得key_valu
						String[] map_para=trans.split(":");
						if (map_para==null || map_para.length<2) {
							err="<DO_SQL> Trans=\"*\" should be 'map:key'";
							break outer;
						}
						if (list.size()>0 && !(list.get(0) instanceof Map)) {
							err="<DO_SQL> Trans=\"*\" 'map.value' value is not map!";
							break outer;  
						}
						String map_key = map_para[1];
						Map<String, Object> map_arr = new HashMap<String, Object>(); 
						Integer i;
						for(i=0; i<list.size(); i++) {
							Object cell=list.get(i);
							if (cell==null || !(cell instanceof Map)) {
								err="<DO_SQL> Trans=\"*\" 'map:key' obj is not map!";
								break outer;
							}
							@SuppressWarnings("unchecked")
							Object key_value;
							if (map_key.equals("#")) {//ly model
								key_value=""+i;  //以记录编号为核心
							}else
								key_value=((Map<String,Object>)cell).get(map_key);
							if (key_value==null) {
								err="<DO_SQL> Trans=\"map:"+map_key+"\" invalid key("+map_key+")!";
								break outer;
							}
							map_arr.put(key_value.toString(), list.get(i));
						}
						if (map_arr.size() < (i-1))
							System.out.println("WARN: !!! <DO_SQL>: trans=\"map\" may has duplicate key!");
						
						paras_map.put(to,map_arr); //返回row_map到指定变量
						//建立key_value的map数组
					}else {
						err="<DO_SQL> Trans=\"*\" should be 'xml|list|map'";
						break outer;
					}
				}
				stat.close();
			} catch (SQLException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
				PRINT(value);
				err=e.toString();
			}
		}else if(type.equals("update")){
			if (params.indexOf("return_keys")>=0) {
				try {
					PreparedStatement stat = db_conn.prepareStatement(value, Statement.RETURN_GENERATED_KEYS);
					int r=stat.executeUpdate();
					if (r<0)
						paras_map.put(to,"null"); //返回row_map到指定变量
					else {
						//成功
						Serializable ret = null;
						ResultSet rs=null;
						rs = stat.getGeneratedKeys();
					    if (rs!=null && rs.next()) {
					        ret = (Serializable) rs.getObject(1);
					    } 
					    paras_map.put(to,""+ret); //返回row_map到指定变量
					}
					stat.close();
				} catch (SQLException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
					PRINT(value);
					err=e.toString();
				}
			}else {
				try {
					Statement stat = db_conn.createStatement();
					int r=stat.executeUpdate(value);
					paras_map.put(to,""+r); //返回row_map到指定变量
					stat.close();
				} catch (SQLException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
					PRINT(value);
					err=e.toString();
				}
			}
		}else
			err="<DO_SQL> attribute->Type should be set to 'query|update'";
		
		if ((err.indexOf("关闭的连接")>=0) || (err.indexOf("Operation timed out")>=0) || (err.indexOf("Closed Connection")>=0)) {//测试数据库是否关闭
			this.GetDBConn();//重新加载数据库
			return "<DO_SQL> connection closed reload now!!!";
		}
		
		return err;
	}
	
	//DO_SQL_XML把HASHMAP里面数据封装成XML
	public String ListMapToXmlStr(List<Map<String, Object>> list, Map<String, Object> add_paras,String add_head_str) {
		if (list==null)
			return "";
		
		StringBuffer sb = new StringBuffer();
		if (add_head_str!=null)
			sb.append(add_head_str);
		if (add_paras!=null) {
			sb.append("<_grparam>\n");
			sb.append(getGridStrByMap_xml(add_paras));
			sb.append("</_grparam>\n");
		}
		sb.append("<result>\n");
		try {
			for (int i = 0; i < list.size(); i++) {
				sb.append("<row>\n");
				Map<String, Object> hashMap = list.get(i);
				sb.append(getGridStrByMap_xml(hashMap));
				sb.append("</row>\n");
			} 
		} catch (Exception e) {
			// TODO: handle exception
			e.printStackTrace();
		}
		sb.append("</result>\n");
		return sb.toString();
	}
	//DO_SQL_XML把HASHMAP里面数据封装成XML
	public String ListMapToJsonStr(List<Map<String, Object>> list, Map<String, Object> add_paras,String add_head_str) {
			if (list==null)
				return "";
			
			StringBuffer sb = new StringBuffer();
			if (add_head_str!=null)
				sb.append(add_head_str);
			if (add_paras!=null) {
				sb.append("grparam:{");
				sb.append(getGridStrByMap_json(add_paras));
				sb.append("},\n");
			}
			sb.append("[\n");
			try {
				for (int i = 0; i < list.size(); i++) {
					sb.append("{");
					Map<String, Object> hashMap = list.get(i);
					sb.append(getGridStrByMap_json(hashMap));
					if (i==(list.size()-1))
						sb.append("}\n");
					else
						sb.append("},\n");
				} 
			} catch (Exception e) {
				// TODO: handle exception
				e.printStackTrace();
			}
			sb.append("]");
			return sb.toString();
	}
	//DO_SQL_xml:把HASHMAP里面数据封装成XML
	public String getGridStrByMap_xml(Map<String, Object> hashMap) {
		String returnStr = null;
		if (hashMap.isEmpty()) {
			returnStr =  "";
		}else{
			StringBuffer strBuffer = new StringBuffer();
			Iterator iterator = hashMap.keySet().iterator();
			while (iterator.hasNext()) {
				String key = (String) iterator.next();
				Object value = hashMap.get(key);
				hashMap.put(key, (value == null ? "" : value.toString())); //这句有些多余，应在外边操作。？？？
				strBuffer.append("<" + key + "><![CDATA["+ (value == null ? "" : value) + "]]></" + key + ">\n");
			}
			returnStr = strBuffer.toString();
		}
		return returnStr;
	}
	
	//DO_SQL_xml:把HASHMAP里面数据封装成XML
	public String getGridStrByMap_json(Map<String, Object> hashMap) {
			String returnStr = null;
			if (hashMap.isEmpty()) {
				returnStr =  "";
			}else{
				StringBuffer strBuffer = new StringBuffer();
				Iterator iterator = hashMap.keySet().iterator();
				while (iterator.hasNext()) {
					String key = (String) iterator.next();
					Object value = hashMap.get(key);
					//hashMap.put(key, (value == null ? "" : value.toString()));  //这句有些多余
					strBuffer.append("\""+key + "\":"+ (value == null ? "\"\"" : "\""+value+"\"") + ",");
				}
				if (strBuffer.length()!=0)
					strBuffer.deleteCharAt(strBuffer.length()-1);
				returnStr = strBuffer.toString();
			}
			return returnStr;
	}
	
	//DO_SQL_LIST:仿照：https://www.cnblogs.com/wangfeng520/p/5457277.html
	public List<Map<String, Object>> getListFromRs_Object(ResultSet rs) throws SQLException{  
		Integer rec=0;
        ResultSetMetaData md = rs.getMetaData();//得到结果集列的属性  
        int columns = md.getColumnCount();//得到记录有多少列  
        int i;  
        List<Map<String, Object>> list = new ArrayList<Map<String, Object>>();  
        
        while(rs.next()){  
        		rec++;
            Map<String, Object> map = new HashMap<String, Object>();  
            for(i = 0; i < columns; i++){ 
            		int col_type=md.getColumnType(i + 1);
            		String col_name=md.getColumnName(i + 1);
            		map.put(col_name, getValueByType(rs, col_type, col_name));  
            }  
            list.add(map);  
        }  
        return list;  
    } 
	//DO_SQL_LIST:
	public List<Map<String, String>> getListFromRs_String(ResultSet rs) throws SQLException{  
        ResultSetMetaData md = rs.getMetaData();//得到结果集列的属性  
        int columns = md.getColumnCount();//得到记录有多少列  
        int i;  
        List<Map<String, String>> list = new ArrayList<Map<String, String>>();  
        while(rs.next()){  
            Map<String, String> map = new HashMap<String, String>();  
            for(i = 0; i < columns; i++){ 
            		map.put(md.getColumnName(i + 1), getValueByType(rs, md.getColumnType(i + 1), md.getColumnName(i + 1)).toString());  
            }  
            list.add(map);  
        }  
        return list;  
    }  
	//DO_SQL_list:
	private Object getValueByType(ResultSet rs, int type, String name) throws SQLException{  
		int i=type;
        switch(i){  
            case Types.NUMERIC:  
                    return rs.getLong(name);                  
            case Types.VARCHAR:  
                //if(rs.getString(name)==null){  
                    //return "";  
                //}  
                return rs.getString(name);  
            case Types.DATE:  
                //if(rs.getDate(name)==null){  
                    //return System.currentTimeMillis();  
            //  }  
                return rs.getDate(name);  
            case Types.TIMESTAMP:  
            		Timestamp t=rs.getTimestamp(name);
            		if (t==null)
            			return null;
            		else
            			return t.toString().substring(0,t.toString().length()-2);  
            case Types.INTEGER:  
                return rs.getInt(name);  
            case Types.DOUBLE:  
                return rs.getDouble(name);  
            case Types.FLOAT:  
                return rs.getFloat(name);  
            case Types.BIGINT:  
                return rs.getLong(name);  
            default:  
                return rs.getObject(name);  
        }  
    }  
	
	public String GetDBConn() {
	//public String GetDBConn(Node parseNode) throws SQLException {	
		if (this.dbConns!=null) {
			this.CloseAllDBConn();
			this.dbConns=null;
		}
		
		String db_name=null;
		Map<String, Connection> map = new HashMap<String, Connection>(); 
		NodeList nodes = this.dbNode.getChildNodes();
		//数据库集打开操作
		if(nodes!=null){
			for (int i = 0; i < nodes.getLength(); i++){
				Node node = nodes.item(i);
				if(node.getNodeType()==Node.ELEMENT_NODE) {
					//取得数据库配置节点
					db_name=node.getNodeName();
					if (map.containsKey(db_name)) {
						return "DBCfg->'"+db_name+"' has already exited!";
					}
					NodeList nodes2 = node.getChildNodes();
					Connection db_conn=null;
					if(nodes2!=null){
						//打开数据库 
						String dbURL=null,userName=null,userPwd=null,driver=null;
						for (int j = 0; j < nodes2.getLength(); j++) {
							Node node2 = nodes2.item(j);
							if(node2.getNodeType()==Node.ELEMENT_NODE) {//取得数据库配置节点
								//解析内容
								switch (node2.getNodeName()) {
								case "dbURL":
									dbURL=Xml_getNodeValue(node2);
									break;
								case "userName":
									userName=Xml_getNodeValue(node2);
									break;
								case "userPwd":
									userPwd=Xml_getNodeValue(node2);
									break;
								case "driver":
									driver=Xml_getNodeValue(node2);
									break;
								default:
								}
							}
						}
						if (dbURL==null || userName==null || userPwd==null || driver==null) {
							return "DBCfg->'"+db_name+"'->'dbUrl'|'userName'|'userPwd'|'driver' not defined!";
						}
						//测试是否存在driver
						try {
							Class.forName(driver);
						} catch (ClassNotFoundException e1) {
							// TODO Auto-generated catch block
							e1.printStackTrace();
							return "DBCfg->'"+db_name+"'->driver class error!";
						}
						try {
							//打开数据库
							System.out.println("DBConnection...dbUrl:"+dbURL+",userName:"+userName);
							db_conn=DriverManager.getConnection(dbURL, userName, userPwd);
							System.out.println("DBConnected!");
						} catch (Exception e2) {
							// TODO Auto-generated catch block
							e2.printStackTrace();
							return "DBCfg->'"+db_name+"' connecting error!";
						}
						//添加数组
						map.put(db_name, db_conn);
					}else
						return "DBCfg->'"+db_name+"' error defined!";
				}
			}
		}else
			return "No any DB Nodes has defined!";
		
		//全部成功再添加dbConns？？？注意数据库断连之后的处理。
		if (map.size()!=0)
			this.dbConns=map;
		else
			return "DBCfg->Not any DB Nodes has defined!";
		return "";
	}
	
	public void CloseAllDBConn() {
		//打开数据库
		Connection db_conn=null;
		if (this.dbConns!=null) {//取得缺省db
			for (Map.Entry<String, Connection> entry : this.dbConns.entrySet()) { 
				System.out.println("Closing db...."); 
				db_conn=entry.getValue();
				try {
					if (!db_conn.isClosed())
						db_conn.close();
				} catch (SQLException e) {
					System.out.println("Error: Closing db ->"); 
					e.printStackTrace();
				}
				break;
			}
		}
	}
	
	//解析内容字符串@param_map; #param_map
	public String ParseValue(String value, Map<String, Object>paras_map, Map<String, Object>row_paras,String[] clear){
		String err="";
		String result=value;
		//init
		paras_map.put("@_@LAST_ERROR","");
		//paras_map.put("@_@THIS_CFG",(Object)this.ConfigFile);
		//参数替换，如果{@***}为对应的参数值。??今后再次改进，需要看看字段是否存在否则返回null
		if (paras_map!=null) {
			Iterator<Map.Entry<String, Object>> it = paras_map.entrySet().iterator();
		    while (it.hasNext()) {
		    		Map.Entry<String, Object> entry = it.next();
		    		if (entry!=null && (entry.getValue())!=null)
		    		 	result=result.replaceAll("\\{"+entry.getKey()+"\\}",""+entry.getValue());
		    }
		}
		
		//替换#参数
		if (row_paras!=null) {//step1
			Iterator<Map.Entry<String, Object>> it = row_paras.entrySet().iterator();
		    while (it.hasNext()) {
		    		Map.Entry<String, Object> entry = it.next();
		    		//if (entry!=null && (entry.getValue())!=null) {
		    		if (entry!=null) {
		    			String v=entry.getValue()==null ? "" :entry.getValue().toString();
		    			result=result.replaceAll("\\{#_"+entry.getKey()+"\\}",v);
		    			result=result.replaceAll("\\{#_.\\}", v);//这要求list生成对象直接生成.为key
		    		}
		    }
		}
		
		//替换特殊字符
		if (result.indexOf("{0x5c22}")>=0)
			result=result.replace("{0x5c22}", "\\\"");
		
		//替换字段 //ly_sync
		String obj_str=""; //此处可以提速，代替前面的替换方式。
		Integer	a=0,b=0,c=0;
		while(true) {//JSONPath 查询;!!!必须确保每次result消减。
			//读取吓一跳记录
			String replace_str="";
			a=result.indexOf("{@_",c);
			b=result.indexOf("}",c);
			if (a>b || a<0 || b<0){
				break;
			}
			obj_str=result.substring(a+1,b); //a确保下次查找的时候递进一位，不会反复替换当前字段，如果被踢换的字段就是自己。
			//System.out.println(obj_str);
			//记录替换处理
			if (obj_str.indexOf("@_JPATH")==0) {
				//分析语法 <PRINT>{JPATH:@_wx_user_json:$.errcode}</PRINT>
				String[] cells=obj_str.split(":");
				if (cells.length <2) {
					err="syntax error (JPATH): JPATH:obj_name:jpath string";
					break;
				}
				Object obj=null,from_obj=paras_map.get(cells[1]);
				//取得json_obj
				if (from_obj instanceof JSONObject) {
					obj=from_obj;
				}else if (from_obj  instanceof String)
					obj = JSON.parseObject((String)from_obj);
				else {
					err="JPATH: from ("+from_obj+") value should be JSONObject or String!";
					break;
				}
				//jpath operation
				try {
					replace_str=""+JSONPath.eval(obj, cells[2]);
				}catch(Exception e){
					e.printStackTrace();
					err="Eval jsonPath("+cells[2]+") error!";
					break;
				}
			}else if (obj_str.indexOf("@_EXIT?")>=0){
				String [] items=obj_str.substring(7).split(":");
				if (items.length <2) {
					err="syntax error (EXIT): {@_EXIT?item1:item2...}";
					break;
				}	
				replace_str="";
				for(int i=0; i<items.length; i++) {
					String temp=items[i].trim();
					if (temp.length()==1)
						continue;
					if (temp.indexOf("'")==0 && temp.lastIndexOf("'")==(temp.length()-1)) {
						String temp1=temp.substring(1);
						replace_str=temp1.substring(0,temp1.length()-1);
					}else if(IsUrlField(temp,paras_map)){
						replace_str=ParseValue("{"+temp+"}",paras_map,null,null);
					}else if (paras_map.containsKey(temp))
						replace_str=(String)paras_map.get(temp);
					if (replace_str.length()>0)
						break;
				}
			}else if (obj_str.indexOf("@_PARSE2")>=0){//ly session 不很规范
				Object dest_obj=paras_map.get(obj_str.substring(7));
				if (dest_obj==null) {
					err="ParseValue():"+"{"+obj_str+"}"+" @_field not found!";
				}else {
					String dest_obj_str=dest_obj.toString();
					replace_str=ParseValue(dest_obj_str,paras_map,null,null);
				}
			}else  if (obj_str.indexOf(".")>0) {//***.***递进查询
				String[] cells=obj_str.split("[.]");
				Map<String, Object> this_obj=paras_map;
				for (int i=0; i<cells.length; i++) {//循环***.***从相应的paras_map中取数据
					if (cells[i].indexOf("|")>0) {
						String [] fields=cells[i].split("[|]");
						for(int j=0; j<fields.length; j++) {
							replace_str+=""+this_obj.get(fields[j])+";";
						}
						replace_str=replace_str.substring(0, replace_str.length()-1);	
						break;
					}else {
						Object cell_obj=this_obj.get(cells[i]);
						if (cell_obj instanceof Map) {
							this_obj=(Map<String, Object>)cell_obj;
							if (i==(cells.length-1)) {//***.***(map)非最后内容
								//result=result.replace("{"+obj_str+"}",this_obj.toString());
								replace_str=this_obj.toString();
								break;
							}else
								continue;
						}else if (cell_obj==null) { 
							//result=result.replace("{"+obj_str+"}","");
							replace_str="";
							break;
						}else {
							//result=result.replace("{"+obj_str+"}",cell_obj.toString());
							replace_str=cell_obj.toString();
							break;
						}
					}
				}
			}else {//普通字段。
				Object dest_obj=paras_map.get(obj_str);
				if (dest_obj==null) {
					err="ParseValue():"+"{"+obj_str+"}"+" @_field not found!";
					//result=result.replace("{"+obj_str+"}","null");
					if(obj_str.indexOf("@_@")==0)
						replace_str="{"+obj_str+"}"; //"null";
					else
						replace_str="null";
				}else {
					//result=result.replace("{"+obj_str+"}",dest_obj.toString());
					replace_str=dest_obj.toString();
				}
			}
			//统一替换操作
			result=result.replace("{"+obj_str+"}",replace_str);
			c=a+replace_str.length();//从下一个字段开始解析，确保每个字段只解析一遍。
		}
		
		//替换clear
		if (clear!=null) {
			for(int i=0; i<clear.length; i++) {
				result=result.replace(clear[i]," ");
			}
			result=result.trim();
		}
		//补充字符串
		if (result.indexOf("{0x20}")>=0)
			result=result.replace("{0x20}", " ");
		if (result.indexOf("{0x0d0a}")>=0)
			result=result.replace("{0x0d0a}", "\r\n");
		if (err.length()!=0) {
			//System.out.println(err);
			paras_map.put("@_@LAST_ERROR",err);
		}
	    return result;
	}
	private String _ParseValueNext(String value) {//此处可以提速，应该为顺序解析，并代替前面的替换方式。
		Integer a=0,b=0;
		a=value.indexOf("{@_");
		b=value.indexOf("}");
		if (a>b || a<0 || b<0){
			//System.out.println("Warn(ParseValue): string should be {{**}} !");
			return null;//”}{“
		}
		return value.substring(a+1,b);
	}
	
	public String ParsValue_JSON(String value, Map<String, Object>paras_map, Map<String, Object>row_paras){
		//参数替换，如果{@***}为对应的参数值。??今后再次改进，需要看看字段是否存在否则返回null
		if (paras_map!=null) {
			Iterator<Map.Entry<String, Object>> it = paras_map.entrySet().iterator();
		    while (it.hasNext()) {
		    		Map.Entry<String, Object> entry = it.next();
		    		String key=entry.getKey();
		    		String []key_arr=key.split(":");
		    		if (key_arr.length==1)
		    			value.replaceAll("@_"+key_arr[0],entry.getValue().toString());
		    		else {//JSON
		    			Object tmp=entry.getValue();
		    		}
		    }
		}
		if (row_paras!=null) {
			Iterator<Map.Entry<String, Object>> it = row_paras.entrySet().iterator();
		    while (it.hasNext()) {
		    		Map.Entry<String, Object> entry = it.next();
		    		value.replaceAll("#_"+entry.getKey(),entry.getValue().toString());
		    		value.replaceAll("#_.",entry.getValue().toString());
		    }
		}
	    return value;
	}
	
	     
	//取得WebService 节点
	public Node getWebServiceCfg(String act_file,String act){
		//打开文件
		if (this.ConfigFile==null || this.ConfigFileName!=act_file) {//打开一次，文件名没有变化则不再打开
			try{
				DocumentBuilderFactory domfac=DocumentBuilderFactory.newInstance();
				DocumentBuilder dombuilder=domfac.newDocumentBuilder();
				InputStream is = WebService.class.getClassLoader().getResourceAsStream(act_file);
				this.ConfigFile=dombuilder.parse(is);
				this.ConfigFileName=act_file;
			}catch(Exception e){
				e.printStackTrace();
				return null;
			}
		}
		//查询节点
		try{	
			Element root = this.ConfigFile.getDocumentElement();
			NodeList nodes = root.getChildNodes();
			if(nodes!=null){
				for (int i = 0; i < nodes.getLength(); i++){
					Node node = nodes.item(i);
					if(node.getNodeType()==Node.ELEMENT_NODE) { 
						if(node.getNodeName().equals(act)==true)
							return node; 
					}
				}
			}
		}catch(Exception e){
			e.printStackTrace();
			return null;
		}
		return null;
	}
	
	public Document Xml_getDocument(String act_file){
		//打开文件
		try{
			DocumentBuilderFactory domfac=DocumentBuilderFactory.newInstance();
			DocumentBuilder dombuilder=domfac.newDocumentBuilder();
			InputStream is = WebService.class.getClassLoader().getResourceAsStream(act_file);
			Document doc=dombuilder.parse(is);
			return doc;
		}catch(Exception e){
			e.printStackTrace();
			return null;
		}
	}
	
	//取得WebService 节点
	public NodeList Xml_getXpathNodes(Document doc,String xpath){//ly xpath
		//打开文件
		if (this.Xpath==null ) {//打开一次，文件名没有变化则不再打开
			try{
				XPathFactory factory = XPathFactory.newInstance();
		        this.Xpath = factory.newXPath();
			}catch(Exception e){
				e.printStackTrace();
				return null;
			}
		}
		//查询节点
		try{	
			XPathExpression exps = this.Xpath.compile(""+xpath);
	        NodeList nodeList = (NodeList) exps.evaluate(doc, XPathConstants.NODESET);
	        
	        return nodeList;
		}catch(Exception e){
			e.printStackTrace();
			return null;
		}
	}
	
	//取得WebService 节点
	public String Xml_getXpathValue(Document doc,String xpath){//ly xpath
		NodeList nodes;
		if ((nodes=this.Xml_getXpathNodes(doc,xpath))!=null) {
			for (int i = 0; i < nodes.getLength(); i++) {
				if(nodes.item(i).getNodeType()==Node.ELEMENT_NODE) { 
					return this.Xml_getNodeValue(nodes.item(i));
				}
			}
		}
		return null;
	}
	
	public String Xml_getNodeValue(Node node2) {
		if (node2!=null &&(node2.getFirstChild())!=null) {
			return node2.getFirstChild().getNodeValue();
		}
		return null;
	}
	
	public Object __GetTableauTicket(Map<String,String> paras, Map<String,Object> paras_map) {
		//String token = getTrustedTicket((String)p.get("host"), (String)p.get("user"),(String)p.get("host"), (String)p.get("uri"), (String)p.get("site"));
		//取得参数List
		if (!paras.containsKey("Url")) {
			this.CallError="Call->GetTableauTicket->Url should be set!"; 
			return null;
		}
		String url=paras.get("Url");
		//
		if (!paras.containsKey("User")) {
			this.CallError="Call->GetTableauTicket->User should be set!"; 
			return null;
		}
		String user=paras.get("User");
		//
		if (!paras.containsKey("Site")) {
			this.CallError="Call->GetTableauTicket->Site should be set!"; 
			return null;
		}
		String site=paras.get("Site");
		//
		if (!paras.containsKey("RemoteAddr")) {
			this.CallError="Call->GetTableauTicket->RemoteAddr should be set!"; 
			return null;
		}
		String remote_addr=paras.get("RemoteAddr");

		//处理
		String token = getTrustedTicket(url, user,site,remote_addr);
		return token;
	}
	

	public String getTrustedTicket(String url_str, String user,String Site,String remoteAddr) {
		OutputStreamWriter out = null;
		BufferedReader in = null;
		String result = "";
		HttpURLConnection conn;

		try {
			// Encode the parameters
			StringBuffer data = new StringBuffer();
			data.append(URLEncoder.encode("username", "UTF-8"));
			data.append("=");
			data.append(URLEncoder.encode(user, "UTF-8"));
			data.append("&");
			data.append(URLEncoder.encode("client_ip", "UTF-8"));
			data.append("=");
			data.append(URLEncoder.encode(remoteAddr, "UTF-8"));
			data.append("&");
			data.append(URLEncoder.encode("target_site", "UTF-8"));
			data.append("=");
			data.append(URLEncoder.encode(Site, "UTF-8"));

			//URL url = new URL(HttpType + wgserver + "/trusted");// 这里SSL注意
			URL url = new URL(url_str);// 这里SSL注意
			// 通过请求地址判断请求类型(http或者是https)
			if (url.getProtocol().toLowerCase().equals("https")) {
				HttpsURLConnection https = (HttpsURLConnection) url
						.openConnection();
				https.setHostnameVerifier(DO_NOT_VERIFY);
				conn = https;
			} else {
				conn = (HttpURLConnection) url.openConnection();
			}
			
			System.out.println(url_str+"?"+data);

			conn.setDoOutput(true);
			out = new OutputStreamWriter(conn.getOutputStream());
			out.write(data.toString());

			out.flush();
			// Read the response
			StringBuffer rsp = new StringBuffer();
			in = new BufferedReader(
					new InputStreamReader(conn.getInputStream()));
			String line;
			while ((line = in.readLine()) != null) {
				rsp.append(line);
			}

			return rsp.toString();
		} catch (Exception e) {
			System.out.println(e);
			return "-1";
		} finally {
			try {
				if (in != null)
					in.close();
				if (out != null)
					out.close();
			} catch (IOException e) {
			}
		}
	}
	
	public static String sendHtpp(String content, String url, Map head_map,String type, String method) {
		String result = "";
		OutputStreamWriter out = null;
		BufferedReader in = null;
		HttpURLConnection conn;
		try {
			//trustAllHosts();
			URL realUrl = new URL(url);
			// 通过请求地址判断请求类型(http或者是https)
			if (realUrl.getProtocol().toLowerCase().equals("https")) {
				trustAllHosts();
				HttpsURLConnection https = (HttpsURLConnection) realUrl
						.openConnection();
				https.setHostnameVerifier(DO_NOT_VERIFY);
				conn = https;
			} else {
				conn = (HttpURLConnection) realUrl.openConnection();
			}
	
			// 设置通用的请求属性
			//conn.setRequestProperty("accept", "*/*");
			if ((method!=null) && (method.equals("")!=true))
				conn.setRequestMethod(method); //set Method;
			
			conn.setRequestProperty("connection", "Keep-Alive");
			conn.setRequestProperty("user-agent",
					"Mozilla/4.0 (compatible; MSIE 6.0; Windows NT 5.1;SV1)");
			if (type=="json") {
				conn.setRequestProperty("Content-Type", "application/json");   //发送报文类型
				conn.setRequestProperty("Accept", "application/json");
			}else {
	        		conn.setRequestProperty("Content-Type", "application/xml");   //发送报文类型
	        		conn.setRequestProperty("Accept", "application/xml");
	        }
			
			//补充add_header
			if (head_map!=null) {
				for(Object key:head_map.keySet()){//keySet获取map集合key的集合  然后在遍历key即可
					String value = head_map.get(key).toString();
//					if (((String)key).equals("X-SAP-LogonToken")) {
//						conn.setRequestProperty((String)key, "\""+value+"\"");
//					}else
						conn.setRequestProperty((String)key, value);
				}
			}

			// 发送POST请求必须设置如下两行
			if (method.equals("POST")==true) {
				conn.setDoOutput(true);
				conn.setDoInput(true);
				out = new OutputStreamWriter(conn.getOutputStream(), "utf-8");
				if (content!=null)
					out.write(content);
				else
					out.write("");
				out.flush();
			}else {
				conn.connect();
			}
			
			// 定义BufferedReader输入流来读取URL的响应
			in = new BufferedReader(
					new InputStreamReader(conn.getInputStream()));
			String line;
			while ((line = in.readLine()) != null) {
				result += line;
			}
		} catch (Exception e) {
			e.printStackTrace();
			result="[\"error:\",\""+e+"\"]";
		} finally {// 使用finally块来关闭输出流、输入流
			try {
				if (out != null) {
					out.close();
				}
				if (in != null) {
					in.close();
				}
			} catch (IOException ex) {
				ex.printStackTrace();
			}
		}
		return result;
	}

	private final static HostnameVerifier DO_NOT_VERIFY = new HostnameVerifier() {
		public boolean verify(String hostname, SSLSession session) {
			return true;
		}
	};

	private static void trustAllHosts() {
		// Create a trust manager that does not validate certificate chains
		TrustManager[] trustAllCerts = new TrustManager[] { new X509TrustManager() {
			public java.security.cert.X509Certificate[] getAcceptedIssuers() {
				return new java.security.cert.X509Certificate[] {};
			}

			public void checkClientTrusted(X509Certificate[] chain,
					String authType) {
			}

			public void checkServerTrusted(X509Certificate[] chain,
					String authType) {
			}
		} };
		// Install the all-trusting trust manager
		try {
			SSLContext sc = SSLContext.getInstance("TLS");
			sc.init(null, trustAllCerts, new java.security.SecureRandom());
			HttpsURLConnection
					.setDefaultSSLSocketFactory(sc.getSocketFactory());
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	
}
