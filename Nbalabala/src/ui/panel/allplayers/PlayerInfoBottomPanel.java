package ui.panel.allplayers;

import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;

import javax.swing.JLabel;

import po.MatchPlayerPO;
import po.PlayerProfilePO;
import po.PlayerSeasonPO;
import po.TeamSeasonPO;
import ui.Images;
import ui.MyFont;
import ui.UIConfig;
import ui.common.SeasonInputPanel;
import ui.common.button.ImgButton;
import ui.common.button.TabButton;
import ui.common.frame.Frame;
import ui.common.label.ImgLabel;
import ui.common.label.MyLabel;
import ui.common.panel.BottomPanel;
import ui.common.panel.Panel;
import ui.controller.MainController;
import utility.Constants;
import utility.Utility;
import vo.PlayerDetailVO;
import bl.matchquerybl.MatchQuery;
import bl.playerquerybl.PlayerQuery;
import blservice.PlayerQueryBLService;
import data.playerdata.PlayerImageCache;
import data.seasondata.SeasonData;
import dataservice.SeasonDataService;

/**
 * 具体球员信息界面
 * @author lsy
 * @version 2015年3月24日 上午10:26:48
 */
@SuppressWarnings("serial")
public class PlayerInfoBottomPanel extends BottomPanel {
	
	/** 最左边的横坐标 */
	private static final int LEFT_LABEL_COLUMN_X = 280;
	/** 中间的横坐标 */
	private static final int MID_LABEL_COLUMN_X = 351;
	/** 右边一列三行开始的横坐标 */
	private static final int RIGHT_LABEL_COLUMN_X = 620;
	/** 最上面一行的纵坐标 */
	private static final int FIRST_LABEL_ROW_Y = 15;
	/** 中间label的纵坐标 */
	private static final int MID_LABEL_ROW_Y = 63;
	/** 最下面场均得分篮板助攻的纵坐标*/
	private static final int BUTTON_LABEL_ROW_Y = 96;


	private String name;
	private PlayerProfilePO profileVO;
	private PlayerQueryBLService playerQuery = new PlayerQuery();
	private SeasonDataService teamSeason = new SeasonData();
	private PlayerDetailVO detailVO;
	
	private ImgButton backButton;

	private BottomPanel lastPanel;
	private MyLabel profileLabel[] = new MyLabel[4];

	/** 赛季选择器 */
	private SeasonInputPanel seasonInput;
	
	private PlayerScoreReboundAssistLabel scoreLabel;
	private PlayerScoreReboundAssistLabel reboundLabel;
	private PlayerScoreReboundAssistLabel assistLabel;
	
	/** 选项卡按钮,分为当前和非当前状态 */
	private TabButton briefTab = new TabButton(Constants.briefText, 
			Images.PLAYER_TAB_MOVE_ON, Images.PLAYER_TAB_CHOSEN);
	private TabButton seasonDataTab = new TabButton(Constants.seasonDataText, 
			Images.PLAYER_TAB_MOVE_ON, Images.PLAYER_TAB_CHOSEN);
	private TabButton matchesDataTab = new TabButton(Constants.matchesDataText, 
			Images.PLAYER_TAB_MOVE_ON, Images.PLAYER_TAB_CHOSEN);
	
	/** 所属球队 */
	private JLabel teamLabel;
	
	// 子面板。通过切换子面板实现三个页面的切换，公共部分不变
	private Panel currentPanel;
	private PlayerInfoBriefPanel briefPanel;
	private PlayerInfoSeasonDataPanel seasonDataPanel;
	private PlayerInfoMatchesDataPanel matchesDataPanel;
	
	public PlayerInfoBottomPanel(String name, BottomPanel lastPanel) {
		super(Images.PLAYER_INFO_BG);
		this.playerQuery = new PlayerQuery();
		this.profileVO = playerQuery.getPlayerProfileByName(name);
		this.name = name;
		
		seasonInput = new SeasonInputPanel(this);
		seasonInput.setSeason(profileVO.toYear);
		seasonInput.setLocation(793,122);	
		this.add(seasonInput);
		
		this.detailVO = playerQuery.getPlayerDetailByName(name, seasonInput.getSeason());
		this.lastPanel = lastPanel;
		
		addTitles();
		addScoreReboundAssistLabels();
		addProfileLabel();
		addPortrait();
		addBackButton();
		addTabButtons();
		
		ArrayList<MatchPlayerPO> latestTwoMatches = new ArrayList<MatchPlayerPO>();
		ArrayList<MatchPlayerPO> playoffMatches = new MatchQuery().
				getMatchRecordByPlayerName(name, Constants.LATEST_SEASON);
		if (playoffMatches.size() > 1) {
			int matchCount = playoffMatches.size();
			latestTwoMatches.add(playoffMatches.get(matchCount - 1));
			latestTwoMatches.add(playoffMatches.get(matchCount - 2));
		}else {
			int matchCount = detailVO.getMatchRecords().size();
			latestTwoMatches.add(detailVO.getMatchRecords().get(matchCount - 1));
			latestTwoMatches.add(detailVO.getMatchRecords().get(matchCount - 2));
		}
		
		// 刚进入的时候显示的是柱状对比图的那个页面
		briefPanel = new PlayerInfoBriefPanel(detailVO.getSeasonRecord(), 
				playerQuery.getFiveArgsAvg(seasonInput.getSeason()), 
				playerQuery.getHighestScoreReboundAssist(seasonInput.getSeason()),
				latestTwoMatches);
		currentPanel = briefPanel;
		addCurrentPanel();
	}
	
