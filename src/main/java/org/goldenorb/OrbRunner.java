/**
 * Licensed to Ravel, Inc. under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  Ravel, Inc. licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 * 
 *     http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * 
 */
package org.goldenorb;

import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Map;
import java.util.Properties;

import org.apache.zookeeper.CreateMode;
import org.apache.zookeeper.ZooKeeper;
import org.goldenorb.conf.OrbConfiguration;
import org.goldenorb.zookeeper.ZookeeperUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This class provides a basis upon which to run a Job given a specified OrbConfiguration. It should be
 * extended to actually run a Job within GoldenOrb.
 * 
 */
public class OrbRunner {
  
  private static Logger logger;
  protected static ZooKeeper ZK;

/**
   * Constructs an OrbRunner object.
   */
  public OrbRunner() {
    logger = LoggerFactory.getLogger(OrbRunner.class);
  }
  
  /**
   * This is the runJob method that begins a Job. It first attempts to connect to the ZooKeeper server as
   * defined in the OrbConfiguration, then tries to create the /GoldenOrb, /GoldenOrb/ClusterName, and
   * /GoldenOrb/ClusterName/JobQueue nodes if they do not already exist. Then, it creates the Job itself as a
   * PERSISTENT_SEQUENTIAL node in the form of JobXXXXXXXXXX.
   * 
   * @param orbConf
   *          - The OrbConfiguration for a specific Job.
   * @exception - IOException
   * @exception - InterruptedException
   * @return jobNumber
   */
  public String runJob(OrbConfiguration orbConf) {
    String jobNumber = null;
    try {
      try {
        ZK = ZookeeperUtils.connect(orbConf.getOrbZooKeeperQuorum());
      } catch (IOException e) {
        logger.info("Failed to connect to zookeeper on " + orbConf.getOrbZooKeeperQuorum());
        logger.error("IOException", e);
      } catch (InterruptedException e) {
        logger.info("Failed to connect to zookeeper on " + orbConf.getOrbZooKeeperQuorum());
        logger.error("InterruptedException", e);
      }
      
      // create JobQueue path "/GoldenOrb/<cluster name>/JobQueue" if it doesn't already exist
      ZookeeperUtils.notExistCreateNode(ZK, "/GoldenOrb", CreateMode.PERSISTENT);
      ZookeeperUtils.notExistCreateNode(ZK, "/GoldenOrb/" + orbConf.getOrbClusterName(),
        CreateMode.PERSISTENT);
      ZookeeperUtils.notExistCreateNode(ZK, "/GoldenOrb/" + orbConf.getOrbClusterName() + "/JobQueue",
        CreateMode.PERSISTENT);
      
      // create the sequential Job using orbConf
      jobNumber = ZookeeperUtils.notExistCreateNode(ZK, "/GoldenOrb/" + orbConf.getOrbClusterName()
                                                        + "/JobQueue/Job", orbConf,
        CreateMode.PERSISTENT_SEQUENTIAL);
      
    } catch (Exception e) {
      logger.info("Cluster does not exist in ZooKeeper on " + orbConf.getOrbZooKeeperQuorum());
      logger.error("Exception", e);
    }
    
	writeLogToDisk(orbConf, System.getProperty("HOME"));
    return jobNumber;
  }

	public OrbConfiguration getConf(boolean b) {
		// TODO Auto-generated method stub
		return null;
	}

	protected void parseArgs(OrbConfiguration orbConf, String[] args, String algorithmName) {
		// TODO Auto-generated method stub
		try {
			for (int i = 0; i < args.length; i++) {
				String arg = args[i];
				if (arg.startsWith("-D")) { 
					String currentJavaOpts = orbConf.get("goldenOrb.orb.partition.javaopts");
					String newJavaOpts = arg.substring(2);
					orbConf.set("goldenOrb.orb.partition.javaopts", currentJavaOpts + " " + newJavaOpts);
				} else if (arg.contains(".")) {
					String[] keyVal = arg.substring(2).split("=");
					orbConf.set(keyVal[0], keyVal[1]);
				} else {
					String argKey = algorithmName+"."+arg.substring(1);
					String argValue = args[++i];
					orbConf.set(argKey, argValue);
				}
			}
		} catch (Exception e) {
			try {
				throw new IllegalArgumentException();
			} catch (Exception e1) {}
		}
	}
	
	private void writeLogToDisk(OrbConfiguration orbConf, String logLocation) {
		String _logLocation = logLocation;
		try {
			System.getProperties().storeToXML(new FileOutputStream(_logLocation + "sys.xml"), "System properties available to golden orb");

			Properties envProp = new Properties();
			Map<String, String> getenv = System.getenv();
			for (String key : getenv.keySet())
				envProp.put(key, getenv.get(key));
			envProp.storeToXML(new FileOutputStream(_logLocation + "env.xml"), "Environment variables available to golden orb.");

			orbConf.writeXml(new FileOutputStream(_logLocation + "orb.xml"));
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
}
