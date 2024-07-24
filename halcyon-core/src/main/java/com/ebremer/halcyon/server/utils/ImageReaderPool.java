package com.ebremer.halcyon.server.utils;

import com.ebremer.halcyon.filereaders.ImageReader;
import java.net.URI;
import java.time.Duration;
import org.apache.commons.pool2.impl.GenericKeyedObjectPool;
import org.apache.commons.pool2.impl.GenericKeyedObjectPoolConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author erich
 */
public class ImageReaderPool extends GenericKeyedObjectPool<URI, ImageReader> {
    
    private static final Logger logger = LoggerFactory.getLogger(ImageReaderPool.class);
    private static ImageReaderPool pool;

    private ImageReaderPool(ImageReaderPoolFactory<URI, ImageReader> factory, GenericKeyedObjectPoolConfig<ImageReader> config) {
        super(factory, config);
    }
            
    @Override
    public ImageReader borrowObject(final URI key) throws Exception {
        logger.debug("borrow: "+key);
        return super.borrowObject(key);
    }
    
    @Override
    public void returnObject(final URI key, final ImageReader obj) {
        logger.debug("return: "+key);
        super.returnObject(key, obj);
    }
    
    public static synchronized ImageReaderPool getPool() {
        GenericKeyedObjectPoolConfig<ImageReader> config = new GenericKeyedObjectPoolConfig<>();
        config.setMaxTotalPerKey(Runtime.getRuntime().availableProcessors());
        config.setMinIdlePerKey(0);
        //config.setMaxWait(Duration.ofMillis(60000));
        config.setBlockWhenExhausted(true);
        config.setMinEvictableIdleDuration(Duration.ofMillis(60000));
        config.setTimeBetweenEvictionRuns(Duration.ofMillis(60000));
        if (pool==null) {
            pool = new ImageReaderPool(new ImageReaderPoolFactory(), config);
        }
        return pool;
    }
}
