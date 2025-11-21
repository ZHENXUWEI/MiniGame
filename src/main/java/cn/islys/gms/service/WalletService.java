package cn.islys.gms.service;

import cn.islys.gms.entity.UserWallet;
import cn.islys.gms.repository.UserWalletRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
public class WalletService {

    @Autowired
    private UserWalletRepository walletRepository;

    /**
     * 获取用户钱包
     */
    public UserWallet getUserWallet(Long userId) {
        Optional<UserWallet> walletOpt = walletRepository.findByUserId(userId);
        if (walletOpt.isPresent()) {
            UserWallet wallet = walletOpt.get();
            System.out.println("获取用户 " + userId + " 钱包，当前金额: " + wallet.getMoney());
            return wallet;
        } else {
            // 创建新钱包
            UserWallet newWallet = new UserWallet();
            newWallet.setUserId(userId);
            newWallet.setMoney(10000);
            UserWallet savedWallet = walletRepository.save(newWallet);
            System.out.println("创建用户 " + userId + " 新钱包，初始金额: 10000");
            return savedWallet;
        }
    }

    /**
     * 消费金钱
     */
    @Transactional
    public boolean spendMoney(Long userId, Integer amount) {
        UserWallet wallet = getUserWallet(userId);
        System.out.println("用户 " + userId + " 尝试消费 " + amount + "，当前金额: " + wallet.getMoney());

        if (wallet.getMoney() >= amount) {
            wallet.setMoney(wallet.getMoney() - amount);
            UserWallet savedWallet = walletRepository.save(wallet);
            System.out.println("消费成功，剩余金额: " + savedWallet.getMoney());
            return true;
        }
        System.out.println("消费失败，金额不足");
        return false;
    }

    /**
     * 添加金钱
     */
    @Transactional
    public void addMoney(Long userId, Integer amount) {
        UserWallet wallet = getUserWallet(userId);
        System.out.println("用户 " + userId + " 添加金钱 " + amount + "，添加前金额: " + wallet.getMoney());

        wallet.setMoney(wallet.getMoney() + amount);
        UserWallet savedWallet = walletRepository.save(wallet);
        System.out.println("添加金钱成功，当前金额: " + savedWallet.getMoney());
    }
}