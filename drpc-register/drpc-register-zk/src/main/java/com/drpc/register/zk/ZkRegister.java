package com.drpc.register.zk;


import com.drpc.NamedThreadFactory;
import com.drpc.register.NotifyListener;
import com.drpc.register.OfflineListener;
import com.drpc.register.Register;
import com.drpc.register.RegisterMetaData;
import com.drpc.util.NetUtil;
import com.google.common.collect.Maps;
import io.netty.util.internal.ConcurrentSet;
import io.netty.util.internal.SystemPropertyUtil;
import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.CuratorFrameworkFactory;
import org.apache.curator.framework.api.BackgroundCallback;
import org.apache.curator.framework.api.CuratorEvent;
import org.apache.curator.framework.recipes.cache.PathChildrenCache;
import org.apache.curator.framework.state.ConnectionState;
import org.apache.curator.framework.state.ConnectionStateListener;
import org.apache.curator.retry.ExponentialBackoffRetry;
import org.apache.zookeeper.CreateMode;
import org.apache.zookeeper.KeeperException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collection;
import java.util.Map;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Created by daiyong
 * zookeeper   注册器实现
 */
public class ZkRegister implements Register {

	private static final Logger logger = LoggerFactory.getLogger("NamedThreadFactory");

	/**
	 * 使用队列注册服务
	 */
	private final LinkedBlockingQueue<RegisterMetaData> queue = new LinkedBlockingQueue<>();

	private final ExecutorService registerExecutor =
			Executors.newSingleThreadExecutor(new NamedThreadFactory("register.executor"));

	private final ScheduledExecutorService registerScheduledExecutor =
			Executors.newSingleThreadScheduledExecutor(new NamedThreadFactory("register.schedule.executor"));

	private final ExecutorService localRegisterWatchExecutor =
			Executors.newSingleThreadExecutor(new NamedThreadFactory("local.register.watch.executor"));

	private final AtomicBoolean shutdown = new AtomicBoolean(false);

	/**
	 *  一个服务  所有的订阅监听者
	 */
	private final ConcurrentMap<RegisterMetaData.ServiceMeta, CopyOnWriteArrayList<NotifyListener>> subscribeListeners =
			Maps.newConcurrentMap();

	/**
	 * 一个服务 所有的下线监听者
	 */
	private final ConcurrentMap<RegisterMetaData.Address, CopyOnWriteArrayList<OfflineListener>> offlineListeners =
			Maps.newConcurrentMap();

	// Consumer已订阅的信息
	private final ConcurrentSet<RegisterMetaData.ServiceMeta> subscribeSet = new ConcurrentSet<>();

	// Provider已发布的注册信息
	private final ConcurrentMap<RegisterMetaData, RegisterState> registerMetaMap = Maps.newConcurrentMap();

	// 没有实际意义, 不要在意它
	private static final AtomicLong sequence = new AtomicLong(0);

	/**
	 * 获取本地ip地址
	 */
	private final String address = SystemPropertyUtil.get("jupiter.local.address", NetUtil.getLocalAddress());

	/**
	 * zk 会话时间 60s
	 */
	private final int sessionTimeoutMs = 60 * 1000;
	/**
	 *  zk 链接时间15s
	 */
	private final int connectionTimeoutMs = 15 * 1000;

	private final ConcurrentMap<RegisterMetaData.ServiceMeta, PathChildrenCache> pathChildrenCaches = Maps.newConcurrentMap();
	// 指定节点都提供了哪些服务
	private final ConcurrentMap<RegisterMetaData.Address, ConcurrentSet<RegisterMetaData.ServiceMeta>> serviceMetaMap = Maps.newConcurrentMap();

	private CuratorFramework configClient;

