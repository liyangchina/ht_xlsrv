package xlsrv.com.action;

import javax.net.ssl.*;
import javax.servlet.RequestDispatcher;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import net.sf.json.JSONArray;
//import org.apache.http.HttpEntity;
//import org.apache.http.HttpStatus;
//import org.apache.http.StatusLine;
//import org.apache.http.client.ClientProtocolException;
//import org.apache.http.client.methods.CloseableHttpResponse;
//import org.apache.http.client.methods.HttpPost;
//import org.apache.http.entity.StringEntity;
//import org.apache.http.impl.client.CloseableHttpClient;
//import org.apache.http.impl.client.HttpClients;
//import org.apache.http.util.EntityUtils;
import net.sf.json.JSONException;
import net.sf.json.JSONObject;
//for xslt
import java.io.*;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import javax.xml.transform.*;
import javax.xml.transform.stream.*;

import org.apache.http.HttpEntity;
import org.apache.http.HttpStatus;
import org.apache.http.StatusLine;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;

//import com.sun.java.util.jar.pack.Package.*;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.Map;

/*wjj 
 * tableau 数据刷新
 * */
public class Restful extends HttpServlet {
	/**
	 *  
	 */
	private static final long serialVersionUID = 1L;

	public void doGet(HttpServletRequest request, HttpServletResponse response)
			throws ServletException, IOException {
		//取得头替换信息
		String act=request.getParameter("act");
		String type=request.getParameter("type");
		String method=request.getParameter("method");
    		String xslt_file=request.getParameter("xslt");
		type= (type==null || type.equals("")==true) ? "json" : type; //缺省为json
		
		//取得配置信息
		JSONObject json_cfg_obj=getActCfgTrans(request,"Action.json",act);
		if (json_cfg_obj==null) {
			response.getWriter().print("[\"error:\":"+"\"Action Cfg: act("+act+")not found!\""+"]");
			return ;
		} 
	    //取得 配置url
	    String req_url_str=(String)json_cfg_obj.get("Url");
	    if (req_url_str==null) {
	    		response.getWriter().print("[\"error:\":"+"\"Action Cfg: Url("+req_url_str+") config error !\""+"]");
			return ;
	    }
	    //取得配置增补头
	    Map head_map =json_cfg_obj.getJSONObject("Headers");
	    	//取得配置请求内容（如果-request没有）
	    	String req_content=(String)json_cfg_obj.get("Query"); //取得请求内容；
	    	//取得配置Method
	    	if (method==null)
	    		method=(String)json_cfg_obj.get("Method");
	    	String ResultAct=(String)json_cfg_obj.get("ResultAct");
		
		// 转发读取操作。
	    String result=sendHtpp(req_content,(req_url_str=="" ? null:req_url_str),head_map,type,method);
	    	//String result=this.rePost((req_url_str=="" ? null:req_url_str),req_content,head_map,type,method);
	    if (result.indexOf("<")==0 && ResultAct.indexOf("ClearXmlns")>=0) {
    			result=result.replaceAll("xmlns","xmlns_bak"); //取消xmlns作用
	    }
	    
	    //如果是xml则进行xslt转换。
	    if (xslt_file!=null && xslt_file.equals("")==false) {//xslt转换后回传
		    	//取得xslt文件
		    	String xslt=getStreamStr(xslt_file);
		    	if (xslt==null) {
					response.getWriter().print("[\"error:\":"+"\"xslt: xslt file(\""+xslt_file+"\") not found!\""+"]");
					return ;
				}
		    	//过滤命名空间
		    	//String srcXml=result.replaceAll("xmlns","xmlns_bak"); //取消xmlns作用
			//转换操作
		    	String err="";
		    	String destXml=transXmlByXslt(result,xslt,err);
		    if (destXml.length()==0 && err.length()!=0) {
		    		response.getWriter().print("[\"error:\":"+"\"xslt transform error("+err+")\""+"]"); 
		    		return;
		    }else {
		    		response.setContentType("text/html;charset=utf-8");
		    		destXml=addXsltAct(destXml);
		    		System.out.println(destXml);
		    		response.getWriter().print(destXml);
		    }
	    }else {
	    		System.out.println(result);
	    		response.getWriter().print(result);//直接回传
	    }
		return ;
	}
	
