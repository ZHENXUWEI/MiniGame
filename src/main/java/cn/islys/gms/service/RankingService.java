package cn.islys.gms.service;

import cn.islys.gms.entity.*;
import cn.islys.gms.repository.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Collectors;

@Service
public class RankingService {

    @Autowired
    private UserStatsRepository userStatsRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RankingBotRepository rankingBotRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    // 段位配置
    private static final Map<String, int[]> RANK_TIERS = new LinkedHashMap<>();

    static {
        RANK_TIERS.put("青铜", new int[]{0, 1999});
        RANK_TIERS.put("白银", new int[]{2000, 3499});
        RANK_TIERS.put("黄金", new int[]{3500, 4999});
        RANK_TIERS.put("铂金", new int[]{5000, 6499});
        RANK_TIERS.put("钻石", new int[]{6500, 7999});
        RANK_TIERS.put("大师", new int[]{8000, 9999});
        RANK_TIERS.put("神话", new int[]{10000, 14999});
        RANK_TIERS.put("传说", new int[]{15000, Integer.MAX_VALUE});
    }

    /**
     * 获取完整排行榜（前30名）
     */
    public List<Map<String, Object>> getRankingList() {
        List<Map<String, Object>> rankingList = new ArrayList<>();

        // 获取真实用户排行榜（前15名）
        List<UserStats> userStats = userStatsRepository.findTop30ByRankPoints();
        for (UserStats stats : userStats) {
            Optional<User> userOpt = userRepository.findById(stats.getUserId());
            if (userOpt.isPresent() && userOpt.get().getStatus() == 1) {
                Map<String, Object> rankingItem = createRankingItem(userOpt.get(), stats, false);
                rankingList.add(rankingItem);
            }

            // 只取前15名真实用户
            if (rankingList.size() >= 15) break;
        }

        // 获取机器人排行榜（15名）
        List<RankingBot> bots = rankingBotRepository.findTopRankingBots(15);
        for (RankingBot bot : bots) {
            Map<String, Object> rankingItem = createRankingItem(bot);
            rankingList.add(rankingItem);
        }

        // 按段位分排序
        rankingList.sort((a, b) -> {
            Integer pointsA = (Integer) a.get("rankPoints");
            Integer pointsB = (Integer) b.get("rankPoints");
            return pointsB.compareTo(pointsA);
        });

        // 添加排名
        for (int i = 0; i < rankingList.size(); i++) {
            rankingList.get(i).put("rank", i + 1);
        }

        return rankingList;
    }

    /**
     * 获取段位统计信息（基于所有玩家和机器人）
     */
    public Map<String, Object> getRankTierStatistics() {
        return calculateRealTimeStatistics();
    }

    /**
     * 实时计算段位统计
     */
    private Map<String, Object> calculateRealTimeStatistics() {
        Map<String, Object> statistics = new HashMap<>();
        Map<String, Integer> tierCount = new LinkedHashMap<>();
        Map<String, Double> tierPercentage = new LinkedHashMap<>();

        // 初始化
        for (String tier : RANK_TIERS.keySet()) {
            tierCount.put(tier, 0);
            tierPercentage.put(tier, 0.0);
        }

        // 统计真实用户
        List<UserStats> allUserStats = userStatsRepository.findAll();
        for (UserStats stats : allUserStats) {
            String tier = stats.getRankTier();
            tierCount.put(tier, tierCount.get(tier) + 1);
        }

        // 统计机器人
        List<RankingBot> allBots = rankingBotRepository.findAll();
        for (RankingBot bot : allBots) {
            String tier = bot.getRankTier();
            tierCount.put(tier, tierCount.get(tier) + 1);
        }

        // 计算总数和百分比
        int totalPlayers = allUserStats.size() + allBots.size();
        for (String tier : tierCount.keySet()) {
            double percentage = totalPlayers > 0 ?
                    Math.round((tierCount.get(tier) * 100.0 / totalPlayers) * 100.0) / 100.0 : 0.0;
            tierPercentage.put(tier, percentage);
        }

        statistics.put("tierCount", tierCount);
        statistics.put("tierPercentage", tierPercentage);
        statistics.put("totalPlayers", totalPlayers);
        statistics.put("lastUpdated", LocalDateTime.now());

        return statistics;
    }

