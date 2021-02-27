package xlsrv.com.action;

import javax.net.ssl.*;
import javax.servlet.RequestDispatcher;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.security.cert.X509Certificate;

/*wjj 
 * tableau 数据刷新
 * */
public class RefreshDB extends HttpServlet {
	public void doGet(HttpServletRequest request, HttpServletResponse response)
			throws ServletException, IOException {
		String res_str = this.getUrl(request, response);
		// wjj 11-21 add
		request.setAttribute("ulr", res_str);
		RequestDispatcher rd = request.getRequestDispatcher("/TabAuto.jsp");
		rd.forward(request, response);
		// System.out.println("给客户端响应的HTML");
		// wjj end
		// response.sendRedirect(tokenviews+"?:embed=y&:showShareOptions=true&:toolbar=no&:tabs=no&:linktarget=_self&:display_count=no&:showVizHome=no");
	}

	public String getUrl2(HttpServletRequest request) throws ServletException,
			IOException {
		String url_tmp = request.getParameter("view");
		String views = "";
		String site = "";
		if (url_tmp.indexOf("/site/") == 0) {
			String view_tmp2 = url_tmp.substring(6);
			site = view_tmp2.substring(0, view_tmp2.indexOf("/"));
			views = "/t/" + view_tmp2;
		} else {
			views = url_tmp;
		}
		// System.out.println(views);
		// tableau服务器的地址
		// String url="https://biuat2.yict.com.cn/trusted/";
		String url = "https://biuat2.yict.com.cn/trusted/";
		// get ticket //tableau服务器的地址 https://biuat.yict.com.cn
		String token = getTrustedTicket("biuat2.yict.com.cn", "julia zhu",
				"biuat2.yict.com.cn", "https://", site);
		String tokenviews = url + token + views;
		String res_str = tokenviews
				+ "?:embed=y&:showShareOptions=true&:toolbar=no&:tabs=no&:linktarget=_self&:display_count=no&:showVizHome=no";
		// System.out.println("获取token成功  token=" + token);
		// System.out.println("RefreshDB.java：版本：1.00.1");
		return res_str;
	}

	public String getUrl3(String url_tmp) throws ServletException,
			IOException {
		//String url_tmp = request.getParameter("view");
		String views = "";
		String site = "";
		if (url_tmp.indexOf("/site/") == 0) {
			String view_tmp2 = url_tmp.substring(6);
			site = view_tmp2.substring(0, view_tmp2.indexOf("/"));
			views = "/t/" + view_tmp2;
		} else {
			views = url_tmp;
		}
		// System.out.println(views);
		// tableau服务器的地址
		// String url="https://biuat2.yict.com.cn/trusted/";
		String url = "https://biuat2.yict.com.cn/trusted/";
		// get ticket //tableau服务器的地址 https://biuat.yict.com.cn
		String token = getTrustedTicket("biuat2.yict.com.cn", "julia zhu",
				"biuat2.yict.com.cn", "https://", site);
		String tokenviews = url + token + views;
		String res_str = tokenviews
				+ "?:embed=y&:showShareOptions=true&:toolbar=no&:tabs=no&:linktarget=_self&:display_count=no&:showVizHome=no";
		System.out.println("获取token成功  token=" + token);
		System.out.println("RefreshDB.java：版本：1.00.1");
		return res_str;
	}
	
	public String getToken(String site) throws ServletException,IOException {
		String token = getTrustedTicket("biuat2.yict.com.cn", "julia zhu","biuat2.yict.com.cn", "https://", site);
		return token;
	}

	public String getUrl(HttpServletRequest request,
			HttpServletResponse response) throws ServletException, IOException {
		String url_tmp = request.getParameter("view");
		String views = "";
		String site = "";
		if (url_tmp.indexOf("/site/") == 0) {
			String view_tmp2 = url_tmp.substring(6);
			site = view_tmp2.substring(0, view_tmp2.indexOf("/"));
			views = "/t/" + view_tmp2;
		} else {
			views = url_tmp;
		}
		// System.out.println(views);
		PrintWriter out = response.getWriter();
		// tableau服务器的地址
		String url = "https://biuat2.yict.com.cn/trusted/";
		// get ticket //tableau服务器的地址 https://biuat.yict.com.cn
		String token = getTrustedTicket("biuat2.yict.com.cn", "julia zhu",
				"biuat2.yict.com.cn", "https://", site);
		System.out.println("token=" + token);
		response.setContentType("text/html;charset=UTF-8");
		response.setStatus(response.SC_MOVED_TEMPORARILY);
		response.setContentType("00000");
		String tokenviews = url + token + views;
		String res_str = tokenviews
				+ "?:embed=y&:showShareOptions=true&:toolbar=no&:tabs=no&:linktarget=_self&:display_count=no&:showVizHome=no";
		System.out.println("获取token成功+url=" + res_str);
		return res_str;
	}

	public void doPost(HttpServletRequest request, HttpServletResponse response)
			throws ServletException, IOException {
		System.out.println("获取POST");
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

	public static String sendHtpps(String a, String url) {
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
			conn.setRequestProperty("Content-Type", "text/plain;charset=utf-8");
			// 发送POST请求必须设置如下两行
			conn.setDoOutput(true);
			conn.setDoInput(true);
			// 获取URLConnection对象对应的输出流
			// 获取URLConnection对象对应的输出流
			// outtStream = new PrintWriter(conn.getOutputStream());
			// 发送请求参数
			// outtStream.print("username="+UserName);
			out = new OutputStreamWriter(conn.getOutputStream(), "utf-8");
			out.write(a);
			// 错误方式，这种方式容易出现乱码
			// PrintWriter out = new PrintWriter(connection.getOutputStream());
			/* out.print(a); */
			// flush输出流的缓冲
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
			// System.out.println("输出Site："+Site );
			// System.out.println("输出Site："+HttpType + wgserver + "/trusted" );

			// create connect for http/https
			trustAllHosts();
			URL url = new URL(HttpType + wgserver + "/trusted");// 这里SSL注意
			// URLConnection conn = url.openConnection();
			// 通过请求地址判断请求类型(http或者是https)
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
}