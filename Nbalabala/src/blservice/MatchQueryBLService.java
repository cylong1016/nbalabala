package blservice;

import java.util.ArrayList;
import java.util.Date;

import vo.MatchDetailVO;

/**
 * 比赛信息查询界面的接口
 * @author Issac Ding
 * @version 2015年3月18日  上午9:42:57
 */
public interface MatchQueryBLService {
	
	/** 注意：因为迭代三在比赛信息查询界面得到的就是MatchDetailVO
	 * (原因是：要知道一场比赛中谁的得分篮板助攻最高，必须读取比赛内容，这不是MatchProfileVO能胜任的了)
	 * 而不是以前那样的MatchProfileVO
	 * 	所以在从比赛信息查询界面点击某一比赛而跳转到该场比赛详情时，应当传递这个比赛的MatchDetailVO
	 * 	 */
	
	/** 根据日期返回符合条件的比赛记录详情*/
	public ArrayList<MatchDetailVO> screenMatchByDate(Date date);
	
	/** 根据球队缩写筛选，两个String为""表示不作限制 */
	public ArrayList<MatchDetailVO> screenMatchByTeam(String abbr1, String abbr2);
	
	/** 通过球队缩写查询其联盟内排名 */
	public int getTeamRamkByAbbr(String abbr);
	
	/** 通过缩写查询其胜负数，数组0位为胜场数，1位为负场数 */
	public int[] getTeamWinsLosesByAbbr(String abbr);

	/** 返回最近一个比赛日的比赛 */
	public ArrayList<MatchDetailVO> getLatestMatches(); 
	
	/** 通过比赛ID查找比赛详情 */
	public MatchDetailVO getMatchDetailByID(int matchID);
	
	/** 返回一场比赛的实录 */
	public ArrayList<String> getLives(Date date, String homeAbbr);
}
