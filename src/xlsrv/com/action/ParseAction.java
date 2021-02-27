package xlsrv.com.action;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.sql.Types;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONException;
import com.alibaba.fastjson.JSONObject;
import com.alibaba.fastjson.JSONPath;

public class ParseAction {
	private Map<String,Connection> dbConns=null;
	private PreparedStatement[] pStemt = null;
	private int ParseDeep;
	private Boolean is_debug=false;
	private String clearStr=null;

	public String ParseAction(Node parseNode,Map<String, Object> paras_map){
		this.ParseDeep++; //用于记录解析层次深度.tool = tool.next_sibling()
		
		String err_str="";
		//测试是否跟踪
		Map<String,String> parent_attrs=getParseNodeAttr(parseNode);
		String debug_string=parent_attrs.get("Debug");
		this.is_debug=false;
		if (debug_string.equals("Yes")==true) {
			is_debug=true;
		}
		//测试是否情况"/n;/t"
		this.clearStr=parent_attrs.get("Clear");
		String[] clear=this.clearStr.split(";");
		
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
							err_str=PRINT(node,value,attrs,paras_map);
							break;
						case "JPATH":
							err_str=JPATH(node,value,attrs,paras_map);
							break;
						case "RETURN":
							err_str = RETURN(node,value,attrs,paras_map);
							return "";//返回
						default:
							err_str = (String)"ParseError: node ("+node.getNodeName()+") not defined!";
						}
						//跟踪结果
						if (is_debug && attrs.containsKey("Debug") && attrs.get("Debug").equals(".")) {
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
		//如果定义则判断。
		if (value.indexOf("==")==-1 && value.indexOf("!=")==-1) {
			//为空则错
			if (value.indexOf("!")==0){
				if (value.length()==0)
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
				if ((value.equals("==")) || (param.length>1 && param[0].equals(param[1])))
					return true;
				else
					return false;
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
		paras_map.put("@@_ParseResult",value);
		return "";
	}
	
	//RETURN 动作
	public String PRINT (Node parseNode, String value, Map<String,String> attrs, Map<String, Object> paras_map){ 
		//递进
		String deep_str="";
		for (int i = 0; i<this.ParseDeep; i++) {
	        deep_str += "    ";
	    }
		System.out.println(deep_str+value);
		return "";
	}
	
	public String CALL (Node parseNode, Map<String,String> attrs, Map<String, Object> paras_map, Map<String, Object> row_map,String[] clear){ 
		//取得属性
		if (attrs==null)
			return "<CALL> Method=\"*\" should be set to your call function";
		String to= attrs.containsKey("To") ? attrs.get("To") : "";
		String obj_str= attrs.containsKey("Method") ? attrs.get("Method") : "";
		if (to.length()==0)
			return "<CALL> To=\"*\" should be set!";
		if (obj_str.length()==0)
			return "<CALL> Method=\"*\" should be set your call function!";
		//解析对象.方法
		String[] obj_method=obj_str.split("\\.");
		String obj=obj_method[0];
		String method=obj_method.length >1 ? obj_method[1] : "";
		Map<String, String> func_params = new HashMap<String, String>();
		if (method.length()!=0) 
			func_params.put("__method",method);
				
		//取得参数
		NodeList nodes = parseNode.getChildNodes();
		if(nodes!=null){
			for (int i = 0; i < nodes.getLength(); i++){
				Node node = nodes.item(i);
				if(node.getNodeType()==Node.ELEMENT_NODE) {
					String param_value=Xml_getNodeValue(node);
					param_value=this.ParseValue(param_value, paras_map, row_map, clear);
					func_params.put(node.getNodeName(),param_value);
				}
			}
		}
		//操作函数
		String err="";
		Object r_s=null;
		try {
    			Method return_func=this.getClass().getMethod("__"+obj, Map.class);
	    		r_s=return_func.invoke(this, func_params);
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
		if (err==null || err.length()==0) {
			paras_map.put(to,r_s);
		}else {
			return err;
		}
		return "";
	}
	
	public Object __WxUtil(Map<String,String> paras) {
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
	
	//SET 动作
	public String SET(Node parseNode, String value, Map<String,String> attrs, Map<String, Object> paras_map){ 
		//取得属性
		if (attrs==null)
			return "<SET> should set attribute(To)";
		String to= attrs.containsKey("To") ? attrs.get("To") : "";
		String method= attrs.containsKey("Method") ? attrs.get("Method") : "";
		if (to.length()==0)
			return "<SET> should set attribute(To)!";
		
		//写入值;
		if (method.length()!=0) {
			//通过函数计算
			String err="",r_s="";	
			try {
	    			Method return_func=this.getClass().getMethod(method, String.class);
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
			if (err==null) {
				paras_map.put(to,r_s);
			}else {
				return err;
			}
		}else
			paras_map.put(to,value==null ? "" : value);
		return "";
	}
		
	//XSLT 动作
	public String XSLT(Node parseNode, String value, Map<String,String> attrs, Map<String, Object> paras_map){ 
		//取得属性
		if (attrs==null)
			return "<XSLT>To=\"*\" should be set";
		String to= attrs.containsKey("To") ? attrs.get("To") : "";
		String path= attrs.containsKey("Path") ? attrs.get("Path") : "";
		String act= attrs.containsKey("Act") ? attrs.get("Act") : "";
		if (to.length()==0)
			return "<XSLT> attribute(To) should be set!";
		if (path.length()==0)
			return "<XSLT> attribute(Path) should be set!";
		if (value==null && value.length()==0)
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
		    	String err="";
		    	String destXml=transXmlByXslt(value,xslt,err);
		    if (destXml.length()==0 && err.length()!=0) 
		    		return "xslt transform error("+err+")"; 
		    else 
		    		destXml=addXsltAct(destXml);
		    return "";
		}else
			return "attribute path must be set!";
	}
	
	public String JPATH(Node parseNode, String value, Map<String,String> attrs, Map<String, Object> paras_map){ 
		//取得属性
		if (attrs==null)
			return "<JPATH> attribute(To) should be set!";
		String to= attrs.containsKey("To") ? attrs.get("To") : "";
		String from= attrs.containsKey("From") ? attrs.get("From") : "";
		String path= attrs.containsKey("Path") ? attrs.get("Path") : "";
		String type= attrs.containsKey("Trans") ? attrs.get("Trans") : "string";
		if (to.length()==0)
			return "<JPATH> attribute(To)  should be set!";
		if (path.length()==0)
			return "<JPATH>attribute(Path)  should be set!";
		if (path.length()==0)
			return "<JPATH>attribute(From)  should be set!";
		if (!paras_map.containsKey(from))
			return "<JPATH> From='"+from+"' value not exited!";
	
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
		Object obj=null,from_obj=paras_map.get(from);
		if (from_obj instanceof JSONObject) {
			obj=from_obj;
		}else if (from_obj  instanceof String)
			obj = JSON.parseObject(from);
		else
			return ("JPATH: from ("+from+") value should be JSONObject or String!");
		
		String out="";
		if (type.equals("list")==true) {
			//取得json数组
			List<String> list_r = (List<String>)JSONPath.eval(obj, path); // 返回enties的所有名称
			if (list_r==null)
				return "Eval jsonPath("+path+") error!";
			//生成目标list_map;
			List<Map<String, Object>> list_out = new ArrayList<Map<String, Object>>(); 
			for(int i=0; i<list_r.size(); i++) {
				Map<String, Object> map = new HashMap<String, Object>();  
	            map.put(".", list_r.get(i));  
	            list_out.add(map);
			}
			paras_map.put(to,list_out);
		}else {
			out=""+JSONPath.eval(obj, path);
			paras_map.put(to,out);
		}
		//写入值;
		return "";
	}
		
	//FOR 动作
	public String FOR(Node parseNode, Map<String,String> attrs, Map<String, Object> paras_map){ 
		this.ParseDeep++;
		String err_str="";
		Boolean is_break=false;
		int j=0;
		//取得属性
		if (attrs==null)
			err_str= "<JPATH> attribute(In) as incoming list!";
		String to= attrs.containsKey("To") ? attrs.get("To") : "";
		String from= attrs.containsKey("In") ? attrs.get("In") : "";
		if (from.length()==0)
			err_str= "<FOR> In=\"*\" should be set as incoming list!";
		if (!paras_map.containsKey(from))
			err_str= "<FOR> In=\"*\" should be set as incoming list!";
		String[] clear=this.clearStr.split(";");
		
		//循环动作
		if (err_str.length()==0) {
			Object list_obj=paras_map.get(from);
			if (list_obj==null)
				err_str="<FOR> In=\"*\" value is null!";  //如果list为空则不循环
			else if (!(list_obj instanceof List)) 
				err_str= "<FOR> In=\""+from+"\" value("+from+") should be list!";
			if (err_str.length()==0) {
				@SuppressWarnings("unchecked")
				List<Map<String, Object>> list = (List<Map<String, Object>>)list_obj;
				//循环查询数据库取得Map字段作为row_map传参
				paras_map.put("@_#for_end","");
				paras_map.put("@_#for_num","");
				for (j = 0; j < list.size(); j++) {
					paras_map.put("@_#for_num",""+j);
					if (j==(list.size()-1))
						paras_map.put("@_#for_end","true");
					Map<String, Object> row_paras=list.get(j);
					//取得子节点
					NodeList nodes = parseNode.getChildNodes();
					if(nodes!=null){
						for (int i = 0; i < nodes.getLength(); i++){
							Node node = nodes.item(i);
							if(node.getNodeType()==Node.ELEMENT_NODE) { 
								//取得value
								String value=Xml_getNodeValue(node);
								if (value!=null && value.length()!=0)
									value=ParseValue(value,paras_map,row_paras,clear);
								//取得属性
								Map<String,String> attrs2=getParseNodeAttr(node);
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
									case "BREAK":
										if (attrs2.containsKey("Error") && attrs2.get("Error").equals("true"))
											err_str=value;//返回差错
										is_break=true;	//普通返回
										break;
									case "JPATH":
										err_str=JPATH(node,value,attrs2,paras_map);
									default:
										err_str=(String)"ParseError: node <"+node.getNodeName()+"> not support!";
									}
									//跟踪结果
									if (is_debug && attrs2.containsKey("Debug") && attrs2.get("Debug").equals(".")) {
										DebugV(attrs2,paras_map,"To");
									}
									if (is_break)
										break;
								}
							}
						}
//						if (err_str.length()!=0) {
//							System.out.println("Error:"+err_str);
//							break;
//						}
					}
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
	
	public String DO_SQL(Node parseNode, String value, Map<String,String> attrs, Map<String, Object> paras_map){ 
		//取得属性
		if (attrs==null)
			return "<DO_SQL> attribute(To)  should be set!";
		String to= attrs.containsKey("To") ? attrs.get("To") : "";
		String type= attrs.containsKey("Type") ? attrs.get("Type") : "";
		String trans= attrs.containsKey("Trans") ? attrs.get("Trans") : "string";
		String db= attrs.containsKey("Db") ? attrs.get("Db") : "";
		//if (to.length()==0)
			//return "<DO_SQL> To=\"*\" should be set";
		if (type.length()==0)
			return "<DO_SQL> Type=\"*\" should be set to 'query|update'";
		if (trans.length()==0)
			return "<DO_SQL> Trans=\"*\" should be set to \"xml|list\"!";
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
		if (db_conn==null)
			return "<DO_SQL> db not opened or error!";
		
		//动作处理
		if (type.equals("query")==true){
			try {
				Statement stat;
				stat = db_conn.createStatement();
				ResultSet rs=stat.executeQuery(value);
				List<Map<String, Object>> list=getListFromRs_Object(rs);//取得结果集row_map
				if (trans.equals("xml")==true)
					paras_map.put(to,ListMapToXmlStr(list,null)); //返回row_map到指定变量,暂时没有变量传输
				else if (trans.equals("list")==true)
					paras_map.put(to,list); //返回row_map到指定变量
				stat.close();
			} catch (SQLException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
				return e.toString();
			}
		}else if(type.equals("update")){
			Statement stat;
			try {
				stat = db_conn.createStatement();
				int rs=stat.executeUpdate(value);
				paras_map.put(to,""+rs); //返回row_map到指定变量
				stat.close();
			} catch (SQLException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
				return e.toString();
			}
		}else
			return "<DO_SQL> attribute->Type should be set to 'query|update'";
		
		return "";
	}
	
	//DO_SQL_XML把HASHMAP里面数据封装成XML
	public String ListMapToXmlStr(List<Map<String, Object>> list, Map<String, Object> add_paras) {
		if (list==null)
			return "";
		
		StringBuffer sb = new StringBuffer();
		sb.append("<?xml version=\"1.0\" encoding=\"utf-8\"?>");
		if (add_paras!=null) {
			sb.append("<_grparam>\n");
			sb.append(getGridStrByMap(add_paras));
			sb.append("</_grparam>\n");
		}
		sb.append("<result>\n");
		for (int i = 0; i < list.size(); i++) {
			sb.append("<row>\n");
			Map<String, Object> hashMap = list.get(i);
			sb.append(getGridStrByMap(hashMap));
			sb.append("</row>\n");
		}
		sb.append("</result>\n");
		return sb.toString();
	}
	//DO_SQL_xml:把HASHMAP里面数据封装成XML
	public String getGridStrByMap(Map<String, Object> hashMap) {
		String returnStr = null;
		if (hashMap.isEmpty()) {
			returnStr =  "";
		}else{
			StringBuffer strBuffer = new StringBuffer();
			Iterator iterator = hashMap.keySet().iterator();
			while (iterator.hasNext()) {
				String key = (String) iterator.next();
				//String value = hashMap.get(key);
				String value = hashMap.get(key).toString();
				hashMap.put(key, (value == null ? "" : value));
				strBuffer.append("<" + key + "><![CDATA["+ (value == null ? "" : value) + "]]></" + key + ">\n");
			}
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
	
	public String GetDBConn(Node parseNode) {
	//public String GetDBConn(Node parseNode) throws SQLException {
		if (this.dbConns!=null)
			return "";
		
		String db_name=null;
		Map<String, Connection> map = new HashMap<String, Connection>(); 
		NodeList nodes = parseNode.getChildNodes();
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
	
	
	//解析内容字符串@param_map; #param_map
	public String ParseValue(String value, Map<String, Object>paras_map, Map<String, Object>row_paras,String[] clear){
		//参数替换，如果{@***}为对应的参数值。??今后再次改进，需要看看字段是否存在否则返回null
		String result=value;
		if (paras_map!=null) {
			Iterator<Map.Entry<String, Object>> it = paras_map.entrySet().iterator();
		    while (it.hasNext()) {
		    		Map.Entry<String, Object> entry = it.next();
		    		if (entry!=null && (entry.getValue())!=null)
		    			result=result.replaceAll("\\{"+entry.getKey()+"\\}",entry.getValue().toString());
		    }
		}
		//替换#参数
		if (row_paras!=null) {
			Iterator<Map.Entry<String, Object>> it = row_paras.entrySet().iterator();
		    while (it.hasNext()) {
		    		Map.Entry<String, Object> entry = it.next();
		    		if (entry!=null && (entry.getValue())!=null) {
		    			result=result.replaceAll("\\{#_"+entry.getKey()+"\\}",entry.getValue().toString());
		    			result=result.replaceAll("\\{#_.\\}", entry.getValue().toString());//这要求list生成对象直接生成.为key
		    		}
		    }
		}
		//替换clear
		if (clear!=null) {
			for(int i=0; i<clear.length; i++) {
				result=result.replaceAll(clear[i]," ");
			}
			result=result.trim();
		}
	    return result;
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
		//取得xml路径
		//WebService qlik=new WebService();
		Document doc=null;
		DocumentBuilderFactory domfac=DocumentBuilderFactory.newInstance();
		try{
			DocumentBuilder dombuilder=domfac.newDocumentBuilder();
			InputStream is = WebService.class.getClassLoader().getResourceAsStream(act_file);
			doc=dombuilder.parse(is);
			Element root = doc.getDocumentElement();
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

	public String addXsltAct(String in_str) {
		String out_tmp=in_str.replaceAll("demo","调用主表");
		String out_str=out_tmp.replaceAll("Schedule History","历史计划");
		return out_str;
	}
	
	public static String transXmlByXslt(String srcXml, String xslt,String err) {
		//输入子串转换
		ByteArrayInputStream xslt_stream = null;
	    try {
	        xslt_stream = new ByteArrayInputStream(xslt.getBytes("UTF-8"));
	    } catch (UnsupportedEncodingException e) {
	        e.printStackTrace();
	        err=e.toString();
			return "";
	    }
	    
	    ByteArrayInputStream xml_stream = null;
	    try {
	        xml_stream = new ByteArrayInputStream(srcXml.getBytes("UTF-8"));
	    } catch (UnsupportedEncodingException e) {
	        e.printStackTrace();
	        err=e.toString();
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
			return xml_dest.toString();
		} catch (TransformerConfigurationException e) {
			e.printStackTrace();
			err=e.toString();
			return "";
		} catch (TransformerException e) {
			e.printStackTrace();
			err=e.toString();
			return "";
		}
	};
	
	private String getStreamStr(String cfg_name){
		   InputStream inputStreamObject = WebService.class.getClassLoader().getResourceAsStream(cfg_name);
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
	
	public String Xml_getNodeValue(Node node2) {
		if (node2!=null &&(node2.getFirstChild())!=null) {
			return node2.getFirstChild().getNodeValue();
		}
		return null;
	}
	
}