	public String addXsltAct(String in_str) {
		String out_tmp=in_str.replaceAll("demo","调用主表");
		String out_str=out_tmp.replaceAll("Schedule History","历史计划");
		return out_str;
	}
	public String getToken(String site) throws ServletException,IOException {
		String token = getTrustedTicket("biuat2.yict.com.cn", "julia zhu","biuat2.yict.com.cn", "https://", site);
		return token;
	}
	public String getToken2(String host,String user,String uri,String site) throws ServletException,IOException {
		String token = getTrustedTicket(host, user,host, uri, site);
		return token;
	}

	public void doPost(HttpServletRequest request, HttpServletResponse response)
			throws ServletException, IOException {
		System.out.println("获取POST");
		String err_str=null;
		
		//取得请求req参数
		String act=request.getParameter("act");
		String type=request.getParameter("type");
		String method=request.getParameter("method");
		String xslt_file=request.getParameter("xslt");
		if (!(type=="" || type.equals("json") || type.equals("xml"))) {
	    		response.getWriter().print("[\"error:\","+"query type is not json or xml!"+"]");
			return ;
	    }
		type=(type=="") ? "json" : type; //缺省为json

	    //取得配置信息
	    //JSONObject json_cfg_act = this.getJsonStream("Action.json");
		JSONObject json_cfg_obj=getActCfgTrans(request,"Action.json",act);
	    if (json_cfg_obj==null) {
	    		response.getWriter().print("[\"error:\":"+"\"Action Cfg("+act+") not found!\""+"]");
			return ;
	    }
	    
	    String req_url_str=(String)json_cfg_obj.get("Url");//取得 redir url
	    //取得请求增补头
	    Map head_map=null;
	    JSONObject json_header=json_cfg_obj.getJSONObject("Headers");
	    head_map=json_header;
	    	//取得请求内容（如果-request没有）
	    	String req_content=(String)json_cfg_obj.get("Query"); //取得请求内容；
	    	//取得ResultAct（如果-request没有）
	    	JSONObject return_obj=json_cfg_obj.getJSONObject("Return"); 
	    	
	    	//取得请求JSON（如果-request有）；
	   	if (type.equals("json")==true) {
		    	String json_obj_str=request.getParameter("dataJson");
			if (json_obj_str==null || json_obj_str.equals("")){
		    		response.getWriter().print("[\"error\":"+"\"Post content: is empty!\"]");
				return ;
			}
			JSONObject json_obj = JSONObject.fromObject(json_obj_str);    //jsonObj.get("name")
		    req_content=json_obj_str; //取得请求内容；
	    	}else {
	    		String xml_obj_str=readRequestData(request);
		    	System.out.println(xml_obj_str);
		    	req_content=xml_obj_str;
	    	}
	    
	    //转发请求：
	    	String result="";
	    	if (type.equals("json")==true)
	    		result=this.rePost((req_url_str=="" ? null:req_url_str),req_content,head_map,type,"POST");
	    	else
	    		result=this.sendHtpp(req_content,(req_url_str=="" ? null:req_url_str),head_map,type,"POST");
	    	
	    	System.out.println(result);
	    	String ResultAct=(String)json_cfg_obj.get("ResultAct");
	    	if (result.indexOf("<")==0 && ResultAct.indexOf("ClearXmlns")>=0) {
    			result=result.replaceAll("xmlns","xmlns_bak"); //取消xmlns作用
	    }
	    if ((result.indexOf("[\"error:\",")!=-1) && (return_obj.toString().equals("null")==false)) {//调用本地指定函数
	    		String jsonMethod = (String)return_obj.get("Method") ; 
	    		Map params = return_obj.getJSONObject("Params") ; 
	    		String r_s=null,err=null;
	    		//转array
	    		try {
		    			Method return_func=this.getClass().getMethod(jsonMethod, Map.class);
			    		r_s=(String)return_func.invoke(this, params);
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
	    			System.out.println(r_s);
	    			response.getWriter().print(r_s);
	    		}else
	    			response.getWriter().print(err);
	    	}else {
	    		System.out.println(result);
	    		response.getWriter().print(result);
	    	}
		return ;
	}
	
	public String getTokenPM(Map p) throws ServletException,IOException {
		String token = getTrustedTicket((String)p.get("host"), (String)p.get("user"),(String)p.get("host"), (String)p.get("uri"), (String)p.get("site"));
		return token;
	}
	
