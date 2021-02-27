package xlsrv.com.action;

import javax.net.ssl.*;
import javax.servlet.RequestDispatcher;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;


import net.sf.json.JSONArray;
import net.sf.json.JSONException;
import net.sf.json.JSONObject;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.security.cert.X509Certificate;
import java.util.Iterator;
import java.util.Map;

/*wjj 
 * tableau 鏁版嵁鍒锋柊
 * */
public class AttachBO extends HttpServlet {
	public void doGet(HttpServletRequest request, HttpServletResponse response)
			throws ServletException, IOException {
		// wjj 11-21 add
		//鍙栧緱璇锋眰JSON锛�
		String req_json_str=readJSONData(request);
		JSONObject jsonObj = JSONObject.fromObject(req_json_str);//jsonObj.get("name")
	    System.out.println(req_json_str);
	    
		RequestDispatcher rd = request.getRequestDispatcher("/TabAuto.jsp");
		rd.forward(request, response);
		// System.out.println("缁欏鎴风鍝嶅簲鐨凥TML");
		// wjj end
		// response.sendRedirect(tokenviews+"?:embed=y&:showShareOptions=true&:toolbar=no&:tabs=no&:linktarget=_self&:display_count=no&:showVizHome=no");
	}
	
	public String getToken(String site) throws ServletException,IOException {
		String token = getTrustedTicket("biuat2.yict.com.cn", "julia zhu","biuat2.yict.com.cn", "https://", site);
		return token;
	}

	public void doPost(HttpServletRequest request, HttpServletResponse response)
			throws ServletException, IOException {
		System.out.println("鑾峰彇POST");
		String err_str=null;
		
		//鍙栧緱璇锋眰req鍙傛暟
		String act=request.getParameter("act");

	    //鍙栧緱閰嶇疆淇℃伅
	    JSONObject json_cfg_act = this.getJsonStream("Action.json");
	    if (json_cfg_act==null) {
	    		err_str="Action Cfg: not found!";
	    		response.getWriter().print("[\"error:\","+err_str+"]");
			return ;
	    }
	    JSONObject json_cfg_obj=json_cfg_act.getJSONObject(act);
	    if (json_cfg_obj==null){
	    		err_str="Action Cfg: not found act="+act+"!";
	    		response.getWriter().print("[\"error:\","+err_str+"]");
			return ;
		}
	    //鍙栧緱 redir url
	    String req_url_str=(String)json_cfg_obj.get("Url");
	    //鍙栧緱璇锋眰澧炶ˉ澶�
	    Map head_map=null;
	    JSONObject json_header=json_cfg_obj.getJSONObject("Headers");
	    head_map=json_header;
	    	//鍙栧緱璇锋眰鍐呭锛堝鏋�-request娌℃湁锛�
	    	String req_json_str=(String)json_cfg_obj.get("Query"); //鍙栧緱璇锋眰鍐呭锛�
	    		    	
	    	//鍙栧緱璇锋眰JSON锛堝鏋�-request鏈夛級锛�
		String json_obj_str=readJSONData(request);
		System.out.println(json_obj_str);
		JSONObject json_obj = JSONObject.fromObject(json_obj_str);    //jsonObj.get("name")
	    req_json_str=json_obj_str; //鍙栧緱璇锋眰鍐呭锛�
	    
	    //杞彂璇锋眰锛�
	    String result=this.sendHtpps(req_json_str,(req_url_str=="" ? null:req_url_str),head_map);
	    response.getWriter().print("[\"success:\","+result+"]");
		return ;
	}

