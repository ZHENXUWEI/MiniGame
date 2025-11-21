package cn.islys.gms.service;

import cn.islys.gms.entity.Item;
import cn.islys.gms.repository.ItemRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.util.List;

@Service
public class ItemService {

    @Autowired
    private ItemRepository itemRepository;

    public List<Item> getAllItems() {
        return itemRepository.findAll();
    }

    public List<Item> getItemsByType(String type) {
        if (type == null || type.isEmpty()) {
            return getAllItems();
        }
        return itemRepository.findByType(type);
    }

    public List<Item> searchItems(String keyword) {
        if (keyword == null || keyword.isEmpty()) {
            return getAllItems();
        }
        return itemRepository.findByNameContaining(keyword);
    }

    public List<Item> searchItemsByTypeAndKeyword(String type, String keyword) {
        if ((type == null || type.isEmpty()) && (keyword == null || keyword.isEmpty())) {
            return getAllItems();
        } else if (type == null || type.isEmpty()) {
            return searchItems(keyword);
        } else if (keyword == null || keyword.isEmpty()) {
            return getItemsByType(type);
        } else {
            return itemRepository.findByTypeAndNameContaining(type, keyword);
        }
    }

    public List<String> getAllTypes() {
        return itemRepository.findDistinctTypes();
    }

    public Item saveItem(Item item) {
        return itemRepository.save(item);
    }

    public void deleteItem(Long id) {
        itemRepository.deleteById(id);
    }

    public Item getItemById(Long id) {
        return itemRepository.findById(id).orElse(null);
    }

    /**
     * 根据类型获取该类型下的所有物品名称
     */
    public List<String> getItemNamesByType(String type) {
        return itemRepository.findItemNamesByType(type);
    }

    /**
     * 根据名称获取物品
     */
    public Item getItemByName(String name) {
        return itemRepository.findByName(name).orElse(null);
    }
}
