package ut.ru.bureau.settings.cache;

import com.atlassian.cache.Cache;
import com.atlassian.cache.CacheLoader;
import com.atlassian.cache.CacheManager;
import com.atlassian.cache.CacheSettings;
import com.atlassian.cache.CachedReference;
import com.atlassian.cache.ManagedCache;
import com.atlassian.cache.Supplier;

import javax.annotation.Nonnull;
import java.util.Collection;
import java.util.Collections;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * In-memory implementation of Atlassian {@link CacheManager} for testing purposes.
 * <p>
 * Creates and manages {@link TestCache} instances. Each call to
 * {@link #getCache(String, CacheLoader, CacheSettings)} with the same name
 * returns the same {@link TestCache} instance.
 */
public class TestCacheManager implements CacheManager {

    private final ConcurrentMap<String, TestCache<?, ?>> caches = new ConcurrentHashMap<>();

    @Override
    @SuppressWarnings("unchecked")
    public <K, V> Cache<K, V> getCache(String name) {
        return (Cache<K, V>) caches.computeIfAbsent(name, n -> new TestCache<>(n, null));
    }

    @Override
    @SuppressWarnings("unchecked")
    public <K, V> Cache<K, V> getCache(Class<?> owningClass, String name) {
        return getCache(owningClass.getName() + "." + name);
    }

    @Override
    @SuppressWarnings("unchecked")
    public <K, V> Cache<K, V> getCache(String name, CacheLoader<K, V> cacheLoader) {
        return (Cache<K, V>) caches.computeIfAbsent(name, n -> new TestCache<>(n, cacheLoader));
    }

    @Override
    @SuppressWarnings("unchecked")
    public <K, V> Cache<K, V> getCache(String name, CacheLoader<K, V> cacheLoader, CacheSettings settings) {
        return (Cache<K, V>) caches.computeIfAbsent(name, n -> new TestCache<>(n, cacheLoader));
    }

    @Override
    @SuppressWarnings("unchecked")
    public <K, V> Cache<K, V> getCache(String name, Class<K> keyClass, Class<V> valueClass) {
        return (Cache<K, V>) caches.computeIfAbsent(name, n -> new TestCache<>(n, null));
    }

    @Override
    public <V> CachedReference<V> getCachedReference(String name, Supplier<V> supplier) {
        throw new UnsupportedOperationException("CachedReference not supported in TestCacheManager");
    }

    @Override
    public <V> CachedReference<V> getCachedReference(String name, Supplier<V> supplier, CacheSettings settings) {
        throw new UnsupportedOperationException("CachedReference not supported in TestCacheManager");
    }

    @Override
    public <V> CachedReference<V> getCachedReference(Class<?> owningClass, String name, Supplier<V> supplier) {
        throw new UnsupportedOperationException("CachedReference not supported in TestCacheManager");
    }

    @Override
    public <V> CachedReference<V> getCachedReference(Class<?> owningClass, String name, Supplier<V> supplier, CacheSettings settings) {
        throw new UnsupportedOperationException("CachedReference not supported in TestCacheManager");
    }

    @Override
    public Collection<Cache<?, ?>> getCaches() {
        return Collections.unmodifiableCollection(caches.values());
    }

    @Override
    public Collection<ManagedCache> getManagedCaches() {
        return Collections.emptyList();
    }

    @Override
    public void flushCaches() {
        caches.values().forEach(TestCache::removeAll);
    }

    @Override
    public ManagedCache getManagedCache(String name) {
        return null;
    }

    @Override
    public void shutdown() {
        caches.clear();
    }

    @SuppressWarnings("unchecked")
    public <K, V> TestCache<K, V> getTestCache(@Nonnull String name) {
        return (TestCache<K, V>) caches.get(name);
    }
}