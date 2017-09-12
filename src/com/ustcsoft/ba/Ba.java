package com.ustcsoft.ba;

import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.Timestamp;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.jsoup.Connection;
import org.jsoup.Connection.Request;
import org.jsoup.Connection.Response;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

public class Ba {
	/**
	 * @param args
	 * @throws Exception
	 */
	public static void main(String[] args) throws Exception {
		Connection conn = Jsoup
				.connect("http://law.npc.gov.cn/FLFG/flfgGjjsAction.action?pagesize=9999&pageCount=11&curPage=1&resultSearch=false&lastStrWhere=++SFYX%3A%28%E6%9C%89%E6%95%88%29+%5E+BTX%3A%E5%AE%89%E5%BE%BD+%5E%28ZLSX%3A1111+%7EZLSX%3D03%29++%5E+SFFB%3DY+&bt=%E5%AE%89%E5%BE%BD&flfgnr=&sxx=%E6%9C%89%E6%95%88&zlsxid=03&bmflid=&xldj=&bbrqbegin=&bbrqend=&sxrqbegin=&sxrqend=&zdjg=&bbwh=&topage1=&topage2=");
		Request request = conn.request();
		request.timeout(9999999);
		Response response = conn.execute();
		Document doc = response.parse();

		// 得到主采集页面的链接
		Elements elementsByTag = doc.getElementsByTag("a");

		List<String> connStrs = new ArrayList<String>();
		// 遍历链接
		for (Iterator<Element> it = elementsByTag.iterator(); it.hasNext();) {
			Element next = it.next();
			// 过滤掉父节点不是td的链接
			if ("td".equals(next.parent().tagName())) {
				String href = next.attr("href");
				if (href != null && !"".equals(href)) {
					// 得到要采集的子页面的参数字符串
					String paramStr = href.replace("javascript:showLocation(",
							"").replace(");", "");
					String[] paramStrArr = paramStr.split(",");

					StringBuffer connStr = new StringBuffer(
							"http://law.npc.gov.cn/FLFG/flfgByID.action");
					// 处理并拼接子页面的链接
					connStr.append("?flfgID=").append(
							paramStrArr[0].replaceAll("'", ""));
					connStr.append("&showDetailType=").append(
							paramStrArr[1].replaceAll("'", ""));
					connStr.append("&keyword=").append(
							paramStrArr[2].replaceAll("'", ""));
					connStr.append("&zlsxid=").append(
							paramStrArr[3].replaceAll("'", ""));

					connStrs.add(connStr.toString());
				}
			}
		}

		// 开始插入数据
		Class.forName("oracle.jdbc.driver.OracleDriver").newInstance();
		String URL = "jdbc:oracle:thin:@192.168.20.190:1521:orcl"; // orcl为数据库的SID
		String Username = "dss"; // 用户名
		String Password = "dss"; // 密码
		java.sql.Connection con = DriverManager.getConnection(URL, Username,
				Password);
		
		int pos = 0;
		// 遍历每一个要采集的子页面的链接并进行数据采集
		for (String connStr : connStrs) {
			Connection connSub = null;
			Response responseSub = null;
			try {
				System.out.println("1");
				connSub = Jsoup.connect(connStr);
				System.out.println("2");
				Request requestSub = connSub.request();
				requestSub.timeout(9999999);
				System.out.println("3");
				responseSub = connSub.execute();
			} catch (Exception e) {
				e.printStackTrace();
			}
			
			System.out.println("4");
			Document docSub = responseSub.parse();
			// 得到子页面所有的width为100%的table标签
			System.out.println("5");
			Elements tables = docSub.select("table[width='100%']");

			System.out.println("6");
			// 以空格分割内容直接分割出所有网页的内容
			// new SimpleDateFormat("yyyy年yy月dd日").parse();
			String[] splitTabStrs = tables.get(0).text().split(" ");
			List<String> splitTabList = Arrays.asList(splitTabStrs);
			Map<String, Object> map = new HashMap<String, Object>();
			int index = -1;
			index = splitTabList.indexOf("制定机关：");
			map.put("ZHI_DING_JI_GUAN", splitTabStrs[index + 1]);
			index = splitTabList.indexOf("颁布文号：");
			map.put("FA_BU_WEN_HAO", splitTabStrs[index + 1]);
			index = splitTabList.indexOf("颁布日期：");
			map.put("FA_BU_RI_QI", new SimpleDateFormat("yyyy年MM月dd日")
					.parse(splitTabStrs[index + 1]));
			index = splitTabList.indexOf("施行日期：");
			map.put("SHI_SHI_RI_QI", new SimpleDateFormat("yyyy年MM月dd日")
					.parse(splitTabStrs[index + 1]));
			index = splitTabList.indexOf("性：");
			map.put("SHI_XIAO_XING", "有效".equals(splitTabStrs[index + 1]) ? "0"
					: "1");
			map.put("XI_TONG_BIAN_MA_ID", "05-01-04-03-04");

			splitTabStrs = tables.get(1).text().split(" ");
			StringBuffer zhengWenNeiRong = new StringBuffer();
			
			map.put("MING_CHENG", splitTabStrs[0]);
			map.put("URL", connStr);
			for (int i = 1; i < splitTabStrs.length; i++) {
				zhengWenNeiRong.append("  ").append(splitTabStrs[i])
						.append(System.getProperty("line.separator"));
			}
			map.put("ZHENG_WEN_NEI_RONG", zhengWenNeiRong.toString());
			
			System.out.println("第" + (++pos) + "行");
			PreparedStatement pstmt = con
					.prepareStatement("INSERT INTO XZZF_J_FLFG(FA_LV_FA_GUI_ID,ZHI_DING_JI_GUAN,FA_BU_WEN_HAO,FA_BU_RI_QI,SHI_SHI_RI_QI,SHI_XIAO_XING,XI_TONG_BIAN_MA_ID,ZHENG_WEN_NEI_RONG,MING_CHENG,URL) VALUES(?,?,?,?,?,?,?,?,?,?)");
			pstmt.setString(1, UUID.randomUUID().toString().replace("-", ""));
			// 试一个
			pstmt.setString(2, (String) map.get("ZHI_DING_JI_GUAN"));
			pstmt.setString(3, (String) map.get("FA_BU_WEN_HAO"));
			pstmt.setTimestamp(4,
					new Timestamp(((Date) map.get("FA_BU_RI_QI")).getTime()));
			pstmt.setTimestamp(5,
					new Timestamp(((Date) map.get("SHI_SHI_RI_QI")).getTime()));
			pstmt.setString(6, (String) map.get("SHI_XIAO_XING"));
			pstmt.setString(7, (String) map.get("XI_TONG_BIAN_MA_ID"));
			pstmt.setString(8, (String) map.get("ZHENG_WEN_NEI_RONG"));
			pstmt.setString(9, (String) map.get("MING_CHENG"));
			pstmt.setString(10, (String) map.get("URL"));
			pstmt.execute();
			pstmt.close();
		}
		con.close();
	}
}