	public ZkRegister() {
		registerExecutor.execute(new Runnable() {

			@Override
			public void run() {
				while (!shutdown.get()) {
					RegisterMetaData meta = null;
					try {
						meta = queue.take();
						registerMetaMap.put(meta, RegisterState.PREPARE);
						doRegister(meta);
					} catch (Throwable t) {
						if (meta != null) {
							logger.error("注册失败:{}", meta.getServiceMeta(), t);

							// 间隔一段时间再重新入队, 让出cpu 1s后重试
							final RegisterMetaData finalMeta = meta;
							registerScheduledExecutor.schedule(new Runnable() {

								@Override
								public void run() {
									queue.add(finalMeta);
								}
							}, 1, TimeUnit.SECONDS);
						}
					}
				}
			}
		});

		/**
		 * 定期检查注册情况
		 */
		localRegisterWatchExecutor.execute(new Runnable() {

			@Override
			public void run() {
				while (!shutdown.get()) {
					try {
						Thread.sleep(3000);
						doCheckRegisterNodeStatus();
					} catch (Throwable t) {
						if (logger.isWarnEnabled()) {
							logger.warn("Check register node status fail: {}, will try again...", t);
						}
					}
				}
			}
		});
	}

	/**
	 * 在zk上注册节点
	 * @param meta
	 */
	private void doRegister(final RegisterMetaData meta) {

		String directory = String.format("/drpc/provider/%s/%s/%s",
				meta.getGroup(),
				meta.getServiceProviderName(),
				meta.getVersion());

		try {
			if (configClient.checkExists().forPath(directory) == null) {
				configClient.create().creatingParentsIfNeeded().forPath(directory);
			}
		} catch (Exception e) {
			if (logger.isWarnEnabled()) {
				logger.warn("Create parent path failed, directory: {}, {}.", directory, e);
			}
		}

		try {
			meta.setHost(address);
			configClient.create().withMode(CreateMode.EPHEMERAL).inBackground(new BackgroundCallback() {
				@Override
				public void processResult(CuratorFramework client, CuratorEvent event) throws Exception {
					logger.info("节点创建成功 {}", meta);
					if (event.getResultCode() == KeeperException.Code.OK.intValue()) {
						registerMetaMap.put(meta, RegisterState.DONE);
					}
				}
			}).forPath(
					String.format("%s/%s:%s:%s",
							directory,
							meta.getHost(),
							String.valueOf(meta.getPort()),
							String.valueOf(meta.getConnCount())));
		} catch (Exception e) {
			if (logger.isWarnEnabled()) {
				logger.warn("创建节点失败 {}.", meta, e);
			}
		}
	}

	/**
	 * 链接注册中心
	 * @param url
	 */
	@Override
	public void connectRegisterCenter(String url) {
		configClient = CuratorFrameworkFactory.newClient(
				url, sessionTimeoutMs, connectionTimeoutMs, new ExponentialBackoffRetry(500, 20));

		configClient.getConnectionStateListenable().addListener(new ConnectionStateListener() {

			@Override
			public void stateChanged(CuratorFramework client, ConnectionState newState) {

				logger.info("Zookeeper connection state changed {}.", newState);

				/**
				 * 如果状态是重连 则重发服务
				 */
				if (newState == ConnectionState.RECONNECTED) {

					logger.info("Zookeeper connection has been re-established, will re-subscribe and re-register.");

					// 重新订阅
					for (RegisterMetaData.ServiceMeta serviceMeta : getSubscribeSet()) {
						doSubscribe(serviceMeta);
					}

					// 重新发布服务
					for (RegisterMetaData meta : getRegisterMetaMap().keySet()) {
						register(meta);
					}
				}
			}
		});

		configClient.start();
	}

	@Override
	public void register(RegisterMetaData meta) {
		queue.add(meta);
	}

	@Override
	public void unregister(RegisterMetaData meta) {

	}

	@Override
	public void subscribe(RegisterMetaData.ServiceMeta serviceMeta, NotifyListener listener) {

	}

	@Override
	public Collection<RegisterMetaData> lookup(RegisterMetaData.ServiceMeta serviceMeta) {
		return null;
	}

	@Override
	public Map<RegisterMetaData.ServiceMeta, Integer> consumers() {
		return null;
	}

	@Override
	public Map<RegisterMetaData, RegisterState> providers() {
		return null;
	}

	@Override
	public boolean isShutdown() {
		return false;
	}

	@Override
	public void shutdownGracefully() {

	}


}
