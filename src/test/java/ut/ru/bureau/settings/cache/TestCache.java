package ut.ru.bureau.settings.cache;

import com.atlassian.cache.Cache;
import com.atlassian.cache.CacheEntryListener;
import com.atlassian.cache.CacheLoader;
import com.atlassian.cache.Supplier;

import javax.annotation.Nonnull;
import java.util.Collection;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;

/**
 * In-memory implementation of Atlassian {@link Cache} for testing purposes.
 * <p>
 * Uses {@link ConcurrentHashMap} as backing store. When {@link #get(Object)} is called
 * and the key is not present, the {@link CacheLoader} provided at construction time
 * is invoked to load the value.
 */
public class TestCache<K, V> implements Cache<K, V> {

    private final String name;
    private final CacheLoader<K, V> cacheLoader;
    private final ConcurrentHashMap<K, V> map = new ConcurrentHashMap<>();

    public TestCache(String name, CacheLoader<K, V> cacheLoader) {
        this.name = name;
        this.cacheLoader = cacheLoader;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public boolean containsKey(K key) {
        return map.containsKey(key);
    }

    @Override
    public Collection<K> getKeys() {
        return map.keySet();
    }

    @Override
    public V get(@Nonnull K key) {
        V value = map.get(key);
        if (value == null && cacheLoader != null) {
            value = cacheLoader.load(key);
            if (value != null) {
                map.put(key, value);
            }
        }
        return value;
    }

    @Override
    public V get(K key, @Nonnull Supplier<? extends V> supplier) {
        return map.computeIfAbsent(key, k -> supplier.get());
    }

    @Override
    public Map<K, V> getBulk(Set<K> keys, Function<Set<K>, Map<K, V>> function) {
        Map<K, V> result = new java.util.HashMap<>();
        Set<K> missing = new java.util.LinkedHashSet<>();
        for (K key : keys) {
            V value = map.get(key);
            if (value != null) {
                result.put(key, value);
            } else {
                missing.add(key);
            }
        }
        if (!missing.isEmpty()) {
            Map<K, V> loaded = function.apply(missing);
            if (loaded != null) {
                map.putAll(loaded);
                result.putAll(loaded);
            }
        }
        return result;
    }

    @Override
    public void put(K key, V value) {
        map.put(key, value);
    }

    @Override
    public V putIfAbsent(K key, V value) {
        return map.putIfAbsent(key, value);
    }

    @Override
    public void remove(K key) {
        map.remove(key);
    }

    @Override
    public boolean remove(K key, V value) {
        return map.remove(key, value);
    }

    @Override
    public void removeAll() {
        map.clear();
    }

    @Override
    public boolean replace(K key, V oldValue, V newValue) {
        return map.replace(key, oldValue, newValue);
    }

    @Override
    public void addListener(CacheEntryListener<K, V> listener, boolean includeValues) {
        // listeners not supported in test implementation
    }

    @Override
    public void removeListener(CacheEntryListener<K, V> listener) {
        // listeners not supported in test implementation
    }
}