package com.naqiran.thalam.configuration;

import java.util.concurrent.atomic.AtomicBoolean;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.SmartLifecycle;
import org.springframework.core.Ordered;
import org.springframework.stereotype.Component;


/**
 * Auto Configurer for Thalam (Platform)
 * @author Nakkeeran Annamalai
 *
 */
@Component
public class AggregatorCoreAutoConfiguration implements SmartLifecycle, Ordered {
    
    private AtomicBoolean started = new AtomicBoolean();
    
    @Autowired
    private ServiceDictionaryBuilder builder;
    
    @Override
    public void start() {
        if (!started.get()) {
            builder.build();
            started.set(true);
        }
    }

    @Override
    public void stop() {
        started.set(false);
    }

    @Override
    public boolean isRunning() {
        return started.get();
    }

    @Override
    public int getOrder() {
        return Ordered.LOWEST_PRECEDENCE;
    }
}