	private String readJSONData(HttpServletRequest request) {
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
	
	private JSONObject getJsonStream(String cfg_name){
	   InputStream inputStreamObject = AttachBO.class.getClassLoader().getResourceAsStream(cfg_name);
	   try {
	       BufferedReader streamReader = new BufferedReader(new InputStreamReader(inputStreamObject, "UTF-8"));
	       StringBuilder responseStrBuilder = new StringBuilder();

	       String inputStr;
	       while ((inputStr = streamReader.readLine()) != null)
	           responseStrBuilder.append(inputStr);
	       String json_str=responseStrBuilder.toString();

	       JSONObject jsonObject = JSONObject.fromObject(json_str);;

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

	public static String sendHtpps(String a, String url, Map head_map) {
		String result = "";
		OutputStreamWriter out = null;
		BufferedReader in = null;
		HttpURLConnection conn;
		try {
			trustAllHosts();
			URL realUrl = new URL(url);
			// 閫氳繃璇锋眰鍦板潃鍒ゆ柇璇锋眰绫诲瀷(http鎴栬�呮槸https)
			if (realUrl.getProtocol().toLowerCase().equals("https")) {
				HttpsURLConnection https = (HttpsURLConnection) realUrl
						.openConnection();
				https.setHostnameVerifier(DO_NOT_VERIFY);
				conn = https;
			} else {
				conn = (HttpURLConnection) realUrl.openConnection();
			}
			// 璁剧疆閫氱敤鐨勮姹傚睘鎬�
			conn.setRequestProperty("accept", "*/*");
			conn.setRequestProperty("connection", "Keep-Alive");
			conn.setRequestProperty("user-agent",
					"Mozilla/4.0 (compatible; MSIE 6.0; Windows NT 5.1;SV1)");
			conn.setRequestProperty("Content-Type", "text/plain;charset=utf-8");
			
			//琛ュ厖add_header
			if (head_map!=null) {
				for(Object key:head_map.keySet()){//keySet鑾峰彇map闆嗗悎key鐨勯泦鍚� 聽鐒跺悗鍦ㄩ亶鍘唊ey鍗冲彲
					String value = head_map.get(key).toString();
					conn.setRequestProperty((String)key, value);
				}
			}

			// 鍙戦�丳OST璇锋眰蹇呴』璁剧疆濡備笅涓よ
			conn.setDoOutput(true);
			conn.setDoInput(true);
			out = new OutputStreamWriter(conn.getOutputStream(), "utf-8");
			out.write(a);
			out.flush();
			// 瀹氫箟BufferedReader杈撳叆娴佹潵璇诲彇URL鐨勫搷搴�
			in = new BufferedReader(
					new InputStreamReader(conn.getInputStream()));
			String line;
			while ((line = in.readLine()) != null) {
				result += line;
			}
		} catch (Exception e) {
			e.printStackTrace();
		} finally {// 浣跨敤finally鍧楁潵鍏抽棴杈撳嚭娴併�佽緭鍏ユ祦
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

	private String getTrustedTicket(String wgserver, String user,
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
			// System.out.println("杈撳嚭Site锛�"+Site );
			// System.out.println("杈撳嚭Site锛�"+HttpType + wgserver + "/trusted" );

			// create connect for http/https
			trustAllHosts();
			URL url = new URL(HttpType + wgserver + "/trusted");// 杩欓噷SSL娉ㄦ剰
			// 閫氳繃璇锋眰鍦板潃鍒ゆ柇璇锋眰绫诲瀷(http鎴栬�呮槸https)
			if (url.getProtocol().toLowerCase().equals("https")) {
				HttpsURLConnection https = (HttpsURLConnection) url
						.openConnection();
				https.setHostnameVerifier(DO_NOT_VERIFY);
				conn = https;
			} else {
				conn = (HttpURLConnection) url.openConnection();
			}

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
	/*
	JSONObject json_header=json_obj.getJSONObject("header");
	Iterator iterator = json_header.keys();
	while(iterator.hasNext()){
	            key = (String) iterator.next();
	        value = jsonObject.getString(key);
	}
	
    JSONArray jsonArray = JSONArray.fromObject(json_header_str);
    Object[] os = jsonArray.toArray();
    for(int i=0; i<os.length; i++) {
        JSONObject jsonObj = JSONObject.fromObject(os[i]);
        System.out.println(jsonObj.get("name"));
    }*/
}