package cn.islys.gms.service;

import cn.islys.gms.entity.Item;
import cn.islys.gms.repository.ItemRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.concurrent.ThreadLocalRandom;

@Service
public class MarketService {

    @Autowired
    private ItemRepository itemRepository;

    @Autowired
    private BackpackService backpackService;

    @Autowired
    private WalletService walletService;

    /**
     * 获取所有物品类型
     */
    public List<String> getAllItemTypes() {
        return itemRepository.findDistinctTypes();
    }

    /**
     * 根据类型获取该类型下的所有物品名称
     */
    public List<String> getItemNamesByType(String type) {
        return itemRepository.findItemNamesByType(type);
    }

    /**
     * 根据物品名称生成市场物品（带价格浮动）
     */
    public List<Map<String, Object>> generateMarketItemsByName(String itemName) {
        Optional<Item> itemOpt = itemRepository.findByName(itemName);
        List<Map<String, Object>> marketItems = new ArrayList<>();

        if (itemOpt.isEmpty()) {
            return marketItems;
        }

        Item item = itemOpt.get();

        // 随机生成10-20个该物品
        int count = ThreadLocalRandom.current().nextInt(10, 21);

        for (int i = 0; i < count; i++) {
            Map<String, Object> marketItem = new HashMap<>();
            marketItem.put("item", item);

            int basePrice = item.getValue();
            int purchasePrice = basePrice;
            Float adjustedDamage = item.getDamage(); // 调整后的伤害

            // 如果是武器类（非子弹），生成随机耐久、价格浮动和伤害调整
            if (!"子弹".equals(item.getType())) {
                String durabilityStr = item.getDurability();
                if (durabilityStr != null && !durabilityStr.isEmpty()) {
                    try {
                        int maxDurability = Integer.parseInt(durabilityStr);
                        int currentDurability = generateRandomDurability(maxDurability);
                        double durabilityRatio = (double) currentDurability / maxDurability;

                        // 根据耐久状态调整伤害
                        String durabilityStatus = getDurabilityStatus(currentDurability, maxDurability);
                        adjustedDamage = calculateAdjustedDamage(item.getDamage(), durabilityStatus);

                        // 价格浮动：基础价格 * 耐久比例 * (0.75 ~ 1.25)
                        double priceMultiplier = ThreadLocalRandom.current().nextDouble(0.75, 1.25);
                        purchasePrice = (int) (basePrice * durabilityRatio * priceMultiplier);

                        marketItem.put("currentDurability", currentDurability);
                        marketItem.put("maxDurability", maxDurability);
                        marketItem.put("durabilityStatus", durabilityStatus);
                        marketItem.put("priceMultiplier", priceMultiplier);
                        marketItem.put("adjustedDamage", adjustedDamage);
                    } catch (NumberFormatException e) {
                        // 耐久值格式错误
                    }
                }
            } else {
                // 子弹类物品：随机数量和价格浮动
                int quantity = ThreadLocalRandom.current().nextInt(3, 21); // 3-20发
                double priceMultiplier = ThreadLocalRandom.current().nextDouble(0.75, 1.25);
                purchasePrice = (int) (basePrice * quantity * priceMultiplier);

                marketItem.put("quantity", quantity);
                marketItem.put("priceMultiplier", priceMultiplier);
                marketItem.put("isBullet", true);
            }

            marketItem.put("purchasePrice", purchasePrice);
            marketItem.put("basePrice", basePrice);
            marketItems.add(marketItem);
        }

        return marketItems;
    }

    /**
     * 对市场物品进行排序
     */
    public List<Map<String, Object>> sortMarketItems(List<Map<String, Object>> marketItems, String sortBy, String sortOrder) {
        marketItems.sort((a, b) -> {
            int result = 0;

            switch (sortBy) {
                case "durability":
                    result = compareDurability(a, b);
                    break;
                case "price":
                    result = comparePrice(a, b);
                    break;
                default:
                    result = 0;
            }

            // 处理排序顺序
            if ("asc".equals(sortOrder)) {
                return result;
            } else {
                return -result;
            }
        });

        return marketItems;
    }

    /**
     * 比较耐久度
     */
    private int compareDurability(Map<String, Object> a, Map<String, Object> b) {
        Integer durabilityA = (Integer) a.get("currentDurability");
        Integer durabilityB = (Integer) b.get("currentDurability");

        if (durabilityA == null && durabilityB == null) return 0;
        if (durabilityA == null) return -1;
        if (durabilityB == null) return 1;

        return durabilityA.compareTo(durabilityB);
    }

    /**
     * 比较价格
     */
    private int comparePrice(Map<String, Object> a, Map<String, Object> b) {
        Integer priceA = (Integer) a.get("purchasePrice");
        Integer priceB = (Integer) b.get("purchasePrice");
        return priceA.compareTo(priceB);
    }

    /**
     * 根据耐久状态计算调整后的伤害
     */
    private Float calculateAdjustedDamage(Float baseDamage, String durabilityStatus) {
        return switch (durabilityStatus) {
            case "崭新出产" -> baseDamage; // 伤害不变
            case "略有磨损" -> baseDamage * 0.85f; // 减少15%伤害
            case "破损不堪" -> baseDamage * 0.70f; // 减少30%伤害
            default -> baseDamage;
        };
    }

    /**
     * 购买物品
     */
    @Transactional
    public boolean purchaseItem(Integer userId, Long itemId, Integer currentDurability,
                                Integer maxDurability, Integer purchasePrice, Integer quantity) {
        // 检查金钱是否足够
        if (!walletService.spendMoney(userId, purchasePrice)) {
            return false;
        }

        // 添加物品到背包
        return backpackService.addItemToBackpack(userId, itemId, currentDurability, maxDurability, purchasePrice, quantity);
    }

    /**
     * 生成随机耐久（排除低于15%的情况）
     */
    private int generateRandomDurability(int maxDurability) {
        // 生成三种状态：崭新出产(85-100%)、略有磨损(60-85%)、破损不堪(15-59%)
        int state = ThreadLocalRandom.current().nextInt(3);

        switch (state) {
            case 0: // 崭新出产
                return ThreadLocalRandom.current().nextInt(
                        (int)(maxDurability * 0.85), maxDurability + 1);
            case 1: // 略有磨损
                return ThreadLocalRandom.current().nextInt(
                        (int)(maxDurability * 0.60), (int)(maxDurability * 0.85));
            case 2: // 破损不堪
                return ThreadLocalRandom.current().nextInt(
                        (int)(maxDurability * 0.15), (int)(maxDurability * 0.60));
            default:
                return maxDurability;
        }
    }

    /**
     * 获取耐久状态描述
     */
    private String getDurabilityStatus(int currentDurability, int maxDurability) {
        double ratio = (double) currentDurability / maxDurability;
        if (ratio >= 0.85) {
            return "崭新出产";
        } else if (ratio >= 0.60) {
            return "略有磨损";
        } else if (ratio >= 0.15) {
            return "破损不堪";
        } else {
            return "无法使用";
        }
    }
}