	public static String rePost(String url, String params, Map head_map, String type, String method) {    
		int state=HttpStatus.SC_OK;
        CloseableHttpClient httpclient = HttpClients.createDefault();  
        HttpPost httpPost = new HttpPost(url);// 创建httpPost     
        if (type.equals("json")==true) {
        		httpPost.setHeader("Accept", "application/json");    //接收报文类型
        		httpPost.setHeader("Content-Type", "application/json");   //发送报文类型
        }else {
        		httpPost.setHeader("Accept", "application/xml");    //接收报文类型
        		httpPost.setHeader("Content-Type", "application/xml");   //发送报文类型
        }
      //补充add_header
		if (head_map!=null) {
			for(Object key:head_map.keySet()){//keySet获取map集合key的集合  然后在遍历key即可
				String value = head_map.get(key).toString();
				if (((String)key).equals("X-SAP-LogonToken")) {
					httpPost.setHeader((String)key, "\""+value+"\"");
				}else
					httpPost.setHeader((String)key, value);
			}
		}
        
        if((params != null) && !("".equals(params))){
            StringEntity entity = new StringEntity(params, "UTF-8");  
            httpPost.setEntity(entity);     
        }     
        
        CloseableHttpResponse response = null;     
        try {  
            response = httpclient.execute(httpPost);  
            StatusLine status = response.getStatusLine();  
            state = status.getStatusCode();  
            if (state == HttpStatus.SC_OK) {  
                HttpEntity responseEntity = response.getEntity();  
                String jsonString = EntityUtils.toString(responseEntity,"UTF-8");  
                return jsonString;  
            }  
            else{  
                System.out.println(state);
            }  
        } catch (ClientProtocolException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }  
        finally { 
                try {  
                    if (response != null)response.close();  
                    httpclient.close();  
                } catch (IOException e) {  
                    e.printStackTrace();  
                }  
        }  
        String err="[\"error:\",\""+state+"\"]";
        return err;  
    } 

