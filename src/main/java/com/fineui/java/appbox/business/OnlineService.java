package com.fineui.java.appbox.business;

import com.fineui.java.appbox.model.Online;
import com.fineui.java.appbox.repository.OnlineRepository;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDateTime;

/**
 * 在线用户统计：登录时登记一条在线记录（IP + 登录时间），之后每次请求刷新「最后操作时间」，
 * 但同一会话每 {@value #UPDATE_INTERVAL_MINUTES} 分钟最多写库一次（上次写库时间记在 Session 里做节流）。
 */
@Service
public class OnlineService {

    /** Session 键：本会话上次刷新在线记录的时间。 */
    private static final String SK_ONLINE_UPDATE_TIME = "OnlineUpdateTime";

    /** 刷新在线记录的最小间隔（分钟）。 */
    private static final int UPDATE_INTERVAL_MINUTES = 5;

    private final OnlineRepository onlineRepository;

    public OnlineService(OnlineRepository onlineRepository) {
        this.onlineRepository = onlineRepository;
    }

    /** 登录成功：登记（或刷新）该用户的在线记录。 */
    @Transactional
    public void register(int userId, String ipAddress, HttpSession session) {
        LocalDateTime now = LocalDateTime.now();
        Online online = onlineRepository.findByUserId(userId).orElseGet(Online::new);
        online.setUserId(userId);
        online.setIpAddress(ipAddress);
        online.setLoginTime(now);
        online.setUpdateTime(now);
        onlineRepository.save(online);
        session.setAttribute(SK_ONLINE_UPDATE_TIME, now);
    }

    /** 每次请求调用：距上次写库超过间隔才刷新最后操作时间。 */
    @Transactional
    public void update(int userId, HttpSession session) {
        LocalDateTime now = LocalDateTime.now();
        Object last = session.getAttribute(SK_ONLINE_UPDATE_TIME);
        if (last instanceof LocalDateTime lastTime
                && Duration.between(lastTime, now).toMinutes() < UPDATE_INTERVAL_MINUTES) {
            return;
        }
        session.setAttribute(SK_ONLINE_UPDATE_TIME, now);
        onlineRepository.findByUserId(userId).ifPresent(online -> {
            online.setUpdateTime(now);
            onlineRepository.save(online);
        });
    }

    /** 最近 {@code minutes} 分钟内有操作的用户数。 */
    public long countActive(int minutes) {
        return onlineRepository.countByUpdateTimeAfter(LocalDateTime.now().minusMinutes(minutes));
    }
}
