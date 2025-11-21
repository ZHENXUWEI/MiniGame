package cn.islys.gms.service;

import cn.islys.gms.entity.RankingBot;
import cn.islys.gms.entity.User;
import cn.islys.gms.entity.UserStats;
import cn.islys.gms.repository.RankingBotRepository;
import cn.islys.gms.repository.UserRepository;
import cn.islys.gms.repository.UserStatsRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.concurrent.ThreadLocalRandom;

@Service
public class RankingService {

    @Autowired
    private UserStatsRepository userStatsRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RankingBotRepository rankingBotRepository;

    // 段位配置
    private static final Map<String, RankTierConfig> RANK_TIERS = new LinkedHashMap<>();

    static {
        RANK_TIERS.put("BRONZE", new RankTierConfig(0, 1499, 3));
        RANK_TIERS.put("SILVER", new RankTierConfig(1500, 2999, 3));
        RANK_TIERS.put("GOLD", new RankTierConfig(3000, 4499, 3));
        RANK_TIERS.put("PLATINUM", new RankTierConfig(4500, 5999, 3));
        RANK_TIERS.put("DIAMOND", new RankTierConfig(6000, 7999, 3));
        RANK_TIERS.put("MASTER", new RankTierConfig(8000, 9999, 1));
        RANK_TIERS.put("MYTHIC", new RankTierConfig(10000, 14999, 1));
        RANK_TIERS.put("LEGEND", new RankTierConfig(15000, Integer.MAX_VALUE, 1));
    }

    /**
     * 获取完整排行榜（前30名）
     */
    public List<Map<String, Object>> getRankingList() {
        List<Map<String, Object>> rankingList = new ArrayList<>();

        // 获取真实用户排行榜
        List<UserStats> userStats = userStatsRepository.findTop30ByRankPoints();
        for (UserStats stats : userStats) {
            Optional<User> userOpt = userRepository.findById(stats.getUserId());
            if (userOpt.isPresent() && userOpt.get().getStatus() == 1) {
                Map<String, Object> rankingItem = createRankingItem(userOpt.get(), stats, false);
                rankingList.add(rankingItem);
            }
        }

        // 如果不足30人，添加机器人
        if (rankingList.size() < 30) {
            int needed = 30 - rankingList.size();
            List<RankingBot> bots = rankingBotRepository.findTop30ByRankPoints();

            for (int i = 0; i < Math.min(needed, bots.size()); i++) {
                RankingBot bot = bots.get(i);
                Map<String, Object> rankingItem = createRankingItem(bot);
                rankingList.add(rankingItem);
            }
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
            // 添加段位图标路径
            rankingList.get(i).put("rankIcon", getRankIconPath(
                    (String) rankingList.get(i).get("rankTier"),
                    (Integer) rankingList.get(i).get("rankSubTier")
            ));
        }

        return rankingList;
    }

    /**
     * 获取用户排名信息
     */
    public Map<String, Object> getUserRanking(Long userId) {
        Map<String, Object> result = new HashMap<>();

        Optional<UserStats> userStatsOpt = userStatsRepository.findByUserId(userId);
        if (userStatsOpt.isPresent()) {
            UserStats userStats = userStatsOpt.get();
            Optional<User> userOpt = userRepository.findById(userId);

            if (userOpt.isPresent()) {
                // 获取所有排名
                List<Map<String, Object>> allRanking = getRankingList();

                // 查找用户排名
                int userRank = -1;
                for (int i = 0; i < allRanking.size(); i++) {
                    Map<String, Object> item = allRanking.get(i);
                    if (userId.equals(item.get("userId"))) {
                        userRank = i + 1;
                        break;
                    }
                }

                // 如果不在前30名，计算准确排名
                if (userRank == -1) {
                    userRank = calculateUserExactRank(userStats);
                }

                result.put("rank", userRank);
                result.put("totalPlayers", getTotalRankedPlayers());
                result.put("userStats", createRankingItem(userOpt.get(), userStats, false));
                result.put("rankProgress", calculateRankProgress(userStats));
                result.put("nextRankInfo", getNextRankInfo(userStats));
            }
        } else {
            // 用户没有战斗记录，创建默认数据
            result.put("rank", -1);
            result.put("totalPlayers", getTotalRankedPlayers());
            result.put("message", "暂无排名数据");
        }

        return result;
    }

