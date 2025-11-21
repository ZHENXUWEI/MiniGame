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
        return walletRepository.findByUserId(userId)
                .orElseGet(() -> {
                    UserWallet newWallet = new UserWallet();
                    newWallet.setUserId(userId);
                    newWallet.setMoney(10000);
                    return walletRepository.save(newWallet);
                });
    }

    /**
     * 消费金钱
     */
    @Transactional
    public boolean spendMoney(Long userId, Integer amount) {
        UserWallet wallet = getUserWallet(userId);
        if (wallet.getMoney() >= amount) {
            wallet.setMoney(wallet.getMoney() - amount);
            walletRepository.save(wallet);
            return true;
        }
        return false;
    }

    /**
     * 添加金钱
     */
    @Transactional
    public void addMoney(Long userId, Integer amount) {
        UserWallet wallet = getUserWallet(userId);
        wallet.setMoney(wallet.getMoney() + amount);
        walletRepository.save(wallet);
    }
}