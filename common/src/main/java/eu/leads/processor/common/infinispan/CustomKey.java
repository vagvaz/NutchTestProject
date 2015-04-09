package eu.leads.processor.common.infinispan;

import java.io.Serializable;

/**
 * Created by vagvaz on 3/30/15.
 */
public class CustomKey implements Comparable,Serializable {
   private String site;
   private String node;
   private String key;
   private Integer counter;
   private static final long  serialVersionUID = -81923791823178123L;
   public CustomKey(String site, String node) {
      this.site = site;
      this.node = node;
   }

   public CustomKey(String site, String node,String key, Integer counter) {
      this.site = site;
      this.node = node;
      this.key = key;
      this.counter = counter;
   }
   public CustomKey(CustomKey other){
      this(other.getSite(),other.getNode(),other.getKey(),other.getCounter());
   }

   public CustomKey() {
      site = "";
      node = "";
      key = "";
      counter = -1;
   }


   public String getSite() {
      return site;
   }

   public void setSite(String site) {
      this.site = site;
   }

   public String getNode() {
      return node;
   }

   public void setNode(String node) {
      this.node = node;
   }

   public String getKey() {
      return key;
   }

   public void setKey(String key) {
      this.key = key;
   }

   public Integer getCounter() {
      return counter;
   }

   public void setCounter(Integer counter) {
      this.counter = new Integer(counter);
   }

   @Override
   public int hashCode() {
      return key.hashCode();//site.hashCode()+node.hashCode()+key.hashCode()+counter.hashCode();
   }

   @Override
   public boolean equals(Object o) {
      if (o == null || getClass() != o.getClass()) return false;
      CustomKey that = (CustomKey) o;

      if (site != null ? !site.equals(that.site) : that.site != null) return false;
      if (node != null ? !node.equals(that.node) : that.node != null) return false;
      if (key != null ? !key.equals(that.key) : that.key != null) return false;
      return !(counter != null ? !counter.equals(that.counter) : that.counter != null);
//      if(site.equals(that.getSite()))
//         if(node.equals(that.getNode()))
//            if(key.equals(that.getKey()))
//               if(counter.equals(that.getCounter()))
//                  return true;
//      return false;

   }

   @Override
   public int compareTo(Object o) {
      if (o == null || getClass() != o.getClass()) return -1;

      CustomKey that = (CustomKey) o;
      int result = -1;
      if (site != null){
         result = site.compareTo(that.site);
         if(result != 0)
            return result;
      }
      else{
         return -1;
      }

      if (node != null){
         result = node.compareTo(that.node);
         if(result != 0)
            return result;
      }
      else{
         return -1;
      }
      if (key != null )
      {
         result = key.compareTo(that.key);
         if(result != 0)
            return result;
      }
      else{
         return -1;
      }
      if(counter != null){
         return counter.compareTo(that.counter);
      }
      return -1;
//      return o.toString().compareTo(this.toString());
   }

   public void next() {
      counter = new Integer(counter+1);
   }

   @Override public String toString() {
      return site+"--"+node+"--"+key+"--"+counter;
   }
}
