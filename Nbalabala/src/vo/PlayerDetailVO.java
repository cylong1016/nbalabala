package vo;

import java.util.ArrayList;

import po.MatchPlayerPO;
import po.PlayerProfilePO;
import po.PlayerSeasonPO;

/**
 * 球员信息详情，包括个人简况、赛季数据分析、所有比赛的数据、全身像
 * @author lsy
 * @version 2015年3月16日  下午8:32:45
 */
public class PlayerDetailVO {
	
	private PlayerProfilePO profile;
	
	private PlayerSeasonPO seasonRecord;
	
	private ArrayList<MatchPlayerPO> matchRecords;
	
	
	public PlayerDetailVO(PlayerProfilePO profile,
			PlayerSeasonPO seasonRecord,
			ArrayList<MatchPlayerPO> matchRecords) {
		super();
		this.profile = profile;
		this.seasonRecord = seasonRecord;
		this.matchRecords = matchRecords;
	}

	public PlayerProfilePO getProfile() {
		return profile;
	}

	//总数据
	public PlayerSeasonPO getSeasonRecord() {
		return seasonRecord;
	}

	//比赛数据
	public ArrayList<MatchPlayerPO> getMatchRecords() {
		return matchRecords;
	}
	
}