    /**
     * 自动刷新排行榜机器人数据 - 修复版本
     */
    @Scheduled(fixedRate = 3600000) // 每小时执行一次
    @Transactional
    public void autoRefreshRankingBots() {
        System.out.println("开始自动刷新排行榜机器人数据...");

        try {
            // 获取所有现有的机器人
            List<RankingBot> existingBots = rankingBotRepository.findAll();

            if (existingBots.isEmpty()) {
                // 如果表是空的，先初始化一些机器人
                initializeRankingBots();
            } else {
                // 更新现有机器人的数据
                updateExistingBotsData(existingBots);
            }

            System.out.println("排行榜机器人数据刷新完成，共更新 " + existingBots.size() + " 个机器人");

            // 不再更新统计缓存，统计数据实时计算

        } catch (Exception e) {
            System.err.println("刷新排行榜机器人数据失败: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * 初始化排行榜机器人（只在第一次运行时调用）
     */
    private void initializeRankingBots() {
        List<RankingBot> initialBots = createInitialRankingBots();
        rankingBotRepository.saveAll(initialBots);
        System.out.println("初始化了 " + initialBots.size() + " 个排行榜机器人");
    }

    /**
     * 创建初始的排行榜机器人
     */
    private List<RankingBot> createInitialRankingBots() {
        List<RankingBot> bots = new ArrayList<>();
        Random random = new Random();

        // 预定义的机器人名字列表
        String[] botNames = {
                "王者杀手", "战神猎手", "暗影战士", "雷霆刺客", "风暴枪手",
                "烈焰先锋", "冰霜守卫", "幽灵大师", "钢铁传奇", "黄金英雄",
                "疾风之魂", "幻影之怒", "星辰之翼", "月光之心", "烈日之眼",
                "寒冰之手", "狂暴之刃", "神圣之盾", "黑暗之灵", "光明之影",
                "龙魂战士", "凤凰刺客", "麒麟枪手", "白虎先锋", "玄武守卫",
                "青龙大师", "朱雀传奇", "麒麟英雄", "白虎之魂", "青龙之怒"
        };

        for (int i = 0; i < botNames.length; i++) {
            RankingBot bot = new RankingBot();
            bot.setName(botNames[i]);

            // 根据排名生成合理的初始数据
            int rankPoints = generateInitialRankPoints(i, botNames.length, random);
            bot.setRankPoints(rankPoints);
            bot.setLevel(generateLevelBasedOnRank(rankPoints, random));
            bot.setPerformance(generatePerformanceBasedOnRank(rankPoints, random));

            // 生成合理的胜负记录
            int[] battleStats = generateBattleStats(rankPoints, random);
            bot.setWins(battleStats[0]);
            bot.setLosses(battleStats[1]);
            bot.setTotalKills(battleStats[2]);
            bot.setTotalDeaths(battleStats[3]);

            bots.add(bot);
        }

        return bots;
    }

    /**
     * 更新现有机器人的数据
     */
    private void updateExistingBotsData(List<RankingBot> bots) {
        Random random = new Random();

        for (RankingBot bot : bots) {
            // 基于当前数据生成新的合理数据（模拟玩家进步）
            updateBotStats(bot, random);
        }

        // 批量保存更新
        rankingBotRepository.saveAll(bots);
    }

    /**
     * 更新单个机器人的统计数据
     */
    private void updateBotStats(RankingBot bot, Random random) {
        // 模拟玩家进步：段位分有小幅波动
        int currentPoints = bot.getRankPoints();
        int pointsChange = random.nextInt(201) - 100; // -100 到 +100 的变化
        int newPoints = Math.max(0, currentPoints + pointsChange);
        bot.setRankPoints(newPoints);

        // 等级缓慢增长
        int levelIncrease = random.nextInt(3); // 0-2级增长
        bot.setLevel(Math.max(1, bot.getLevel() + levelIncrease));

        // 表现值有小幅波动
        int performanceChange = random.nextInt(41) - 20; // -20 到 +20 的变化
        int newPerformance = Math.max(50, Math.min(500, bot.getPerformance() + performanceChange));
        bot.setPerformance(newPerformance);

        // 增加新的胜负记录（模拟继续游戏）
        int newWins = random.nextInt(5); // 0-4场胜利
        int newLosses = random.nextInt(4); // 0-3场失败

        bot.setWins(bot.getWins() + newWins);
        bot.setLosses(bot.getLosses() + newLosses);

        // 更新击杀死亡数据
        int newKills = newWins * 2 + random.nextInt(3); // 每胜约2杀，加上随机
        int newDeaths = newLosses * 2 + random.nextInt(3); // 每败约2死，加上随机

        bot.setTotalKills(bot.getTotalKills() + newKills);
        bot.setTotalDeaths(bot.getTotalDeaths() + newDeaths);
    }

    /**
     * 生成基于排名的初始段位分
     */
    private int generateInitialRankPoints(int index, int totalBots, Random random) {
        // 根据排名位置生成合理的段位分分布
        double positionRatio = (double) index / totalBots;

        if (positionRatio < 0.1) { // 前10%：传说/神话段位
            return random.nextInt(5000) + 10000; // 10000-15000
        } else if (positionRatio < 0.25) { // 10%-25%：大师段位
            return random.nextInt(2000) + 8000; // 8000-10000
        } else if (positionRatio < 0.45) { // 25%-45%：钻石段位
            return random.nextInt(1500) + 6500; // 6500-8000
        } else if (positionRatio < 0.65) { // 45%-65%：铂金段位
            return random.nextInt(1500) + 5000; // 5000-6500
        } else if (positionRatio < 0.8) { // 65%-80%：黄金段位
            return random.nextInt(1500) + 3500; // 3500-5000
        } else if (positionRatio < 0.9) { // 80%-90%：白银段位
            return random.nextInt(1500) + 2000; // 2000-3500
        } else { // 后10%：青铜段位
            return random.nextInt(2000); // 0-2000
        }
    }

    /**
     * 基于段位分生成等级
     */
    private int generateLevelBasedOnRank(int rankPoints, Random random) {
        if (rankPoints >= 10000) return random.nextInt(20) + 80; // 80-100级
        if (rankPoints >= 8000) return random.nextInt(20) + 60;  // 60-80级
        if (rankPoints >= 6500) return random.nextInt(20) + 40;  // 40-60级
        if (rankPoints >= 5000) return random.nextInt(20) + 30;  // 30-50级
        if (rankPoints >= 3500) return random.nextInt(15) + 20;  // 20-35级
        if (rankPoints >= 2000) return random.nextInt(10) + 15;  // 15-25级
        return random.nextInt(10) + 5; // 5-15级
    }

    /**
     * 基于段位分生成表现值
     */
    private int generatePerformanceBasedOnRank(int rankPoints, Random random) {
        if (rankPoints >= 10000) return random.nextInt(101) + 400; // 400-500 (S级)
        if (rankPoints >= 8000) return random.nextInt(101) + 350;  // 350-450 (A-S级)
        if (rankPoints >= 6500) return random.nextInt(101) + 300;  // 300-400 (A级)
        if (rankPoints >= 5000) return random.nextInt(101) + 250;  // 250-350 (B-A级)
        if (rankPoints >= 3500) return random.nextInt(101) + 200;  // 200-300 (B级)
        if (rankPoints >= 2000) return random.nextInt(101) + 150;  // 150-250 (C-B级)
        return random.nextInt(101) + 100; // 100-200 (C-D级)
    }

    /**
     * 基于段位分生成战斗统计数据
     */
    private int[] generateBattleStats(int rankPoints, Random random) {
        int baseGames;

        if (rankPoints >= 10000) baseGames = random.nextInt(200) + 300; // 300-500场
        else if (rankPoints >= 8000) baseGames = random.nextInt(150) + 200; // 200-350场
        else if (rankPoints >= 6500) baseGames = random.nextInt(100) + 150; // 150-250场
        else if (rankPoints >= 5000) baseGames = random.nextInt(80) + 100;  // 100-180场
        else if (rankPoints >= 3500) baseGames = random.nextInt(60) + 70;   // 70-130场
        else if (rankPoints >= 2000) baseGames = random.nextInt(40) + 50;   // 50-90场
        else baseGames = random.nextInt(30) + 30; // 30-60场

        // 根据段位分计算胜率（高段位胜率更高）
        double winRate;
        if (rankPoints >= 10000) winRate = 0.7 + random.nextDouble() * 0.2; // 70%-90%
        else if (rankPoints >= 8000) winRate = 0.6 + random.nextDouble() * 0.2; // 60%-80%
        else if (rankPoints >= 6500) winRate = 0.55 + random.nextDouble() * 0.15; // 55%-70%
        else if (rankPoints >= 5000) winRate = 0.5 + random.nextDouble() * 0.15; // 50%-65%
        else if (rankPoints >= 3500) winRate = 0.45 + random.nextDouble() * 0.15; // 45%-60%
        else if (rankPoints >= 2000) winRate = 0.4 + random.nextDouble() * 0.15; // 40%-55%
        else winRate = 0.3 + random.nextDouble() * 0.2; // 30%-50%

        int wins = (int) (baseGames * winRate);
        int losses = baseGames - wins;

        // 计算击杀死亡数（胜场击杀多，败场死亡多）
        int totalKills = wins * 2 + random.nextInt(wins) + losses; // 每胜约2杀，每败约1杀
        int totalDeaths = losses * 2 + random.nextInt(losses) + wins; // 每败约2死，每胜约1死

        return new int[]{wins, losses, totalKills, totalDeaths};
    }

    /**
     * 生成真实的排行榜机器人数据
     */
    private List<RankingBot> generateRealisticRankingBots(int count) {
        List<RankingBot> bots = new ArrayList<>();
        Random random = new Random();

        String[] firstNames = {"王者", "战神", "暗影", "雷霆", "风暴", "烈焰", "冰霜", "幽灵", "钢铁", "黄金"};
        String[] lastNames = {"杀手", "猎手", "战士", "刺客", "枪手", "先锋", "守卫", "大师", "传奇", "英雄"};

        Set<String> usedNames = new HashSet<>();

        for (int i = 0; i < count; i++) {
            RankingBot bot = new RankingBot();

            // 生成唯一的名字
            String name;
            do {
                String firstName = firstNames[random.nextInt(firstNames.length)];
                String lastName = lastNames[random.nextInt(lastNames.length)];
                name = firstName + lastName + (random.nextInt(900) + 100);
            } while (usedNames.contains(name) || rankingBotRepository.existsByName(name));
            usedNames.add(name);
            bot.setName(name);

            // 根据排名生成合理的段位分（模拟真实分布）
            int rankPoints;
            if (i < 3) {
                // 前3名：传说段位
                rankPoints = ThreadLocalRandom.current().nextInt(15000, 20000);
            } else if (i < 8) {
                // 4-8名：神话段位
                rankPoints = ThreadLocalRandom.current().nextInt(10000, 15000);
            } else if (i < 15) {
                // 9-15名：大师段位
                rankPoints = ThreadLocalRandom.current().nextInt(8000, 10000);
            } else if (i < 25) {
                // 16-25名：钻石段位
                rankPoints = ThreadLocalRandom.current().nextInt(6500, 8000);
            } else if (i < 35) {
                // 26-35名：铂金段位
                rankPoints = ThreadLocalRandom.current().nextInt(5000, 6500);
            } else if (i < 40) {
                // 36-40名：黄金段位
                rankPoints = ThreadLocalRandom.current().nextInt(3500, 5000);
            } else if (i < 45) {
                // 41-45名：白银段位
                rankPoints = ThreadLocalRandom.current().nextInt(2000, 3500);
            } else {
                // 其他：青铜段位
                rankPoints = ThreadLocalRandom.current().nextInt(0, 2000);
            }

            bot.setRankPoints(rankPoints);
            bot.setLevel(random.nextInt(50) + 1);
            bot.setPerformance(random.nextInt(451) + 50);

            // 根据段位生成合理的胜负记录
            int baseWins = rankPoints / 30;
            int baseLosses = (int) (baseWins * (0.3 + random.nextDouble() * 0.4));

            bot.setWins(baseWins + random.nextInt(50));
            bot.setLosses(baseLosses + random.nextInt(30));
            bot.setTotalKills(bot.getWins() * 2 + random.nextInt(20));
            bot.setTotalDeaths(bot.getLosses() * 2 + random.nextInt(15));

            bots.add(bot);
        }

        return bots;
    }

    /**
     * 创建排行榜项（真实用户）
     */
    private Map<String, Object> createRankingItem(User user, UserStats stats, boolean isBot) {
        Map<String, Object> item = new HashMap<>();
        item.put("userId", user.getId());
        item.put("username", user.getUsername());
        item.put("nickname", user.getNickname());
        item.put("isBot", false); // 不显示AI标识
        item.put("level", stats.getLevel());
        item.put("rankPoints", stats.getRankPoints());
        item.put("performance", stats.getPerformance());
        item.put("performanceGrade", stats.getPerformanceGrade());
        item.put("rankTier", stats.getRankTier());
        item.put("rankSubTier", stats.getRankSubTier());
        item.put("wins", stats.getWins());
        item.put("losses", stats.getLosses());
        item.put("totalKills", stats.getTotalKills());
        item.put("totalDeaths", stats.getTotalDeaths());
        item.put("winRate", Math.round(stats.getWins() * 100.0 / (stats.getWins() + stats.getLosses()) * 100.0) / 100.0);
        item.put("kdRatio", stats.getTotalDeaths() > 0 ?
                Math.round(stats.getTotalKills() * 100.0 / stats.getTotalDeaths()) / 100.0 :
                Math.round(stats.getTotalKills() * 100.0) / 100.0);
        return item;
    }

    /**
     * 创建排行榜项（机器人）
     */
    private Map<String, Object> createRankingItem(RankingBot bot) {
        Map<String, Object> item = new HashMap<>();
        item.put("userId", -bot.getId()); // 负值表示机器人，但不显示
        item.put("username", bot.getName());
        item.put("nickname", bot.getName());
        item.put("isBot", false); // 不显示AI标识
        item.put("level", bot.getLevel());
        item.put("rankPoints", bot.getRankPoints());
        item.put("performance", bot.getPerformance());
        item.put("performanceGrade", bot.getPerformanceGrade());
        item.put("rankTier", bot.getRankTier());
        item.put("rankSubTier", bot.getRankSubTier());
        item.put("wins", bot.getWins());
        item.put("losses", bot.getLosses());
        item.put("totalKills", bot.getTotalKills());
        item.put("totalDeaths", bot.getTotalDeaths());
        item.put("winRate", bot.getWinRate());
        item.put("kdRatio", bot.getKDRatio());
        return item;
    }

    /**
     * 获取用户排名信息
     */
    public Map<String, Object> getUserRanking(Long userId) {
        Map<String, Object> result = new HashMap<>();

        try {
            Optional<UserStats> userStatsOpt = userStatsRepository.findByUserId(userId);
            if (userStatsOpt.isPresent()) {
                UserStats userStats = userStatsOpt.get();
                Optional<User> userOpt = userRepository.findById(userId);

                if (userOpt.isPresent()) {
                    // 获取所有排名数据
                    List<Map<String, Object>> allRanking = getRankingList();

                    // 查找用户在前30名中的排名
                    int userRankInTop30 = -1;
                    for (int i = 0; i < allRanking.size(); i++) {
                        Map<String, Object> item = allRanking.get(i);
                        Object itemUserId = item.get("userId");
                        if (itemUserId != null && itemUserId.equals(userId)) {
                            userRankInTop30 = i + 1;
                            break;
                        }
                    }

                    // 如果用户不在前30名，计算精确排名
                    int exactRank;
                    if (userRankInTop30 != -1) {
                        exactRank = userRankInTop30;
                    } else {
                        exactRank = calculateUserExactRank(userStats);
                    }

                    // 获取总玩家数
                    int totalPlayers = getTotalRankedPlayers();

                    // 创建用户排名信息
                    Map<String, Object> userRankingInfo = createUserRankingInfo(userOpt.get(), userStats, exactRank);

                    result.put("rank", exactRank);
                    result.put("totalPlayers", totalPlayers);
                    result.put("userStats", userRankingInfo);
                    result.put("rankProgress", calculateRankProgress(userStats));
                    result.put("nextRankInfo", getNextRankInfo(userStats));
                    result.put("inTop30", userRankInTop30 != -1);

                } else {
                    result.put("error", "用户不存在");
                }
            } else {
                // 用户没有战斗记录，创建默认排名信息
                result.put("rank", -1);
                result.put("totalPlayers", getTotalRankedPlayers());
                result.put("message", "暂无排名数据，参与排位赛即可获得排名");
                result.put("userStats", createDefaultUserRankingInfo(userId));
            }

        } catch (Exception e) {
            result.put("error", "获取用户排名失败: " + e.getMessage());
        }

        return result;
    }

    /**
     * 计算用户精确排名
     */
    private int calculateUserExactRank(UserStats userStats) {
        // 计算排名高于该用户的真实玩家数量
        long higherRealPlayers = userStatsRepository.countByRankPointsGreaterThan(userStats.getRankPoints());

        // 计算排名高于该用户的机器人数量
        long higherBots = rankingBotRepository.countByRankPointsBetween(userStats.getRankPoints() + 1, Integer.MAX_VALUE);

        return (int) (higherRealPlayers + higherBots) + 1;
    }

    /**
     * 获取总排位玩家数
     */
    private int getTotalRankedPlayers() {
        long userCount = userStatsRepository.count();
        long botCount = rankingBotRepository.count();
        return (int) (userCount + botCount);
    }

    /**
     * 创建用户排名信息
     */
    private Map<String, Object> createUserRankingInfo(User user, UserStats stats, int rank) {
        Map<String, Object> info = new HashMap<>();
        info.put("userId", user.getId());
        info.put("username", user.getUsername());
        info.put("nickname", user.getNickname());
        info.put("level", stats.getLevel());
        info.put("rankPoints", stats.getRankPoints());
        info.put("performance", stats.getPerformance());
        info.put("performanceGrade", stats.getPerformanceGrade());
        info.put("rankTier", stats.getRankTier());
        info.put("rankSubTier", stats.getRankSubTier());
        info.put("wins", stats.getWins());
        info.put("losses", stats.getLosses());
        info.put("totalKills", stats.getTotalKills());
        info.put("totalDeaths", stats.getTotalDeaths());

        // 计算胜率和KD比
        double winRate = stats.getWins() + stats.getLosses() > 0 ?
                Math.round((stats.getWins() * 100.0 / (stats.getWins() + stats.getLosses())) * 100.0) / 100.0 : 0.0;
        double kdRatio = stats.getTotalDeaths() > 0 ?
                Math.round((stats.getTotalKills() * 1.0 / stats.getTotalDeaths()) * 100.0) / 100.0 :
                (stats.getTotalKills() > 0 ? Math.round(stats.getTotalKills() * 100.0) / 100.0 : 0.0);

        info.put("winRate", winRate);
        info.put("kdRatio", kdRatio);
        info.put("winLossRecord", "W" + stats.getWins() + "/L" + stats.getLosses());

        return info;
    }

    /**
     * 创建默认用户排名信息
     */
    private Map<String, Object> createDefaultUserRankingInfo(Long userId) {
        Map<String, Object> info = new HashMap<>();
        info.put("userId", userId);
        info.put("username", "未知");
        info.put("nickname", "新玩家");
        info.put("level", 1);
        info.put("rankPoints", 0);
        info.put("performance", 50);
        info.put("performanceGrade", "D");
        info.put("rankTier", "BRONZE");
        info.put("rankSubTier", 1);
        info.put("wins", 0);
        info.put("losses", 0);
        info.put("totalKills", 0);
        info.put("totalDeaths", 0);
        info.put("winRate", 0.0);
        info.put("kdRatio", 0.0);
        info.put("winLossRecord", "W0/L0");
        return info;
    }

    /**
     * 计算段位进度
     */
    private Map<String, Object> calculateRankProgress(UserStats stats) {
        Map<String, Object> progress = new HashMap<>();
        String currentTier = stats.getRankTier();
        int[] tierRange = RANK_TIERS.get(currentTier);

        if (tierRange != null) {
            int pointsInTier = stats.getRankPoints() - tierRange[0];
            int tierTotalPoints = tierRange[1] - tierRange[0];
            double progressPercentage = tierTotalPoints > 0 ?
                    Math.min(100, Math.round((pointsInTier * 100.0 / tierTotalPoints) * 100.0) / 100.0) : 100.0;

            progress.put("currentPoints", pointsInTier);
            progress.put("tierRange", tierTotalPoints);
            progress.put("percentage", progressPercentage);
            progress.put("pointsToNext", tierRange[1] - stats.getRankPoints() + 1);
        }

        return progress;
    }

    /**
     * 获取下一段位信息
     */
    private Map<String, Object> getNextRankInfo(UserStats stats) {
        Map<String, Object> nextRank = new HashMap<>();
        String currentTier = stats.getRankTier();

        // 找到当前段位在配置中的位置
        List<String> tiers = new ArrayList<>(RANK_TIERS.keySet());
        int currentIndex = tiers.indexOf(currentTier);

        if (currentIndex < tiers.size() - 1) {
            String nextTier = tiers.get(currentIndex + 1);
            int[] nextTierRange = RANK_TIERS.get(nextTier);

            nextRank.put("tier", nextTier);
            nextRank.put("minPoints", nextTierRange[0]);
            nextRank.put("pointsNeeded", Math.max(0, nextTierRange[0] - stats.getRankPoints()));
            nextRank.put("tierName", getTierDisplayName(nextTier));
        } else {
            nextRank.put("tier", "MAX");
            nextRank.put("minPoints", stats.getRankPoints());
            nextRank.put("pointsNeeded", 0);
            nextRank.put("tierName", "最高段位");
        }

        return nextRank;
    }

    /**
     * 获取段位显示名称
     */
    private String getTierDisplayName(String tier) {
        Map<String, String> tierNames = new HashMap<>();
        tierNames.put("BRONZE", "青铜");
        tierNames.put("SILVER", "白银");
        tierNames.put("GOLD", "黄金");
        tierNames.put("PLATINUM", "铂金");
        tierNames.put("DIAMOND", "钻石");
        tierNames.put("MASTER", "大师");
        tierNames.put("MYTHIC", "神话");
        tierNames.put("LEGEND", "传说");
        return tierNames.getOrDefault(tier, tier);
    }
}