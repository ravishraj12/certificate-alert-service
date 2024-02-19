package com.wibmo.accosa2.utils;

import java.util.Properties;
import java.util.concurrent.CompletableFuture;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.diabolicallabs.vertx.cron.CronObservable;
import com.wibmo.accosa2.common.util.StringUtil;
import com.wibmo.accosa2.observer.FirstObserver;
import com.wibmo.accosa2.observer.SecondObserver;

import io.vertx.core.Future;
import io.vertx.core.Promise;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import io.vertx.rx.java.RxHelper;
import rx.Scheduler;

public class CertificateJobScheduler {

	private static Logger logger = LoggerFactory.getLogger(CertificateJobScheduler.class);

	public Future<Void> executeJob(Vertx vertx) {
		Promise<Void> promise = Promise.promise();

		try {

			Scheduler scheduler = RxHelper.scheduler(vertx);

			JsonObject config = CommonUtility.getAlertConfig();
			Properties properties = CommonUtility.properties;
			String timeZone = properties.getProperty("TIMEZONE");

			String cronExpression1 = config.getString("cronPattern1", "0 45 9 * * ?"); // Every 4 minutes
			String cronExpression2 = config.getString("cronPattern2", "0 0 * ? * * *"); // Every 1 minutes

			logger.info(
					"executeJob - First Job has been configured with pattern: {}, Second Job has been configured with pattern: {}",
					cronExpression1, cronExpression2);

			// Creating an observer instance
			FirstObserver observer1 = new FirstObserver(vertx);
			SecondObserver observer2 = new SecondObserver(vertx);

			// Scheduling the observer to run based on the cron expression
			CronObservable.cronspec(scheduler, cronExpression1, timeZone).subscribe(observer1);

			CronObservable.cronspec(scheduler, cronExpression2, timeZone).subscribe(observer2);

			CompletableFuture.supplyAsync(() -> {
				observer1.onNext(vertx);
				logger.info("Observer1 triggered");
				return StringUtil.EMPTY;
			}).thenApply(res -> {
				try {
					String threadSleepTime = CommonUtility.properties.getProperty("THREAD_SLEEP_TIME", "120000");
					Thread.sleep(Long.parseLong(threadSleepTime));
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
				logger.info("Observer2 triggered");
				observer2.onNext(vertx);
				return StringUtil.EMPTY;
			});

			promise.complete();
		} catch (Exception e) {
			logger.error("executeJobs - Exception occurred : {}", e);
			promise.fail(e);
		}
		return promise.future();
	}
}