	private void addTabButtons() {
		briefTab.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				remove(currentPanel);
				String season = seasonInput.getSeason();
				PlayerSeasonPO seasonVO = detailVO.getSeasonRecord();
				
				currentPanel = briefPanel;
				addCurrentPanel();
				briefPanel.updateContent(seasonVO, playerQuery.getFiveArgsAvg(season), 
						playerQuery.getHighestScoreReboundAssist(season));
				
				briefTab.setOn();
				seasonDataTab.setOff();
				matchesDataTab.setOff();
				repaint();
			}
		});
		briefTab.setLocation(24,198);
		this.add(briefTab);
		briefTab.setOn();	//一开始本选项卡是当前选项卡
		
		seasonDataTab.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				remove(currentPanel);
				if (seasonDataPanel == null) {
					seasonDataPanel = new PlayerInfoSeasonDataPanel();
					TeamSeasonPO teamSeasonPO = teamSeason.getTeamDataByAbbr
							(detailVO.getSeasonRecord().getTeamAbbr(), seasonInput.getSeason());
					seasonDataPanel.update(seasonInput.getSeason(), detailVO.getSeasonRecord(), teamSeasonPO);
				}
				currentPanel = seasonDataPanel;
				addCurrentPanel();
				
				briefTab.setOff();
				seasonDataTab.setOn();
				matchesDataTab.setOff();
				
				repaint();
			}
		});
		seasonDataTab.setLocation(341,198);
		this.add(seasonDataTab);
		
		matchesDataTab.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				remove(currentPanel);
				if (matchesDataPanel == null) {
					matchesDataPanel = new PlayerInfoMatchesDataPanel();
					matchesDataPanel.updateContent(detailVO.getMatchRecords());
				}
				currentPanel = matchesDataPanel;
				addCurrentPanel();
				
				briefTab.setOff();
				seasonDataTab.setOff();
				matchesDataTab.setOn();
				repaint();
			}
		});
		matchesDataTab.setLocation(657,198);
		this.add(matchesDataTab);
	}
	
	// 标题信息，包括球衣号码、名字、位置、球队
	// TODO 这里的字体字号不造都是啥
	// TODO 没有号码的数据怎么办
	private void addTitles() {
		PlayerSeasonPO seasonPO = detailVO.getSeasonRecord();
		
//		JLabel numLabel = new JLabel("12");
//		numLabel.setForeground(UIConfig.ORANGE_TEXT_COLOR);
		//TODO 球衣号码，那个硕大的橙色的文字的字体和bounds
//		numLabel.setBounds(LEFT_LABEL_COLUMN_X, FIRST_LABEL_ROW_Y, 50, 42);
//		numLabel.setFont(new Font("方正姚体", Font.PLAIN, 50));
//		this.add(numLabel);
		
		JLabel nameLabel = new JLabel(Utility.trimName(name));
		nameLabel.setOpaque(false);
		//TODO 球员名字的字体和bounds
		nameLabel.setBounds(MID_LABEL_COLUMN_X, FIRST_LABEL_ROW_Y, 350, 50);
		nameLabel.setFont(MyFont.YT_XL);
		nameLabel.setForeground(MyFont.BLACK_GRAY);
		this.add(nameLabel);
		
		// 位置有可能是 前锋-中锋 所以宽度不一定，所以队名的位置也不一定
		JLabel positionLabel = new JLabel(Constants.translatePosition(profileVO.getPosition())
				+ " / ");
		Dimension preferred = positionLabel.getPreferredSize();
		positionLabel.setFont(UIConfig.LABEL_PLAIN_FONT);
		positionLabel.setBounds(MID_LABEL_COLUMN_X, MID_LABEL_ROW_Y, (int)preferred.getWidth() + 10, (int)preferred.getHeight());
		positionLabel.setFont(MyFont.YH_S);
		positionLabel.setForeground(MyFont.BLACK_GRAY);
		this.add(positionLabel);
		
		// 改变赛季以后teamLabel可能会改变
		teamLabel = new JLabel(Constants.translateTeamAbbrToLocation(seasonPO.getTeamAbbr()) + " " +
				Constants.translateTeamAbbr(seasonPO.getTeamAbbr()));
		teamLabel.setFont(UIConfig.LABEL_PLAIN_FONT);
		teamLabel.setFont(MyFont.YH_S);
		teamLabel.setForeground(UIConfig.BLUE_TEXT_COLOR);
		teamLabel.setBounds((int)preferred.getWidth() + 351, 63, 120, 16);
		this.add(teamLabel);
	}
	
	/**
	 * 场均得分、篮板、助攻排名
	 */
	private void addScoreReboundAssistLabels() {
		int[] ranks = playerQuery.getScoreReboundAssistRank(name, seasonInput.getSeason());
		PlayerSeasonPO seasonVO = detailVO.getSeasonRecord();
		
		scoreLabel = new PlayerScoreReboundAssistLabel(Constants.scoreAvgText, seasonVO.scoreAvg, ranks[0]);
		scoreLabel.setLocation(LEFT_LABEL_COLUMN_X, BUTTON_LABEL_ROW_Y);
		this.add(scoreLabel);
		
		reboundLabel = new PlayerScoreReboundAssistLabel(Constants.reboundAvgText, 
				seasonVO.totalReboundAvg, ranks[1]);
		reboundLabel.setLocation(385, BUTTON_LABEL_ROW_Y);
		this.add(reboundLabel);
		
		assistLabel = new PlayerScoreReboundAssistLabel(Constants.assistAvgText, 
				seasonVO.assistAvg, ranks[2]);
		assistLabel.setLocation(493, BUTTON_LABEL_ROW_Y);
		this.add(assistLabel);
	}

	/**
	 * 个人身高体重生日等资料
	 */
	private void addProfileLabel() {
		String birthday = Constants.translateDate(profileVO.getBirthDate());
		if (birthday != null) {
			birthday = birthday.replace('/', '-');
		}
		String[] profileLabelStr = new String[]{Constants.translateHeight(profileVO.getHeightFoot() + "-" + profileVO.getHeightInch()) + " / " + Constants.translateWeight(profileVO.getWeight()),
				Constants.birthdayText + "：" + birthday,
				Constants.veteranText + "：" + (profileVO.getToYear() - profileVO.getFromYear()),
				Constants.schoolText + "：" + profileVO.getSchool()};
		for(int i = 0; i < 4; i++) {
			profileLabel[i] = new MyLabel(profileLabelStr[i]);
			profileLabel[i].setBounds(RIGHT_LABEL_COLUMN_X, 20 + i * UIConfig.PROFILE_LABEL_INTER_Y, 250, 16);
			profileLabel[i].setFont(UIConfig.LABEL_SMALL_FONT);
			profileLabel[i].setForeground(MyFont.LIGHT_GRAY);
			this.add(profileLabel[i]);
		}
	}

	// title（主要是所属球队）和三大数据以及排名 不随赛季变化（只关注现在的）
	public void refresh() {
		String season = seasonInput.getSeason();
		detailVO = playerQuery.getPlayerDetailByName(name, season);
		PlayerSeasonPO seasonVO = detailVO.getSeasonRecord();
		int [] ranks = playerQuery.getScoreReboundAssistRank(name, season);
		if (scoreLabel != null) scoreLabel.update(seasonVO.scoreAvg, ranks[0]);
		if (reboundLabel != null) reboundLabel.update(seasonVO.totalReboundAvg, ranks[1]);
		if (assistLabel != null) assistLabel.update(seasonVO.assistAvg, ranks[2]);
		
		if (briefPanel != null) {
			briefPanel.updateContent(seasonVO, playerQuery.getFiveArgsAvg(season), 
					playerQuery.getHighestScoreReboundAssist(season));
		}
		if (seasonDataPanel != null) {
			TeamSeasonPO teamSeasonPO = teamSeason.getTeamDataByAbbr(seasonVO.getTeamAbbr(), season);
			seasonDataPanel.update(season, seasonVO, teamSeasonPO);
		}
		if (matchesDataPanel != null) {
			matchesDataPanel.updateContent(detailVO.getMatchRecords());
		}
		
//		teamLabel.setText(Constants.translateTeamAbbr(seasonVO.getTeam()));
		repaint();
	}

	/**
	 * 返回按钮
	 * @author lsy
	 * @version 2015年3月24日 下午4:20:16
	 */
	private void addBackButton() {
		backButton = new ImgButton(Images.RETURN_BTN, UIConfig.RETURN_X, UIConfig.RETURN_Y, Images.RETURN_BTN_ON);
		this.add(backButton);
		backButton.addMouseListener(new MouseAdapter() {
			public void mousePressed(MouseEvent e) {
				MainController.backToOnePanel(lastPanel);
			}
		});
	}

	/**
	 * 添加头像
	 * @author lsy
	 * @version 2015年3月24日 上午11:17:35
	 */
	private void addPortrait() {
		ImgLabel label = new ImgLabel(53, -15, 200, 160, PlayerImageCache.getPortraitByName(name));
		this.add(label);
	}

	private void addCurrentPanel() {
		currentPanel.setBounds(25,236,946,363);
		this.add(currentPanel);
	}
	
	//TODO 测试代码
	public static void main(String[]args) {
		Frame frame = new Frame();
		MainController.frame = frame;
		new PlayerImageCache().loadPortrait();;
		frame.setPanel(new PlayerInfoBottomPanel("Kobe Bryant$01", null));
		frame.start();
	}
}
