package com.naqiran.thalam.service;

import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Metrics Collector for Thalam (Platform)
 * @author Nakkeeran Annamalai
 * 
 */
public interface MetricsService {

    double countEvent(final String eventName, final String... tags);
    void gaugeEvent(String name, double number, String... tags);
    
    class DefaultMetricsService implements MetricsService {

        @Autowired
        private MeterRegistry registry;

        @Override
        public double countEvent(final String eventName, final String... tags) {
            return registry.counter(eventName, tags).count();
        }

        @Override
        public void gaugeEvent(final String name, final double number, final String... tags) {
            registry.gauge(name, number);
        }
    }
}
