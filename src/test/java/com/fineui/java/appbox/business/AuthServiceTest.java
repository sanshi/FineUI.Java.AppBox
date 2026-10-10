package com.fineui.java.appbox.business;

import com.fineui.java.appbox.model.Power;
import com.fineui.java.appbox.model.User;
import com.fineui.java.appbox.repository.PowerRepository;
import com.fineui.java.appbox.repository.RoleRepository;
import com.fineui.java.appbox.repository.UserRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class AuthServiceTest {

    private static final String POWER_NAME = "CoreUserDelete";
    private static final int USER_ID = 105;
    private static final AppBoxUser USER = new AppBoxUser(USER_ID, "tester", List.of(1));

    private UserRepository userRepository;
    private PowerRepository powerRepository;
    private AuthService authService;
    private MockHttpSession session;

    @BeforeEach
    void setUp() {
        userRepository = mock(UserRepository.class);
        powerRepository = mock(PowerRepository.class);
        authService = new AuthService(
                mock(SecurityContextRepository.class),
                mock(RoleRepository.class),
                powerRepository,
                userRepository,
                mock(OnlineService.class));
        session = new MockHttpSession();
        bindRequest(session, USER);
    }

    @AfterEach
    void clearRequest() {
        SecurityContextHolder.clearContext();
        RequestContextHolder.resetRequestAttributes();
    }

    @Test
    void staleQueryMustBeRequeriedAfterRevocation() throws Exception {
        CountDownLatch oldPermissionsRead = new CountDownLatch(1);
        CountDownLatch allowQueryToReturn = new CountDownLatch(1);
        AtomicReference<List<String>> databasePermissions = new AtomicReference<>(List.of(POWER_NAME));
        pauseFirstQuery(databasePermissions, oldPermissionsRead, allowQueryToReturn);
        ExecutorService executor = Executors.newSingleThreadExecutor();

        try {
            Future<List<String>> oldRequest = submitRequest(executor, session, authService::getRolePowerNames);
            await(oldPermissionsRead);

            // 查询已取到“可以删除”后再撤权：旧请求可以返回旧快照，下一请求必须重新查询并拒绝删除。
            databasePermissions.set(List.of());
            AuthService.invalidatePermissionCaches();
            allowQueryToReturn.countDown();

            assertEquals(List.of(POWER_NAME), oldRequest.get(5, TimeUnit.SECONDS));
            assertEquals(List.of(), authService.getRolePowerNames());
            assertFalse(authService.checkPower(POWER_NAME));
            verify(userRepository, times(2)).findPowerNamesByUserId(USER_ID);
        } finally {
            allowQueryToReturn.countDown();
            stop(executor);
        }
    }

    @Test
    void concurrentWritesMustKeepPermissionsAndVersionTogether() throws Exception {
        CountDownLatch oldPermissionsRead = new CountDownLatch(1);
        CountDownLatch allowOldQueryToReturn = new CountDownLatch(1);
        CountDownLatch freshCacheWritten = new CountDownLatch(1);
        CountDownLatch allowFreshRequestToFinish = new CountDownLatch(1);
        AtomicReference<Thread> freshWriter = new AtomicReference<>();
        AtomicReference<List<String>> databasePermissions = new AtomicReference<>(List.of(POWER_NAME));
        pauseFirstQuery(databasePermissions, oldPermissionsRead, allowOldQueryToReturn);

        MockHttpSession sharedSession = new MockHttpSession() {
            @Override
            public void setAttribute(String name, Object value) {
                super.setAttribute(name, value);
                if ("UserPowerList".equals(name) && Thread.currentThread() == freshWriter.get()) {
                    freshCacheWritten.countDown();
                    await(allowFreshRequestToFinish);
                }
            }
        };
        bindRequest(sharedSession, USER);
        ExecutorService executor = Executors.newFixedThreadPool(2);

        try {
            Future<List<String>> oldRequest = submitRequest(executor, sharedSession, authService::getRolePowerNames);
            await(oldPermissionsRead);
            databasePermissions.set(List.of());
            AuthService.invalidatePermissionCaches();

            Future<List<String>> freshRequest = submitRequest(executor, sharedSession, () -> {
                freshWriter.set(Thread.currentThread());
                return authService.getRolePowerNames();
            });
            await(freshCacheWritten);

            // 新请求写入新列表后暂停，旧请求再覆盖缓存，最后让新请求结束，检验两份结果会不会被拼接。
            allowOldQueryToReturn.countDown();
            assertEquals(List.of(POWER_NAME), oldRequest.get(5, TimeUnit.SECONDS));
            allowFreshRequestToFinish.countDown();
            assertEquals(List.of(), freshRequest.get(5, TimeUnit.SECONDS));

            assertEquals(List.of(), authService.getRolePowerNames());
            assertFalse(authService.checkPower(POWER_NAME));
            verify(userRepository, times(3)).findPowerNamesByUserId(USER_ID);
        } finally {
            allowOldQueryToReturn.countDown();
            allowFreshRequestToFinish.countDown();
            stop(executor);
        }
    }

    @Test
    void legacyListCacheMustBeRecomputedEvenWithCurrentVersion() {
        AtomicLong version = (AtomicLong) ReflectionTestUtils.getField(AuthService.class, "PERMISSION_VERSION");
        session.setAttribute("UserPowerList", new ArrayList<>(List.of(POWER_NAME)));
        session.setAttribute("UserPowerVersion", version.get());
        when(userRepository.findPowerNamesByUserId(USER_ID)).thenReturn(List.of());

        assertEquals(List.of(), authService.getRolePowerNames());
        verify(userRepository).findPowerNamesByUserId(USER_ID);
    }

    @Test
    void returnedPermissionsMustBeImmutableAndIndependentOfRepositoryResults() {
        List<String> databasePermissions = new ArrayList<>(List.of(POWER_NAME));
        when(userRepository.findPowerNamesByUserId(USER_ID)).thenReturn(databasePermissions);

        List<String> permissions = authService.getRolePowerNames();
        assertThrows(UnsupportedOperationException.class, () -> permissions.add("CoreUserEdit"));
        databasePermissions.clear();
        assertEquals(List.of(POWER_NAME), authService.getRolePowerNames());
        verify(userRepository).findPowerNamesByUserId(USER_ID);
    }

    @Test
    void unchangedVersionMustReuseCacheWithoutQueryingAgain() {
        when(userRepository.findPowerNamesByUserId(USER_ID)).thenReturn(List.of(POWER_NAME));

        assertEquals(List.of(POWER_NAME), authService.getRolePowerNames());
        assertTrue(authService.checkPower(POWER_NAME));
        verify(userRepository).findPowerNamesByUserId(USER_ID);
    }

    @Test
    void administratorMustReceiveAllPermissions() {
        Power power = new Power();
        power.setName(POWER_NAME);
        when(powerRepository.findAll()).thenReturn(List.of(power));
        bindRequest(session, new AppBoxUser(1, "admin", List.of()));

        assertEquals(List.of(POWER_NAME), authService.getRolePowerNames());
        verifyNoInteractions(userRepository);
    }

    @Test
    void anonymousRequestMustNotCreateSessionOrQueryPermissions() {
        MockHttpServletRequest request = bindRequest(null, null);

        assertEquals(List.of(), authService.getRolePowerNames());
        assertNull(request.getSession(false));
        verifyNoInteractions(userRepository, powerRepository);
    }

    @Test
    void cachedValueMustSurviveJavaSerialization() throws Exception {
        when(userRepository.findPowerNamesByUserId(USER_ID)).thenReturn(List.of(POWER_NAME));
        authService.getRolePowerNames();
        Object cached = session.getAttribute("UserPowerList");
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        try (ObjectOutputStream output = new ObjectOutputStream(bytes)) {
            output.writeObject(cached);
        }

        try (ObjectInputStream input = new ObjectInputStream(new ByteArrayInputStream(bytes.toByteArray()))) {
            assertEquals(cached, input.readObject());
        }
    }

    @Test
    void loginMustClearCurrentAndLegacyPermissionCaches() {
        when(userRepository.findPowerNamesByUserId(USER_ID)).thenReturn(List.of(POWER_NAME));
        authService.getRolePowerNames();
        session.setAttribute("UserPowerVersion", 1L);
        session.setAttribute("ActiveCheckedAt", System.currentTimeMillis());
        User nextUser = new User();
        nextUser.setId(106);
        nextUser.setName("next-user");

        authService.login(nextUser);

        assertNull(session.getAttribute("UserPowerList"));
        assertNull(session.getAttribute("UserPowerVersion"));
        assertNull(session.getAttribute("ActiveCheckedAt"));
    }

    private void pauseFirstQuery(AtomicReference<List<String>> permissions,
                                 CountDownLatch oldPermissionsRead, CountDownLatch allowReturn) {
        when(userRepository.findPowerNamesByUserId(USER_ID)).thenAnswer(invocation -> {
            List<String> snapshot = new ArrayList<>(permissions.get());
            if (oldPermissionsRead.getCount() > 0) {
                oldPermissionsRead.countDown();
                await(allowReturn);
            }
            return snapshot;
        });
    }

    private Future<List<String>> submitRequest(ExecutorService executor, MockHttpSession requestSession,
                                               Callable<List<String>> action) {
        return executor.submit(() -> {
            bindRequest(requestSession, USER);
            try {
                return action.call();
            } finally {
                clearRequest();
            }
        });
    }

    private static MockHttpServletRequest bindRequest(MockHttpSession requestSession, AppBoxUser user) {
        MockHttpServletRequest request = new MockHttpServletRequest();
        if (requestSession != null) {
            request.setSession(requestSession);
        }
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request, new MockHttpServletResponse()));
        SecurityContextHolder.clearContext();
        if (user != null) {
            SecurityContextHolder.getContext().setAuthentication(
                    UsernamePasswordAuthenticationToken.authenticated(user, null, List.of()));
        }
        return request;
    }

    private static void await(CountDownLatch latch) {
        try {
            assertTrue(latch.await(5, TimeUnit.SECONDS), "等待并发测试的指定阶段超时");
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("并发测试被中断", exception);
        }
    }

    private static void stop(ExecutorService executor) throws InterruptedException {
        executor.shutdownNow();
        assertTrue(executor.awaitTermination(5, TimeUnit.SECONDS), "测试线程未在限定时间内结束");
    }
}
