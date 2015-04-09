package eu.leads.processor.common.utils;

import eu.leads.processor.common.infinispan.CacheManagerFactory;
import eu.leads.processor.common.infinispan.InfinispanManager;
import eu.leads.processor.conf.LQPConfiguration;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by vagvaz on 4/9/15.
 */
public class NutchTestBootStrap {
   static int numberOfCacheManagers = 4;
   public static void main(String[] args) throws IOException {
      List<InfinispanManager> managers = new ArrayList<>();
      LQPConfiguration.initialize();
      for (int index = 0; index < numberOfCacheManagers; index++) {
         managers.add(CacheManagerFactory.createCacheManager());
      }

      System.out.println("Press any key to stop");
      System.in.read();
      for(InfinispanManager manager : managers){
         manager.stopManager();
      }

      System.exit(0);
   }
}
