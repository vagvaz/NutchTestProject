package eu.leads.processor.common.infinispan;

import eu.leads.processor.common.StringConstants;
import eu.leads.processor.common.plugins.PluginPackage;
import eu.leads.processor.common.utils.FSUtilities;
import eu.leads.processor.common.utils.storage.LeadsStorage;
import eu.leads.processor.common.utils.storage.LeadsStorageFactory;
import eu.leads.processor.plugins.EventType;
import eu.leads.processor.plugins.PluginInterface;
import org.apache.commons.configuration.ConfigurationException;
import org.apache.commons.configuration.XMLConfiguration;
import org.infinispan.Cache;
import org.infinispan.configuration.parsing.ConfigurationBuilderHolder;
import org.infinispan.configuration.parsing.ParserRegistry;
import org.infinispan.manager.DefaultCacheManager;
import org.infinispan.manager.EmbeddedCacheManager;
import org.infinispan.metadata.Metadata;
import org.infinispan.notifications.cachelistener.filter.CacheEventFilter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.vertx.java.core.json.JsonArray;
import org.vertx.java.core.json.JsonObject;

import java.io.*;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.*;

import static eu.leads.processor.plugins.EventType.*;

/**
 * Created by vagvaz on 9/29/14.
 */
public class PluginRunnerFilter implements CacheEventFilter,Serializable {

  transient private JsonObject conf;
  private String configString;
  private String managerAddress ;
  transient private EmbeddedCacheManager manager;
  transient private ClusterInfinispanManager imanager;
  transient private Cache pluginsCache;
  transient private  Cache targetCache;
  transient private String targetCacheName;

  transient private Logger log = LoggerFactory.getLogger(PluginRunnerFilter.class) ;
  transient private PluginInterface plugin;
  transient private String pluginsCacheName;
  transient private String pluginName;
  transient private List<EventType> type;
  transient private boolean isInitialized = false;
  transient private LeadsStorage storageLayer = null;
  transient private String user;
  public PluginRunnerFilter(EmbeddedCacheManager manager,String confString){
//    log = LoggerFactory.getLogger(manager.getAddress().toString()+":"+PluginRunnerFilter.class.toString());
//    log.error("Manager init");
//    this.manager = manager;
//    log.error("set config string");
    this.configString = confString;
//    log.error("initJson conf");
//    this.conf = new JsonObject(configString);
//    log.error("init Imanager");
//    imanager = new ClusterInfinispanManager(InfinispanClusterSingleton.getInstance().getManager().getCacheManager());
//    managerAddress = manager.getAddress().toString();
//    log.error("init");
    System.err.println("Construct");

    //initialize();
  }

//  private void writeObject(ObjectOutputStream o)
//          throws IOException {
//    o.defaultWriteObject();
//    o.writeObject(configString);
//    System.err.println("Serialize");
//  //  o.writeObject(managerAddress);
//
//
//  }
//
//  private void readObject(ObjectInputStream i)
//          throws IOException, ClassNotFoundException {
//    i.defaultReadObject();
//    System.err.println("DeSerialize");
//
//    configString = (String) i.readObject();
//    //managerAddress= (String) i.readObject()+"Deserialized";
//
//    //this.conf = new JsonObject(configString);
//  //  log = LoggerFactory.getLogger(PluginRunnerFilter.class);
//   // imanager = new ClusterInfinispanManager(InfinispanClusterSingleton.getInstance().getManager().getCacheManager());
//
////    ParserRegistry registry = new ParserRegistry();
////    ConfigurationBuilderHolder holder  = registry.parseFile(StringConstants.ISPN_CLUSTER_FILE);
////    manager = new DefaultCacheManager(holder, true);
//  //  manager = InfinispanClusterSingleton.getInstance().getManager().getCacheManager();
//  //  initialize();
//  }

  private void initialize() {
    System.err.println("Initilize");

    isInitialized = true;

    this.manager = InfinispanClusterSingleton.getInstance().getManager().getCacheManager();
    this.conf = new JsonObject(configString);
    imanager = new ClusterInfinispanManager(manager);

    log.error("get activePluginCache");
    pluginsCacheName = conf.getString("activePluginCache");//StringConstants.PLUGIN_ACTIVE_CACHE);

    log.error("get pluginName");
    pluginName = conf.getString("pluginName");
    log.error("get user");
    user = conf.getString("user");
    log.error("get types");
    JsonArray types = conf.getArray("types");
    //InferTypes

    type = new ArrayList<EventType>(3);
    if(types != null ) {
      Iterator<Object> iterator = types.iterator();
      if (iterator.hasNext()) {
        log.error("READ EVent type ");
        type.add( EventType.valueOf(iterator.next().toString()));
      }
    }

    if(type.size() == 0){
      type.add(CREATED);
      type.add(REMOVED);
      type.add(MODIFIED);
    }

    log.error("init pluginscache");
    pluginsCache = (Cache) imanager.getPersisentCache(pluginsCacheName);
    log.error("init logger");
    log = LoggerFactory.getLogger(managerAddress+ " PluginRunner."+pluginName+":"+
                                    pluginsCacheName);
    log.error("init storage");
    String storagetype = this.conf.getString("storageType");
    Properties storageConfiguration = new Properties();
    byte[] storageConfBytes =  this.conf.getBinary("storageConfiguration");
    ByteArrayInputStream bais = new ByteArrayInputStream(storageConfBytes);
    try {
      storageConfiguration.load(bais);
      storageLayer = LeadsStorageFactory.getInitializedStorage(storagetype,storageConfiguration);
    } catch (IOException e) {
      e.printStackTrace();
    }
    log.error("init targetCacheName");
    targetCacheName = conf.getString("targetCache");
    log.error("init Targetcache");
    targetCache = (Cache) imanager.getPersisentCache(targetCacheName);
    log.error("init plugin");
    initializePlugin(pluginsCache, pluginName, user);
    log.error("Initialized plugin " + pluginName + " on " + targetCacheName);
    System.err.println("Initialized plugin " + pluginName + " on " + targetCacheName);
  }