    /**
     * 获取段位排行榜（按段位分组）
     */
    public Map<String, List<Map<String, Object>>> getRankingByTiers() {
        Map<String, List<Map<String, Object>>> tierRanking = new LinkedHashMap<>();

        // 初始化每个段位的列表
        for (String tier : RANK_TIERS.keySet()) {
            tierRanking.put(tier, new ArrayList<>());
        }

        // 获取所有排行榜数据
        List<Map<String, Object>> allRanking = getRankingList();

        // 按段位分组
        for (Map<String, Object> player : allRanking) {
            String tier = (String) player.get("rankTier");
            if (tierRanking.containsKey(tier)) {
                tierRanking.get(tier).add(player);
            }
        }

        return tierRanking;
    }

    /**
     * 获取附近排名（用户前后5名）
     */
    public List<Map<String, Object>> getNearbyRanking(Long userId) {
        List<Map<String, Object>> nearbyRanking = new ArrayList<>();
        List<Map<String, Object>> allRanking = getRankingList();

        int userIndex = -1;
        for (int i = 0; i < allRanking.size(); i++) {
            if (userId.equals(allRanking.get(i).get("userId"))) {
                userIndex = i;
                break;
            }
        }

        if (userIndex == -1) {
            // 用户不在前30名，返回末尾几名
            int start = Math.max(0, allRanking.size() - 5);
            return allRanking.subList(start, allRanking.size());
        }

        int start = Math.max(0, userIndex - 2);
        int end = Math.min(allRanking.size(), userIndex + 3);

        return allRanking.subList(start, end);
    }

    /**
     * 刷新排行榜机器人数据（定时任务）
     */
    @Transactional
    @Scheduled(fixedRate = 3600000) // 每小时执行一次
    public void refreshRankingBots() {
        System.out.println("开始刷新排行榜机器人数据...");

        // 删除旧的机器人数据
        rankingBotRepository.deleteAll();

        // 生成新的机器人数据
        List<RankingBot> bots = generateRankingBots(50); // 生成50个机器人确保有足够的选择

        rankingBotRepository.saveAll(bots);
        System.out.println("排行榜机器人数据刷新完成，生成 " + bots.size() + " 个机器人");
    }

    /**
     * 手动刷新排行榜机器人数据
     */
    @Transactional
    public void manualRefreshRankingBots() {
        refreshRankingBots();
    }

    /**
     * 获取段位统计信息
     */
    public Map<String, Object> getRankTierStatistics() {
        Map<String, Object> statistics = new HashMap<>();
        List<Map<String, Object>> allRanking = getRankingList();

        Map<String, Integer> tierCount = new HashMap<>();
        Map<String, Double> tierPercentage = new HashMap<>();

        // 初始化
        for (String tier : RANK_TIERS.keySet()) {
            tierCount.put(tier, 0);
            tierPercentage.put(tier, 0.0);
        }

        // 统计各段位人数
        for (Map<String, Object> player : allRanking) {
            String tier = (String) player.get("rankTier");
            tierCount.put(tier, tierCount.get(tier) + 1);
        }

        // 计算百分比
        int total = allRanking.size();
        for (String tier : tierCount.keySet()) {
            double percentage = total > 0 ? (tierCount.get(tier) * 100.0 / total) : 0;
            tierPercentage.put(tier, Math.round(percentage * 100.0) / 100.0);
        }

        statistics.put("tierCount", tierCount);
        statistics.put("tierPercentage", tierPercentage);
        statistics.put("totalPlayers", total);

        return statistics;
    }

