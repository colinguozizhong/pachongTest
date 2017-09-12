package com.ustcsoft.ba;

import java.io.IOException;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.Timestamp;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Iterator;
import java.util.UUID;

import org.jsoup.Connection;
import org.jsoup.Jsoup;
import org.jsoup.Connection.Request;
import org.jsoup.Connection.Response;
import org.jsoup.helper.StringUtil;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

public class JiangSu {

	private static int maxPage = 1;
	
	private static int currentPage = 1;
	
	public static void main(String[] args) {
		while(currentPage<=maxPage){
			// TODO 获取当前页面的页面内容
			String URL = "http://218.94.123.119:8080/wcm/dev/simp_gov_list_t.jsp?classinfoid=81&channelid=159&page="+currentPage;
			try {
				produceList(URL);
			} catch (Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}finally{
				currentPage++;
			}
		}
	}

	
	public static void produceList(String URL) throws Exception{
		Connection conn = Jsoup.connect(URL);
		Request request = conn.request();
		request.timeout(9999999);
		Response response = conn.execute();
		Document doc = response.parse();
		if(maxPage == 1){
			Element pagecontent = doc.getElementById("pagecontent");
			Elements blist = pagecontent.getElementsByTag("b");
			if(blist!=null && blist.size()>0){
				Element b = blist.get(0);
				String pageNumStr = b.text();
				if(checkNumber(pageNumStr)){
					maxPage = Integer.valueOf(pageNumStr);
					System.out.println(maxPage);
				}
			}
			//System.out.println(pagecontent.html());
		}
		
		Element documentContainer = doc.getElementById("documentContainer");
		if(documentContainer!=null){
			Elements rows = doc.getElementsByClass("row");
			
			// 开始插入数据
			Class.forName("oracle.jdbc.driver.OracleDriver").newInstance();
			String jdbcUrl = "jdbc:oracle:thin:@192.168.20.190:1521:orcl"; // orcl为数据库的SID
			String Username = "dss"; // 用户名
			String Password = "dss"; // 密码
			java.sql.Connection con = DriverManager.getConnection(jdbcUrl, Username,
					Password);
			
			for (Iterator<Element> it = rows.iterator(); it.hasNext();) {
				
				String mcStr = "";
				String fbrqStr = "";
				String whStr = "";
				String whLeiBie = "";
				String nrUrl = "";
				String fileUrl = "";
				
				Element next = it.next();
				Elements mcs = next.getElementsByClass("mc");
				if(mcs!=null && mcs.size()>0){
					Element mc = mcs.get(0);
					if(mc!=null){
						Elements as = mc.getElementsByTag("a");
						if(as!=null && as.size()>0){
							Element a = as.get(0);
							if(a!=null){
								mcStr = a.text();
								nrUrl = a.attr("href");
							}
							
						}else{
							mcStr = mc.text();
						}
					}
					
				}
				Elements fbrqs = next.getElementsByClass("fbrq");
				if(fbrqs!=null && fbrqs.size()>0){
					Element fbrq = fbrqs.get(0);
					if(fbrq!=null){
						fbrqStr = fbrq.text();
					}
				}
				Elements whs = next.getElementsByClass("wh");
				if(whs!=null && whs.size()>0){
					Element wh = whs.get(0);
					if(wh!=null){
						whStr = wh.text();
						if(whStr.indexOf("〔")>=0){
							whLeiBie = whStr.substring(0, whStr.indexOf("〔"));
						}
					}
				}
			//	System.out.println("NO. "+currentPage+"  mc="+mcStr+"  fbrqs="+fbrqStr+"  wh="+whStr+"  whlb="+whLeiBie+"  url="+nrUrl);
				
				// 获取每个下载页面的连接
				Connection connInner = Jsoup.connect(nrUrl);
				Request requestInner = connInner.request();
				requestInner.timeout(9999999);
				Response responseInner = connInner.execute();
				Document docInner = responseInner.parse();
				Elements innerContents = docInner.getElementsByClass("content");
				if(innerContents!=null && innerContents.size()>0){
					Element innerContent = innerContents.get(0);
					Elements innerAs = innerContent.getElementsByTag("a");
					if(innerAs!=null && innerAs.size()>0){
						for(int i=innerAs.size()-1;i>=0;i--){
							Element innerA = innerAs.get(i);
							String tempUrl = innerA.attr("href");
							if(StringUtil.isBlank(tempUrl)){
								continue;
							}else{
								fileUrl = nrUrl.substring(0,nrUrl.lastIndexOf("/"))+tempUrl.substring(1);
							//	System.out.println("文件连接："+fileUrl);
							}
						}
					}
				}
				Date fbrq = new SimpleDateFormat("yyyy年yy月dd日").parse(fbrqStr);
				
				PreparedStatement pstmt = con
						.prepareStatement("INSERT INTO M_ZCWJ(WEN_JIAN_ID,WEN_JIAN_MING_CHENG,WEN_HAO,LEI_BIE_MING,NEI_RONG_URL,FA_BU_RI_QI,CREATETIME,DELETE_FLAG,FA_BU_RI_QI_TYPE,DATAFROM) VALUES(?,?,?,?,?,?,?,?,?,?)");
				pstmt.setString(1, UUID.randomUUID().toString());
				// 试一个
				pstmt.setString(2, mcStr);
				pstmt.setString(3, whStr);
				pstmt.setString(4, whLeiBie);
				pstmt.setString(5, fileUrl);
				pstmt.setTimestamp(6, new Timestamp((fbrq).getTime()));
				pstmt.setTimestamp(7, new Timestamp((new Date()).getTime()));
				pstmt.setString(8, "00");
				pstmt.setString(9, "0");
				pstmt.setString(10, "jsgov");
				pstmt.execute();
				pstmt.close();
			}
			con.close();
		}
	}
	
	public static boolean checkNumber(String value){  
        String regex = "^(-?[1-9]\\d*\\.?\\d*)|(-?0\\.\\d*[1-9])|(-?[0])|(-?[0]\\.\\d*)$";  
        return value.matches(regex);  
    }  
}
