package cn.islys.gms.service;

import cn.islys.gms.entity.Backpack;
import cn.islys.gms.entity.Item;
import cn.islys.gms.repository.BackpackRepository;
import cn.islys.gms.repository.ItemRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class BackpackService {

    @Autowired
    private BackpackRepository backpackRepository;

    @Autowired
    private ItemRepository itemRepository;

    @Autowired
    private WalletService walletService;

    /**
     * 获取用户背包所有物品（包含物品详情）
     */
    public List<Backpack> getUserBackpack(Long userId) {
        List<Backpack> backpackItems = backpackRepository.findByUserIdOrderByPurchaseTimeDesc(userId);

        // 为每个背包物品设置物品详情
        for (Backpack backpack : backpackItems) {
            Optional<Item> itemOpt = itemRepository.findById(backpack.getItemId());
            itemOpt.ifPresent(backpack::setItem);
        }

        return backpackItems;
    }

    /**
     * 按类型筛选背包物品
     */
    public List<Backpack> getBackpackByType(Long userId, String type) {
        List<Backpack> backpackItems = backpackRepository.findByUserIdAndItemType(userId, type);

        for (Backpack backpack : backpackItems) {
            Optional<Item> itemOpt = itemRepository.findById(backpack.getItemId());
            itemOpt.ifPresent(backpack::setItem);
        }

        return backpackItems;
    }

    /**
     * 按关键词搜索背包物品
     */
    public List<Backpack> searchBackpackByKeyword(Long userId, String keyword) {
        List<Backpack> backpackItems = backpackRepository.findByUserIdAndItemNameContaining(userId, keyword);

        for (Backpack backpack : backpackItems) {
            Optional<Item> itemOpt = itemRepository.findById(backpack.getItemId());
            itemOpt.ifPresent(backpack::setItem);
        }

        return backpackItems;
    }

    /**
     * 按类型和关键词搜索背包物品
     */
    public List<Backpack> searchBackpackByTypeAndKeyword(Long userId, String type, String keyword) {
        List<Backpack> backpackItems = backpackRepository.findByUserIdAndItemTypeAndItemNameContaining(userId, type, keyword);

        for (Backpack backpack : backpackItems) {
            Optional<Item> itemOpt = itemRepository.findById(backpack.getItemId());
            itemOpt.ifPresent(backpack::setItem);
        }

        return backpackItems;
    }

    /**
     * 出售物品
     */
    @Transactional
    public boolean sellItem(Long userId, Long backpackId) {
        Optional<Backpack> backpackOpt = backpackRepository.findById(backpackId);

        if (backpackOpt.isPresent()) {
            Backpack backpack = backpackOpt.get();

            // 检查物品是否属于该用户
            if (!backpack.getUserId().equals(userId)) {
                return false;
            }

            // 获取物品信息来计算出售价格
            Optional<Item> itemOpt = itemRepository.findById(backpack.getItemId());
            if (itemOpt.isEmpty()) {
                return false;
            }

            backpack.setItem(itemOpt.get()); // 设置物品信息

            // 计算出售价格
            Integer sellPrice = calculateSellPrice(backpack);
            System.out.println("出售物品: " + backpack.getItem().getName() + ", 价格: " + sellPrice);

            // 如果是子弹且数量大于1，减少数量
            if (backpack.getQuantity() > 1) {
                backpack.setQuantity(backpack.getQuantity() - 1);
                backpackRepository.save(backpack);
            } else {
                backpackRepository.deleteById(backpackId);
            }

            // 添加金钱到用户钱包
            walletService.addMoney(userId, sellPrice);
            System.out.println("用户 " + userId + " 获得金钱: " + sellPrice);

            return true;
        }
        return false;
    }

    /**
     * 计算物品出售价格
     */
    private Integer calculateSellPrice(Backpack backpack) {
        if (backpack.getItem() == null) {
            System.out.println("物品信息为空");
            return 0;
        }

        // 子弹类物品按原价出售
        if ("子弹".equals(backpack.getItem().getType())) {
            int price = backpack.getItem().getValue() * backpack.getQuantity();
            System.out.println("子弹类物品，价格: " + price);
            return price;
        }

        // 武器类物品按耐久比例计算价格
        if (backpack.getCurrentDurability() != null && backpack.getMaxDurability() != null) {
            double durabilityRatio = (double) backpack.getCurrentDurability() / backpack.getMaxDurability();
            int price = (int) (backpack.getItem().getValue() * durabilityRatio);
            System.out.println("武器类物品，耐久比例: " + durabilityRatio + ", 价格: " + price);
            return price;
        }

        System.out.println("默认价格: " + backpack.getItem().getValue());
        return backpack.getItem().getValue();
    }

    /**
     * 获取背包中存在的物品类型
     */
    public List<String> getBackpackItemTypes(Long userId) {
        return backpackRepository.findDistinctItemTypesByUserId(userId);
    }

    /**
     * 添加物品到背包（购买时调用）
     */
    @Transactional
    public boolean addItemToBackpack(Long userId, Long itemId, Integer currentDurability,
                                     Integer maxDurability, Integer purchasePrice, Integer quantity) {
        // 查找物品信息
        Optional<Item> itemOpt = itemRepository.findById(itemId);
        if (itemOpt.isEmpty()) {
            return false;
        }

        Item item = itemOpt.get();

        // 如果是子弹类物品，尝试堆叠
        if ("子弹".equals(item.getType())) {
            List<Backpack> existingBullets = backpackRepository.findByUserIdAndItemId(userId, itemId);
            if (!existingBullets.isEmpty()) {
                // 堆叠子弹
                Backpack existing = existingBullets.get(0);
                existing.setQuantity(existing.getQuantity() + (quantity != null ? quantity : 1));
                backpackRepository.save(existing);
                return true;
            }
        }

        // 创建新的背包物品
        Backpack backpackItem = new Backpack();
        backpackItem.setUserId(userId);
        backpackItem.setItemId(itemId);
        backpackItem.setCurrentDurability(currentDurability);
        backpackItem.setMaxDurability(maxDurability);
        backpackItem.setPurchasePrice(purchasePrice);
        backpackItem.setQuantity(quantity != null ? quantity : 1);

        backpackRepository.save(backpackItem);
        return true;
    }
}