	public static String sendHtpp(String a, String url, Map head_map,String type, String method) {
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
				if (a!=null)
					out.write(a);
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

	private String readRequestData1(HttpServletRequest request) {
        StringBuffer json=new StringBuffer();
        String lineString=null;
        try {
            BufferedReader reader=request.getReader();
            while ((lineString=reader.readLine())!=null) {
                json.append(lineString);                
            }
        } catch (Exception e) {
            System.out.println(e.toString());
        }
        return json.toString();
    }
	
	private String readRequestData(HttpServletRequest request) {
		 BufferedReader br = null;
	        try {
	            br = new BufferedReader(new InputStreamReader(request.getInputStream(), "UTF-8"));
	        } catch (IOException e) {
	            // TODO Auto-generated catch block
	            e.printStackTrace();
	        }
	        String line = null;
	        StringBuilder sb = new StringBuilder();
	        try {
	            while ((line = br.readLine()) != null) {
	                sb.append(line);
	            }
	        } catch (IOException e) {
	            // TODO Auto-generated catch block
	            e.printStackTrace();
	        }
	        return sb.toString();
    }
	
	private JSONObject getJsonStream(String cfg_name){
	   InputStream inputStreamObject = Restful.class.getClassLoader().getResourceAsStream(cfg_name);
	   try {
	       BufferedReader streamReader = new BufferedReader(new InputStreamReader(inputStreamObject, "UTF-8"));
	       StringBuilder responseStrBuilder = new StringBuilder();

	       String inputStr;
	       while ((inputStr = streamReader.readLine()) != null)
	           responseStrBuilder.append(inputStr);
	       String json_str=responseStrBuilder.toString();

	       JSONObject jsonObject = JSONObject.fromObject(json_str);

	       //returns the json object
	       return jsonObject;

	   } catch (IOException e) {
	       e.printStackTrace();
	   } catch (JSONException e) {
	       e.printStackTrace();
	   }
	   
	    //if something went wrong, return null
	    return null;
	}
	private String getStreamStr(String cfg_name){
		   InputStream inputStreamObject = Restful.class.getClassLoader().getResourceAsStream(cfg_name);
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

	public JSONObject getActCfgTrans(HttpServletRequest request, String act_file,String act){
		Enumeration enu=request.getParameterNames();  
		
		//替换信息
		String act_str_tmp=getStreamStr(act_file);
		if (act_str_tmp==null) 
    			return null;
		String act_str=transParams(request,enu,act_str_tmp);//替换参数
		
		//取得配置信息
		JSONObject json_cfg_act = JSONObject.fromObject(act_str);;
	    if (json_cfg_act==null) 
	    		return null;

	    JSONObject json_cfg_obj=(act!=null)? json_cfg_obj=json_cfg_act.getJSONObject(act) : json_cfg_act.getJSONObject("Default");
	    if (json_cfg_obj==null)
			return null;

	    return json_cfg_obj;
	}
	
	public String transParams(HttpServletRequest request,Enumeration enu,String src_str){
		//参数替换，如果{@***}为对应的参数值。
		String to_str=src_str;
		while(enu.hasMoreElements()){  
			String paraName=(String)enu.nextElement();  
			if (paraName.indexOf("param_")==0) {
				String transValue=request.getParameter(paraName);
				String transParam=(String)"{@"+paraName+"}";
				if (transValue!=null) 
					to_str=to_str.replace(transParam,transValue);
				else
					to_str=to_str.replace(transParam,"");
			}
		}
		//！！！今后加入自动查找所有{@***}置空的操作。
		return to_str;
	}
	
	/**
	 * 使用XSLT转换XML文件
	 * @param srcXml	源XML文件路径
	 * @param dstXml	目标XML文件路径
	 * @param xslt		XSLT文件路径
	 */
	/*public static String cutXmlns(String srcXml) {
		return srcXml.replaceAll("xmlns","xmlns_bak");
	}*/
//	Integer start=srcXml.indexOf("xmlns");
//	Integer end1=srcXml.indexOf("\"",start);
//	Integer end2=srcXml.indexOf("\"",end1);
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

	public static String sendHtppsOld(String a, String url, Map head_map) {
		String result = "";
		OutputStreamWriter out = null;
		BufferedReader in = null;
		HttpURLConnection conn;
		try {
			trustAllHosts();
			URL realUrl = new URL(url);
			// 通过请求地址判断请求类型(http或者是https)
			if (realUrl.getProtocol().toLowerCase().equals("https")) {
				HttpsURLConnection https = (HttpsURLConnection) realUrl
						.openConnection();
				https.setHostnameVerifier(DO_NOT_VERIFY);
				conn = https;
			} else {
				conn = (HttpURLConnection) realUrl.openConnection();
			}
			// 设置通用的请求属性
			conn.setRequestProperty("accept", "*/*");
			conn.setRequestProperty("connection", "Keep-Alive");
			conn.setRequestProperty("user-agent",
					"Mozilla/4.0 (compatible; MSIE 6.0; Windows NT 5.1;SV1)");
			//conn.setRequestProperty("Content-Type", "text/plain;charset=utf-8");
			
			//补充add_header
			if (head_map!=null) {
				for(Object key:head_map.keySet()){//keySet获取map集合key的集合  然后在遍历key即可
					String value = head_map.get(key).toString();
					conn.setRequestProperty((String)key, value);
				}
			}

			// 发送POST请求必须设置如下两行
			conn.setDoOutput(true);
			conn.setDoInput(true);
			out = new OutputStreamWriter(conn.getOutputStream(), "utf-8");
			out.write(a);
			out.flush();
			// 定义BufferedReader输入流来读取URL的响应
			in = new BufferedReader(
					new InputStreamReader(conn.getInputStream()));
			String line;
			while ((line = in.readLine()) != null) {
				result += line;
			}
		} catch (Exception e) {
			e.printStackTrace();
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
	
	public String getTrustedTicket(String wgserver, String user,
			String remoteAddr, String HttpType, String Site)
			throws ServletException {
		OutputStreamWriter out = null;
		BufferedReader in = null;
		String result = "";
		// OutputStreamWriter out = null;
		// BufferedReader in = null;
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

			// Send the request
			// System.out.println("输出Site："+Site );
			// System.out.println("输出Site："+HttpType + wgserver + "/trusted" );

			// create connect for http/https
			//trustAllHosts();
			URL url = new URL(HttpType + wgserver + "/trusted");// 这里SSL注意
			// 通过请求地址判断请求类型(http或者是https)
			if (url.getProtocol().toLowerCase().equals("https")) {
				HttpsURLConnection https = (HttpsURLConnection) url
						.openConnection();
				https.setHostnameVerifier(DO_NOT_VERIFY);
				conn = https;
			} else {
				conn = (HttpURLConnection) url.openConnection();
			}
			
			System.out.println(""+HttpType + wgserver + "/trusted:"+data);

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
			throw new ServletException(e);
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

	public static void main(String[] args) {
		System.out.println("123");
	}

}
