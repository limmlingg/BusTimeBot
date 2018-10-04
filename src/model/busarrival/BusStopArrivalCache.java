package model.busarrival;

import java.util.Date;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.logging.log4j.LogManager;

public class BusStopArrivalCache {
    public static final org.apache.logging.log4j.Logger logger = LogManager.getLogger(BusStopArrivalCache.class);
    public static final int CACHE_REFRESH_INTERVAL = 1000 * 30; //30 seconds in milliseconds before refreshing cache with new data
    public static ConcurrentHashMap<String, BusStopArrival> cache;

    public static void initialize() {
       cache = new ConcurrentHashMap<String, BusStopArrival>();
       //Background thread to clear cache
       Runnable r = new PeriodicCacheClearer(cache);
       new Thread(r).start();
    }

    public static boolean isOutdated(BusStopArrival busStopArrival) {
        return (System.currentTimeMillis() - busStopArrival.requestedTime) > BusStopArrivalCache.CACHE_REFRESH_INTERVAL;
    }
}

/**
 *  Periodic runnable to clear the cache every hour
 */
class PeriodicCacheClearer implements Runnable {
    public static final org.apache.logging.log4j.Logger logger = LogManager.getLogger(PeriodicCacheClearer.class);
    private static final long CACHE_CLEAR_INTERVAL = 1000 * 60 * 60 * 1; //1 hour in milliseconds before clearing the cache
    private ConcurrentHashMap<String, BusStopArrival> cache;

    public PeriodicCacheClearer(ConcurrentHashMap<String, BusStopArrival> cache) {
        this.cache = cache;
    }

    @Override
    public void run() {
        while (true) {
            try {
                logger.info("Time to clear cache at " + new Date());
                cache.clear();
                Thread.sleep(CACHE_CLEAR_INTERVAL);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }
}