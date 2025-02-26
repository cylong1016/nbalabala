package capture;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 抓取赛季数据
 * http://www.basketball-reference.com/leagues
 * @author cylong
 * @version 2015年5月12日  下午7:54:54
 */
public class Match extends NBAData {

    /** 最近多少年的数据 */
    protected int maxYear = 30;
    /** 比赛ID */
    protected int matchID = 1;
    
    /** 这个是为了子类写继续扒最新数据用，由于代码设计问题，这块写的不太好，如果以后有机会会改掉 */
    protected String maxDate;
    
    public Match() {
    	this.captureUrl = ROOT + "/leagues";
    }
    
    /**
     * 抓取html网页赛季数据
     * @author cylong
     * @version 2015年5月18日  上午2:17:26
     */
    public void capture() {
    	new Capture().start();
    }
    
    /**
     * 抓取赛季日期和详细信息的url
     * @author cylong
     * @version 2015年5月19日  下午8:20:07
     */
    protected void captureMatchData() {
    	InputStream input = null;
    	BufferedReader reader = null;
    	HttpURLConnection urlConn = getConn(captureUrl);
    	int i = 1; // 记录最近多少年
    	try {
    		input = urlConn.getInputStream();
    		reader = new BufferedReader(new InputStreamReader(input, urlConn.getContentType().equals("text-html; charset=gb2312")?"UTF-8":"gb2312"));
    		// 匹配赛季日期和数据的链接
    		String reg = "(<td.*?>(<a href=\"(?<href>.*)\">))(?<season>\\d{4}-\\d{2})(</a></td>)";
    		Pattern pattern = Pattern.compile(reg);
    		String temp = null;
    		while((temp = reader.readLine()) != null) {
    			Matcher matcher = pattern.matcher(temp);
    			if(matcher.find()) {
    				String href = ROOT + matcher.group("href").replaceAll(".html", "_games.html");
    				String season = matcher.group("season");
    				captureNameAndScore(season, href);
    				i++;
    			}
    			if(i > maxYear) {
    				break;
    			}
    		}
    	} catch (IOException e) {
    		e.printStackTrace();
    	} finally {
    		try {
    			reader.close();
    			input.close();
    			urlConn.disconnect();
    		} catch (IOException e) {
    			e.printStackTrace();
    		}
    	}
    }
    
