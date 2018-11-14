package com.naqiran.thalam.service;


/**
 * Metrics Collector for Thalam (Platform)
 * @author Nakkeeran Annamalai
 * 
 */
public interface ServiceAggregatorMetrics {
    public void countEvent(final String eventName);
    public void gaugeEvent();
    
    public static class DefaultServiceMetrics implements ServiceAggregatorMetrics {
        
        
        @Override
        public void countEvent(String eventName) {
        }

        @Override
        public void gaugeEvent() {
            // TODO (@ Nakkeeran Annamalai) Write the Metrics Collector
        }
        
    }
    
}