  private void initializePlugin(Cache cache, String plugName, String user) {

    log.error("INITPLUG:" + targetCacheName + ":" + plugName + user);
    PluginPackage pluginPackage = (PluginPackage) cache.get(targetCacheName+":"+plugName+user);
    log.error("INITPLUG:" + (pluginPackage == null));
    String tmpdir = System.getProperties().getProperty("java.io.tmpdir")+"/"+StringConstants
                                                                          .TMPPREFIX+"/runningPlugins/"+ UUID
                                                                                                     .randomUUID()
                                                                                    .toString()+"/";
    log.error("using tmpdir " + tmpdir);
    String  jarFileName = tmpdir+pluginPackage.getClassName()+".jar";
    log.error("Download... " + "plugins/" + plugName + " -> " + jarFileName);
    storageLayer.download("plugins/"+plugName,jarFileName);
    ClassLoader classLoader = null;
    File file = new File(jarFileName);
    try {
      classLoader =
        new URLClassLoader(new URL[] {file.toURI().toURL()}, PluginRunnerFilter.class.getClassLoader());
    } catch (MalformedURLException e) {
      log.error("exception " + e.getClass().toString());
      log.error("exception " + e.getMessage());
      e.printStackTrace();
    }


    //    ConfigurationUtilities.addToClassPath(jarFileName);
//      .addToClassPath(System.getProperty("java.io.tmpdir") + "/leads/plugins/" + plugName
//                        + ".jar");

//    byte[] config = (byte[]) cache.get(plugName + ":conf");

    byte[] config = pluginPackage.getConfig();
    log.error("Flush config to dist " + tmpdir + plugName + "-conf.xml " + config.length);
    FSUtilities.flushToTmpDisk(tmpdir + plugName + "-conf.xml", config);
    log.error("read config");
    XMLConfiguration pluginConfig = null;
    try {
      pluginConfig =
        new XMLConfiguration(tmpdir + plugName + "-conf.xml");
    } catch (ConfigurationException e) {
      log.error("exception " + e.getClass().toString());
      log.error("exception " + e.getMessage());
      e.printStackTrace();
    }
//    String className = (String) cache.get(plugName + ":className");
    String className = pluginPackage.getClassName();
    log.error("Init plugClass");
    if (className != null && !className.equals("")) {
      try {
        Class<?> plugClass =
          Class.forName(className, true, classLoader);
        Constructor<?> con = plugClass.getConstructor();
        log.error("get plugin new Instance");
        plugin = (PluginInterface) con.newInstance();
        log.error("initialize plugin ");
        plugin.initialize(pluginConfig, imanager);

      } catch (ClassNotFoundException e) {
        log.error("exception " + e.getClass().toString());
        log.error("exception " + e.getMessage());
        e.printStackTrace();
      } catch (NoSuchMethodException e) {
        log.error("exception " + e.getClass().toString());
        log.error("exception " + e.getMessage());
        e.printStackTrace();
      } catch (InvocationTargetException e) {
        log.error("exception " + e.getClass().toString());
        log.error("exception " + e.getMessage());
        e.printStackTrace();
      } catch (InstantiationException e) {
        log.error("exception " + e.getClass().toString());
        log.error("exception " + e.getMessage());
        e.printStackTrace();
      } catch (IllegalAccessException e) {
        log.error("exception " + e.getClass().toString());
        log.error("exception " + e.getMessage());
        e.printStackTrace();
      }
      catch (Exception e){
        log.error("exception " + e.getClass().toString());
        log.error("exception " + e.getMessage());
      }
    } else {
      log.error("Could not find the name for " + plugName);
    }
  }


  @Override
  public boolean accept(Object key, Object oldValue, Metadata oldMetadata, Object newValue,
                         Metadata newMetadata,
                         org.infinispan.notifications.cachelistener.filter.EventType eventType) {
    if(!isInitialized)
      initialize();
    String o1 = (String)key;
    String value = (String)newValue;
    switch (eventType.getType()) {
      case CACHE_ENTRY_CREATED:
        if(type.contains(CREATED))
          plugin.created(key, value, targetCache);
        break;
      case CACHE_ENTRY_REMOVED:
        if(type.contains(REMOVED))
          plugin.removed(key, value, targetCache);
        break;
      case CACHE_ENTRY_MODIFIED:
        if(type.contains(MODIFIED))
          plugin.modified(key, value, targetCache);
        break;
      default:
        break;
    }
    return false;
  }
}