    /**
	 * 抓取某个赛季的球队名称和总比分
	 * @param season 赛季年份
	 * @param url 详细信息的url
	 * @author cylong
	 * @version 2015年5月19日  下午8:28:39
	 */
	protected void captureNameAndScore(String season, String url) {
		InputStream input = null;
		BufferedReader reader = null;
		HttpURLConnection urlConn = getConn(url);
		String regularOrPlayoff = season + "R"; // 常规赛或者季后赛，默认常规赛
		int culumnNum = 8; // 表格有8列数据
		int i = culumnNum;
		boolean hasDetailData = true; // 有些行没有球队的比赛信息
		ArrayList<String> cellData = new ArrayList<String>();
		try {
			input = urlConn.getInputStream();
			reader = new BufferedReader(new InputStreamReader(input, urlConn.getContentType().equals("text-html; charset=gb2312")?"UTF-8":"gb2312"));
			String cellReg = "<td.*?>(<a href=\"(?<href>.*)\">)*(?<info>[^<]*)(</a>)*</td>";
			Pattern patternCell = Pattern.compile(cellReg);
			String temp = null;
			while((temp = reader.readLine()) != null) {
				if(Pattern.matches("<h2.*[^>]>Playoffs</h2>", temp)) { // 检测到季后赛的表
					regularOrPlayoff = season + "P";
				}
				Matcher matcherCell = patternCell.matcher(temp);
				if(matcherCell.find()) {
					String info = matcherCell.group("info");
					String href = ROOT + matcherCell.group("href");
					if(i == 7) { // Box Score所在的那一行
						if(!info.equals("Box Score")) { // 这一行没有球队的比赛信息
							hasDetailData = false;
						}
						cellData.add(href);
						System.out.println(href + "， gameID: " + matchID);
					} else {
						cellData.add(info);
					}
					i--;
					if(i == 0) { // 一行数据读取完毕
						// 存入数据库
						if(hasDetailData) { // 有详细信息再存入数据库
							String date = dateFormat(cellData.get(0)); // 将读取下来的date转化成mysql标准格式
							cellData.set(0, date);
							if(maxDate == null || maxDate.compareTo(date) < 0) {
								insertIntoMatchProfile(cellData, regularOrPlayoff); // 存储球队比赛数据
								matchID++;
							}
						}
						hasDetailData = true;
						i = culumnNum;
						cellData.clear();
					}
				}
			}
		} catch (IOException e) {
			e.printStackTrace();
		} finally {
			try {
				reader.close();
				input.close();
				urlConn.disconnect();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}

	/** 
	 * 抓取每小节的比分和球员的比赛信息
	 * @param url
	 * @author cylong
	 * @version 2015年5月19日  下午10:31:49
	 */
	protected ArrayList<String> captureSectionAndPlayerData(String url) {
		InputStream input = null;
		BufferedReader reader = null;
		HttpURLConnection urlConn = getConn(url);
		// 将要返回的数据，包括球队缩写，小节数和四个小节的比分
		ArrayList<String> returnList = new ArrayList<String>();
		// 读取的小节分数，第0位保存着球队缩写
		ArrayList<String> sectionList = new ArrayList<String>();
		// 两只球队的加时赛信息
		ArrayList<String> extraTime = new ArrayList<String>();
		int k = 0; // 记录是哪个球队，0是第一行的球队，1是第二行的球队
		try {
			input = urlConn.getInputStream();
			reader = new BufferedReader(new InputStreamReader(input, urlConn.getContentType().equals("text-html; charset=gb2312")?"UTF-8":"gb2312"));
			String teamNameReg = "<td><a.*[^>]>(?<teamName>[A-Z]*)</a></td>";
			Pattern patternTeamName = Pattern.compile(teamNameReg);
			String temp = null;
			while((temp = reader.readLine()) != null) {
				if(Pattern.matches("<th.*[^>]>Scoring</th>", temp)) { // 找到Scoring表
					while((temp = reader.readLine()) != null) {
						Matcher matcherTeamName = patternTeamName.matcher(temp);
						if(matcherTeamName.find()) {
							String teamName = matcherTeamName.group("teamName");
							sectionList.add(teamName); // 球队缩写
							String sectionScoreReg = "<td.*[^>]>(?<score>[0-9]*)</td>";
							Pattern patternSectionScore = Pattern.compile(sectionScoreReg);
							while((temp = reader.readLine()) != null) {
								Matcher matcherSectionScore = patternSectionScore.matcher(temp);
								if(matcherSectionScore.find()) {
									String score = matcherSectionScore.group("score");
									sectionList.add(score);
								}
								if(Pattern.matches("</tr>", temp)) { // 一行数据的结束
									if(k == 0) {
										returnList.add(String.valueOf(sectionList.size() - 2)); // 小节数
									}
									for(int i = 0; i < 5; i++) { // 球队名加上4个小节的分数
										returnList.add(sectionList.get(i));
									}
									if(sectionList.size() > 6) { // 对名、总分、加上4小节是6,如果大于6说明有加时赛
										for(int i = 5; i < sectionList.size() - 1; i++) {
											extraTime.add(sectionList.get(i));
										}
									}
									sectionList.clear();
									break;
								}
							}
							k++;
							if(k == 2) { // 两个球队的小节比分全部读取完毕
								insertIntoExtraTime(extraTime); // 保存加时赛信息
								break;
							}
						}
					}
					break;
				}
			}
			capturePlayerMatch(reader, returnList.get(1), returnList.get(6)); // 继续读取球员的比赛信息
		} catch (IOException e) {
			e.printStackTrace();
		} finally {
			try {
				reader.close();
				input.close();
				urlConn.disconnect();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		return returnList;
	}

	/**
	 * 读取每个球员的比赛信息
	 * @param reader 
	 * @param road 客场球队名缩写
	 * @param home 主场球队名缩写
     * @version 2015年5月28日  下午3:45:12
     */
	protected void capturePlayerMatch(BufferedReader reader, String road, String home) {
    	String BASIC_BOX_SCORE_STATS = "Basic Box Score Stats"; // 球员比赛数据表格的表头
    	char homeOrRoad = 'R'; // R:客场、H:主场
    	String reserves = "Reserves"; // 候补球员的表头
    	try {
    		String playerDataReg = "<td.*?>(<a href=.*(?<nameID>[0-9]{2})\\.html\">?>)?(?<playerData>.*?)(</a>)?</td>";
    		Pattern patternPlayerData = Pattern.compile(playerDataReg);
			String temp = null;
			while((temp = reader.readLine()) != null) {
				// 球员的比赛数据
				ArrayList<String> playerMatch = new ArrayList<String>();
				// 找到球员比赛信息的第一个表【客场】
				if(Pattern.matches(".*<th.*>" + BASIC_BOX_SCORE_STATS + "</th>", temp)) {
					int isStarter = 1; // 是否先发：1是、0不是
					while((temp = reader.readLine()) != null) {
						if(Pattern.matches(".*<th.*>" + reserves + "</th>", temp)) { // 读取到后发球员
							isStarter = 0;
						}
						Matcher matcherPlayerData = patternPlayerData.matcher(temp);
						if(matcherPlayerData.find()) {
							String nameID = matcherPlayerData.group("nameID");
							String playerData = matcherPlayerData.group("playerData");
							if(nameID != null) { // 名称后面加上编号来区分同名球员
								playerData += ("$" + nameID);
							}
							playerMatch.add(playerData);
						}
						if(Pattern.matches("</tr>", temp)) { // 一个球员的数据读取完毕
							playerMatch.add(String.valueOf(homeOrRoad)); // 主场或者客场
							playerMatch.add(String.valueOf(isStarter)); // 是否先发
							if(homeOrRoad == 'R') {
								playerMatch.add(road);
							} else {
								playerMatch.add(home);
							}
							// 球员数据为24个，少于24个数据的就是没有记录
							// 好吧，事实证明在之前没有 +/-这项，所以之前是23项
							// 表格最后一行是Team Totals，不记录
							if(playerMatch.size() >= 23) {
								// 球员比赛信息表的最后一行，接着读取下一个Basic Box Score Stats表
								if(playerMatch.get(0).equals("Team Totals")) {
									homeOrRoad = 'H'; // 下一个表是主场
									break; // 内层的while循环
								}
								if(playerMatch.size() == 23) {
									playerMatch.add(20, "0"); // +/- 设置为0
								}
								insertIntoPlayerMatch(playerMatch);
							}
							playerMatch.clear();
						}
					}
				}
			}
		} catch (IOException e) {
			e.printStackTrace();
		} finally {
			try {
    			reader.close();
    		} catch (IOException e) {
    			e.printStackTrace();
    		}
		}
	}

	/**
	 * 将球员比赛数据插入到数据库match_player表中
	 * @param playerMatch
	 * @author cylong
	 * @version 2015年5月28日  下午5:00:06
	 */
	protected void insertIntoPlayerMatch(ArrayList<String> playerMatch) {
		for(int i = 0; i < playerMatch.size(); i++) {
			// 有些数据网站上是空的，存到数据库中为0
			if(playerMatch.get(i).equals("")) {
				playerMatch.set(i, "0");
			}
		}
		String sql = "INSERT INTO match_player ("
				+ "match_id, "
				+ "home_or_road, "
				+ "team_abbr, "
				+ "player_name, "
				+ "is_starter, "
				+ "time_played, "
				+ "field_made, "
				+ "field_attempt, "
				+ "field_percent, "
				+ "threepoint_made, "
				+ "threepoint_attempt, "
				+ "threepoint_percent, "
				+ "freethrow_made, "
				+ "freethrow_attempt, "
				+ "freethrow_percent, "
				+ "offensive_rebound, "
				+ "defensive_rebound, "
				+ "total_rebound, "
				+ "assist, "
				+ "steal, "
				+ "block, "
				+ "turnover, "
				+ "foul, "
				+ "score, "
				+ "plus_minus"
				+ ") VALUES ("
					+ matchID + ", "
					+ "'" + playerMatch.get(21) + "', "
					+ "'" + playerMatch.get(23) + "', "
					+ "'" + playerMatch.get(0).replaceAll("'", "\\\\'") + "', " // 名字中有单引号
					+ playerMatch.get(22) + ", "
					+ "'" + playerMatch.get(1) + "', "
					+ playerMatch.get(2) + ", "
					+ playerMatch.get(3) + ", "
					+ playerMatch.get(4) + ", "
					+ playerMatch.get(5) + ", "
					+ playerMatch.get(6) + ", "
					+ playerMatch.get(7) + ", " 
					+ playerMatch.get(8) + ", "
					+ playerMatch.get(9) + ", "
					+ playerMatch.get(10) + ", "
					+ playerMatch.get(11) + ", "
					+ playerMatch.get(12) + ", "
					+ playerMatch.get(13) + ", "
					+ playerMatch.get(14) + ", "
					+ playerMatch.get(15) + ", "
					+ playerMatch.get(16) + ", "
					+ playerMatch.get(17) + ", "
					+ playerMatch.get(18) + ", "
					+ playerMatch.get(19) + ", "
					+ playerMatch.get(20)
					+ ")";
		System.out.println(sql);
		try {
			Statement statement = mysqlConn.createStatement();
			statement.execute(sql); // 插入
		} catch (SQLException e) {
			e.printStackTrace();
			System.exit(0);
		}
	}

	/**
	 * 将球队比赛信息保存到match_profile表中
	 * @param cellData
	 * @param regularOrPlayoff 常规赛或者季后赛
	 * @author cylong
	 * @version 2015年5月25日  下午3:59:41
	 */
	protected void insertIntoMatchProfile(ArrayList<String> cellData, String regularOrPlayoff) {
		ArrayList<String> sectionScore = captureSectionAndPlayerData(cellData.get(1)); // 读取每小节分数和球员数据
		String date = cellData.get(0);
		if(date != null) {
			date = "'" + date + "'";
		}
		String sql = "INSERT INTO match_profile ("
				+ "match_id, "
				+ "date, "
				+ "road_total_score, "
				+ "home_total_score, "
				+ "section, "
				+ "road_abbr, "
				+ "road_section_1, "
				+ "road_section_2, "
				+ "road_section_3, "
				+ "road_section_4, "
				+ "home_abbr, "
				+ "home_section_1, "
				+ "home_section_2, "
				+ "home_section_3, "
				+ "home_section_4, "
				+ "season"
				+ ") VALUES ("
				+ matchID + ", "
				+ date + ", "
				+ cellData.get(3) + ", "
				+ cellData.get(5) + ", "
				+ sectionScore.get(0) + ", "
				+ "'" + sectionScore.get(1) + "', "
				+ sectionScore.get(2) + ", "
				+ sectionScore.get(3) + ", "
				+ sectionScore.get(4) + ", "
				+ sectionScore.get(5) + ", "
				+ "'" + sectionScore.get(6) + "', "
				+ sectionScore.get(7) + ", "
				+ sectionScore.get(8) + ", "
				+ sectionScore.get(9) + ", "
				+ sectionScore.get(10) + ", "
				+ "'" + regularOrPlayoff + "'"
				+ ")";
		System.out.println(sql);
		try {
			// statement用来执行SQL语句   
			Statement statement = mysqlConn.createStatement();
			statement.execute(sql); // 插入
		} catch (SQLException e) {
			e.printStackTrace();
		}
    }
    
    /**
     * 将加时赛信息存入表extra_time中
	 * @param extraTime
	 * @author cylong
	 * @version 2015年5月24日  上午12:07:41
	 */
	protected void insertIntoExtraTime(ArrayList<String> extraTime) {
		int otNum = extraTime.size() / 2; // 加时赛数
		for(int i = 0; i < otNum; i++) {
			String sql = "INSERT INTO extra_time ("
					+ "match_id, "
					+ "extra_order, "
					+ "road_score, "
					+ "home_score"
					+ ") VALUES (" 
						+ matchID + ", "
						+ (i + 1) + ", "
						+ extraTime.get(i) + ", "
						+ extraTime.get(i + otNum)
					+ ")";
			System.out.println(sql);
			try {
				Statement statement = mysqlConn.createStatement();
				statement.execute(sql);
			} catch (SQLException e) {
				e.printStackTrace();
			}
			
		}
	}

    class Capture extends Thread {
    	@Override
    	public void run() {
    		long start = System.currentTimeMillis();
    		captureMatchData();
    		long stop = System.currentTimeMillis();
    		long millisecond = stop - start;
    		long hour = millisecond / 1000 / 60 / 60;
    		long minute = millisecond / 1000 / 60 % 60;
    		long second = millisecond / 1000 % 60;
    		System.out.println("最近" + maxYear + "年数据抓取完毕！");
    		System.out.println("共有" + (matchID--) + "场比赛！");
    		System.out.println("耗时：" + hour + "小时" + minute + "分钟" + second + "秒");
    	}
    }
    
}
