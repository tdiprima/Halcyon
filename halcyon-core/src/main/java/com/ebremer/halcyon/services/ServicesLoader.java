package com.ebremer.halcyon.services;

import java.util.ArrayList;
import java.util.List;
import java.util.ServiceLoader;


public class ServicesLoader {
    private ServiceLoader<Service> serviceLoader = null;
    private final List<Service> plugins = new ArrayList<>();
    private static ServicesLoader servicesloader = null;

    private ServicesLoader() {
        System.out.println("Starting Service Loader...");
        serviceLoader = ServiceLoader.load(Service.class);
        for (Service plugin : serviceLoader) {
            System.out.println("Starting Service Loader...Adding --> "+plugin.getName());
            plugins.add(plugin);
        }
    }

    public List<Service> getPlugins() {
        return plugins;
    }
    
    public static void init() {
        if (servicesloader==null) {
            servicesloader = new ServicesLoader();
        }
    }
    
    public static ServicesLoader getInstance() {
        init();
        return servicesloader;
    }
}
