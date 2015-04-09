package eu.leads.processor.common.utils;

import eu.leads.processor.common.infinispan.CacheManagerFactory;
import eu.leads.processor.common.infinispan.CustomKey;
import eu.leads.processor.common.infinispan.InfinispanClusterSingleton;
import eu.leads.processor.common.infinispan.InfinispanManager;
import eu.leads.processor.conf.LQPConfiguration;
import org.infinispan.Cache;
import org.infinispan.commons.api.BasicCache;

/**
 * Created by vagvaz on 3/30/15.
 */
public class CustomKeyTest {

   static BasicCache dataCache;

   static String[] nodes= {"node0"};//,"node1","node2","node3","node00","node11","node22","node33"};
   static String[] microClouds = {"mc0"};//,"mc1"};//,"mc2","mc01","mc11","mc21"};
   static String[] keys;
   static int numOfkeys = 2;
   static int valuesPerKey = 1000;
   
   public static void main(String[] args) {
      LQPConfiguration.initialize();
      InfinispanManager manager = InfinispanClusterSingleton.getInstance().getManager();
      InfinispanManager manager11 = CacheManagerFactory.createCacheManager();
      InfinispanManager manager12 = CacheManagerFactory.createCacheManager();
      InfinispanManager manager13 = CacheManagerFactory.createCacheManager();
      InfinispanManager manager14 = CacheManagerFactory.createCacheManager();
      Cache dataCache = (Cache) manager.getPersisentCache("testCache");


      keys = new String[numOfkeys];
      for (int index = 0; index < numOfkeys; index++) {
         keys[index] = "key"+index;
      }
      //generate intermediate keyValuePairs
      generateIntermKeyValue( dataCache, valuesPerKey, keys, nodes, microClouds);

      int counter = 0;

      for(String k : keys){
         int keyCounter = 0;
//         LeadsIntermediateIterator iterator = new LeadsIntermediateIterator(k,"prefix",InfinispanClusterSingleton.getInstance().getManager());
//         while(iterator.hasNext()){
//            System.out.println(keyCounter + ": "+ iterator.next().toString());
//            keyCounter++;
//            counter++;
//         }
//         System.err.println("key " + k + " " + keyCounter);
         for(String mc : microClouds){
            for(String node : nodes){
               for (int i = 0; i < 2*valuesPerKey; i++) {
                  CustomKey key = new CustomKey(mc,node,k,i);
                  Object o = dataCache.get(key);
                  if(o!=null){
                     counter++;
                  }
               }

            }
         }
      }
      System.err.println("Total counted " + counter + " total " + keys.length * microClouds.length * nodes.length * valuesPerKey);
      System.exit(0);
   }

   private static void generateIntermKeyValue( BasicCache dataCache,
                                              int valuesPerKey, String[] keys,
                                              String[] nodes, String[] microClouds) {
//    IndexedCustomKey indexedKey = new IndexedCustomKey();
//    CustomKey ikey = new CustomKey();
      for(String node : nodes){
         for(String site : microClouds) {
            for (String key : keys) {
               for (int counter = 0; counter < valuesPerKey; counter++) {
                  CustomKey ikey = new CustomKey();
                  ikey.setCounter(counter);
                  ikey.setKey(key);
                  ikey.setSite(site);
                  ikey.setNode(node);
                  dataCache.put(new CustomKey(ikey), new CustomKey(ikey));
               }
               System.out.println("Generated " + node + " " + site + " " + key);
            }
         }
      }

   }
}