    /**
     * 生成排行榜机器人
     */
    private List<RankingBot> generateRankingBots(int count) {
        List<RankingBot> bots = new ArrayList<>();
        Random random = new Random();

        for (int i = 0; i < count; i++) {
            RankingBot bot = new RankingBot();
            bot.setName(generateBotName());

            // 根据排名生成合理的段位分
            int rankPoints;
            if (i < 5) {
                // 前5名：传说/神话段位
                rankPoints = ThreadLocalRandom.current().nextInt(10000, 15000);
            } else if (i < 15) {
                // 6-15名：大师/钻石段位
                rankPoints = ThreadLocalRandom.current().nextInt(6000, 10000);
            } else if (i < 30) {
                // 16-30名：铂金/黄金段位
                rankPoints = ThreadLocalRandom.current().nextInt(3000, 6000);
            } else {
                // 其他：白银/青铜段位
                rankPoints = ThreadLocalRandom.current().nextInt(0, 3000);
            }

            bot.setRankPoints(rankPoints);
            bot.setLevel(random.nextInt(50) + 1);
            bot.setPerformance(random.nextInt(451) + 50);

            // 根据段位生成合理的胜负记录
            int baseWins = rankPoints / 100;
            int baseLosses = (int) (baseWins * (0.3 + random.nextDouble() * 0.4));

            bot.setWins(baseWins + random.nextInt(50));
            bot.setLosses(baseLosses + random.nextInt(30));
            bot.setUpdatedTime(java.time.LocalDateTime.now());

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
        item.put("isBot", isBot);
        item.put("level", stats.getLevel());
        item.put("experience", stats.getExperience());
        item.put("rankPoints", stats.getRankPoints());
        item.put("performance", stats.getPerformance());
        item.put("performanceGrade", stats.getPerformanceGrade());
        item.put("rankTier", stats.getRankTier());
        item.put("rankSubTier", stats.getRankSubTier());
        item.put("wins", stats.getWins());
        item.put("losses", stats.getLosses());
        item.put("totalKills", stats.getTotalKills());
        item.put("totalDeaths", stats.getTotalDeaths());
        item.put("winRate", calculateWinRate(stats.getWins(), stats.getLosses()));
        item.put("kdRatio", calculateKDRatio(stats.getTotalKills(), stats.getTotalDeaths()));
        item.put("lastUpdated", stats.getUpdatedTime());
        return item;
    }

    /**
     * 创建排行榜项（机器人）
     */
    private Map<String, Object> createRankingItem(RankingBot bot) {
        Map<String, Object> item = new HashMap<>();
        item.put("userId", -bot.getId()); // 负值表示机器人
        item.put("username", bot.getName());
        item.put("nickname", bot.getName());
        item.put("isBot", true);
        item.put("level", bot.getLevel());
        item.put("experience", bot.getLevel() * 1000);
        item.put("rankPoints", bot.getRankPoints());
        item.put("performance", bot.getPerformance());
        item.put("performanceGrade", getPerformanceGrade(bot.getPerformance()));
        item.put("rankTier", getRankTier(bot.getRankPoints()));
        item.put("rankSubTier", getRankSubTier(bot.getRankPoints()));
        item.put("wins", bot.getWins());
        item.put("losses", bot.getLosses());
        item.put("totalKills", bot.getWins() * 2); // 估算击杀数
        item.put("totalDeaths", bot.getLosses() * 2); // 估算死亡数
        item.put("winRate", calculateWinRate(bot.getWins(), bot.getLosses()));
        item.put("kdRatio", calculateKDRatio(bot.getWins() * 2, bot.getLosses() * 2));
        item.put("lastUpdated", bot.getUpdatedTime());
        return item;
    }

    /**
     * 计算用户精确排名
     */
    private int calculateUserExactRank(UserStats userStats) {
        // 计算排名高于该用户的玩家数量
        long higherRankCount = userStatsRepository.countByRankPointsGreaterThan(userStats.getRankPoints());
        return (int) higherRankCount + 1;
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
     * 计算段位进度
     */
    private Map<String, Object> calculateRankProgress(UserStats stats) {
        Map<String, Object> progress = new HashMap<>();
        String currentTier = stats.getRankTier();
        RankTierConfig tierConfig = RANK_TIERS.get(currentTier);

        if (tierConfig != null) {
            int pointsInTier = stats.getRankPoints() - tierConfig.minPoints;
            int tierRange = tierConfig.maxPoints - tierConfig.minPoints;
            double progressPercentage = (double) pointsInTier / tierRange * 100;

            progress.put("currentPoints", pointsInTier);
            progress.put("tierRange", tierRange);
            progress.put("percentage", Math.min(100, Math.round(progressPercentage * 100.0) / 100.0));
            progress.put("pointsToNext", tierConfig.maxPoints - stats.getRankPoints() + 1);
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
            RankTierConfig nextConfig = RANK_TIERS.get(nextTier);

            nextRank.put("tier", nextTier);
            nextRank.put("minPoints", nextConfig.minPoints);
            nextRank.put("pointsNeeded", nextConfig.minPoints - stats.getRankPoints());
        } else {
            nextRank.put("tier", "MAX");
            nextRank.put("minPoints", stats.getRankPoints());
            nextRank.put("pointsNeeded", 0);
        }

        return nextRank;
    }

    /**
     * 获取段位图标路径
     */
    private String getRankIconPath(String tier, Integer subTier) {
        if ("LEGEND".equals(tier)) {
            return "/images/ranks/legend.png";
        } else if (subTier != null) {
            return String.format("/images/ranks/%s_%d.png", tier.toLowerCase(), subTier);
        } else {
            return String.format("/images/ranks/%s.png", tier.toLowerCase());
        }
    }

    /**
     * 生成机器人名字
     */
    private String generateBotName() {
        String[] firstNames = {"暗影", "雷霆", "风暴", "烈焰", "冰霜", "幽灵", "钢铁", "黄金", "王者", "战神",
                "疾风", "幻影", "星辰", "月光", "烈日", "寒冰", "狂暴", "神圣", "黑暗", "光明"};
        String[] lastNames = {"杀手", "猎手", "战士", "刺客", "枪手", "先锋", "守卫", "大师", "传奇", "英雄",
                "之魂", "之怒", "之翼", "之心", "之眼", "之手", "之刃", "之盾", "之灵", "之影"};

        Random random = new Random();
        String firstName = firstNames[random.nextInt(firstNames.length)];
        String lastName = lastNames[random.nextInt(lastNames.length)];

        return firstName + lastName + (random.nextInt(900) + 100);
    }

    /**
     * 计算胜率
     */
    private double calculateWinRate(Integer wins, Integer losses) {
        int total = wins + losses;
        if (total == 0) return 0.0;
        return Math.round((wins * 100.0 / total) * 100.0) / 100.0;
    }

    /**
     * 计算KD比率
     */
    private double calculateKDRatio(Integer kills, Integer deaths) {
        if (deaths == 0) return kills > 0 ? kills : 0.0;
        return Math.round((kills * 1.0 / deaths) * 100.0) / 100.0;
    }

    /**
     * 获取表现等级
     */
    private String getPerformanceGrade(Integer performance) {
        if (performance >= 400) return "S";
        if (performance >= 300) return "A";
        if (performance >= 200) return "B";
        if (performance >= 100) return "C";
        return "D";
    }

    /**
     * 获取段位等级
     */
    private String getRankTier(Integer rankPoints) {
        for (Map.Entry<String, RankTierConfig> entry : RANK_TIERS.entrySet()) {
            if (rankPoints >= entry.getValue().minPoints && rankPoints <= entry.getValue().maxPoints) {
                return entry.getKey();
            }
        }
        return "BRONZE";
    }

    /**
     * 获取小段位
     */
    private Integer getRankSubTier(Integer rankPoints) {
        for (Map.Entry<String, RankTierConfig> entry : RANK_TIERS.entrySet()) {
            RankTierConfig config = entry.getValue();
            if (rankPoints >= config.minPoints && rankPoints <= config.maxPoints) {
                if (config.subTiers == 1) return null;

                int pointsInTier = rankPoints - config.minPoints;
                int pointsPerSubTier = (config.maxPoints - config.minPoints) / config.subTiers;
                return (pointsInTier / pointsPerSubTier) + 1;
            }
        }
        return 1;
    }

    /**
     * 段位配置内部类
     */
    private static class RankTierConfig {
        int minPoints;
        int maxPoints;
        int subTiers;

        RankTierConfig(int minPoints, int maxPoints, int subTiers) {
            this.minPoints = minPoints;
            this.maxPoints = maxPoints;
            this.subTiers = subTiers;
        }
    }
}